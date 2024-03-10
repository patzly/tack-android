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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ToggleButton
import androidx.wear.compose.material3.ToggleButtonDefaults
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.rotaryinput.rotaryWithScroll
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.SOUND
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@OptIn(ExperimentalHorologistApi::class)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
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
              text = stringResource(id = R.string.wear_title_settings),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          val sound by viewModel.sound.observeAsState(Constants.DEF.SOUND)
          var name = stringResource(id = R.string.settings_sound_sine)
          if (sound == SOUND.MECHANICAL) {
            name = stringResource(id = R.string.settings_sound_mechanical)
          } else if (sound == SOUND.WOOD) {
            name = stringResource(id = R.string.settings_sound_wood)
          }
          ClickChip(
            label = stringResource(R.string.settings_sound),
            secondaryLabel = name,
            onClick = onSoundClick
          )
        }
        item {
          val gain by viewModel.gain.observeAsState(Constants.DEF.GAIN)
          ClickChip(
            label = stringResource(R.string.settings_gain),
            secondaryLabel = stringResource(R.string.wear_label_db, gain),
            onClick = onGainClick
          )
        }
        item {
          val latency by viewModel.latency.observeAsState(Constants.DEF.LATENCY)
          ClickChip(
            label = stringResource(R.string.settings_latency),
            secondaryLabel = stringResource(R.string.wear_label_ms, latency),
            onClick = onLatencyClick
          )
        }
        item {
          val ignoreFocus by viewModel.ignoreFocus.observeAsState(Constants.DEF.IGNORE_FOCUS)
          ToggleChip(
            checked = ignoreFocus,
            onCheckedChange = {
              viewModel.changeIgnoreFocus(it)
            },
            label = stringResource(id = R.string.settings_ignore_focus),
            secondaryLabel = stringResource(id = R.string.settings_ignore_focus_description)
          )
        }
        item {
          val alwaysVibrate by viewModel.alwaysVibrate.observeAsState(Constants.DEF.ALWAYS_VIBRATE)
          ToggleChip(
            checked = alwaysVibrate,
            onCheckedChange = {
              viewModel.changeAlwaysVibrate(it)
            },
            label = stringResource(id = R.string.settings_always_vibrate),
            secondaryLabel = stringResource(id = R.string.settings_always_vibrate_description)
          )
        }
        item {
          val keepAwake by viewModel.keepAwake.observeAsState(Constants.DEF.KEEP_AWAKE)
          ToggleChip(
            checked = keepAwake,
            onCheckedChange = {
              viewModel.changeKeepAwake(it)
            },
            label = stringResource(id = R.string.settings_keep_awake),
            secondaryLabel = stringResource(id = R.string.settings_keep_awake_description)
          )
        }
        item {
          ClickChip(
            label = stringResource(R.string.settings_rate),
            secondaryLabel = stringResource(R.string.settings_rate_description),
            onClick = onRateClick
          )
        }
      }
    }
  }
}

@Composable
fun ClickChip(
  label: String,
  secondaryLabel: String,
  onClick: () -> Unit
) {
  Chip(
    onClick = onClick,
    label = {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
      )
    },
    secondaryLabel = {
      Text(
        text = secondaryLabel,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    },
    colors = ChipDefaults.secondaryChipColors(
      backgroundColor = MaterialTheme.colorScheme.surface
    ),
    modifier = Modifier.fillMaxWidth()
  )
}

@Composable
fun ToggleChip(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  label: String,
  secondaryLabel: String
) {
  ToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    toggleControl = {
      Switch()
    },
    modifier = Modifier.fillMaxWidth(),
    enabled = true,
    colors = ToggleButtonDefaults.toggleButtonColors(),
    interactionSource = null,
    icon = null,
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