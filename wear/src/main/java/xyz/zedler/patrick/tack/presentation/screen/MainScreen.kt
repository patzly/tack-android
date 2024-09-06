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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavBackStackEntry
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.components.WrapContentCard
import xyz.zedler.patrick.tack.presentation.dialog.PermissionDialog
import xyz.zedler.patrick.tack.presentation.dialog.VolumeDialog
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.AnimatedVectorDrawable
import xyz.zedler.patrick.tack.util.accessScalingLazyListState
import xyz.zedler.patrick.tack.util.isSmallScreen
import xyz.zedler.patrick.tack.util.spToDp
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun MainScreen(
  viewModel: MainViewModel = MainViewModel(),
  backStackEntry: NavBackStackEntry? = null,
  onTempoCardClick: () -> Unit = {},
  onSettingsButtonClick: () -> Unit = {},
  onBeatsButtonClick: () -> Unit = {},
  onPermissionRequestClick: () -> Unit = {}
) {
  TackTheme {
    val flashTrigger by viewModel.flashTrigger.observeAsState(false)
    val flashStrongTrigger by viewModel.flashStrongTrigger.observeAsState(false)
    val background = if (flashStrongTrigger) {
      MaterialTheme.colorScheme.error
    } else if (flashTrigger) {
      MaterialTheme.colorScheme.tertiary
    } else {
      MaterialTheme.colorScheme.background
    }

    val keepAwake by viewModel.keepAwake.observeAsState(Constants.Def.KEEP_AWAKE)
    val isPlaying by viewModel.isPlaying.observeAsState(false)
    val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)
    val controlsAlpha by animateFloatAsState(
      targetValue = if (isPlaying && keepAwake) .5f else 1f,
      label = "controlsAlpha",
      animationSpec = TweenSpec(durationMillis = if (reduceAnim) 0 else 300)
    )

    ScreenScaffold(
      timeText = {
        TimeText(
          timeTextStyle = MaterialTheme.typography.labelMedium,
          modifier = Modifier.graphicsLayer(alpha = controlsAlpha)
        )
      },
      modifier = Modifier.background(color = background)
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        val playAnimTrigger = remember { mutableStateOf(isPlaying) }
        var showVolumeDialog by remember { mutableStateOf(false) }
        val showPermissionDialog by viewModel.showPermissionDialog.observeAsState(false)

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
          val (settingsButton, tempoCard, playButton) = createRefs()
          val (beatsButton, tempoTapButton) = createRefs()
          val (bookmarkButton, beatModeButton) = createRefs()

          val tempo by viewModel.tempo.observeAsState(initial = Constants.Def.TEMPO)
          val pickerOption = remember { tempo - 1 }
          val pickerCoroutineScope = rememberCoroutineScope()
          val pickerState = rememberPickerState(
            initialNumberOfOptions = Constants.TEMPO_MAX,
            initiallySelectedOption = pickerOption,
            repeatItems = false
          )
          LaunchedEffect(tempo) {
            if (!viewModel.tempoChangedByPicker) {
              pickerCoroutineScope.launch {
                if (viewModel.animateTempoChange && !reduceAnim) {
                  pickerState.animateScrollToOption(tempo - 1)
                } else {
                  pickerState.scrollToOption(tempo - 1)
                }
              }
            }
          }
          LaunchedEffect(pickerState.selectedOption) {
            viewModel.changeTempo(pickerState.selectedOption + 1, picker = true)
          }

          val beatModeVibrate by viewModel.beatModeVibrate.observeAsState(
            Constants.Def.BEAT_MODE_VIBRATE
          )
          val alwaysVibrate by viewModel.alwaysVibrate.observeAsState(Constants.Def.ALWAYS_VIBRATE)
          val gain by viewModel.gain.observeAsState(Constants.Def.GAIN)

          SettingsButton(
            onClick = onSettingsButtonClick,
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(settingsButton) {
                top.linkTo(parent.top, margin = 16.dp)
                bottom.linkTo(tempoCard.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
              }
          )
          TempoCard(
            viewModel = viewModel,
            state = pickerState,
            onClick = onTempoCardClick,
            modifier = Modifier.constrainAs(tempoCard) {
              top.linkTo(parent.top)
              bottom.linkTo(parent.bottom)
              start.linkTo(parent.start)
              end.linkTo(parent.end)
            }
          )
          PlayButton(
            viewModel = viewModel,
            animTrigger = playAnimTrigger,
            onClick = {
              val startedWithGain = viewModel.metronomeUtil?.neverStartedWithGainBefore() == false
              if (isPlaying || (gain == 0 || startedWithGain)) {
                if (viewModel.togglePlaying()) {
                  playAnimTrigger.value = !playAnimTrigger.value
                }
              } else {
                showVolumeDialog = true
              }
            },
            modifier = Modifier.constrainAs(playButton) {
              top.linkTo(tempoCard.bottom)
              bottom.linkTo(parent.bottom)
              start.linkTo(parent.start)
              end.linkTo(parent.end)
            }
          )
          BeatsButton(
            onClick = onBeatsButtonClick,
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(beatsButton) {
                top.linkTo(parent.top, margin = 40.dp)
                bottom.linkTo(tempoTapButton.top)
                start.linkTo(parent.start)
                end.linkTo(playButton.start)
              }
          )
          TempoTapButton(
            viewModel = viewModel,
            onClick = {
              viewModel.tempoTap()
            },
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(tempoTapButton) {
                top.linkTo(beatsButton.bottom)
                bottom.linkTo(parent.bottom, margin = 40.dp)
                start.linkTo(parent.start)
                end.linkTo(playButton.start)
              }
          )
          BookmarkButton(
            viewModel = viewModel,
            onClick = {
              viewModel.toggleBookmark()
            },
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(bookmarkButton) {
                top.linkTo(parent.top, margin = 40.dp)
                bottom.linkTo(beatModeButton.top)
                start.linkTo(playButton.end)
                end.linkTo(parent.end)
              }
          )
          BeatModeButton(
            viewModel = viewModel,
            beatModeVibrate = beatModeVibrate,
            alwaysVibrate = alwaysVibrate,
            onClick = {
              viewModel.toggleBeatModeVibrate()
            },
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(beatModeButton) {
                top.linkTo(bookmarkButton.bottom)
                bottom.linkTo(parent.bottom, margin = 40.dp)
                start.linkTo(playButton.end)
                end.linkTo(parent.end)
              }
          )
        }
        VolumeDialog(
          show = showVolumeDialog,
          onDismissRequest = {
            showVolumeDialog = false
          },
          onConfirm = {
            viewModel.togglePlaying()
            playAnimTrigger.value = !playAnimTrigger.value
            showVolumeDialog = false
          },
          onDismiss = {
            viewModel.changeGain(0)
            viewModel.togglePlaying()
            playAnimTrigger.value = !playAnimTrigger.value
            showVolumeDialog = false
          }
        )
        PermissionDialog(
          show = showPermissionDialog,
          onDismissRequest = {
            viewModel.changeShowPermissionDialog(false)
          },
          onRetry = {
            viewModel.changeShowPermissionDialog(false)
            onPermissionRequestClick()
          },
          onDismiss = {
            viewModel.changeShowPermissionDialog(false)
          }
        )
      }
    }
  }
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
fun MainScreenSmall() {
  MainScreen()
}

@Composable
fun TempoCard(
  viewModel: MainViewModel,
  state: PickerState,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val isPlaying by viewModel.isPlaying.observeAsState(false)
  val keepAwake by viewModel.keepAwake.observeAsState(Constants.Def.KEEP_AWAKE)
  val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)

  val borderColorTarget = if (isPlaying && keepAwake) {
    MaterialTheme.colorScheme.background
  } else {
    MaterialTheme.colorScheme.outline
  }
  val borderColor by animateColorAsState(
    targetValue = borderColorTarget,
    label = "borderColor",
    animationSpec = TweenSpec(durationMillis = if (reduceAnim) 0 else 250)
  )

  val backgroundColorTarget = if (isPlaying && keepAwake) {
    MaterialTheme.colorScheme.background
  } else {
    MaterialTheme.colorScheme.surfaceContainerHigh
  }
  val backgroundColor by animateColorAsState(
    targetValue = backgroundColorTarget,
    label = "backgroundColor",
    animationSpec = TweenSpec(durationMillis = if (reduceAnim) 0 else 250)
  )

  WrapContentCard(
    onClick = onClick,
    modifier = modifier.wrapContentWidth(),
    border = BorderStroke(2.dp, borderColor),
    backgroundPainter = ColorPainter(backgroundColor),
    shape = MaterialTheme.shapes.extraLarge,
    contentPadding = PaddingValues(0.dp)
  ) {
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

    val minRatio = 0.001f // 0 would cause a small y-offset
    val gradientRatio by animateFloatAsState(
      targetValue = if (isPlaying && keepAwake) 0.2f else minRatio,
      label = "gradientRatio",
      animationSpec = TweenSpec(durationMillis = if (reduceAnim) 0 else 250)
    )
    val pickerAlpha by animateFloatAsState(
      targetValue = if (isPlaying && keepAwake) .5f else 1f,
      label = "pickerAlpha",
      animationSpec = TweenSpec(durationMillis = if (reduceAnim) 0 else 300)
    )

    Picker(
      gradientRatio = if (gradientRatio > minRatio) gradientRatio else minRatio,
      state = state,
      contentDescription = contentDescription,
      modifier = Modifier
        .graphicsLayer(alpha = pickerAlpha)
        .size(
          spToDp(spValue = if (isSmallScreen()) 80 else 94),
          spToDp(spValue = if (isSmallScreen()) 48 else 56)
        )
        .rotaryScrollable(
          behavior = RotaryScrollableDefaults.snapBehavior(
            scrollableState = accessScalingLazyListState(state)!!,
            hapticFeedbackEnabled = !isPlaying || (!beatModeVibrate && !alwaysVibrate)
          ),
          focusRequester = rememberActiveFocusRequester()
        )
    ) {
      Text(
        modifier = Modifier.wrapContentSize(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.displayMedium.copy(
          fontSize = if (isSmallScreen()) 30.sp else 36.sp
        ),
        text = buildAnnotatedString {
          withStyle(style = SpanStyle(fontFeatureSettings = "tnum")) {
            append(items[it].toString())
          }
        }
      )
    }
  }
}

@Composable
fun PlayButton(
  viewModel: MainViewModel,
  animTrigger: MutableState<Boolean>,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val isPlaying by viewModel.isPlaying.observeAsState(false)
  val keepAwake by viewModel.keepAwake.observeAsState(Constants.Def.KEEP_AWAKE)
  val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)

  val containerColorTarget = if (isPlaying && keepAwake) {
    MaterialTheme.colorScheme.background
  } else {
    IconButtonDefaults.filledIconButtonColors().containerColor
  }
  val containerColor by animateColorAsState(
    targetValue = containerColorTarget,
    label = "containerColor",
    animationSpec = TweenSpec(durationMillis = if (reduceAnim) 0 else 300)
  )

  val contentColorTarget = if (isPlaying && keepAwake) {
    MaterialTheme.colorScheme.primaryDim
  } else {
    IconButtonDefaults.filledIconButtonColors().contentColor
  }
  val contentColor by animateColorAsState(
    targetValue = contentColorTarget,
    label = "contentColor",
    animationSpec = TweenSpec(durationMillis = if (reduceAnim) 0 else 300)
  )

  val borderColorTarget = if (isPlaying && keepAwake) {
    MaterialTheme.colorScheme.outlineVariant
  } else {
    IconButtonDefaults.filledIconButtonColors().containerColor
  }
  val borderColor by animateColorAsState(
    targetValue = borderColorTarget,
    label = "borderColor",
    animationSpec = TweenSpec(durationMillis = if (reduceAnim) 0 else 300)
  )

  val interactionSource = remember { MutableInteractionSource() }
  FilledIconButton(
    onClick = onClick,
    colors = IconButtonDefaults.filledIconButtonColors(
      containerColor = containerColor
    ),
    shape = if (reduceAnim) {
      IconButtonDefaults.shape
    } else {
      IconButtonDefaults.animatedShape(
        interactionSource = interactionSource,
        pressedShape = MaterialTheme.shapes.medium
      )
    },
    border = BorderStroke(2.dp, borderColor),
    interactionSource = interactionSource,
    modifier = modifier
  ) {
    AnimatedVectorDrawable(
      resId1 = R.drawable.ic_rounded_play_to_stop_anim,
      resId2 = R.drawable.ic_rounded_stop_to_play_anim,
      description = stringResource(id = R.string.wear_action_play_stop),
      color = contentColor,
      trigger = animTrigger,
      modifier = Modifier.size(IconButtonDefaults.LargeIconSize),
      animated = !reduceAnim
    )
  }
}

@Composable
fun SettingsButton(
  onClick: () -> Unit,
  modifier: Modifier
) {
  IconButton(
    onClick = onClick,
    modifier = modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_rounded_settings),
      contentDescription = stringResource(id = R.string.wear_title_settings),
      tint = IconButtonDefaults.iconButtonColors().contentColor
    )
  }
}

@Composable
fun BeatsButton(
  onClick: () -> Unit,
  modifier: Modifier
) {
  IconButton(
    onClick = onClick,
    modifier = modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_rounded_steppers),
      contentDescription = stringResource(id = R.string.wear_title_beats),
      tint = IconButtonDefaults.iconButtonColors().contentColor
    )
  }
}

@Composable
fun TempoTapButton(
  viewModel: MainViewModel,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(false) }
  val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)
  IconButton(
    onClick = {},
    modifier = modifier
      .touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
      .pointerInput(Unit) {
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
      color = IconButtonDefaults.iconButtonColors().contentColor,
      trigger = animTrigger.value,
      animated = !reduceAnim
    )
  }
}

@Composable
fun BookmarkButton(
  viewModel: MainViewModel,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(false) }
  val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)
  IconButton(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    modifier = modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
  ) {
    AnimatedVectorDrawable(
      resId = R.drawable.ic_rounded_bookmark_anim,
      description = stringResource(id = R.string.wear_action_bookmark),
      color = IconButtonDefaults.iconButtonColors().contentColor,
      trigger = animTrigger.value,
      animated = !reduceAnim
    )
  }
}

@Composable
fun BeatModeButton(
  viewModel: MainViewModel,
  beatModeVibrate: Boolean,
  alwaysVibrate: Boolean,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(beatModeVibrate) }
  val reduceAnim by viewModel.reduceAnim.observeAsState(Constants.Def.REDUCE_ANIM)
  IconButton(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    modifier = modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
  ) {
    val resId1 = if (alwaysVibrate) {
      R.drawable.ic_rounded_volume_off_to_volume_up_anim
    } else {
      R.drawable.ic_rounded_vibration_to_volume_up_anim
    }
    val resId2 = if (alwaysVibrate) {
      R.drawable.ic_rounded_volume_up_to_volume_off_anim
    } else {
      R.drawable.ic_rounded_volume_up_to_vibration_anim
    }
    AnimatedVectorDrawable(
      resId1 = resId2,
      resId2 = resId1,
      description = stringResource(id = R.string.wear_action_beat_mode),
      color = IconButtonDefaults.iconButtonColors().contentColor,
      trigger = animTrigger,
      animated = !reduceAnim
    )
  }
}