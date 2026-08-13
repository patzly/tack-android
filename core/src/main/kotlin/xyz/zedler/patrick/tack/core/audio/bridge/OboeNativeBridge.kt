/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.core.audio.bridge

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
