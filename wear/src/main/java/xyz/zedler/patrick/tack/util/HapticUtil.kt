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
package xyz.zedler.patrick.tack.util

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticUtil(context: Context) {

  private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= VERSION_CODES.S) {
    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    manager.defaultVibrator
  } else {
    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
  }
  private var enabled: Boolean

  init {
    enabled = hasVibrator()
  }

  private fun vibrate(duration: Long) {
    if (!enabled) {
      return
    }
    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
  }

  private fun vibrate(effectId: Int) {
    if (enabled && Build.VERSION.SDK_INT >= VERSION_CODES.Q) {
      vibrator.vibrate(VibrationEffect.createPredefined(effectId))
    }
  }

  fun tick(strong: Boolean) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.Q && !strong) {
      vibrate(VibrationEffect.EFFECT_TICK)
    } else {
      vibrate(if (strong) TICK_STRONG else TICK)
    }
  }

  fun click(strong: Boolean) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.Q && !strong) {
      vibrate(VibrationEffect.EFFECT_CLICK)
    } else {
      vibrate(if (strong) CLICK_STRONG else CLICK)
    }
  }

  fun heavyClick(strong: Boolean) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.Q && !strong) {
      vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)
    } else {
      vibrate(if (strong) HEAVY_STRONG else HEAVY)
    }
  }

  fun setEnabled(enabled: Boolean) {
    this.enabled = enabled && hasVibrator()
  }

  fun setStrongVibration(enabled: Boolean) {
    this.enabled = enabled && hasVibrator()
  }

  fun hasVibrator(): Boolean {
    return vibrator.hasVibrator()
  }

  companion object {
    const val TICK: Long = 13
    const val TICK_STRONG: Long = 20
    const val CLICK: Long = 20
    const val CLICK_STRONG: Long = 50
    const val HEAVY: Long = 50
    const val HEAVY_STRONG: Long = 80
  }
}
