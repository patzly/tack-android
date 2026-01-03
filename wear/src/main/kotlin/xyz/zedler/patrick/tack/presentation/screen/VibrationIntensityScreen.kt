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
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun VibrationIntensityScreen(
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
              text = stringResource(id = R.string.wear_settings_vibration_intensity),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        if (state.supportsVibrationEffects) {
          item {
            VibrationIntensityOption(
              label = stringResource(id = R.string.wear_settings_vibration_intensity_auto),
              selected = state.vibrationIntensity == Constants.VibrationIntensity.AUTO,
              onSelected = {
                viewModel.updateVibrationIntensity(Constants.VibrationIntensity.AUTO)
              }
            )
          }
        }
        item {
          VibrationIntensityOption(
            label = stringResource(id = R.string.wear_settings_vibration_intensity_soft),
            selected = state.vibrationIntensity == Constants.VibrationIntensity.SOFT,
            onSelected = {
              viewModel.updateVibrationIntensity(Constants.VibrationIntensity.SOFT)
            }
          )
        }
        item {
          VibrationIntensityOption(
            label = stringResource(id = R.string.wear_settings_vibration_intensity_strong),
            selected = state.vibrationIntensity == Constants.VibrationIntensity.STRONG,
            onSelected = {
              viewModel.updateVibrationIntensity(Constants.VibrationIntensity.STRONG)
            }
          )
        }
      }
    }
  }
}

@Composable
fun VibrationIntensityOption(
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