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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants.Sound
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun SettingsScreen(
  viewModel: MainViewModel = MainViewModel(),
  onSoundClick: () -> Unit = {},
  onGainClick: () -> Unit = {},
  onLatencyClick: () -> Unit = {},
  onRateClick: () -> Unit = {}
) {
  TackTheme {
    val scrollableState = rememberScalingLazyListState()
    ScreenScaffold(
      scrollState = scrollableState,
      modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
      val state by viewModel.state.collectAsState()
      ScalingLazyColumn(
        state = scrollableState,
        modifier = Modifier
          .fillMaxSize()
          .rotaryScrollable(
            RotaryScrollableDefaults.behavior(scrollableState = scrollableState),
            focusRequester = rememberActiveFocusRequester()
          )
      ) {
        item {
          ListHeader(
            // Necessary padding to prevent cut-off by time text
            contentPadding = PaddingValues(top = 24.dp)
          ) {
            Text(
              text = stringResource(id = R.string.wear_title_settings),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          var name = stringResource(id = R.string.wear_settings_sound_sine)
          when (state.sound) {
            Sound.WOOD -> {
              name = stringResource(id = R.string.wear_settings_sound_wood)
            }
            Sound.MECHANICAL -> {
              name = stringResource(id = R.string.wear_settings_sound_mechanical)
            }
            Sound.BEATBOXING_1 -> {
              name = stringResource(id = R.string.wear_settings_sound_beatboxing_1)
            }
            Sound.BEATBOXING_2 -> {
              name = stringResource(id = R.string.wear_settings_sound_beatboxing_2)
            }
            Sound.HANDS -> {
              name = stringResource(id = R.string.wear_settings_sound_hands)
            }
            Sound.FOLDING -> {
              name = stringResource(id = R.string.wear_settings_sound_folding)
            }
          }
          ClickCard(
            title = stringResource(R.string.wear_settings_sound),
            subtitle = name,
            onClick = onSoundClick
          )
        }
        item {
          ClickCard(
            title = stringResource(R.string.wear_settings_gain),
            subtitle = stringResource(R.string.wear_label_db, state.gain),
            onClick = onGainClick
          )
        }
        item {
          ClickCard(
            title = stringResource(R.string.wear_settings_latency),
            subtitle = stringResource(R.string.wear_label_ms, state.latency),
            onClick = onLatencyClick
          )
        }
        item {
          SwitchCard(
            checked = state.ignoreFocus,
            onCheckedChange = { viewModel.updateIgnoreFocus(it) },
            label = stringResource(id = R.string.wear_settings_ignore_focus),
            secondaryLabel = stringResource(id = R.string.wear_settings_ignore_focus_description)
          )
        }
        item {
          SwitchCard(
            checked = state.alwaysVibrate,
            onCheckedChange = { viewModel.updateAlwaysVibrate(it) },
            label = stringResource(id = R.string.wear_settings_always_vibrate),
            secondaryLabel = stringResource(id = R.string.wear_settings_always_vibrate_description)
          )
        }
        item {
          SwitchCard(
            checked = state.strongVibration,
            onCheckedChange = { viewModel.updateStrongVibration(it) },
            label = stringResource(id = R.string.wear_settings_strong_vibration),
            secondaryLabel = stringResource(id = R.string.wear_settings_strong_vibration_description)
          )
        }
        item {
          SwitchCard(
            checked = state.flashScreen,
            onCheckedChange = { viewModel.updateFlashScreen(it) },
            label = stringResource(id = R.string.wear_settings_flash_screen),
            secondaryLabel = stringResource(id = R.string.wear_settings_flash_screen_description)
          )
        }
        item {
          SwitchCard(
            checked = state.keepAwake,
            onCheckedChange = { viewModel.updateKeepAwake(it) },
            label = stringResource(id = R.string.wear_settings_keep_awake),
            secondaryLabel = stringResource(id = R.string.wear_settings_keep_awake_description)
          )
        }
        item {
          SwitchCard(
            checked = state.reduceAnim,
            onCheckedChange = { viewModel.updateReduceAnim(it) },
            label = stringResource(id = R.string.wear_settings_reduce_animations),
            secondaryLabel = stringResource(
              id = R.string.wear_settings_reduce_animations_description
            )
          )
        }
        item {
          ClickCard(
            title = stringResource(R.string.wear_settings_rate),
            subtitle = stringResource(R.string.wear_settings_rate_description),
            onClick = onRateClick
          )
        }
        item {
          Column(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
          ) {
            Text(
              text = stringResource(id = R.string.wear_label_developer),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
              text = stringResource(id = R.string.wear_app_developer),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.align(Alignment.CenterHorizontally)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ClickCard(
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  TitleCard(
    onClick = onClick,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
    },
    subtitle = {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
    },
    modifier = Modifier.fillMaxWidth()
  )
}

@Composable
fun SwitchCard(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  label: String,
  secondaryLabel: String
) {
  SwitchButton (
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = Modifier.fillMaxWidth(),
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
    },
    secondaryLabel = {
      Text(
        text = secondaryLabel,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  )
}