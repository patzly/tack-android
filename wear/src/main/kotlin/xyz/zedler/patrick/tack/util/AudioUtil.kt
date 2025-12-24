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
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util

import android.media.AudioAttributes
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

object AudioUtil {

  private const val DATA_CHUNK_SIZE = 8
  private val DATA_MARKER = "data".toByteArray(StandardCharsets.US_ASCII)

  fun getAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_MEDIA)
      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
      .build()
  }

  @Throws(IOException::class)
  fun readDataFromWavFloat(input: InputStream): FloatArray {
    val content = input.use { it.readBytes() }

    val indexOfDataMarker = getIndexOfDataMarker(content)
    if (indexOfDataMarker < 0) {
      throw RuntimeException("Could not find data marker in the content")
    }

    val startOfSound = indexOfDataMarker + DATA_CHUNK_SIZE
    if (startOfSound > content.size) {
      throw RuntimeException("Too short data chunk")
    }

    val floatBuffer = ByteBuffer.wrap(
      content, startOfSound, content.size - startOfSound
    ).apply {
      order(ByteOrder.LITTLE_ENDIAN)
    }.asFloatBuffer()

    val data = FloatArray(floatBuffer.remaining())
    floatBuffer.get(data)
    return data
  }

  private fun getIndexOfDataMarker(array: ByteArray): Int {
    if (DATA_MARKER.isEmpty()) return 0

    val limit = array.size - DATA_MARKER.size

    for (i in 0..limit) {
      var match = true
      for (j in DATA_MARKER.indices) {
        if (array[i + j] != DATA_MARKER[j]) {
          match = false
          break
        }
      }
      if (match) return i
    }
    return -1
  }
}