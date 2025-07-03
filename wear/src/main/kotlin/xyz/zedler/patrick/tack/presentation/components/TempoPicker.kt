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

package xyz.zedler.patrick.tack.presentation.components

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
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
  val items = (Constants.TEMPO_MIN..Constants.TEMPO_MAX).toList()
  Picker(
    state = state,
    contentDescription = { "${state.selectedOptionIndex + 1}" },
    modifier = modifier,
    verticalSpacing = verticalSpacing,
    gradientRatio = gradientRatio,
    rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(
      scrollableState = state,
      layoutInfoProvider = state.toRotarySnapLayoutInfoProvider(),
      hapticFeedbackEnabled = hapticFeedbackEnabled
    )
  ) {
    Text(
      modifier = Modifier.wrapContentSize(),
      textAlign = TextAlign.Center,
      color = textColor,
      style = textStyle,
      text = buildAnnotatedString {
        withStyle(style = SpanStyle(fontFeatureSettings = "tnum")) {
          append(items[it].toString())
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
private fun PickerState.toRotarySnapLayoutInfoProvider(): RotarySnapLayoutInfoProvider =
  remember(this) { PickerRotarySnapLayoutInfoProvider(this) }

private class PickerRotarySnapLayoutInfoProvider(scrollableState: PickerState) :
  RotarySnapLayoutInfoProvider {

  private val scalingLazyListState = accessScalingLazyListState(scrollableState)!!

  /** Returns a height of a first item, as all items in picker have the same height. */
  override val averageItemSize: Float
    get() =
      scalingLazyListState.layoutInfo.visibleItemsInfo
        .firstOrNull()
        ?.unadjustedSize
        ?.toFloat() ?: 0f

  /** Current (centred) item index. */
  override val currentItemIndex: Int
    get() = scalingLazyListState.centerItemIndex

  /** An offset from the item centre. */
  override val currentItemOffset: Float
    get() = scalingLazyListState.centerItemScrollOffset.toFloat()

  override val totalItemCount: Int
    get() = scalingLazyListState.layoutInfo.totalItemsCount

  private fun accessScalingLazyListState(pickerState: PickerState): ScalingLazyListState? {
    return try {
      val field: Field = PickerState::class.java.getDeclaredField(
        "scalingLazyListState"
      )
      field.isAccessible = true
      field.get(pickerState) as? ScalingLazyListState
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}