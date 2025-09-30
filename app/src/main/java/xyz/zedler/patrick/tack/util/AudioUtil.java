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

package xyz.zedler.patrick.tack.util;

import android.media.AudioAttributes;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

public class AudioUtil {

  private static final int DATA_CHUNK_SIZE = 8;
  private static final byte[] DATA_MARKER = "data".getBytes(StandardCharsets.US_ASCII);

  public static AudioAttributes getAttributes() {
    return new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build();
  }

  public static float[] readDataFromWavFloat(InputStream input) throws IOException {
    byte[] content;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      content = input.readAllBytes();
    } else {
      content = readInputStreamToBytes(input);
    }
    int indexOfDataMarker = getIndexOfDataMarker(content);
    if (indexOfDataMarker < 0) {
      throw new RuntimeException("Could not find data marker in the content");
    }
    int startOfSound = indexOfDataMarker + DATA_CHUNK_SIZE;
    if (startOfSound > content.length) {
      throw new RuntimeException("Too short data chunk");
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(
        content, startOfSound, content.length - startOfSound
    );
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
    float[] data = new float[floatBuffer.remaining()];
    floatBuffer.get(data);
    return data;
  }

  private static byte[] readInputStreamToBytes(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int read;
    byte[] data = new byte[4096];
    while ((read = input.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, read);
    }
    return buffer.toByteArray();
  }

  private static int getIndexOfDataMarker(byte[] array) {
    if (DATA_MARKER.length == 0) {
      return 0;
    }
    outer:
    for (int i = 0; i < array.length - DATA_MARKER.length + 1; i++) {
      for (int j = 0; j < DATA_MARKER.length; j++) {
        if (array[i + j] != DATA_MARKER[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  public static float dbToLinearVolume(int reductionDb) {
    return (float) Math.pow(10f, reductionDb / 20f);
  }
}