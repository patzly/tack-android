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

package xyz.zedler.patrick.tack.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryDefaults
import androidx.wear.compose.foundation.rotary.rotary
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.components.TextIconButton
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.AnimatedVectorDrawable
import xyz.zedler.patrick.tack.util.accessScalingLazyListState
import xyz.zedler.patrick.tack.util.spToDp
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun TempoScreen(viewModel: MainViewModel = MainViewModel()) {
  TackTheme {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.Center
    ) {
      TimeText(
        timeTextStyle = MaterialTheme.typography.labelMedium
      )
      ConstraintLayout(
        modifier = Modifier.fillMaxSize()
      ) {
        val (tempoPicker, tapButton) = createRefs()
        val (plus5Button, minus5Button, plus10Button, minus10Button) = createRefs()

        var pickerOption = viewModel.tempo.value?.minus(1) ?: Constants.DEF.TEMPO
        val pickerCoroutineScope = rememberCoroutineScope()
        val pickerState = rememberPickerState(
          initialNumberOfOptions = 400,
          initiallySelectedOption = pickerOption,
          repeatItems = false
        )

        TempoPicker(
          viewModel = viewModel,
          state = pickerState,
          onOptionChange = {
            pickerOption = it
            viewModel.changeTempo(it + 1)
          },
          modifier = Modifier.constrainAs(tempoPicker) {
            top.linkTo(parent.top, margin = 32.dp)
            bottom.linkTo(tapButton.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }
        )
        TapButton(
          onClick = {
            val tempo = viewModel.tempoTap()
            pickerCoroutineScope.launch {
              pickerState.animateScrollToOption(tempo - 1)
            }
          },
          modifier = Modifier.constrainAs(tapButton) {
            top.linkTo(tempoPicker.bottom)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }
        )
        TextIconButton(
          label = "-5",
          onClick = {
            pickerCoroutineScope.launch {
              pickerState.animateScrollToOption(pickerOption - 5)
            }
          },
          modifier = Modifier.constrainAs(minus5Button) {
            top.linkTo(parent.top, margin = 40.dp)
            bottom.linkTo(minus10Button.top)
            start.linkTo(parent.start)
            end.linkTo(tapButton.start)
          }
        )
        TextIconButton(
          label = "-10",
          onClick = {
            pickerCoroutineScope.launch {
              pickerState.animateScrollToOption(pickerOption - 10)
            }
          },
          modifier = Modifier.constrainAs(minus10Button) {
            top.linkTo(minus5Button.bottom)
            bottom.linkTo(parent.bottom, margin = 40.dp)
            start.linkTo(parent.start)
            end.linkTo(tapButton.start)
          }
        )
        TextIconButton(
          label = "+5",
          onClick = {
            pickerCoroutineScope.launch {
              pickerState.animateScrollToOption(pickerOption + 5)
            }
          },
          modifier = Modifier.constrainAs(plus5Button) {
            top.linkTo(parent.top, margin = 40.dp)
            bottom.linkTo(minus10Button.top)
            start.linkTo(tapButton.end)
            end.linkTo(parent.end)
          }
        )
        TextIconButton(
          label = "+10",
          onClick = {
            pickerCoroutineScope.launch {
              pickerState.animateScrollToOption(pickerOption + 10)
            }
          },
          modifier = Modifier.constrainAs(plus10Button) {
            top.linkTo(minus5Button.bottom)
            bottom.linkTo(parent.bottom, margin = 40.dp)
            start.linkTo(tapButton.end)
            end.linkTo(parent.end)
          }
        )
      }
    }
  }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun TempoPicker(
  viewModel: MainViewModel,
  state: PickerState,
  onOptionChange: (Int) -> Unit,
  modifier: Modifier
) {
  val isPlaying by viewModel.isPlaying.observeAsState(false)
  val beatModeVibrate by viewModel.beatModeVibrate.observeAsState(
    Constants.DEF.BEAT_MODE_VIBRATE
  )
  val alwaysVibrate by viewModel.alwaysVibrate.observeAsState(Constants.DEF.ALWAYS_VIBRATE)

  val items = (1..400).toList()
  val bpm = stringResource(
    id = R.string.wear_label_bpm_value,
    state.selectedOption + 1
  )
  val contentDescription by remember { derivedStateOf { bpm } }

  LaunchedEffect(state.selectedOption) {
    onOptionChange(state.selectedOption)
  }
  Picker(
    state = state,
    contentDescription = contentDescription,
    modifier = modifier
      .size(spToDp(spValue = 88), spToDp(spValue = 104))
      .rotary(
        rotaryBehavior = RotaryDefaults.snapBehavior(
          state = accessScalingLazyListState(state)!!,
          hapticFeedbackEnabled = !isPlaying || (!beatModeVibrate && !alwaysVibrate)
        ),
        focusRequester = rememberActiveFocusRequester()
      )
  ) {
    Text(
      modifier = Modifier.wrapContentWidth(),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onBackground,
      style = MaterialTheme.typography.displayMedium,
      text = buildAnnotatedString {
        withStyle(style = SpanStyle(fontFeatureSettings = "tnum")) {
          append(items[it].toString())
        }
      }
    )
  }
}

@Composable
fun TapButton(
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(false) }
  IconButton(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    modifier = modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
  ) {
    AnimatedVectorDrawable(
      resId = R.drawable.ic_round_touch_app_anim,
      description = stringResource(id = R.string.wear_action_tempo_tap),
      color = IconButtonDefaults.iconButtonColors().contentColor,
      trigger = animTrigger
    )
  }
}