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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PickerState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.rememberPickerState
import androidx.wear.compose.material3.touchTargetAwareSize
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.components.AnimatedIcon
import xyz.zedler.patrick.tack.presentation.components.TempoPicker
import xyz.zedler.patrick.tack.presentation.components.WrapContentCard
import xyz.zedler.patrick.tack.presentation.dialog.PermissionDialog
import xyz.zedler.patrick.tack.presentation.dialog.VolumeDialog
import xyz.zedler.patrick.tack.presentation.state.MainState
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.isSmallScreen
import xyz.zedler.patrick.tack.util.spToDp
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun MainScreen(
  viewModel: MainViewModel = MainViewModel(),
  onTempoCardClick: () -> Unit = {},
  onSettingsButtonClick: () -> Unit = {},
  onBeatsButtonClick: () -> Unit = {},
  onTempoTapButtonClick: () -> Unit = {},
  onBookmarksButtonClick: () -> Unit = {},
  onPermissionRequestClick: () -> Unit = {}
) {
  TackTheme {
    val state by viewModel.state.collectAsState()

    val background = if (state.flashStrong) {
      MaterialTheme.colorScheme.error
    } else if (state.flash) {
      MaterialTheme.colorScheme.primary
    } else {
      MaterialTheme.colorScheme.background
    }

    val controlsAlpha by animateFloatAsState(
      targetValue = if (state.isPlaying && state.keepAwake) .5f else 1f,
      label = "controlsAlpha",
      animationSpec = TweenSpec(durationMillis = if (state.reduceAnim) 0 else 300)
    )

    ScreenScaffold(
      modifier = Modifier.background(color = background)
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        var showVolumeDialog by remember { mutableStateOf(false) }

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
          val (settingsButton, tempoCard, playButton) = createRefs()
          val (beatsButton, tempoTapButton) = createRefs()
          val (bookmarksButton, beatModeButton) = createRefs()

          val pickerOption = remember { state.tempo - 1 }
          val pickerCoroutineScope = rememberCoroutineScope()
          val pickerState = rememberPickerState(
            initialNumberOfOptions = Constants.TEMPO_MAX,
            initiallySelectedIndex = pickerOption,
            shouldRepeatOptions = false
          )
          LaunchedEffect(state.tempo) {
            if (!state.tempoChangedByPicker) {
              pickerCoroutineScope.launch {
                if (state.animateTempoChange && !state.reduceAnim) {
                  pickerState.animateScrollToOption(state.tempo - 1)
                } else {
                  pickerState.scrollToOption(state.tempo - 1)
                }
              }
            }
          }
          LaunchedEffect(pickerState.selectedOptionIndex) {
            viewModel.updateTempo(pickerState.selectedOptionIndex + 1, picker = true)
          }

          IconButton(
            onClick = onSettingsButtonClick,
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(settingsButton) {
                top.linkTo(parent.top, margin = 16.dp)
                bottom.linkTo(tempoCard.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
              }
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_rounded_settings),
              contentDescription = stringResource(id = R.string.wear_title_settings)
            )
          }
          TempoCard(
            mainState = state,
            pickerState = pickerState,
            onClick = onTempoCardClick,
            modifier = Modifier.constrainAs(tempoCard) {
              top.linkTo(parent.top)
              bottom.linkTo(parent.bottom)
              start.linkTo(parent.start)
              end.linkTo(parent.end)
            }
          )
          PlayButton(
            state = state,
            animTrigger = state.isPlaying,
            onClick = {
              if (state.isPlaying || (state.gain == 0 || state.startedWithGain)) {
                viewModel.togglePlaying()
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
          IconButton(
            onClick = onBeatsButtonClick,
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(beatsButton) {
                top.linkTo(parent.top, margin = 40.dp)
                bottom.linkTo(tempoTapButton.top)
                start.linkTo(parent.start)
                end.linkTo(playButton.start)
              }
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_rounded_steppers),
              contentDescription = stringResource(id = R.string.wear_title_beats)
            )
          }
          IconButton(
            onClick = onTempoTapButtonClick,
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(tempoTapButton) {
                top.linkTo(beatsButton.bottom)
                bottom.linkTo(parent.bottom, margin = 40.dp)
                start.linkTo(parent.start)
                end.linkTo(playButton.start)
              }
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_rounded_touch_app),
              contentDescription = stringResource(id = R.string.wear_action_tempo_tap)
            )
          }
          IconButton(
            onClick = onBookmarksButtonClick,
            onLongClick = {
              viewModel.circulateThroughBookmarks()
            },
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(bookmarksButton) {
                top.linkTo(parent.top, margin = 40.dp)
                bottom.linkTo(beatModeButton.top)
                start.linkTo(playButton.end)
                end.linkTo(parent.end)
              }
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_rounded_bookmarks),
              contentDescription = stringResource(id = R.string.wear_title_bookmarks)
            )
          }
          BeatModeButton(
            state = state,
            beatModeVibrate = state.beatModeVibrate,
            alwaysVibrate = state.alwaysVibrate,
            onClick = {
              viewModel.updateBeatModeVibrate(!state.beatModeVibrate)
            },
            modifier = Modifier
              .graphicsLayer(alpha = controlsAlpha)
              .constrainAs(beatModeButton) {
                top.linkTo(bookmarksButton.bottom)
                bottom.linkTo(parent.bottom, margin = 40.dp)
                start.linkTo(playButton.end)
                end.linkTo(parent.end)
              }
          )
        }
        VolumeDialog(
          visible = showVolumeDialog,
          onConfirm = {
            viewModel.togglePlaying()
            showVolumeDialog = false
          },
          onDismiss = {
            viewModel.updateGain(0)
            viewModel.togglePlaying()
            showVolumeDialog = false
          },
          onSwipeDismiss = {
            showVolumeDialog = false
          }
        )
        PermissionDialog(
          visible = state.showPermissionDialog,
          onConfirm = {
            viewModel.updateShowPermissionDialog(false)
            onPermissionRequestClick()
          },
          onDismiss = {
            viewModel.updateShowPermissionDialog(false)
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
  mainState: MainState,
  pickerState: PickerState,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val ambient = mainState.isPlaying && mainState.keepAwake
  val borderColorTarget = if (ambient) {
    MaterialTheme.colorScheme.background
  } else {
    MaterialTheme.colorScheme.outline
  }
  val borderColor by animateColorAsState(
    targetValue = borderColorTarget,
    label = "borderColor",
    animationSpec = TweenSpec(durationMillis = if (mainState.reduceAnim) 0 else 250)
  )

  WrapContentCard(
    onClick = onClick,
    modifier = modifier.wrapContentWidth(),
    border = BorderStroke(2.dp, borderColor),
    shape = MaterialTheme.shapes.extraLarge,
    contentPadding = PaddingValues(0.dp)
  ) {
    val minRatio = 0.001f // 0 would cause a small y-offset
    val gradientRatio by animateFloatAsState(
      targetValue = if (ambient) 0.2f else 0f,
      label = "gradientRatio",
      animationSpec = TweenSpec(durationMillis = if (mainState.reduceAnim) 0 else 250)
    )
    val pickerAlpha by animateFloatAsState(
      targetValue = if (ambient) .5f else 1f,
      label = "pickerAlpha",
      animationSpec = TweenSpec(durationMillis = if (mainState.reduceAnim) 0 else 300)
    )

    TempoPicker(
      state = pickerState,
      modifier = Modifier
        .graphicsLayer(alpha = pickerAlpha)
        .size(
          spToDp(spValue = if (isSmallScreen()) 76 else 100),
          spToDp(spValue = if (isSmallScreen()) 44 else 56)
        ),
      verticalSpacing = if (isSmallScreen()) (-8).dp else (-7).dp,
      textColor = MaterialTheme.colorScheme.onSurface,
      textStyle = MaterialTheme.typography.displayMedium.copy(
        fontSize = if (isSmallScreen()) 30.sp else 40.sp
      ),
      gradientRatio = if (gradientRatio > minRatio) gradientRatio else minRatio,
      hapticFeedbackEnabled = !mainState.isPlaying ||
          (!mainState.beatModeVibrate && !mainState.alwaysVibrate)
    )
  }
}

@Composable
fun PlayButton(
  state: MainState,
  animTrigger: Boolean,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val ambient = state.isPlaying && state.keepAwake
  val containerColorTarget = if (ambient) {
    MaterialTheme.colorScheme.background
  } else {
    IconButtonDefaults.filledIconButtonColors().containerColor
  }
  val containerColor by animateColorAsState(
    targetValue = containerColorTarget,
    label = "containerColor",
    animationSpec = TweenSpec(durationMillis = if (state.reduceAnim) 0 else 300)
  )

  val contentColorTarget = if (ambient) {
    MaterialTheme.colorScheme.primaryDim
  } else {
    IconButtonDefaults.filledIconButtonColors().contentColor
  }
  val contentColor by animateColorAsState(
    targetValue = contentColorTarget,
    label = "contentColor",
    animationSpec = TweenSpec(durationMillis = if (state.reduceAnim) 0 else 300)
  )

  val borderColorTarget = if (ambient) {
    MaterialTheme.colorScheme.outlineVariant
  } else {
    IconButtonDefaults.filledIconButtonColors().containerColor
  }
  val borderColor by animateColorAsState(
    targetValue = borderColorTarget,
    label = "borderColor",
    animationSpec = TweenSpec(durationMillis = if (state.reduceAnim) 0 else 300)
  )

  val interactionSource = remember { MutableInteractionSource() }
  FilledIconButton(
    onClick = onClick,
    colors = IconButtonDefaults.filledIconButtonColors(
      containerColor = containerColor,
      contentColor = contentColor
    ),
    shapes = if (state.reduceAnim) {
      IconButtonDefaults.shapes()
    } else {
      IconButtonDefaults.animatedShapes(
        pressedShape = MaterialTheme.shapes.small
      )
    },
    border = BorderStroke(2.dp, borderColor),
    interactionSource = interactionSource,
    modifier = modifier
  ) {
    AnimatedIcon(
      resId1 = R.drawable.ic_rounded_play_to_stop_anim,
      resId2 = R.drawable.ic_rounded_stop_to_play_anim,
      description = stringResource(id = R.string.wear_action_play_stop),
      trigger = animTrigger,
      modifier = Modifier.size(IconButtonDefaults.LargeIconSize),
      animated = !state.reduceAnim
    )
  }
}

@Composable
fun BeatModeButton(
  state: MainState,
  beatModeVibrate: Boolean,
  alwaysVibrate: Boolean,
  onClick: () -> Unit,
  modifier: Modifier
) {
  IconButton(
    onClick = onClick,
    colors = IconButtonDefaults.iconButtonColors(
      contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
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
    AnimatedIcon(
      resId1 = resId2,
      resId2 = resId1,
      description = stringResource(id = R.string.wear_action_beat_mode),
      trigger = beatModeVibrate,
      animated = !state.reduceAnim
    )
  }
}