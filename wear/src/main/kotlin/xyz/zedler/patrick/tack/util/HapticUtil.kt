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

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import xyz.zedler.patrick.tack.Constants

class HapticUtil(context: Context) {

  private val vibrator: Vibrator = getVibrator(context)

  var supportsMainEffects: Boolean = areMainEffectsSupported(context)
    private set

  var enabled: Boolean = false
    set(value) {
      field = value && hasVibrator
    }

  var intensity: String =
    if (supportsMainEffects) Constants.VibrationIntensity.AUTO
    else Constants.VibrationIntensity.SOFT
    set(value) {
      field = if (value == Constants.VibrationIntensity.AUTO && !supportsMainEffects) {
        Constants.VibrationIntensity.SOFT
      } else {
        value
      }
    }

  val hasVibrator: Boolean
    get() = vibrator.hasVibrator()

  private val vibrationAttributesTouch: VibrationAttributes? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    } else {
      null
    }
  private val vibrationAttributesMedia: VibrationAttributes? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA)
    } else {
      null
    }

  private val audioAttributes: AudioAttributes? =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .build()
    } else {
      null
    }

  init {
    enabled = hasVibrator
    intensity = getDefaultIntensity(context)
  }

  fun tick(isTouchEvent: Boolean = true) = vibrate(
    if (intensity == Constants.VibrationIntensity.AUTO
      && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
      VibrationEffect.EFFECT_TICK else null,
    if (intensity == Constants.VibrationIntensity.STRONG) TICK_STRONG else TICK,
    isTouchEvent
  )

  fun click(isTouchEvent: Boolean = true) = vibrate(
    if (intensity == Constants.VibrationIntensity.AUTO
      && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
      VibrationEffect.EFFECT_CLICK else null,
    if (intensity == Constants.VibrationIntensity.STRONG) CLICK_STRONG else CLICK,
    isTouchEvent
  )

  fun heavyClick(isTouchEvent: Boolean = true) = vibrate(
    if (intensity == Constants.VibrationIntensity.AUTO
      && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
      VibrationEffect.EFFECT_HEAVY_CLICK else null,
    if (intensity == Constants.VibrationIntensity.STRONG) HEAVY_STRONG else HEAVY,
    isTouchEvent
  )

  private fun vibrate(effectId: Int?, duration: Long, isTouchEvent: Boolean) {
    if (!enabled) return

    val effect = if (effectId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      VibrationEffect.createPredefined(effectId)
    } else {
      VibrationEffect.createOneShot(
        duration,
        if (intensity == Constants.VibrationIntensity.STRONG) 255
        else VibrationEffect.DEFAULT_AMPLITUDE
      )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      vibrator.vibrate(
        effect,
        if (isTouchEvent) vibrationAttributesTouch as VibrationAttributes
        else vibrationAttributesMedia as VibrationAttributes
      )
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(effect, audioAttributes as AudioAttributes)
    }
  }

  companion object {
    const val TICK = 2L
    const val TICK_STRONG = 20L
    const val CLICK = 8L
    const val CLICK_STRONG = 50L
    const val HEAVY = 40L
    const val HEAVY_STRONG = 80L

    private fun getVibrator(context: Context): Vibrator {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
          .defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
      }
    }

    fun areMainEffectsSupported(context: Context): Boolean {
      val vibrator = getVibrator(context)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.hasAmplitudeControl()) {
        val result = vibrator.areAllEffectsSupported(
          VibrationEffect.EFFECT_CLICK,
          VibrationEffect.EFFECT_HEAVY_CLICK,
          VibrationEffect.EFFECT_TICK
        )
        return result == Vibrator.VIBRATION_EFFECT_SUPPORT_YES
      }
      return false
    }

    fun getDefaultIntensity(context: Context): String {
      return if (areMainEffectsSupported(context)) {
        Constants.VibrationIntensity.AUTO
      } else {
        Constants.VibrationIntensity.SOFT
      }
    }
  }
}