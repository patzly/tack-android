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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.components.WrapContentCard
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.AnimatedVectorDrawable
import xyz.zedler.patrick.tack.util.spToDp
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun MainScreen(
  viewModel: MainViewModel = MainViewModel(),
  onTempoCardClick: () -> Unit = {},
  onTempoCardSwipe: (Int) -> Unit = {},
  onPlayButtonClick: () -> Unit = {},
  onSettingsButtonClick: () -> Unit = {},
  onBeatsButtonClick: () -> Unit = {},
  onTempoTapButtonClick: () -> Unit = {},
  onBookmarkButtonClick: () -> Unit = {},
  onBeatModeButtonClick: () -> Unit = {}
) {
  TackTheme {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.background),
      contentAlignment = Alignment.Center,
    ) {
      TimeText(
        timeTextStyle = TextStyle(
          fontFamily = remember { FontFamily(Font(R.font.jost_medium)) }
        )
      )
      ConstraintLayout(
        modifier = Modifier.fillMaxSize()
      ) {
        val (settingsButton, tempoCard, playButton) = createRefs()
        val (beatsButton, tempoTapButton) = createRefs()
        val (bookmarkButton, beatModeButton) = createRefs()

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
          viewModel = viewModel,
          onClick = onTempoCardClick,
          onTempoCardSwipe = onTempoCardSwipe,
          modifier = Modifier.constrainAs(tempoCard) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }
        )
        PlayButton(
          viewModel = viewModel,
          onClick = onPlayButtonClick,
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
          onClick = onTempoTapButtonClick,
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
          viewModel = viewModel,
          onClick = onBeatModeButtonClick,
          modifier = Modifier.constrainAs(beatModeButton) {
            top.linkTo(bookmarkButton.bottom)
            bottom.linkTo(parent.bottom, margin = 40.dp)
            start.linkTo(playButton.end)
            end.linkTo(parent.end)
          }
        )
      }
    }
  }
}

@Composable
fun TempoCard(
  viewModel: MainViewModel,
  onClick: () -> Unit,
  onTempoCardSwipe: (Int) -> Unit,
  modifier: Modifier
) {
  val tempo by viewModel.tempo.observeAsState(Constants.DEF.TEMPO)
  WrapContentCard(
    onClick = onClick,
    modifier = modifier.wrapContentWidth(),
    shape = RoundedCornerShape(32.dp),
    contentPadding = PaddingValues(0.dp)
  ) {
    val items = (1..400).toList()
    val state = rememberPickerState(
      initialNumberOfOptions = items.size,
      initiallySelectedOption = tempo - 1,
      repeatItems = false
    )
    val contentDescription by remember { derivedStateOf { "${state.selectedOption + 1}" } }
    val itemFont = remember { FontFamily(Font(R.font.jost_book)) }
    LaunchedEffect(state.selectedOption) {
      onTempoCardSwipe(state.selectedOption + 1)
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
        color = MaterialTheme.colors.primary,
        style = MaterialTheme.typography.display2,
        fontFamily = itemFont,
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
  onClick: () -> Unit,
  modifier: Modifier
) {
  val isPlaying by viewModel.isPlaying.observeAsState(false)
  val animTrigger = remember { mutableStateOf(isPlaying) }
  Button(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    modifier = modifier
  ) {
    AnimatedVectorDrawable(
      resId1 = R.drawable.ic_round_play_to_stop_anim,
      resId2 = R.drawable.ic_round_stop_to_play_anim,
      description = stringResource(id = R.string.action_play_stop),
      trigger = animTrigger,
      modifier = Modifier.size(32.dp)
    )
  }
}

@Composable
fun SettingsButton(
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(false) }
  CompactButton(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    colors = ButtonDefaults.iconButtonColors(),
    modifier = modifier
  ) {
    AnimatedVectorDrawable(
      resId = R.drawable.ic_round_settings_anim,
      description = stringResource(id = R.string.wear_title_settings),
      trigger = animTrigger
    )
  }
}

@Composable
fun BeatsButton(
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(false) }
  CompactButton(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    colors = ButtonDefaults.iconButtonColors(),
    modifier = modifier
  ) {
    AnimatedVectorDrawable(
      resId = R.drawable.ic_round_hdr_strong_anim,
      description = stringResource(id = R.string.action_add_beat),
      trigger = animTrigger
    )
  }
}

@Composable
fun TempoTapButton(
  onClick: () -> Unit,
  modifier: Modifier
) {
  val animTrigger = remember { mutableStateOf(false) }
  CompactButton(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    colors = ButtonDefaults.iconButtonColors(),
    modifier = modifier
  ) {
    AnimatedVectorDrawable(
      resId = R.drawable.ic_round_touch_app_anim,
      description = stringResource(id = R.string.action_tempo_tap),
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
  CompactButton(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    colors = ButtonDefaults.iconButtonColors(),
    modifier = modifier
  ) {
    AnimatedVectorDrawable(
      resId = R.drawable.ic_round_bookmark_anim,
      description = stringResource(id = R.string.action_bookmark),
      trigger = animTrigger
    )
  }
}

@Composable
fun BeatModeButton(
  viewModel: MainViewModel,
  onClick: () -> Unit,
  modifier: Modifier
) {
  val beatModeVibrate by viewModel.beatModeVibrate.observeAsState(Constants.DEF.BEAT_MODE_VIBRATE)
  val alwaysVibrate by viewModel.alwaysVibrate.observeAsState(Constants.DEF.ALWAYS_VIBRATE)
  val animTrigger = remember { mutableStateOf(beatModeVibrate) }
  CompactButton(
    onClick = {
      onClick()
      animTrigger.value = !animTrigger.value
    },
    colors = ButtonDefaults.iconButtonColors(),
    modifier = modifier
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
      trigger = animTrigger
    )
  }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun MainPreview() {
  MainScreen()
}