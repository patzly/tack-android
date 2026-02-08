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
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.VIBRATION_INTENSITY;

public class HapticUtil {

  public static final long TICK = 2;
  public static final long TICK_STRONG = 20;
  public static final long CLICK = 8;
  public static final long CLICK_STRONG = 50;
  public static final long HEAVY = 40;
  public static final long HEAVY_STRONG = 80;

  private final Vibrator vibrator;
  private final boolean supportsMainEffects;
  private final VibrationAttributes vibrationAttributesTouch, vibrationAttributesMedia;
  private final AudioAttributes audioAttributes;
  private boolean enabled;
  private String intensity;

  public HapticUtil(Context context) {
    vibrator = getVibrator(context);
    supportsMainEffects = areMainEffectsSupported(context);
    enabled = hasVibrator();

    intensity = getDefaultIntensity(context);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      vibrationAttributesTouch = VibrationAttributes.createForUsage(
          VibrationAttributes.USAGE_TOUCH
      );
      vibrationAttributesMedia = VibrationAttributes.createForUsage(
          VibrationAttributes.USAGE_MEDIA
      );
    } else {
      vibrationAttributesTouch = null;
      vibrationAttributesMedia = null;
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      audioAttributes = new AudioAttributes.Builder()
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
          .build();
    } else {
      audioAttributes = null;
    }
  }

  public void tick() {
    tick(true);
  }

  public void tick(boolean isTouchEvent) {
    int effectId = intensity.equals(Constants.VIBRATION_INTENSITY.AUTO)
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ? VibrationEffect.EFFECT_TICK
        : -1;
    long duration = intensity.equals(VIBRATION_INTENSITY.STRONG) ? TICK_STRONG : TICK;

    vibrate(effectId, duration, isTouchEvent);
  }

  public void click() {
    click(true);
  }

  public void click(boolean isTouchEvent) {
    int effectId = intensity.equals(Constants.VIBRATION_INTENSITY.AUTO)
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ? VibrationEffect.EFFECT_CLICK
        : -1;
    long duration = intensity.equals(VIBRATION_INTENSITY.STRONG) ? CLICK_STRONG : CLICK;

    vibrate(effectId, duration, isTouchEvent);
  }

  public void heavyClick() {
    heavyClick(true);
  }

  public void heavyClick(boolean isTouchEvent) {
    int effectId = intensity.equals(Constants.VIBRATION_INTENSITY.AUTO)
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ? VibrationEffect.EFFECT_HEAVY_CLICK
        : -1;
    long duration = intensity.equals(VIBRATION_INTENSITY.STRONG) ? HEAVY_STRONG : HEAVY;

    vibrate(effectId, duration, isTouchEvent);
  }

  public void hapticReject(View view) {
    if (VERSION.SDK_INT >= VERSION_CODES.R && intensity.equals(VIBRATION_INTENSITY.AUTO)) {
      view.performHapticFeedback(HapticFeedbackConstants.REJECT);
    } else {
      click();
    }
  }

  public void hapticSegmentTick(View view, boolean frequent) {
    if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE
        && intensity.equals(VIBRATION_INTENSITY.AUTO)) {
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

  public void setIntensity(String intensity) {
    this.intensity = intensity;
    if (intensity.equals(VIBRATION_INTENSITY.AUTO) && !supportsMainEffects) {
      this.intensity = VIBRATION_INTENSITY.SOFT;
    }
  }

  public String getIntensity() {
    return intensity;
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

  private void vibrate(int effectId, long duration, boolean isTouchEvent) {
    if (!enabled) return;

    VibrationEffect effect = null;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectId != -1) {
      effect = VibrationEffect.createPredefined(effectId);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      effect = VibrationEffect.createOneShot(
          duration,
          intensity.equals(VIBRATION_INTENSITY.STRONG)
              ? 255
              : VibrationEffect.DEFAULT_AMPLITUDE
      );
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && effect != null) {
      vibrator.vibrate(effect, isTouchEvent ? vibrationAttributesTouch : vibrationAttributesMedia);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && effect != null) {
      vibrator.vibrate(effect, audioAttributes);
    } else {
      vibrator.vibrate(duration, audioAttributes);
    }
  }

  @SuppressWarnings("deprecation")
  private static Vibrator getVibrator(Context context) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
      VibratorManager manager =
          (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
      return manager.getDefaultVibrator();
    } else {
      return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
  }

  public static boolean areMainEffectsSupported(Context context) {
    Vibrator vibrator = getVibrator(context);
    boolean hasAmplitudeControl =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl();
    if (hasAmplitudeControl && VERSION.SDK_INT >= VERSION_CODES.R) {
      int result = vibrator.areAllEffectsSupported(
          VibrationEffect.EFFECT_CLICK,
          VibrationEffect.EFFECT_HEAVY_CLICK,
          VibrationEffect.EFFECT_TICK
      );
      return result == Vibrator.VIBRATION_EFFECT_SUPPORT_YES;
    } else {
      return false;
    }
  }

  public static String getDefaultIntensity(Context context) {
    return areMainEffectsSupported(context) ? VIBRATION_INTENSITY.AUTO : VIBRATION_INTENSITY.SOFT;
  }
}
