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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun LatencyScreen(
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
        state = scrollableState,
        modifier = Modifier
          .fillMaxSize()
          .rotaryScrollable(
            RotaryScrollableDefaults.behavior(scrollableState = scrollableState),
            focusRequester = rememberActiveFocusRequester()
          )
      ) {
        item {
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_settings_latency),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          LatencySlider(
            latency = state.latency,
            onValueChange = {
              viewModel.updateLatency(it.toLong())
            }
          )
        }
        item {
          Text(
            text = stringResource(id = R.string.wear_label_ms, state.latency),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp, bottom = 12.dp)
          )
        }
      }
    }
  }
}

@Composable
fun LatencySlider(
  latency: Long,
  onValueChange: (Int) -> Unit = {}
) {
  Slider(
    value = latency.toInt(),
    onValueChange = { onValueChange(it) },
    valueProgression = IntProgression.fromClosedRange(0, 200, 5),
    segmented = false,
    decreaseIcon = {
      Icon(
        painter = painterResource(id = R.drawable.ic_rounded_remove),
        contentDescription = stringResource(id = R.string.wear_action_decrease)
      )
    },
    increaseIcon = {
      Icon(
        painter = painterResource(id = R.drawable.ic_rounded_add),
        contentDescription = stringResource(id = R.string.wear_action_increase)
      )
    }
  )
}