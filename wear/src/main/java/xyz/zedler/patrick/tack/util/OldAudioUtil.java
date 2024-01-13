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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;

public class OldAudioUtil {

  private final Context context;
  private final SoundPool soundPool;
  private final int[] soundIds = new int[4];

  public OldAudioUtil(Context context) {
    this.context = context;
    soundPool = new SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        ).build();
    soundIds[0] = soundPool.load(context, R.raw.wood, 1);
    soundIds[1] = soundPool.load(context, R.raw.click, 1);
    soundIds[2] = soundPool.load(context, R.raw.ding, 1);
    soundIds[3] = soundPool.load(context, R.raw.beep, 1);
  }

  public void destroy() {
    soundPool.release();
  }

  public void play(String sound, boolean isEmphasis) {
    int soundId;
    switch (sound) {
      case Constants.SOUND.CLICK:
        soundId = soundIds[1];
        break;
      case Constants.SOUND.DING:
        soundId = soundIds[2];
        break;
      case Constants.SOUND.BEEP:
        soundId = soundIds[3];
        break;
      default:
        soundId = soundIds[0];
        break;
    }
    soundPool.play(
        soundId, 1, 1, 1, 0, isEmphasis ? 1.5f : 1
    );
  }

  public boolean isSpeakerAvailable() {
    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT);
  }
}
