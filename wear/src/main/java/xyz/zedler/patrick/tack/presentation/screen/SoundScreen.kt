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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants.Sound
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun SoundScreen(
  viewModel: MainViewModel = MainViewModel()
) {
  TackTheme {
    val scrollableState = rememberScalingLazyListState()
    ScreenScaffold(
      scrollState = scrollableState,
      modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
    ) {
      val state by viewModel.state.collectAsState()
      ScalingLazyColumn(
        state = scrollableState
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
          SoundOption(
            label = stringResource(id = R.string.wear_settings_sound_sine),
            selected = state.sound == Sound.SINE,
            onSelected = {
              viewModel.updateSound(Sound.SINE)
            }
          )
        }
        item {
          SoundOption(
            label = stringResource(id = R.string.wear_settings_sound_wood),
            selected = state.sound == Sound.WOOD,
            onSelected = {
              viewModel.updateSound(Sound.WOOD)
            }
          )
        }
        item {
          SoundOption(
            label = stringResource(id = R.string.wear_settings_sound_mechanical),
            selected = state.sound == Sound.MECHANICAL,
            onSelected = {
              viewModel.updateSound(Sound.MECHANICAL)
            }
          )
        }
        item {
          SoundOption(
            label = stringResource(id = R.string.wear_settings_sound_beatboxing_1),
            selected = state.sound == Sound.BEATBOXING_1,
            onSelected = {
              viewModel.updateSound(Sound.BEATBOXING_1)
            }
          )
        }
        item {
          SoundOption(
            label = stringResource(id = R.string.wear_settings_sound_beatboxing_2),
            selected = state.sound == Sound.BEATBOXING_2,
            onSelected = {
              viewModel.updateSound(Sound.BEATBOXING_2)
            }
          )
        }
        item {
          SoundOption(
            label = stringResource(id = R.string.wear_settings_sound_hands),
            selected = state.sound == Sound.HANDS,
            onSelected = {
              viewModel.updateSound(Sound.HANDS)
            }
          )
        }
        item {
          SoundOption(
            label = stringResource(id = R.string.wear_settings_sound_folding),
            selected = state.sound == Sound.FOLDING,
            onSelected = {
              viewModel.updateSound(Sound.FOLDING)
            }
          )
        }
      }
    }
  }
}

@Composable
fun SoundOption(
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