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

package xyz.zedler.patrick.tack.core.hardware

import android.view.View
import xyz.zedler.patrick.tack.core.model.VibrationIntensity

interface HapticProvider {
  val hasVibrator: Boolean
  val defaultIntensity: VibrationIntensity
  val supportsMainEffects: Boolean
  var isEnabled: Boolean
  var intensity: VibrationIntensity
  var isHapticPossible: Boolean
  
  fun tick(isTouchEvent: Boolean = true)
  fun click(isTouchEvent: Boolean = true)
  fun longClick(view: View)
  fun heavyClick(isTouchEvent: Boolean = true)
  fun segmentTick(view: View, frequent: Boolean = false)
}
