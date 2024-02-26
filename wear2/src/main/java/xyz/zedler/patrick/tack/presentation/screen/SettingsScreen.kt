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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.scrollAway
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun SettingsScreen(
) {
  TackTheme {
    val scrollableState = rememberScalingLazyListState()
    Scaffold(
      timeText = {
        TimeText(
          timeTextStyle = TextStyle(
            fontFamily = remember { FontFamily(Font(R.font.jost_medium)) }
          ),
          modifier = Modifier.scrollAway(scrollableState)
        )
      }
    ) {
      val jostBookFont = remember { FontFamily(Font(R.font.jost_book)) }
      ScalingLazyColumn(
        state = scrollableState,
        modifier = Modifier
          .fillMaxSize()
          .background(color = MaterialTheme.colors.background)
      ) {
        item {
          ListHeader {
            Text(
              text = stringResource(id = R.string.wear_title_settings),
              color = MaterialTheme.colors.onSurface,
              style = MaterialTheme.typography.title2,
              fontFamily = jostBookFont
            )
          }
        }
        item {
          ToggleChip(
            label = {
              Text(
                text = stringResource(id = R.string.settings_always_vibrate),
                fontFamily = jostBookFont,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
              )
            },
            secondaryLabel = {
              Text(
                text = stringResource(id = R.string.settings_always_vibrate_description),
                fontFamily = jostBookFont,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            },
            checked = true,
            colors = ToggleChipDefaults.toggleChipColors(
              uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
            ),
            toggleControl = {
              Switch(
                checked = true,
                enabled = true,
              )
            },
            onCheckedChange = {},
            modifier = Modifier.fillMaxWidth()
          )
        }
        items(20) {
          Chip(
            onClick = { },
            label = { Text("List item $it") },
            colors = ChipDefaults.secondaryChipColors()
          )
        }
      }
    }
  }
}