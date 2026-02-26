#include <android/log.h>
#include <jni.h>
#include <oboe/Oboe.h>
#include <algorithm>
#include <atomic>
#include <cmath>
#include <memory>
#include <vector>
#include <unistd.h>
#include <array>
#include <thread>

#define LOG_TAG "OboeAudioEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

constexpr int32_t NATIVE_TICK_TYPE_STRONG = 1;
constexpr int32_t NATIVE_TICK_TYPE_NORMAL = 2;
constexpr int32_t NATIVE_TICK_TYPE_SUB = 3;

constexpr int32_t kNumVoices = 10;

class OboeAudioEngine: public oboe::AudioStreamCallback {
 public:
  OboeAudioEngine() {
    reset();

    mMasterVolume.store(1.0f);
    mDuckingVolume.store(1.0f);
    mMuted.store(false);

    mSampleRate = 48000;  // default; overwritten after openStream()

    // initialize empty buffers
    std::atomic_store(
        &mTickStrongPtr, std::make_shared<std::vector<float>>());
    std::atomic_store(
        &mTickNormalPtr, std::make_shared<std::vector<float>>());
    std::atomic_store(
        &mTickSubPtr, std::make_shared<std::vector<float>>());
  }

  ~OboeAudioEngine() override {
    if (mStream) {
      mStream->stop();
      mStream->close();
      mStream.reset();
    }
  }

  bool init(bool avoidInitialFade) {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(1)
        ->setSampleRate(mSampleRate)
        ->setDataCallback(this)
        ->setErrorCallback(this)
        ->setUsage(oboe::Usage::Game)
        ->setContentType(oboe::ContentType::Sonification);

    oboe::Result result = builder.openStream(mStream);

    if (result != oboe::Result::OK) {
      // Bluetooth or other device that doesn't support exclusive mode
      builder.setSharingMode(oboe::SharingMode::Shared);
      result = builder.openStream(mStream);
    }

    if (result != oboe::Result::OK) {
      LOGE("Failed to open Oboe stream: %s", oboe::convertToText(result));
      return false;
    }

    // use real sample rate reported by stream
    mSampleRate = mStream->getSampleRate();

    // quick start/stop to avoid initial fade-in
    if (avoidInitialFade) {
      bool success = start();
      if (success) {
        usleep(200 * 1000);
        stop();
      }
    }

    return true;
  }

  bool start() {
    if (!mStream) {
      LOGE("Stream was null, attempting to re-initialize");
      if (!init(false)) return false;
    }

    constexpr int64_t kTimeoutNanos = 1 * 1000 * 1000 * 1000; // 1 second
    oboe::Result result = mStream->start(kTimeoutNanos);
    if (result != oboe::Result::OK) {
      LOGE("Failed to start stream (Error: %s). Attempting recovery...",
          oboe::convertToText(result));

      mStream->close();
      mStream.reset();

      if (init(true)) {
        result = mStream->start(kTimeoutNanos);
        if (result == oboe::Result::OK) {
          LOGE("Stream recovered successfully.");
          mIsPlaying = true;
          return true;
        }
      }

      LOGE("Recovery failed: %s", oboe::convertToText(result));
      return false;
    }
    mIsPlaying = true;
    return true;
  }

  bool stop() {
    if (!mStream) return false;

    constexpr int64_t kTimeoutNanos = 1 * 1000 * 1000 * 1000;  // 1 second
    oboe::Result result = mStream->pause(kTimeoutNanos);

    if (result != oboe::Result::OK) {
      LOGE("Failed to pause Oboe stream: %s",
          oboe::convertToText(result));
      return false;
    }
    mStream->requestFlush();
    mResetRequested.store(true);

    // clear pending voices
    for (int i = 0; i < kNumVoices; ++i) {
      mTickToPlay[i].store(-1);
      mPendingTickType[i].store(-1);
    }
    mNextVoiceToSteal.store(0);

    mIsPlaying = false;
    return true;
  }

  // realtime audio callback
  oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream,
      void *audioData,
      int32_t numFrames) override {
    auto *outputBuffer = static_cast<float *>(audioData);

    if (mResetRequested.exchange(false)) {
      for (int i = 0; i < kNumVoices; ++i) {
        mReadIndexLocal[i] = 0;
        mPrevLocalTickToPlay[i] = -1;
      }
    }

    // calculate gain
    float masterVol = mMasterVolume.load();
    float duckingVol = mDuckingVolume.load();
    bool muted = mMuted.load();
    float inputGain = muted ? 0.0f : (masterVol * duckingVol);

    int32_t localTickToPlay[kNumVoices];
    bool voiceJustChanged[kNumVoices]; // remember if voice assignment changed

    for (int v = 0; v < kNumVoices; ++v) {
      localTickToPlay[v] = mTickToPlay[v].load(std::memory_order_acquire);
      voiceJustChanged[v] = false;
    }

    // atomically load buffers once per callback
    auto strong = std::atomic_load(&mTickStrongPtr);
    auto normal = std::atomic_load(&mTickNormalPtr);
    auto sub = std::atomic_load(&mTickSubPtr);

    // Map voice -> buffer shared_ptr (snapshot)
    std::shared_ptr<std::vector<float>> sourceData[kNumVoices];

    for (int v = 0; v < kNumVoices; ++v) {
      // check if voice assignment changed
      if (mPrevLocalTickToPlay[v] != localTickToPlay[v]) {
        voiceJustChanged[v] = true;
        mPrevLocalTickToPlay[v] = localTickToPlay[v];
      }

      // unpack tick type (lower 4 bits)
      int32_t rawVal = localTickToPlay[v];
      int32_t tickType = (rawVal == -1) ? -1 : (rawVal & 0x0F);

      if (tickType == NATIVE_TICK_TYPE_STRONG) {
        sourceData[v] = strong;
      } else if (tickType == NATIVE_TICK_TYPE_NORMAL) {
        sourceData[v] = normal;
      } else if (tickType == NATIVE_TICK_TYPE_SUB) {
        sourceData[v] = sub;
      } else {
        sourceData[v] = nullptr;
      }

      // If assigned but not started, suppress output to avoid processing
      // with stale index or accidental cleanup
      if (voiceJustChanged[v] && localTickToPlay[v] != -1) {
        sourceData[v] = nullptr; // Wait for pending tick sync
        mReadIndexLocal[v] = 0;
      }
    }

    static float sLimiterGain = 1.0f;
    const float kTargetCeiling = 0.95f; // below 1.0 to avoid clipping
    const float kReleaseSpeed = 0.0005f; // how fast the gain recovers

    for (int i = 0; i < numFrames; ++i) {
      float drySample = 0.0f;

      // Check pending tick events before mixing for this sample
      for (int v = 0; v < kNumVoices; ++v) {
        int32_t pending = mPendingTickType[v].load(
            std::memory_order_acquire);
        if (pending != -1) {
          mPendingTickType[v].store(-1, std::memory_order_release);

          // Assign the tick type to the active voice (mTickToPlay already set
          // by playTick, but refresh localTickToPlay and sourceData for safety)
          localTickToPlay[v] = mTickToPlay[v].load(
              std::memory_order_relaxed);

          // unpack tick type (lower 4 bits)
          int32_t rawVal = localTickToPlay[v];
          int32_t tickType = (rawVal == -1) ? -1 : (rawVal & 0x0F);

          if (tickType == NATIVE_TICK_TYPE_STRONG) {
            sourceData[v] = strong;
          } else if (tickType == NATIVE_TICK_TYPE_NORMAL) {
            sourceData[v] = normal;
          } else if (tickType == NATIVE_TICK_TYPE_SUB) {
            sourceData[v] = sub;
          } else {
            sourceData[v] = nullptr;
          }

          // Start playing at the current sample
          mReadIndexLocal[v] = 0;
          // Set to raw value including sequence id
          mPrevLocalTickToPlay[v] = localTickToPlay[v];
        }
      }

      // Mix active voices for this sample
      for (int v = 0; v < kNumVoices; ++v) {
        auto &buf = sourceData[v];
        if (buf && mReadIndexLocal[v] < static_cast<int32_t>(buf->size())) {
          drySample += (*buf)[mReadIndexLocal[v]];
          ++mReadIndexLocal[v];
        } else {
          // Voice is inactive or finished
          // Only kill voice if we don't wait for a pending start
          if (localTickToPlay[v] != -1 && !voiceJustChanged[v]) {
            localTickToPlay[v] = -1;
            mReadIndexLocal[v] = 0;
            sourceData[v] = nullptr;
          }
        }
      }

      float amplified = drySample * inputGain;
      float absSample = std::fabs(amplified);

      // Calculate desired gain to avoid clipping
      float desiredGain = 1.0f;
      if (absSample > kTargetCeiling) {
        desiredGain = kTargetCeiling / absSample;
      }

      // Apply gain with instant attack and smooth release
      if (desiredGain < sLimiterGain) {
        sLimiterGain = desiredGain;
      } else {
        sLimiterGain += (1.0f - sLimiterGain) * kReleaseSpeed;
      }

      outputBuffer[i] = amplified * sLimiterGain;
    }

    // Publish voice active/inactive state back to atomics
    for (int v = 0; v < kNumVoices; ++v) {
      if (localTickToPlay[v] == -1) {
        int32_t finishedTickType = mPrevLocalTickToPlay[v];
        if (finishedTickType != -1) {
          // Voice just finished, clear mTickToPlay
          // if still set to finishedTickType
          mTickToPlay[v].compare_exchange_strong(
              finishedTickType,
              -1,
              std::memory_order_release,
              std::memory_order_relaxed);
        }
      }
    }

    return oboe::DataCallbackResult::Continue;
  }

  void onErrorAfterClose(
      oboe::AudioStream *oboeStream, oboe::Result error) override {
    if (error == oboe::Result::ErrorDisconnected) {
      LOGE("Stream disconnected, restarting...");
      std::thread t(&OboeAudioEngine::restart, this);
      t.detach();
    } else {
      LOGE("Error occurred: %s", oboe::convertToText(error));
    }
  }

  void setTickData(int32_t tickType, const float *data, int32_t length) {
    auto newVec = std::make_shared<std::vector<float>>(
        data, data + length);
    if (tickType == NATIVE_TICK_TYPE_STRONG) {
      std::atomic_store(&mTickStrongPtr, newVec);
    } else if (tickType == NATIVE_TICK_TYPE_NORMAL) {
      std::atomic_store(&mTickNormalPtr, newVec);
    } else if (tickType == NATIVE_TICK_TYPE_SUB) {
      std::atomic_store(&mTickSubPtr, newVec);
    }
  }

  void playTick(int32_t tickType) {
    // increment tick counter, overflow is fine
    int32_t sequence = mTickCounter.fetch_add(1);

    // packing: 4 bits tick type, rest sequence id
    int32_t taggedTick = (sequence << 4) | (tickType & 0x0F);

    // find free voice
    for (int v = 0; v < kNumVoices; ++v) {
      int32_t currentVal = mTickToPlay[v].load(std::memory_order_acquire);
      // check for free voice
      if (currentVal == -1) {
        if (mTickToPlay[v].compare_exchange_strong(
            currentVal, taggedTick,
            std::memory_order_acq_rel, std::memory_order_acquire)) {

          mPendingTickType[v].store(
              taggedTick, std::memory_order_release);
          return;
        }
      }
    }

    // no free voice found, steal next voice
    int32_t voiceToSteal = mNextVoiceToSteal.fetch_add(1) % kNumVoices;
    mTickToPlay[voiceToSteal].store(
        taggedTick, std::memory_order_release);
    mPendingTickType[voiceToSteal].store(
        taggedTick, std::memory_order_release);
  }

  void setMasterVolume(float volume) {
    mMasterVolume.store(volume);
  }

  void setDuckingVolume(float volume) {
    mDuckingVolume.store(volume);
  }

  void setMuted(bool muted) {
    mMuted.store(muted);
  }

 private:
  void reset() {
    for (int i = 0; i < kNumVoices; ++i) {
      mTickToPlay[i].store(-1);
      mPendingTickType[i].store(-1);
      mReadIndexLocal[i] = 0;
      mPrevLocalTickToPlay[i] = -1;
    }
    mNextVoiceToSteal.store(0);
  }

  void restart() {
    usleep(300 * 1000); // wait 300ms
    if (mStream) {
      mStream->close();
      mStream.reset();
    }

    if (init(false) && mIsPlaying.load()) {
      if (mIsPlaying.load()) {
        oboe::Result result = mStream->start();
        if (result != oboe::Result::OK) {
          LOGE("Restart failed inside thread: %s",
              oboe::convertToText(result));
          mIsPlaying = false;
        }
      }
    }
  }

  std::shared_ptr<oboe::AudioStream> mStream;

  // which tick type is assigned to each voice (-1 = free)
  std::atomic<int32_t> mTickToPlay[kNumVoices]{};
  std::atomic<int32_t> mTickCounter{0};

  // pending tick requests for sample-accurate start (-1 = none)
  std::atomic<int32_t> mPendingTickType[kNumVoices]{};

  // read indices owned by audio thread only (not atomic)
  int32_t mReadIndexLocal[kNumVoices]{};
  int32_t mPrevLocalTickToPlay[kNumVoices]{};

  std::atomic<int32_t> mNextVoiceToSteal = 0;
  std::atomic<bool> mResetRequested = true;

  std::atomic<float> mMasterVolume = 1.0f;
  std::atomic<float> mDuckingVolume = 1.0f;
  std::atomic<bool> mMuted = false;

  // buffers swapped atomically (no locks)
  std::shared_ptr<std::vector<float>> mTickStrongPtr;
  std::shared_ptr<std::vector<float>> mTickNormalPtr;
  std::shared_ptr<std::vector<float>> mTickSubPtr;

  int32_t mSampleRate;

  std::atomic<bool> mIsPlaying{false};
};


extern "C" {

JNIEXPORT jlong JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeCreate(
    JNIEnv *env, jobject jEngine) {
  auto *engine = new OboeAudioEngine();
  return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeDestroy(
    JNIEnv *env, jobject jEngine, jlong handle) {
  delete reinterpret_cast<OboeAudioEngine *>(handle);
}

JNIEXPORT jboolean JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeInit(
    JNIEnv *env, jobject jEngine, jlong handle) {
  return reinterpret_cast<OboeAudioEngine *>(handle)->init(true);
}

JNIEXPORT jboolean JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeStart(
    JNIEnv *env, jobject jEngine, jlong handle) {
  return reinterpret_cast<OboeAudioEngine *>(handle)->start();
}

JNIEXPORT jboolean JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeStop(
    JNIEnv *env, jobject jEngine, jlong handle) {
  return reinterpret_cast<OboeAudioEngine *>(handle)->stop();
}

JNIEXPORT void JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeSetTickData(
    JNIEnv *env, jobject jEngine, jlong handle, jint tick_type,
    jfloatArray data) {
  jfloat *rawData = env->GetFloatArrayElements(data, nullptr);
  jsize length = env->GetArrayLength(data);

  reinterpret_cast<OboeAudioEngine *>(handle)->setTickData(
      tick_type,rawData, length);

  env->ReleaseFloatArrayElements(data, rawData, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativePlayTick(
    JNIEnv *env, jobject jEngine, jlong handle, jint tick_type) {
  reinterpret_cast<OboeAudioEngine *>(handle)->playTick(tick_type);
}

JNIEXPORT void JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeSetMasterVolume(
    JNIEnv *env, jobject jEngine, jlong handle, jfloat volume) {
  reinterpret_cast<OboeAudioEngine *>(handle)->setMasterVolume(volume);
}

JNIEXPORT void JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeSetDuckingVolume(
    JNIEnv *env, jobject jEngine, jlong handle, jfloat volume) {
  reinterpret_cast<OboeAudioEngine *>(handle)->setDuckingVolume(volume);
}

JNIEXPORT void JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeSetMuted(
    JNIEnv *env, jobject jEngine, jlong handle, jboolean muted) {
  reinterpret_cast<OboeAudioEngine *>(handle)->setMuted(muted);
}

} // extern "C"