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

package xyz.zedler.patrick.tack.presentation.components

import android.util.Log
import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapLayoutInfoProvider
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.PickerDefaults
import androidx.wear.compose.material3.PickerState
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import java.lang.reflect.Field

@Composable
fun TempoPicker(
  state: PickerState,
  modifier: Modifier = Modifier,
  verticalSpacing: Dp = 0.dp,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  textStyle: TextStyle = MaterialTheme.typography.displayMedium,
  @FloatRange(from = 0.0, to = 0.5) gradientRatio: Float = PickerDefaults.GradientRatio,
  hapticFeedbackEnabled: Boolean = true
) {
  val items = remember { (Constants.TEMPO_MIN..Constants.TEMPO_MAX).toList() }

  Picker(
    state = state,
    contentDescription = { "${items.getOrNull(state.selectedOptionIndex + 1) ?: ""}" },
    modifier = modifier,
    verticalSpacing = verticalSpacing,
    gradientRatio = gradientRatio,
    rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(
      scrollableState = state,
      layoutInfoProvider = state.toRotarySnapLayoutInfoProvider(),
      hapticFeedbackEnabled = hapticFeedbackEnabled
    )
  ) {
    val text = items.getOrNull(it)?.toString() ?: ""
    Text(
      modifier = Modifier.wrapContentSize(),
      textAlign = TextAlign.Center,
      color = textColor,
      style = textStyle,
      text = buildAnnotatedString {
        withStyle(style = SpanStyle(fontFeatureSettings = "tnum")) {
          append(text)
        }
      }
    )
  }
}

@Preview
@Composable
fun TempoPickerPreview() {
  TackTheme {
    val state = rememberPickerState(
      initialNumberOfOptions = 10,
      shouldRepeatOptions = false
    )
    TempoPicker(
      state = state,
      modifier = Modifier.size(100.dp, 100.dp)
    )
  }
}

@Composable
private fun PickerState.toRotarySnapLayoutInfoProvider(): RotarySnapLayoutInfoProvider {
  val density = LocalDensity.current
  return remember(this) { PickerRotarySnapLayoutInfoProvider(this, density) }
}

private class PickerRotarySnapLayoutInfoProvider(
  scrollableState: PickerState,
  private val density: Density
) : RotarySnapLayoutInfoProvider {

  private val scalingLazyListState = accessScalingLazyListState(scrollableState)

  /** Returns a height of a first item, as all items in picker have the same height. */
  override val averageItemSize: Float
    get() {
      val state = scalingLazyListState ?: return 0f
      val firstItem = state.layoutInfo.visibleItemsInfo.firstOrNull()

      return if (firstItem != null && firstItem.unadjustedSize > 0) {
        firstItem.unadjustedSize.toFloat()
      } else {
        // fallback size in pixels
        with(density) { 40.dp.toPx() }
      }
    }

  override val currentItemIndex: Int
    get() = scalingLazyListState?.centerItemIndex ?: 0

  override val currentItemOffset: Float
    get() = scalingLazyListState?.centerItemScrollOffset?.toFloat() ?: 0f

  override val totalItemCount: Int
    get() = scalingLazyListState?.layoutInfo?.totalItemsCount ?: 0

  private fun accessScalingLazyListState(pickerState: PickerState): ScalingLazyListState? {
    return try {
      val field: Field = PickerState::class.java.getDeclaredField("scalingLazyListState")
      field.isAccessible = true
      field.get(pickerState) as? ScalingLazyListState
    } catch (e: Exception) {
      Log.e(TAG, "accessScalingLazyListState: ", e)
      null
    }
  }

  companion object {
    private const val TAG = "TempoPicker"
  }
}