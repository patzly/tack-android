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

package xyz.zedler.patrick.tack.presentation

import android.annotation.SuppressLint
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.HdrStrong
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.components.WrapContentCard
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.AnimatedVectorDrawable
import xyz.zedler.patrick.tack.util.MetronomeUtil
import xyz.zedler.patrick.tack.util.TempoTapUtil
import xyz.zedler.patrick.tack.util.spToDp
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

  private lateinit var metronomeUtil: MetronomeUtil
  private lateinit var tempoTapUtil: TempoTapUtil
  private lateinit var viewModel: MainViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    metronomeUtil = MetronomeUtil(this)
    metronomeUtil.addListener(object : MetronomeUtil.MetronomeListener {
      override fun onMetronomeStart() {
        viewModel.onPlayingChange(true)
      }
      override fun onMetronomeStop() {
        viewModel.onPlayingChange(false)
      }
      override fun onMetronomePreTick(tick: MetronomeUtil.Tick?) {}
      override fun onMetronomeTick(tick: MetronomeUtil.Tick?) {}
      override fun onMetronomeTempoChanged(tempoOld: Int, tempoNew: Int) {}
    })

    tempoTapUtil = TempoTapUtil()

    viewModel = MainViewModel(metronomeUtil)

    setContent {
      MainScreen(
        viewModel = viewModel,
        onTempoCardSwipe = {
          metronomeUtil.tempo = it
        },
        onPlayButtonClick = {
          if (metronomeUtil.isPlaying) {
            metronomeUtil.stop()
          } else {
            metronomeUtil.start()
          }
        },
        onTempoTapButtonClick = {
          if (tempoTapUtil.tap()) {
            metronomeUtil.tempo = tempoTapUtil.tempo
            viewModel.onTempoChange(tempoTapUtil.tempo)
          }
        },
        onBeatModeButtonClick = {
          metronomeUtil.isBeatModeVibrate = !metronomeUtil.isBeatModeVibrate
          viewModel.onBeatModeVibrateChange(metronomeUtil.isBeatModeVibrate)
        }
      )
    }
  }
}

@Composable
fun MainScreen(
  viewModel: MainViewModel = MainViewModel(),
  onTempoCardClick: () -> Unit = {},
  onTempoCardSwipe: (Int) -> Unit = {},
  onPlayButtonClick: () -> Unit = {},
  onTempoTapButtonClick: () -> Unit = {},
  onBeatModeButtonClick: () -> Unit = {}
) {
  val tempo by viewModel.tempo.observeAsState(DEF.TEMPO)
  val isPlaying by viewModel.isPlaying.observeAsState(false)
  val beatModeVibrate by viewModel.beatModeVibrate.observeAsState(DEF.BEAT_MODE_VIBRATE)
  val alwaysVibrate by viewModel.alwaysVibrate.observeAsState(DEF.ALWAYS_VIBRATE)
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

        // Main column
        val settingsAnimTrigger = remember { mutableStateOf(false) }
        CompactButton(
          onClick = {
            settingsAnimTrigger.value = !settingsAnimTrigger.value
          },
          colors = ButtonDefaults.iconButtonColors(),
          modifier = Modifier.constrainAs(settingsButton) {
            top.linkTo(parent.top, margin = 16.dp)
            bottom.linkTo(tempoCard.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }
        ) {
          AnimatedVectorDrawable(
            resId = R.drawable.ic_round_settings_anim,
            description = stringResource(id = R.string.wear_title_settings),
            trigger = settingsAnimTrigger
          )
        }
        TempoCard(
          tempo = tempo,
          onClick = onTempoCardClick,
          onTempoCardSwipe,
          modifier = Modifier.constrainAs(tempoCard) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }
        )
        PlayButton(
          isPlaying = isPlaying,
          onClick = onPlayButtonClick,
          modifier = Modifier.constrainAs(playButton) {
            top.linkTo(tempoCard.bottom)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
          }
        )

        // Left column
        IconButton(
          onClick = {},
          imageVector = Icons.Rounded.HdrStrong,
          contentDescription = "TODO",
          modifier = Modifier.constrainAs(beatsButton) {
            top.linkTo(parent.top, margin = 40.dp)
            bottom.linkTo(tempoTapButton.top)
            start.linkTo(parent.start)
            end.linkTo(playButton.start)
          }
        )
        val tempoTapAnimTrigger = remember { mutableStateOf(false) }
        CompactButton(
          onClick = {
            onTempoTapButtonClick()
            tempoTapAnimTrigger.value = !tempoTapAnimTrigger.value
          },
          colors = ButtonDefaults.iconButtonColors(),
          modifier = Modifier.constrainAs(tempoTapButton) {
            top.linkTo(beatsButton.bottom)
            bottom.linkTo(parent.bottom, margin = 40.dp)
            start.linkTo(parent.start)
            end.linkTo(playButton.start)
          }
        ) {
          AnimatedVectorDrawable(
            resId = R.drawable.ic_round_touch_app_anim,
            description = stringResource(id = R.string.action_tempo_tap),
            trigger = tempoTapAnimTrigger
          )
        }

        // Right column
        IconButton(
          onClick = {},
          imageVector = Icons.Rounded.Bookmark,
          contentDescription = "TODO",
          modifier = Modifier.constrainAs(bookmarkButton) {
            top.linkTo(parent.top, margin = 40.dp)
            bottom.linkTo(beatModeButton.top)
            start.linkTo(playButton.end)
            end.linkTo(parent.end)
          }
        )
        val beatModeIcon: ImageVector = if (beatModeVibrate) {
          if (alwaysVibrate) {
            Icons.AutoMirrored.Rounded.VolumeOff
          } else {
            Icons.Rounded.Vibration
          }
        } else {
          Icons.AutoMirrored.Rounded.VolumeUp
        }

        val beatModeAnimTrigger = remember { mutableStateOf(beatModeVibrate) }
        CompactButton(
          onClick = {
            onBeatModeButtonClick()
            beatModeAnimTrigger.value = !beatModeAnimTrigger.value
          },
          colors = ButtonDefaults.iconButtonColors(),
          modifier = Modifier.constrainAs(beatModeButton) {
            top.linkTo(bookmarkButton.bottom)
            bottom.linkTo(parent.bottom, margin = 40.dp)
            start.linkTo(playButton.end)
            end.linkTo(parent.end)
          }
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
            trigger = beatModeAnimTrigger
          )
        }
      }
    }
  }
}

@Composable
fun IconButton(
  onClick: () -> Unit,
  imageVector: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier
) {
  CompactButton(
    onClick = onClick,
    colors = ButtonDefaults.iconButtonColors(),
    modifier = modifier
  ) {
    Icon(
      imageVector,
      contentDescription,
    )
  }
}

@Composable
fun TempoCard(
  tempo: Int,
  onClick: () -> Unit,
  onTempoCardSwipe: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
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
  isPlaying: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
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

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun Preview() {
  MainScreen()
}