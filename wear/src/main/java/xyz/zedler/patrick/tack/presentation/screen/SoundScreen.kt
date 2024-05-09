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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.SOUND
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@OptIn(ExperimentalWearFoundationApi::class)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun SoundScreen(viewModel: MainViewModel = MainViewModel()) {
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
      val sound by viewModel.sound.observeAsState(Constants.DEF.SOUND)
      ScalingLazyColumn(
        state = scrollableState,
        modifier = Modifier
          .fillMaxSize()
          .background(color = MaterialTheme.colorScheme.background)
          .rotaryScrollable(
            RotaryScrollableDefaults.behavior(scrollableState = scrollableState),
            focusRequester = rememberActiveFocusRequester()
          )
      ) {
        item {
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_settings_sound),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          RadioChip(
            label = stringResource(id = R.string.wear_settings_sound_sine),
            selected = sound == SOUND.SINE,
            onSelected = {
              viewModel.changeSound(SOUND.SINE)
            }
          )
        }
        item {
          RadioChip(
            label = stringResource(id = R.string.wear_settings_sound_wood),
            selected = sound == SOUND.WOOD,
            onSelected = {
              viewModel.changeSound(SOUND.WOOD)
            }
          )
        }
        item {
          RadioChip(
            label = stringResource(id = R.string.wear_settings_sound_mechanical),
            selected = sound == SOUND.MECHANICAL,
            onSelected = {
              viewModel.changeSound(SOUND.MECHANICAL)
            }
          )
        }
        item {
          RadioChip(
            label = stringResource(id = R.string.wear_settings_sound_beatboxing_1),
            selected = sound == SOUND.BEATBOXING_1,
            onSelected = {
              viewModel.changeSound(SOUND.BEATBOXING_1)
            }
          )
        }
        item {
          RadioChip(
            label = stringResource(id = R.string.wear_settings_sound_folding),
            selected = sound == SOUND.FOLDING,
            onSelected = {
              viewModel.changeSound(SOUND.FOLDING)
            }
          )
        }
      }
    }
  }
}

@Composable
fun RadioChip(
  label: String,
  selected: Boolean,
  onSelected: () -> Unit,
) {
  RadioButton(
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
    },
    selected = selected,
    onSelect = onSelected,
    modifier = Modifier.fillMaxWidth()
  )
}