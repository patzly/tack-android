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

package xyz.zedler.patrick.tack.ui.util

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import xyz.zedler.patrick.tack.core.hardware.HapticProvider
import xyz.zedler.patrick.tack.core.model.VibrationIntensity

private object NoOpHapticProvider : HapticProvider {
  override val hasVibrator: Boolean = false
  override val defaultIntensity: VibrationIntensity = VibrationIntensity.UNSET
  override val supportsMainEffects: Boolean = false
  override var isEnabled: Boolean = false
  override var intensity: VibrationIntensity = VibrationIntensity.UNSET
  override var isHapticPossible: Boolean = false

  override fun tick(isTouchEvent: Boolean) = Unit
  override fun click(isTouchEvent: Boolean) = Unit
  override fun longClick(view: View) = Unit
  override fun heavyClick(isTouchEvent: Boolean) = Unit
  override fun segmentTick(view: View, frequent: Boolean) = Unit
}

private val LocalRawHapticProvider = staticCompositionLocalOf<HapticProvider> {
  NoOpHapticProvider
}

class ComposeHapticProvider(
  private val provider: HapticProvider,
  private val view: View
) : HapticProvider by provider {

  fun longClick() {
    provider.longClick(view)
  }

  fun segmentTick(frequent: Boolean = false) {
    provider.segmentTick(view, frequent)
  }

  inline fun withClick(crossinline action: () -> Unit): () -> Unit = {
    click()
    action()
  }

  inline fun <T> withClick(crossinline action: (T) -> Unit): (T) -> Unit = {
    click()
    action(it)
  }
}

object LocalHaptic {
  val current: ComposeHapticProvider
    @Composable
    get() {
      val provider = LocalRawHapticProvider.current
      val view = LocalView.current
      return remember(provider, view) {
        ComposeHapticProvider(provider, view)
      }
    }

  infix fun provides(provider: HapticProvider): ProvidedValue<HapticProvider> {
    return LocalRawHapticProvider provides provider
  }
}
