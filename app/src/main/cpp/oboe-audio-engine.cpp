#include <jni.h>
#include <oboe/Oboe.h>
#include <vector>
#include <atomic>
#include <mutex>
#include <android/log.h>
#include <cmath>
#include <algorithm>

#define LOG_TAG "OboeAudioEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

constexpr int32_t NATIVE_TICK_TYPE_STRONG = 1;
constexpr int32_t NATIVE_TICK_TYPE_NORMAL = 2;
constexpr int32_t NATIVE_TICK_TYPE_SUB = 3;

constexpr int32_t kNumVoices = 10;

class OboeAudioEngine: public oboe::AudioStreamDataCallback {
 public:
  OboeAudioEngine() {
    for (int i = 0; i < kNumVoices; ++i) {
      mTickToPlay[i].store(-1);
      mReadIndex[i].store(0);
    }
    mNextVoiceToSteal.store(0);

    mMasterVolume.store(1.0f);
    mDuckingVolume.store(1.0f);
    mMuted.store(false);

    mSampleRate = 48000;

    // Threshold: -12 dB. All above this level will be compressed.
    const float kThreshold_dB = -12.0f;
    // Ratio: 4:1. For each 4 dB above the threshold, output increases by 1 dB.
    const float kRatio = 8.0f;
    // Attack: 1 ms. How quickly the compressor responds to increasing levels.
    const float kAttackTime_s = 0.001f;
    // Release: 50 ms. How quickly the compressor stops reducing gain after the
    // signal falls below the threshold.
    const float kReleaseTime_s = 0.05f;

    mCompThreshold = decibelsToLinear(kThreshold_dB);
    mCompSlope = 1.0f / kRatio;

    mAttackCoeff =
        std::exp(-1.0f / (kAttackTime_s * static_cast<float>(mSampleRate)));
    mReleaseCoeff =
        std::exp(-1.0f / (kReleaseTime_s * static_cast<float>(mSampleRate)));

    mCompressorEnvelope = 0.0f;
  }

  ~OboeAudioEngine() override {
    stop();
  }

  bool start() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(1)
        ->setSampleRate(mSampleRate)
        ->setDataCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
      LOGE("Failed to open Oboe stream: %s", oboe::convertToText(result));
      return false;
    }

    constexpr int64_t kTimeoutNanos = 1 * 1000 * 1000 * 1000; // 1 second
    result = mStream->start(kTimeoutNanos);
    if (result != oboe::Result::OK) {
      LOGE("Failed to start Oboe stream: %s",
          oboe::convertToText(result));
      return false;
    }
    return true;
  }

  void stop() {
    if (mStream) {
      mStream->stop();
      mStream->close();
      mStream.reset();
    }
  }

  oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream,
      void *audioData,
      int32_t numFrames) override {
    auto *outputBuffer = static_cast<float *>(audioData);

    float masterVol = mMasterVolume.load();
    float duckingVol = mDuckingVolume.load();
    bool muted = mMuted.load();

    float makeupGain = muted ? 0.0f : (masterVol * duckingVol);

    float attack = mAttackCoeff;
    float release = mReleaseCoeff;
    float threshold = mCompThreshold;
    float slope = mCompSlope;
    float envelope = mCompressorEnvelope;

    // load voice states into local variables
    int32_t localReadIndex[kNumVoices];
    int32_t localTickToPlay[kNumVoices];
    std::vector<float> *sourceData[kNumVoices];

    for (int v = 0; v < kNumVoices; ++v) {
      localTickToPlay[v] = mTickToPlay[v].load(std::memory_order_relaxed);
      localReadIndex[v] = mReadIndex[v].load(std::memory_order_relaxed);
    }

    {
      std::lock_guard<std::mutex> lock(mLock);
      for (int v = 0; v < kNumVoices; ++v) {
        int32_t tickType = localTickToPlay[v];
        if (tickType == NATIVE_TICK_TYPE_STRONG) {
          sourceData[v] = &mTickStrong;
        } else if (tickType == NATIVE_TICK_TYPE_NORMAL) {
          sourceData[v] = &mTickNormal;
        } else if (tickType == NATIVE_TICK_TYPE_SUB) {
          sourceData[v] = &mTickSub;
        } else {
          sourceData[v] = nullptr;
        }
      }
    }

    for (int i = 0; i < numFrames; ++i) {
      float drySample = 0.0f;
      // mix all active voices
      for (int v = 0; v < kNumVoices; ++v) {
        if (sourceData[v] != nullptr && localReadIndex[v] < sourceData[v]->size()) {
          drySample += (*sourceData[v])[localReadIndex[v]];
          localReadIndex[v]++;
        } else {
          // voice is inactive or finished
          if (localTickToPlay[v] != -1) {
            localTickToPlay[v] = -1;
          }
        }
      }

      float sampleAbs = std::fabs(drySample);
      if (sampleAbs > envelope) {
        envelope = attack * envelope + (1.0f - attack) * sampleAbs;
      } else {
        envelope = release * envelope + (1.0f - release) * sampleAbs;
      }

      float gain = 1.0f;
      if (envelope > threshold) {
        float overshoot = envelope / threshold;
        float gainReduction = std::pow(overshoot, slope - 1.0f);
        gain = gainReduction;
      }

      float compressedSample = drySample * gain;
      outputBuffer[i] = std::tanh(compressedSample * makeupGain);
    }

    // store local variables back to atomic variables
    mCompressorEnvelope = envelope;
    for (int v = 0; v < kNumVoices; ++v) {
      mTickToPlay[v].store(
          localTickToPlay[v], std::memory_order_relaxed);
      mReadIndex[v].store(localReadIndex[v], std::memory_order_relaxed);
    }

    return oboe::DataCallbackResult::Continue;
  }

  void setTickData(int32_t tickType, const float *data, int32_t length) {
    std::lock_guard<std::mutex> lock(mLock);
    std::vector<float> *targetVector = nullptr;

    if (tickType == NATIVE_TICK_TYPE_STRONG) targetVector = &mTickStrong;
    else if (tickType == NATIVE_TICK_TYPE_NORMAL) targetVector = &mTickNormal;
    else if (tickType == NATIVE_TICK_TYPE_SUB) targetVector = &mTickSub;

    if (targetVector) {
      targetVector->assign(data, data + length);
    }
  }

  void playTick(int32_t tickType) {
    // find free voice
    for (int v = 0; v < kNumVoices; ++v) {
      int32_t expected = -1; // default is inactive
      // atomic compare-and-swap
      // swap -1 with tickType if current value is still -1
      if (mTickToPlay[v].compare_exchange_strong(expected, tickType)) {
        mReadIndex[v].store(0);
        return;
      }
    }
    // no free voice found, steal next voice
    int32_t voiceToSteal = mNextVoiceToSteal.fetch_add(1) % kNumVoices;
    mTickToPlay[voiceToSteal].store(tickType);
    mReadIndex[voiceToSteal].store(0);
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

  static inline float decibelsToLinear(float dB) {
    return std::pow(10.0f, dB / 20.0f);
  }

 private:
  std::shared_ptr<oboe::AudioStream> mStream;

  std::atomic<int32_t> mTickToPlay[kNumVoices];
  std::atomic<int32_t> mReadIndex[kNumVoices];

  std::atomic<int32_t> mNextVoiceToSteal;

  std::atomic<float> mMasterVolume;
  std::atomic<float> mDuckingVolume;
  std::atomic<bool> mMuted;

  std::mutex mLock;
  std::vector<float> mTickStrong;
  std::vector<float> mTickNormal;
  std::vector<float> mTickSub;

  int32_t mSampleRate;

  float mCompThreshold;
  float mCompSlope;
  float mAttackCoeff;
  float mReleaseCoeff;

  float mCompressorEnvelope;
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
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeStart(
    JNIEnv *env, jobject jEngine, jlong handle) {
  return reinterpret_cast<OboeAudioEngine *>(handle)->start();
}

JNIEXPORT void JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeStop(
    JNIEnv *env, jobject jEngine, jlong handle) {
  reinterpret_cast<OboeAudioEngine *>(handle)->stop();
}

JNIEXPORT void JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeSetTickData(
    JNIEnv *env,
    jobject jEngine,
    jlong handle,
    jint tick_type,
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