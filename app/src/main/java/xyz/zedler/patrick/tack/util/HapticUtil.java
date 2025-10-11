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

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class HapticUtil {

  private final Vibrator vibrator;
  private boolean enabled;
  private final boolean hasAmplitudeControl, supportsMainEffects;

  public static final long TICK = 20;
  public static final long CLICK = 50;
  public static final long HEAVY = 80;

  public HapticUtil(Context context) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
      VibratorManager manager =
          (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
      vibrator = manager.getDefaultVibrator();
    } else {
      vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
    enabled = hasVibrator();
    hasAmplitudeControl =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl();
    if (hasAmplitudeControl && VERSION.SDK_INT >= VERSION_CODES.R) {
      int result = vibrator.areAllEffectsSupported(
          VibrationEffect.EFFECT_CLICK,
          VibrationEffect.EFFECT_HEAVY_CLICK,
          VibrationEffect.EFFECT_TICK
      );
      supportsMainEffects = result == Vibrator.VIBRATION_EFFECT_SUPPORT_YES;
    } else {
      supportsMainEffects = false;
    }
  }

  public void tick(boolean useEffect) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasAmplitudeControl && useEffect) {
      vibrate(VibrationEffect.EFFECT_TICK);
    } else {
      vibrate(TICK);
    }
  }

  public void tick() {
    tick(true);
  }

  public void click(boolean useEffect) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasAmplitudeControl && useEffect) {
      vibrate(VibrationEffect.EFFECT_CLICK);
    } else {
      vibrate(CLICK);
    }
  }

  public void click() {
    click(true);
  }

  public void heavyClick(boolean useEffect) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasAmplitudeControl && useEffect) {
      vibrate(VibrationEffect.EFFECT_HEAVY_CLICK);
    } else {
      vibrate(HEAVY);
    }
  }

  public void heavyClick() {
    heavyClick(true);
  }

  public void hapticReject(View view) {
    if (VERSION.SDK_INT >= VERSION_CODES.R) {
      view.performHapticFeedback(HapticFeedbackConstants.REJECT);
    } else {
      click();
    }
  }

  public void hapticSegmentTick(View view, boolean frequent) {
    if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
      view.performHapticFeedback(
          frequent
              ? HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
              : HapticFeedbackConstants.SEGMENT_TICK
      );
    } else {
      tick();
    }
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled && hasVibrator();
  }

  public boolean hasVibrator() {
    return vibrator.hasVibrator();
  }

  public boolean supportsMainEffects() {
    return supportsMainEffects;
  }

  public static boolean areSystemHapticsTurnedOn(Context context) {
    int hapticFeedbackEnabled = Settings.System.getInt(
        context.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 0
    );
    return hapticFeedbackEnabled != 0;
  }

  public void vibrate(long duration) {
    if (enabled) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
        );
      } else {
        vibrator.vibrate(duration);
      }
    }
  }

  private void vibrate(int effectId) {
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrator.vibrate(VibrationEffect.createPredefined(effectId));
    }
  }
}
