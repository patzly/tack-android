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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Switch
import androidx.wear.compose.material3.SwitchDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.ToggleButton
import androidx.wear.compose.material3.ToggleButtonDefaults
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun SettingsScreen(
  viewModel: MainViewModel = MainViewModel()
) {
  val alwaysVibrate by viewModel.alwaysVibrate.observeAsState(Constants.DEF.ALWAYS_VIBRATE)

  TackTheme {
    val scrollableState = rememberScalingLazyListState()
    Scaffold(
      timeText = {
        TimeText(
          timeTextStyle = MaterialTheme.typography.labelMedium,
          modifier = Modifier.scrollAway(scrollableState)
        )
      }
    ) {
      ScalingLazyColumn(
        state = scrollableState,
        modifier = Modifier
          .fillMaxSize()
          .background(color = MaterialTheme.colorScheme.background)
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
          ToggleButton(
            checked = alwaysVibrate,
            onCheckedChange = { viewModel.changeAlwaysVibrate(it) },
            toggleControl = {
              Switch(
                colors = SwitchDefaults.colors()
              )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            colors = ToggleButtonDefaults.toggleButtonColors(),
            interactionSource = null,
            icon = null,
            label = {
              Text(
                text = stringResource(id = R.string.settings_always_vibrate),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
              )
            },
            secondaryLabel = {
              Text(
                text = stringResource(id = R.string.settings_always_vibrate_description),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          )
        }
        item {
          Chip(
            onClick = {},
            label = {
              Text(
                text = stringResource(id = R.string.settings_rate),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
              )
            },
            secondaryLabel = {
              Text(
                text = stringResource(id = R.string.settings_rate_description),
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
      }
    }
  }
}