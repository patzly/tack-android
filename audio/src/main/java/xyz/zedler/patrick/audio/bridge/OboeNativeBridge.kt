package xyz.zedler.patrick.audio.bridge

import androidx.annotation.Keep

@Keep
internal class OboeNativeBridge {

    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeInit(handle: Long): Boolean
    external fun nativeStart(handle: Long): Boolean
    external fun nativeStop(handle: Long): Boolean
    external fun nativeSetTickData(handle: Long, tickType: Int, data: FloatArray)
    external fun nativePlayTick(handle: Long, tickType: Int)
    external fun nativeSetMasterVolume(handle: Long, volume: Float)
    external fun nativeSetDuckingVolume(handle: Long, volume: Float)
    external fun nativeSetMuted(handle: Long, muted: Boolean)

    companion object {
        const val NATIVE_TICK_TYPE_STRONG: Int = 1
        const val NATIVE_TICK_TYPE_NORMAL: Int = 2
        const val NATIVE_TICK_TYPE_SUB: Int = 3

        init {
            System.loadLibrary("oboe-audio-engine")
        }
    }
}