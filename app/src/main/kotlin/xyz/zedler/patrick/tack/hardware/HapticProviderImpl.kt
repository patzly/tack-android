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

package xyz.zedler.patrick.tack.hardware

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import xyz.zedler.patrick.tack.core.hardware.HapticProvider
import xyz.zedler.patrick.tack.core.model.VibrationIntensity

class HapticProviderImpl(context: Context) : HapticProvider {

  private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val manager =
      context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    manager.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
  }

  override val hasVibrator: Boolean = vibrator.hasVibrator()
  override val supportsMainEffects: Boolean = areMainEffectsSupported()
  override val defaultIntensity: VibrationIntensity by lazy {
    if (supportsMainEffects) VibrationIntensity.AUTO else VibrationIntensity.SOFT
  }
  override var isEnabled: Boolean = vibrator.hasVibrator()
    set(value) {
      field = value && vibrator.hasVibrator()
    }
  override var intensity: VibrationIntensity = defaultIntensity
    set(value) {
      field = if (value == VibrationIntensity.AUTO && !supportsMainEffects) {
        VibrationIntensity.SOFT
      } else {
        value
      }
    }
  override var isHapticPossible: Boolean = true

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

  override fun tick(isTouchEvent: Boolean) {
    val effectId =
      if (intensity == VibrationIntensity.AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.EFFECT_TICK
      } else -1
    val duration = if (intensity == VibrationIntensity.STRONG) TICK_STRONG else TICK

    vibrate(effectId, duration, isTouchEvent)
  }

  override fun click(isTouchEvent: Boolean) {
    val effectId =
      if (intensity == VibrationIntensity.AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.EFFECT_CLICK
      } else -1
    val duration = if (intensity == VibrationIntensity.STRONG) CLICK_STRONG else CLICK

    vibrate(effectId, duration, isTouchEvent)
  }

  override fun heavyClick(isTouchEvent: Boolean) {
    val effectId =
      if (intensity == VibrationIntensity.AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VibrationEffect.EFFECT_HEAVY_CLICK
      } else -1
    val duration = if (intensity == VibrationIntensity.STRONG) HEAVY_STRONG else HEAVY

    vibrate(effectId, duration, isTouchEvent)
  }

  private fun vibrate(effectId: Int, fallbackDuration: Long, isTouchEvent: Boolean) {
    if (!isEnabled || !isHapticPossible) return

    val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectId != -1) {
      VibrationEffect.createPredefined(effectId)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      VibrationEffect.createOneShot(
        fallbackDuration,
        if (intensity == VibrationIntensity.STRONG) 255 else VibrationEffect.DEFAULT_AMPLITUDE
      )
    } else null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && effect != null) {
      vibrator.vibrate(
        effect,
        (if (isTouchEvent) vibrationAttributesTouch else vibrationAttributesMedia)!!
      )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && effect != null) {
      @Suppress("DEPRECATION")
      vibrator.vibrate(effect, audioAttributes)
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(fallbackDuration, audioAttributes)
    }
  }

  private fun areMainEffectsSupported(): Boolean {
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

  companion object {
    const val TICK: Long = 2L
    const val TICK_STRONG: Long = 20L
    const val CLICK: Long = 8L
    const val CLICK_STRONG: Long = 50L
    const val HEAVY: Long = 40L
    const val HEAVY_STRONG: Long = 80L
  }
}
