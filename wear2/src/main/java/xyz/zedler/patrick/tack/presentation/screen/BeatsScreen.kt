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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun BeatsScreen(viewModel: MainViewModel = MainViewModel()) {
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
      val gain by viewModel.gain.observeAsState(Constants.DEF.GAIN)
      ScalingLazyColumn(
        state = scrollableState,
        modifier = Modifier
          .fillMaxSize()
          .background(color = MaterialTheme.colorScheme.background)
      ) {
        item {
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_title_beats),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          Card(
            onClick = {},
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row {
              IconButton(
                onClick = {},
                modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_round_remove),
                  contentDescription = stringResource(id = R.string.action_remove_beat),
                  tint = IconButtonDefaults.filledTonalIconButtonColors().contentColor
                )
              }
              Row(
                modifier = Modifier.fillMaxHeight().weight(1f)
              ) {

              }
              IconButton(
                onClick = {},
                modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_round_add),
                  contentDescription = stringResource(id = R.string.action_add_beat),
                  tint = IconButtonDefaults.filledTonalIconButtonColors().contentColor
                )
              }
            }
          }
        }
        item {
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_title_subdivisions),
              style = MaterialTheme.typography.titleMedium
            )
          }
        }
        item {
          Card(
            onClick = {},
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row {
              IconButton(
                onClick = {},
                modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_round_remove),
                  contentDescription = stringResource(id = R.string.action_remove_beat),
                  tint = IconButtonDefaults.filledTonalIconButtonColors().contentColor
                )
              }
              Row(
                modifier = Modifier.fillMaxHeight().weight(1f)
              ) {

              }
              IconButton(
                onClick = {},
                modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_round_add),
                  contentDescription = stringResource(id = R.string.action_add_beat),
                  tint = IconButtonDefaults.filledTonalIconButtonColors().contentColor
                )
              }
            }
          }
        }
      }
    }
  }
}