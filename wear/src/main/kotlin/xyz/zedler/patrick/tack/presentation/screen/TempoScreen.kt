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

package xyz.zedler.patrick.tack.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PickerState
import androidx.wear.compose.material3.rememberPickerState
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.components.AnimatedIcon
import xyz.zedler.patrick.tack.presentation.components.TempoPicker
import xyz.zedler.patrick.tack.presentation.components.TextIconButton
import xyz.zedler.patrick.tack.presentation.state.MainState
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.isSmallScreen
import xyz.zedler.patrick.tack.util.spToDp
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun TempoScreen(
  viewModel: MainViewModel = MainViewModel()
) {
  TackTheme {
    val state by viewModel.state.collectAsState()
    val pickerCoroutineScope = rememberCoroutineScope()
    var pickerOption = remember { state.tempo - 1 }
    val pickerState = rememberPickerState(
      initialNumberOfOptions = Constants.TEMPO_MAX,
      initiallySelectedIndex = pickerOption,
      shouldRepeatOptions = false
    )
    fun safelyAnimateToOption(index: Int) {
      val safeIndex = index.coerceIn(Constants.TEMPO_MIN - 1, Constants.TEMPO_MAX - 1)
      pickerCoroutineScope.launch {
        if (state.reduceAnim) {
          pickerState.scrollToOption(safeIndex)
        } else {
          pickerState.animateScrollToOption(safeIndex)
        }
      }
    }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background)
    ) {
      ConstraintLayout(modifier = Modifier.fillMaxWidth().weight(1f)) {
        val (tempoPicker) = createRefs()
        val (plus5Button, minus5Button, plus10Button, minus10Button) = createRefs()
        CenterPicker(
          mainState = state,
          pickerState = pickerState,
          onOptionChange = {
            pickerOption = it
            viewModel.updateTempo(it + 1)
          },
          modifier = Modifier.constrainAs(tempoPicker) {
            top.linkTo(parent.top, margin = 24.dp)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }
        )
        TextIconButton(
          label = "-5",
          reduceAnim = state.reduceAnim,
          onClick = {
            safelyAnimateToOption(state.tempo - 6)
          },
          modifier = Modifier.constrainAs(minus5Button) {
            top.linkTo(parent.top, margin = 40.dp)
            bottom.linkTo(minus10Button.top)
            start.linkTo(parent.start, margin = 12.dp)
            end.linkTo(tempoPicker.start)
          }
        )
        TextIconButton(
          label = "-10",
          reduceAnim = state.reduceAnim,
          onClick = {
            safelyAnimateToOption(state.tempo - 11)
          },
          modifier = Modifier.constrainAs(minus10Button) {
            top.linkTo(minus5Button.bottom)
            bottom.linkTo(parent.bottom, margin = 0.dp)
            start.linkTo(parent.start, margin = 12.dp)
            end.linkTo(tempoPicker.start)
          }
        )
        TextIconButton(
          label = "+5",
          reduceAnim = state.reduceAnim,
          onClick = {
            safelyAnimateToOption(state.tempo + 4)
          },
          modifier = Modifier.constrainAs(plus5Button) {
            top.linkTo(parent.top, margin = 40.dp)
            bottom.linkTo(plus10Button.top)
            start.linkTo(tempoPicker.end)
            end.linkTo(parent.end, margin = 12.dp)
          }
        )
        TextIconButton(
          label = "+10",
          reduceAnim = state.reduceAnim,
          onClick = {
            safelyAnimateToOption(state.tempo + 9)
          },
          modifier = Modifier.constrainAs(plus10Button) {
            top.linkTo(plus5Button.bottom)
            bottom.linkTo(parent.bottom, margin = 0.dp)
            start.linkTo(tempoPicker.end)
            end.linkTo(parent.end, margin = 12.dp)
          }
        )
      }
      TapButton(
        state = state,
        onClick = {
          safelyAnimateToOption(viewModel.tempoTap() - 1)
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
      )
    }
  }
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun TempoScreenSmall() {
  TempoScreen()
}

@Composable
fun CenterPicker(
  mainState: MainState,
  pickerState: PickerState,
  onOptionChange: (Int) -> Unit,
  modifier: Modifier
) {
  LaunchedEffect(pickerState.selectedOptionIndex) {
    onOptionChange(pickerState.selectedOptionIndex)
  }
  TempoPicker(
    state = pickerState,
    modifier = modifier.size(
      spToDp(spValue = if (isSmallScreen()) 64 else 72),
      spToDp(spValue = if (isSmallScreen()) 104 else 140)
    ),
    verticalSpacing = if (isSmallScreen()) (-4).dp else (-6).dp,
    textStyle = MaterialTheme.typography.displayMedium.copy(
      fontSize = if (isSmallScreen()) 24.sp else 32.sp
    ),
    hapticFeedbackEnabled = !mainState.isPlaying ||
        (!mainState.beatModeVibrate && !mainState.alwaysVibrate)
  )
}

@Composable
fun TapButton(
  state: MainState,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(false) }
  EdgeButton(
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.tertiary,
      contentColor = MaterialTheme.colorScheme.onTertiary
    ),
    buttonSize = if (isSmallScreen()) EdgeButtonSize.ExtraSmall else EdgeButtonSize.Small,
    onClick = {},
    modifier = modifier.pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press) {
              onClick()
              animTrigger.value = !animTrigger.value
            }
          }
        }
      }
  ) {
    AnimatedIcon(
      resId = R.drawable.ic_rounded_touch_app_anim,
      description = stringResource(id = R.string.wear_action_tempo_tap),
      trigger = animTrigger.value,
      animated = !state.reduceAnim
    )
  }
}