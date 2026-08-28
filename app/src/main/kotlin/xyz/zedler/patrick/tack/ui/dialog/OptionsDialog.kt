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
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.component.ScrollableAlertDialog
import xyz.zedler.patrick.tack.ui.component.ScrollableAlertDialogContent
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsDialog(
  onDismissRequest: () -> Unit
) {
  val haptic = LocalHaptic.current

  ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
    OptionsDialogContent(
      onCloseClick = {
        haptic.click()
        onDismissRequest()
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsDialogContent(
  onCloseClick: () -> Unit = {}
) {
  ScrollableAlertDialogContent(
    title = {
      Text(stringResource(R.string.title_options))
    },
    confirmButton = {
      TextButton(
        onClick = onCloseClick,
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_close))
      }
    }
  ) {
    OptionsContent()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsContent() {
  Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
    val itemCount = 2

    val colors = ListItemDefaults.colors(
      containerColor = MaterialTheme.colorScheme.surfaceBright
    )

    SegmentedListItem(
      onClick = {},
      shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
      colors = colors,
      leadingContent = {
        Box(modifier = Modifier.padding(vertical = 10.dp)) {
          Icon(
            painter = painterResource(R.drawable.ic_rounded_star),
            contentDescription = null
          )
        }
      },
      content = { Text(stringResource(R.string.action_rate)) },
      supportingContent = { Text(stringResource(R.string.action_rate_description)) }
    )

    SegmentedListItem(
      onClick = {},
      shapes = ListItemDefaults.segmentedShapes(
        index = 1,
        count = itemCount
      ),
      colors = colors,
      leadingContent = {
        Box(modifier = Modifier.padding(vertical = 10.dp)) {
          Icon(
            painter = painterResource(R.drawable.ic_rounded_group),
            contentDescription = null
          )
        }
      },
      content = { Text(stringResource(R.string.action_recommend)) },
      supportingContent = { Text(stringResource(R.string.action_recommend_description)) }
    )
  }
}

@Preview
@Composable
fun OptionsDialogPreview() {
  TackTheme {
    OptionsDialogContent()
  }
}
