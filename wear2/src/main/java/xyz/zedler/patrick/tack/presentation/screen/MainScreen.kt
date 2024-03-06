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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
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
import xyz.zedler.patrick.tack.util.spToDp
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun MainScreen(
  viewModel: MainViewModel = MainViewModel(),
  backStackEntry: NavBackStackEntry? = null,
  onTempoCardClick: () -> Unit = {},
  onSettingsButtonClick: () -> Unit = {},
  onBeatsButtonClick: () -> Unit = {},
  onBookmarkButtonClick: () -> Unit = {},
  onPermissionRequestClick: () -> Unit = {}
) {
  TackTheme {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.Center,
    ) {
      val isPlaying by viewModel.isPlaying.observeAsState(false)
      val playAnimTrigger = remember { mutableStateOf(isPlaying) }
      var showVolumeDialog by remember { mutableStateOf(false) }
      val showPermissionDialog by viewModel.showPermissionDialog.observeAsState(false)

      TimeText(
        timeTextStyle = MaterialTheme.typography.labelMedium
      )
      ConstraintLayout(
        modifier = Modifier.fillMaxSize()
      ) {
        val (settingsButton, tempoCard, playButton) = createRefs()
        val (beatsButton, tempoTapButton) = createRefs()
        val (bookmarkButton, beatModeButton) = createRefs()

        val tempo by viewModel.tempo.observeAsState(initial = Constants.DEF.TEMPO)
        val pickerOption = remember { tempo - 1 }
        val pickerCoroutineScope = rememberCoroutineScope()
        val pickerState = rememberPickerState(
          initialNumberOfOptions = 400,
          initiallySelectedOption = pickerOption,
          repeatItems = false
        )
        LaunchedEffect(tempo) {
          pickerCoroutineScope.launch {
            pickerState.animateScrollToOption(tempo - 1)
          }
        }
        val beatModeVibrate by viewModel.beatModeVibrate.observeAsState(
          Constants.DEF.BEAT_MODE_VIBRATE
        )
        val alwaysVibrate by viewModel.alwaysVibrate.observeAsState(Constants.DEF.ALWAYS_VIBRATE)
        val gain by viewModel.gain.observeAsState(Constants.DEF.GAIN)

        SettingsButton(
          onClick = onSettingsButtonClick,
          modifier = Modifier.constrainAs(settingsButton) {
            top.linkTo(parent.top, margin = 16.dp)
            bottom.linkTo(tempoCard.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }
        )
        TempoCard(
          state = pickerState,
          onClick = onTempoCardClick,
          onOptionChange = {
            viewModel.changeTempo(it + 1)
          },
          modifier = Modifier.constrainAs(tempoCard) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }
        )
        PlayButton(
          animTrigger = playAnimTrigger,
          onClick = {
            val startedWithGain = viewModel.metronomeUtil?.neverStartedWithGainBefore() == false
            if (isPlaying || (gain == 0 || startedWithGain)) {
              viewModel.togglePlaying()
              playAnimTrigger.value = !playAnimTrigger.value
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
          modifier = Modifier.constrainAs(beatsButton) {
            top.linkTo(parent.top, margin = 40.dp)
            bottom.linkTo(tempoTapButton.top)
            start.linkTo(parent.start)
            end.linkTo(playButton.start)
          }
        )
        TempoTapButton(
          onClick = {
            viewModel.onTempoTap()
          },
          modifier = Modifier.constrainAs(tempoTapButton) {
            top.linkTo(beatsButton.bottom)
            bottom.linkTo(parent.bottom, margin = 40.dp)
            start.linkTo(parent.start)
            end.linkTo(playButton.start)
          }
        )
        BookmarkButton(
          onClick = onBookmarkButtonClick,
          modifier = Modifier.constrainAs(bookmarkButton) {
            top.linkTo(parent.top, margin = 40.dp)
            bottom.linkTo(beatModeButton.top)
            start.linkTo(playButton.end)
            end.linkTo(parent.end)
          }
        )
        BeatModeButton(
          beatModeVibrate = beatModeVibrate,
          alwaysVibrate = alwaysVibrate,
          onClick = {
            viewModel.toggleBeatModeVibrate()
          },
          modifier = Modifier.constrainAs(beatModeButton) {
            top.linkTo(bookmarkButton.bottom)
            bottom.linkTo(parent.bottom, margin = 40.dp)
            start.linkTo(playButton.end)
            end.linkTo(parent.end)
          }
        )
      }
      VolumeDialog(
        showDialog = showVolumeDialog,
        onDismissRequest = {
          showVolumeDialog = false
        },
        onPositiveClick = {
          viewModel.togglePlaying()
          playAnimTrigger.value = !playAnimTrigger.value
          showVolumeDialog = false
        },
        onNegativeClick = {
          viewModel.changeGain(0)
          viewModel.togglePlaying()
          playAnimTrigger.value = !playAnimTrigger.value
          showVolumeDialog = false
        }
      )
      PermissionDialog(
        showDialog = showPermissionDialog,
        onDismissRequest = {
          viewModel.changeShowPermissionDialog(false)
        },
        onPositiveClick = {
          viewModel.changeShowPermissionDialog(false)
          onPermissionRequestClick()
        },
        onNegativeClick = {
          viewModel.changeShowPermissionDialog(false)
        }
      )
    }
  }
}

@Composable
fun TempoCard(
  state: PickerState,
  onClick: () -> Unit,
  onOptionChange: (Int) -> Unit,
  modifier: Modifier
) {
  WrapContentCard(
    onClick = onClick,
    modifier = modifier.wrapContentWidth(),
    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    shape = MaterialTheme.shapes.extraLarge,
    contentPadding = PaddingValues(0.dp)
  ) {
    val items = (1..400).toList()
    val contentDescription by remember { derivedStateOf { "${state.selectedOption + 1}" } }
    LaunchedEffect(state.selectedOption) {
      onOptionChange(state.selectedOption)
    }
    Picker(
      gradientRatio = 0f,
      modifier = Modifier.size(spToDp(spValue = 88), spToDp(spValue = 56)),
      state = state,
      contentDescription = contentDescription
    ) {
      Text(
        modifier = Modifier.wrapContentSize(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.displayMedium.copy(
          fontSize = 36.sp
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
  animTrigger: MutableState<Boolean>,
  onClick: () -> Unit,
  modifier: Modifier
) {
  FilledIconButton(
    onClick = onClick,
    modifier = modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
  ) {
    AnimatedVectorDrawable(
      resId1 = R.drawable.ic_round_play_to_stop_anim,
      resId2 = R.drawable.ic_round_stop_to_play_anim,
      description = stringResource(id = R.string.action_play_stop),
      color = IconButtonDefaults.filledIconButtonColors().contentColor,
      trigger = animTrigger,
      modifier = Modifier.size(IconButtonDefaults.LargeIconSize)
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
      painter = painterResource(id = R.drawable.ic_round_settings),
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
      painter = painterResource(id = R.drawable.ic_round_hdr_strong),
      contentDescription = stringResource(id = R.string.action_add_beat),
      tint = IconButtonDefaults.iconButtonColors().contentColor
    )
  }
}

@Composable
fun TempoTapButton(
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
      description = stringResource(id = R.string.action_tempo_tap),
      color = IconButtonDefaults.iconButtonColors().contentColor,
      trigger = animTrigger
    )
  }
}

@Composable
fun BookmarkButton(
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
      resId = R.drawable.ic_round_bookmark_anim,
      description = stringResource(id = R.string.action_bookmark),
      color = IconButtonDefaults.iconButtonColors().contentColor,
      trigger = animTrigger
    )
  }
}

@Composable
fun BeatModeButton(
  beatModeVibrate: Boolean,
  alwaysVibrate: Boolean,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(beatModeVibrate) }
  IconButton(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    modifier = modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
  ) {
    val resId1 = if (alwaysVibrate) {
      R.drawable.ic_round_volume_off_to_volume_on_anim
    } else {
      R.drawable.ic_round_vibrate_to_volume_anim
    }
    val resId2 = if (alwaysVibrate) {
      R.drawable.ic_round_volume_on_to_volume_off_anim
    } else {
      R.drawable.ic_round_volume_to_vibrate_anim
    }
    AnimatedVectorDrawable(
      resId1 = resId2,
      resId2 = resId1,
      description = stringResource(id = R.string.action_beat_mode),
      color = IconButtonDefaults.iconButtonColors().contentColor,
      trigger = animTrigger
    )
  }
}