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

class OboeAudioEngine: public oboe::AudioStreamDataCallback {
 public:
  OboeAudioEngine() {
    mTickToPlay.store(-1);
    mReadIndex.store(0);
    mMasterVolume.store(1.0f);
    mDuckingVolume.store(1.0f);
    mMuted.store(false);

    mSampleRate = 48000.0f;

    // Threshold: -12 dB. All above this level will be compressed.
    const float kThreshold_dB = -12.0f;
    // Ratio: 4:1. For each 4 dB above the threshold, output increases by 1 dB.
    const float kRatio = 8.0f;
    // Attack: 1 ms. How quickly the compressor responds to increasing levels.
    const float kAttackTime_s = 0.001f;
    // Release: 50 ms. How quickly the compressor stops reducing gain after the signal falls below the threshold.
    const float kReleaseTime_s = 0.05f;

    mCompThreshold = decibelsToLinear(kThreshold_dB);
    mCompSlope = 1.0f / kRatio;

    mAttackCoeff = std::exp(-1.0f / (kAttackTime_s * mSampleRate));
    mReleaseCoeff = std::exp(-1.0f / (kReleaseTime_s * mSampleRate));

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
      LOGE("Failed to start Oboe stream: %s", oboe::convertToText(result));
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
    float *outputBuffer = static_cast<float *>(audioData);

    float masterVol = mMasterVolume.load();
    float duckingVol = mDuckingVolume.load();
    bool muted = mMuted.load();

    float makeupGain = muted ? 0.0f : (masterVol * duckingVol);

    int32_t tickType = mTickToPlay.load();
    int32_t readIndex = mReadIndex.load();

    std::vector<float> *sourceData = nullptr;

    std::lock_guard<std::mutex> lock(mLock);
    if (tickType == NATIVE_TICK_TYPE_STRONG) sourceData = &mTickStrong;
    else if (tickType == NATIVE_TICK_TYPE_NORMAL) sourceData = &mTickNormal;
    else if (tickType == NATIVE_TICK_TYPE_SUB) sourceData = &mTickSub;

    float attack = mAttackCoeff;
    float release = mReleaseCoeff;
    float threshold = mCompThreshold;
    float slope = mCompSlope;
    float envelope = mCompressorEnvelope;

    for (int i = 0; i < numFrames; ++i) {
      float drySample = 0.0f;

      if (sourceData && readIndex < sourceData->size()) {
        drySample = (*sourceData)[readIndex];
        readIndex++;
      }

      // Compression algorithm

      // measure the input level
      float sampleAbs = std::fabs(drySample);
      if (sampleAbs > envelope) {
        // ATTACK
        envelope = attack * envelope + (1.0f - attack) * sampleAbs;
      } else {
        // RELEASE
        envelope = release * envelope + (1.0f - release) * sampleAbs;
      }

      // Compute gain reduction
      float gain = 1.0f;
      if (envelope > threshold) {
        // Signal is above threshold
        // gain = (threshold / envelope)^(1 - slope)
        float overshoot = envelope / threshold;
        float gainReduction = std::pow(overshoot, slope - 1.0f);
        gain = gainReduction;
      }

      float compressedSample = drySample * gain;
      // apply makeup gain
      outputBuffer[i] = std::tanh(compressedSample * makeupGain);
    }

    mCompressorEnvelope = envelope;

    if (sourceData && readIndex >= sourceData->size()) {
      mTickToPlay.store(-1);
      readIndex = 0;
    }
    mReadIndex.store(readIndex);

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
    mReadIndex.store(0);
    mTickToPlay.store(tickType);
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

  inline float decibelsToLinear(float dB) {
    return std::pow(10.0f, dB / 20.0f);
  }

 private:
  std::shared_ptr<oboe::AudioStream> mStream;

  std::atomic<int32_t>
      mTickToPlay;  // -1 = silence, 1 = strong, 2 = normal, 3 = sub
  std::atomic<int32_t> mReadIndex;   // current position in tick sample

  std::atomic<float> mMasterVolume;
  std::atomic<float> mDuckingVolume;
  std::atomic<bool> mMuted;

  std::mutex mLock;
  std::vector<float> mTickStrong;
  std::vector<float> mTickNormal;
  std::vector<float> mTickSub;

  float mSampleRate;

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

  reinterpret_cast<OboeAudioEngine *>(handle)->setTickData(tick_type,
      rawData,
      length);

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