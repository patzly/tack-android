#include <android/log.h>
#include <jni.h>
#include <oboe/Oboe.h>
#include <algorithm>
#include <atomic>
#include <cmath>
#include <memory>
#include <vector>

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
      mPendingTickType[i].store(-1);
      mReadIndexLocal[i] = 0;
      mPrevLocalTickToPlay[i] = -1;
    }
    mNextVoiceToSteal.store(0);

    mMasterVolume.store(1.0f);
    mDuckingVolume.store(1.0f);
    mMuted.store(false);

    mSampleRate = 48000;  // default; overwritten after openStream()

    // Compressor defaults
    mAttackTime_s = 0.001f;
    mReleaseTime_s = 0.25f;

    const float kThreshold_dB = -12.0f;
    const float kRatio = 5.0f;

    mCompThreshold = decibelsToLinear(kThreshold_dB);
    mCompSlope = 1.0f / kRatio;

    recomputeCompressorCoeffs();
    recomputeCompressorTable();

    mCompressorEnvelope = 0.0f;

    // initialize empty buffers
    std::atomic_store(
        &mTickStrongPtr, std::make_shared<std::vector<float>>());
    std::atomic_store(
        &mTickNormalPtr, std::make_shared<std::vector<float>>());
    std::atomic_store(
        &mTickSubPtr, std::make_shared<std::vector<float>>());
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

    // use real sample rate reported by stream
    mSampleRate = mStream->getSampleRate();
    recomputeCompressorCoeffs();

    constexpr int64_t kTimeoutNanos = 1 * 1000 * 1000 * 1000;  // 1 second
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

  // Realtime audio callback
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

    // Load which tick types are assigned to voices (atomic snapshot)
    int32_t localTickToPlay[kNumVoices];
    for (int v = 0; v < kNumVoices; ++v) {
      localTickToPlay[v] = mTickToPlay[v].load(std::memory_order_acquire);
    }

    // Atomically load buffers once per callback
    auto strong = std::atomic_load(&mTickStrongPtr);
    auto normal = std::atomic_load(&mTickNormalPtr);
    auto sub = std::atomic_load(&mTickSubPtr);

    // Map voice -> buffer shared_ptr (snapshot)
    std::shared_ptr<std::vector<float>> sourceData[kNumVoices];
    for (int v = 0; v < kNumVoices; ++v) {
      int32_t tickType = localTickToPlay[v];
      if (tickType == NATIVE_TICK_TYPE_STRONG)
        sourceData[v] = strong;
      else if (tickType == NATIVE_TICK_TYPE_NORMAL)
        sourceData[v] = normal;
      else if (tickType == NATIVE_TICK_TYPE_SUB)
        sourceData[v] = sub;
      else
        sourceData[v] = nullptr;

      // initialize prev state if different
      if (mPrevLocalTickToPlay[v] != localTickToPlay[v]) {
        if (localTickToPlay[v] != -1) {
          // If assignment was present at the moment of snapshot, we keep
          // readIndexLocal as-is (it may be 0 or a continued index).
          // The pending mechanism handles sample-accurate starts.
        } else {
          mReadIndexLocal[v] = 0;
        }
        mPrevLocalTickToPlay[v] = localTickToPlay[v];
      }
    }

    for (int i = 0; i < numFrames; ++i) {
      float drySample = 0.0f;

      // Check pending tick events BEFORE mixing for this sample
      for (int v = 0; v < kNumVoices; ++v) {
        int32_t pending = mPendingTickType[v].load(
            std::memory_order_acquire);
        if (pending != -1) {
          // There is a pending immediate start for voice v.
          // Atomically clear pending
          mPendingTickType[v].store(-1, std::memory_order_release);

          // Assign the tick type to the active voice (mTickToPlay already set
          // by playTick, but refresh localTickToPlay and sourceData for safety)
          localTickToPlay[v] = mTickToPlay[v].load(
              std::memory_order_relaxed);

          if (localTickToPlay[v] == NATIVE_TICK_TYPE_STRONG)
            sourceData[v] = strong;
          else if (localTickToPlay[v] == NATIVE_TICK_TYPE_NORMAL)
            sourceData[v] = normal;
          else if (localTickToPlay[v] == NATIVE_TICK_TYPE_SUB)
            sourceData[v] = sub;
          else
            sourceData[v] = nullptr;

          // Start playing at the current sample
          mReadIndexLocal[v] = 0;
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
          // voice is inactive or finished
          if (localTickToPlay[v] != -1) {
            localTickToPlay[v] = -1;
            mPrevLocalTickToPlay[v] = -1;
            mReadIndexLocal[v] = 0;
            sourceData[v] = nullptr;
          }
        }
      }

      if ((i & 7) == 0) {
        float sampleAbs = std::fabs(drySample);
        if (sampleAbs > envelope)
          envelope = attack * envelope + (1.0f - attack) * sampleAbs;
        else
          envelope = release * envelope + (1.0f - release) * sampleAbs;
      }

      float gain = 1.0f;
      if (envelope > threshold) {
        float overshoot = envelope / threshold;
        float t = std::min(overshoot, 11.0f);
        int idx = static_cast<int>((t - 1.0f) * (kCompTableSize - 1) / 10.0f);
        gain = mCompGainTable[idx];
      }

      float compressedSample = drySample * gain;
      outputBuffer[i] = fastTanh(compressedSample * makeupGain);
    }

    // publish voice active/inactive state back to atomics
    for (int v = 0; v < kNumVoices; ++v) {
      mTickToPlay[v].store(
          localTickToPlay[v], std::memory_order_release);
    }

    mCompressorEnvelope = envelope;
    return oboe::DataCallbackResult::Continue;
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
    // find free voice
    for (int v = 0; v < kNumVoices; ++v) {
      int32_t expected = -1;
      // use acq_rel to synchronize properly with audio thread
      if (mTickToPlay[v].compare_exchange_strong(
          expected, tickType,
          std::memory_order_acq_rel, std::memory_order_acquire)) {
        mPendingTickType[v].store(tickType, std::memory_order_release);
        return;
      }
    }
    // no free voice found, steal next voice
    int32_t voiceToSteal = mNextVoiceToSteal.fetch_add(1) % kNumVoices;
    mTickToPlay[voiceToSteal].store(tickType, std::memory_order_release);
    mPendingTickType[voiceToSteal].store(
        tickType, std::memory_order_release);
  }

  void setMasterVolume(float volume) { mMasterVolume.store(volume); }

  void setDuckingVolume(float volume) { mDuckingVolume.store(volume); }

  void setMuted(bool muted) { mMuted.store(muted); }

  void recomputeCompressorTable() {
    for (int i = 0; i < kCompTableSize; ++i) {
      float overshoot = 1.0f +(10.0f * static_cast<float>(i) /
          static_cast<float>(kCompTableSize - 1));
      float gain = std::pow(overshoot, mCompSlope - 1.0f);
      mCompGainTable[i] = gain;
    }
  }

  static inline float decibelsToLinear(float dB) {
    return std::pow(10.0f, dB / 20.0f);
  }

  static inline float fastTanh(float x) {
    float x2 = x * x;
    return x * (27.0f + x2) / (27.0f + 9.0f * x2);
  }

 private:
  void recomputeCompressorCoeffs() {
    mAttackCoeff =
        std::exp(-1.0f / (mAttackTime_s * static_cast<float>(mSampleRate)));
    mReleaseCoeff =
        std::exp(-1.0f / (mReleaseTime_s * static_cast<float>(mSampleRate)));
  }

  std::shared_ptr<oboe::AudioStream> mStream;

  // which tick type is assigned to each voice (-1 = free)
  std::atomic<int32_t> mTickToPlay[kNumVoices];

  // pending tick requests for sample-accurate start (-1 = none)
  std::atomic<int32_t> mPendingTickType[kNumVoices];

  // read indices owned by audio thread only (not atomic)
  int32_t mReadIndexLocal[kNumVoices];
  int32_t mPrevLocalTickToPlay[kNumVoices];

  std::atomic<int32_t> mNextVoiceToSteal;

  std::atomic<float> mMasterVolume;
  std::atomic<float> mDuckingVolume;
  std::atomic<bool> mMuted;

  // buffers swapped atomically (no locks)
  std::shared_ptr<std::vector<float>> mTickStrongPtr;
  std::shared_ptr<std::vector<float>> mTickNormalPtr;
  std::shared_ptr<std::vector<float>> mTickSubPtr;

  int32_t mSampleRate;

  // compressor state
  float mCompThreshold;
  float mCompSlope;
  float mAttackCoeff;
  float mReleaseCoeff;
  float mAttackTime_s;
  float mReleaseTime_s;

  float mCompressorEnvelope;

  static constexpr int kCompTableSize = 1024;
  std::array<float, kCompTableSize> mCompGainTable;
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