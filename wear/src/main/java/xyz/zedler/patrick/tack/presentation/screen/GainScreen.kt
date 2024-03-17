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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ExperimentalWearMaterial3Api
import androidx.wear.compose.material3.InlineSlider
import androidx.wear.compose.material3.InlineSliderDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.rotaryinput.rotaryWithScroll
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@OptIn(ExperimentalHorologistApi::class)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun GainScreen(viewModel: MainViewModel = MainViewModel()) {
  TackTheme {
    val scrollableState = rememberScalingLazyListState()
    Scaffold(
      timeText = {
        TimeText(
          timeTextStyle = MaterialTheme.typography.labelMedium,
          modifier = Modifier.scrollAway(scrollableState)
        )
      },
      positionIndicator = {
        PositionIndicator(
          scalingLazyListState = scrollableState
        )
      }
    ) {
      val gain by viewModel.gain.observeAsState(Constants.DEF.GAIN)
      ScalingLazyColumn(
        state = scrollableState,
        modifier = Modifier
          .fillMaxSize()
          .background(color = MaterialTheme.colorScheme.background)
          .rotaryWithScroll(scrollableState = scrollableState)
      ) {
        item {
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_settings_gain),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          GainSlider(
            gain = gain,
            onValueChange = {
              viewModel.changeGain(it)
            }
          )
        }
        item {
          Text(
            text = stringResource(id = R.string.wear_label_db, gain),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp, bottom = 12.dp)
          )
        }
        item {
          Card(
            onClick = {},
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = stringResource(id = R.string.wear_settings_gain_disclaimer),
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun GainSlider(
  gain: Int,
  onValueChange: (Int) -> Unit = {}
) {
  InlineSlider(
    value = gain,
    onValueChange = onValueChange,
    valueProgression = IntProgression.fromClosedRange(0, 20, 5),
    segmented = true,
    decreaseIcon = {
      val targetTint = if (gain > 0) {
        InlineSliderDefaults.colors().buttonIconColor
      } else {
        InlineSliderDefaults.colors().disabledButtonIconColor
      }
      val tint by animateColorAsState(
        targetValue = targetTint,
        label = "decreaseGain",
        animationSpec = TweenSpec(durationMillis = 200)
      )
      Icon(
        painter = painterResource(id = R.drawable.ic_round_volume_down),
        contentDescription = stringResource(id = R.string.wear_action_decrease),
        tint = tint
      )
    },
    increaseIcon = {
      val targetTint = if (gain < 20) {
        InlineSliderDefaults.colors().buttonIconColor
      } else {
        InlineSliderDefaults.colors().disabledButtonIconColor
      }
      val tint by animateColorAsState(
        targetValue = targetTint,
        label = "increaseGain",
        animationSpec = TweenSpec(durationMillis = 200)
      )
      Icon(
        painter = painterResource(id = R.drawable.ic_round_volume_up),
        contentDescription = stringResource(id = R.string.wear_action_increase),
        tint = tint
      )
    }
  )
}