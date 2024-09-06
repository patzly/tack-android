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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.components.TextIconButton
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.AnimatedVectorDrawable
import xyz.zedler.patrick.tack.util.accessScalingLazyListState
import xyz.zedler.patrick.tack.util.isSmallScreen
import xyz.zedler.patrick.tack.util.spToDp
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun TempoScreen(viewModel: MainViewModel = MainViewModel()) {
  TackTheme {
    val tempo by viewModel.tempo.observeAsState(initial = Constants.Def.TEMPO)
    val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)
    val pickerCoroutineScope = rememberCoroutineScope()
    var pickerOption = remember { tempo - 1 }
    val pickerState = rememberPickerState(
      initialNumberOfOptions = Constants.TEMPO_MAX,
      initiallySelectedOption = pickerOption,
      repeatItems = false
    )
    fun safelyAnimateToOption(index: Int) {
      val safeIndex = index.coerceIn(Constants.TEMPO_MIN - 1, Constants.TEMPO_MAX - 1)
      pickerCoroutineScope.launch {
        if (reduceAnim) {
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
      ConstraintLayout(
        modifier = Modifier.fillMaxWidth().weight(1f)
      ) {
        val (tempoPicker) = createRefs()
        val (plus5Button, minus5Button, plus10Button, minus10Button) = createRefs()
        TempoPicker(
          viewModel = viewModel,
          state = pickerState,
          onOptionChange = {
            pickerOption = it
            viewModel.changeTempo(it + 1)
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
          reduceAnim = reduceAnim,
          onClick = {
            safelyAnimateToOption(pickerOption - 5)
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
          reduceAnim = reduceAnim,
          onClick = {
            safelyAnimateToOption(pickerOption - 10)
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
          reduceAnim = reduceAnim,
          onClick = {
            safelyAnimateToOption(pickerOption + 5)
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
          reduceAnim = reduceAnim,
          onClick = {
            safelyAnimateToOption(pickerOption + 10)
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
        viewModel = viewModel,
        onClick = {
          safelyAnimateToOption(viewModel.tempoTap() - 1)
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
      )
    }
  }
}

@Composable
fun TempoPicker(
  viewModel: MainViewModel,
  state: PickerState,
  onOptionChange: (Int) -> Unit,
  modifier: Modifier
) {
  val isPlaying by viewModel.isPlaying.observeAsState(false)
  val beatModeVibrate by viewModel.beatModeVibrate.observeAsState(
    Constants.Def.BEAT_MODE_VIBRATE
  )
  val alwaysVibrate by viewModel.alwaysVibrate.observeAsState(Constants.Def.ALWAYS_VIBRATE)

  val items = (Constants.TEMPO_MIN..Constants.TEMPO_MAX).toList()
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
      .size(spToDp(spValue = 64), spToDp(spValue = 104))
      .rotaryScrollable(
        behavior = RotaryScrollableDefaults.snapBehavior(
          scrollableState = accessScalingLazyListState(state)!!,
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
  viewModel: MainViewModel,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(false) }
  val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)
  EdgeButton(
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.tertiary
    ),
    buttonHeight = if (isSmallScreen()) {
      ButtonDefaults.EdgeButtonHeightExtraSmall
    } else {
      ButtonDefaults.EdgeButtonHeightSmall
    },
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
    AnimatedVectorDrawable(
      resId = R.drawable.ic_rounded_touch_app_anim,
      description = stringResource(id = R.string.wear_action_tempo_tap),
      color = MaterialTheme.colorScheme.onTertiary,
      trigger = animTrigger.value,
      animated = !reduceAnim
    )
  }
}