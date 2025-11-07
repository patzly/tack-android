#include <jni.h>
#include <oboe/Oboe.h>
#include <vector>
#include <atomic>
#include <mutex>
#include <android/log.h>

#define LOG_TAG "OboeAudioEngine"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

constexpr int32_t NATIVE_TICK_TYPE_STRONG = 1;
constexpr int32_t NATIVE_TICK_TYPE_NORMAL = 2;
constexpr int32_t NATIVE_TICK_TYPE_SUB    = 3;

class OboeAudioEngine : public oboe::AudioStreamDataCallback {
 public:
  OboeAudioEngine() {
    mTickToPlay.store(-1);
    mReadIndex.store(0);
    mMasterVolume.store(1.0f);
    mDuckingVolume.store(1.0f);
    mMuted.store(false);
  }

  ~OboeAudioEngine() override {
    stop();
  }

  bool start() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setSessionId(oboe::SessionId::Allocate)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(1)
        ->setSampleRate(48000)
        ->setDataCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
      LOGE("Failed to open Oboe stream: %s", oboe::convertToText(result));
      return false;
    }

    result = mStream->requestStart();
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

  /**
   * Der Kern-Audio-Callback. Wird vom System auf einem Echtzeit-Thread aufgerufen.
   * Muss extrem schnell sein, nicht blockieren und keinen Speicher allozieren.
   */
  oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream, void *audioData, int32_t numFrames) override {
    float *outputBuffer = static_cast<float *>(audioData);

    float masterVol = mMasterVolume.load();
    float duckingVol = mDuckingVolume.load();
    bool muted = mMuted.load();
    float finalVolume = muted ? 0.0f : (masterVol * duckingVol);

    int32_t tickType = mTickToPlay.load();
    int32_t readIndex = mReadIndex.load();

    std::vector<float>* sourceData = nullptr;

    std::lock_guard<std::mutex> lock(mLock);
    if (tickType == NATIVE_TICK_TYPE_STRONG) sourceData = &mTickStrong;
    else if (tickType == NATIVE_TICK_TYPE_NORMAL) sourceData = &mTickNormal;
    else if (tickType == NATIVE_TICK_TYPE_SUB) sourceData = &mTickSub;

    for (int i = 0; i < numFrames; ++i) {
      if (sourceData && readIndex < sourceData->size()) {
        outputBuffer[i] = (*sourceData)[readIndex] * finalVolume;
        readIndex++;
      } else {
        outputBuffer[i] = 0.0f;
      }
    }

    // Wenn der Tick zu Ende gespielt wurde, setze den Status zurück
    if (sourceData && readIndex >= sourceData->size()) {
      mTickToPlay.store(-1); // Keinen Tick mehr spielen
      readIndex = 0;
    }
    mReadIndex.store(readIndex);

    return oboe::DataCallbackResult::Continue;
  }

  void setTickData(int32_t tickType, const float* data, int32_t length) {
    std::lock_guard<std::mutex> lock(mLock);
    std::vector<float>* targetVector = nullptr;

    if (tickType == NATIVE_TICK_TYPE_STRONG) targetVector = &mTickStrong;
    else if (tickType == NATIVE_TICK_TYPE_NORMAL) targetVector = &mTickNormal;
    else if (tickType == NATIVE_TICK_TYPE_SUB) targetVector = &mTickSub;

    if (targetVector) {
      targetVector->assign(data, data + length);
    }
  }

  void playTick(int32_t tickType) {
    // Setze den Lese-Index auf 0 zurück und signalisiere, welcher Tick gespielt werden soll.
    // Dies wird vom nächsten `onAudioReady`-Callback sofort aufgenommen.
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

  int32_t getSessionId() {
    if (mStream) {
      return mStream->getSessionId();
    }
    return -1; // oboe::SessionId::None
  }

 private:
  std::shared_ptr<oboe::AudioStream> mStream;

  std::atomic<int32_t> mTickToPlay;  // -1 = Stille, 1 = Strong, 2 = Normal, 3 = Sub
  std::atomic<int32_t> mReadIndex;   // Aktuelle Position im Tick-Sample

  std::atomic<float> mMasterVolume;
  std::atomic<float> mDuckingVolume;
  std::atomic<bool> mMuted;

  std::mutex mLock;
  std::vector<float> mTickStrong;
  std::vector<float> mTickNormal;
  std::vector<float> mTickSub;
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
    JNIEnv *env, jobject jEngine, jlong handle, jint tick_type, jfloatArray data) {
  jfloat *rawData = env->GetFloatArrayElements(data, nullptr);
  jsize length = env->GetArrayLength(data);

  reinterpret_cast<OboeAudioEngine *>(handle)->setTickData(tick_type, rawData, length);

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

JNIEXPORT jint JNICALL
Java_xyz_zedler_patrick_tack_metronome_AudioEngine_nativeGetSessionId(
    JNIEnv *env, jobject jEngine, jlong handle) {
  return reinterpret_cast<OboeAudioEngine *>(handle)->getSessionId();
}

} // extern "C"