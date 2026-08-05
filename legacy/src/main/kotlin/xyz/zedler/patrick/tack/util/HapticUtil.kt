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

package xyz.zedler.patrick.tack.util

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.VIBRATION_INTENSITY

class HapticUtil(context: Context) {

  private val vibrator: Vibrator = getVibrator(context)
  private val supportsMainEffects: Boolean = areMainEffectsSupported(context)
  private var enabled: Boolean = hasVibrator()
  var intensity: String = getDefaultIntensity(context)
    set(value) {
      field = if (value == VIBRATION_INTENSITY.AUTO && !supportsMainEffects) {
        VIBRATION_INTENSITY.SOFT
      } else {
        value
      }
    }

  private val vibrationAttributesTouch: VibrationAttributes? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    } else null
  }

  private val vibrationAttributesMedia: VibrationAttributes? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA)
    } else null
  }

  private val audioAttributes: AudioAttributes? by lazy {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .build()
    } else null
  }

  fun tick(isTouchEvent: Boolean = true) {
    val effectId =
      if (intensity == VIBRATION_INTENSITY.AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.EFFECT_TICK
      } else -1
    val duration = if (intensity == VIBRATION_INTENSITY.STRONG) TICK_STRONG else TICK

    vibrate(effectId, duration, isTouchEvent)
  }

  fun click(isTouchEvent: Boolean = true) {
    val effectId =
      if (intensity == VIBRATION_INTENSITY.AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.EFFECT_CLICK
      } else -1
    val duration = if (intensity == VIBRATION_INTENSITY.STRONG) CLICK_STRONG else CLICK

    vibrate(effectId, duration, isTouchEvent)
  }

  fun heavyClick(isTouchEvent: Boolean = true) {
    val effectId =
      if (intensity == VIBRATION_INTENSITY.AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.EFFECT_HEAVY_CLICK
      } else -1
    val duration = if (intensity == VIBRATION_INTENSITY.STRONG) HEAVY_STRONG else HEAVY

    vibrate(effectId, duration, isTouchEvent)
  }

  fun hapticReject(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && intensity == VIBRATION_INTENSITY.AUTO) {
      view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    } else {
      click()
    }
  }

  fun hapticSegmentTick(view: View, frequent: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
      && intensity == VIBRATION_INTENSITY.AUTO
    ) {
      view.performHapticFeedback(
        if (frequent) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        else HapticFeedbackConstants.SEGMENT_TICK
      )
    } else {
      tick()
    }
  }

  fun setEnabled(enabled: Boolean) {
    this.enabled = enabled && hasVibrator()
  }

  fun hasVibrator(): Boolean = vibrator.hasVibrator()

  fun supportsMainEffects(): Boolean = supportsMainEffects

  private fun vibrate(effectId: Int, duration: Long, isTouchEvent: Boolean) {
    if (!enabled) return

    val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectId != -1) {
      VibrationEffect.createPredefined(effectId)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      VibrationEffect.createOneShot(
        duration,
        if (intensity == VIBRATION_INTENSITY.STRONG) 255 else VibrationEffect.DEFAULT_AMPLITUDE
      )
    } else null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && effect != null) {
      vibrator.vibrate(
        effect,
        (if (isTouchEvent) vibrationAttributesTouch else vibrationAttributesMedia)!!
      )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && effect != null) {
      vibrator.vibrate(effect, audioAttributes)
    } else {
      vibrator.vibrate(duration, audioAttributes)
    }
  }

  companion object {
    const val TICK: Long = 2
    const val TICK_STRONG: Long = 20
    const val CLICK: Long = 8
    const val CLICK_STRONG: Long = 50
    const val HEAVY: Long = 40
    const val HEAVY_STRONG: Long = 80

    @JvmStatic
    fun areSystemHapticsTurnedOn(context: Context): Boolean {
      val hapticFeedbackEnabled = Settings.System.getInt(
        context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0
      )
      return hapticFeedbackEnabled != 0
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    private fun getVibrator(context: Context): Vibrator {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(
          Context.VIBRATOR_MANAGER_SERVICE
        ) as VibratorManager
        manager.defaultVibrator
      } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
      }
    }

    @JvmStatic
    fun areMainEffectsSupported(context: Context): Boolean {
      val vibrator = getVibrator(context)
      val hasAmplitudeControl =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()
      return if (hasAmplitudeControl && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val result = vibrator.areAllEffectsSupported(
          VibrationEffect.EFFECT_CLICK,
          VibrationEffect.EFFECT_HEAVY_CLICK,
          VibrationEffect.EFFECT_TICK
        )
        result == Vibrator.VIBRATION_EFFECT_SUPPORT_YES
      } else {
        false
      }
    }

    @JvmStatic
    fun getDefaultIntensity(context: Context): String {
      return if (areMainEffectsSupported(context)) VIBRATION_INTENSITY.AUTO
      else VIBRATION_INTENSITY.SOFT
    }
  }
}
