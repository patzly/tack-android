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
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class HapticUtil {

  private final Vibrator vibrator;
  private boolean enabled;

  public static final long TICK = 13;
  public static final long TICK_STRONG = 20;
  public static final long CLICK = 20;
  public static final long CLICK_STRONG = 50;
  public static final long HEAVY = 50;
  public static final long HEAVY_STRONG = 80;

  public HapticUtil(Context context) {
    vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    enabled = hasVibrator();
  }

  public void vibrate(long duration) {
    if (!enabled) {
      return;
    }
    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
  }

  private void vibrate(int effectId) {
    if (enabled && VERSION.SDK_INT >= VERSION_CODES.Q) {
      vibrator.vibrate(VibrationEffect.createPredefined(effectId));
    }
  }

  public void tick(boolean strong) {
    if (VERSION.SDK_INT >= VERSION_CODES.Q && !strong) {
      vibrate(VibrationEffect.EFFECT_TICK);
    } else {
      vibrate(strong ? TICK_STRONG : TICK);
    }
  }

  public void click(boolean strong) {
    if (VERSION.SDK_INT >= VERSION_CODES.Q && !strong) {
      vibrate(VibrationEffect.EFFECT_CLICK);
    } else {
      vibrate(strong ? CLICK_STRONG : CLICK);
    }
  }

  public void heavyClick(boolean strong) {
    if (VERSION.SDK_INT >= VERSION_CODES.Q && !strong) {
      vibrate(VibrationEffect.EFFECT_HEAVY_CLICK);
    } else {
      vibrate(strong ? HEAVY_STRONG : HEAVY);
    }
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled && hasVibrator();
  }

  public void setStrongVibration(boolean enabled) {
    this.enabled = enabled && hasVibrator();
  }

  public boolean hasVibrator() {
    return vibrator.hasVibrator();
  }
}
