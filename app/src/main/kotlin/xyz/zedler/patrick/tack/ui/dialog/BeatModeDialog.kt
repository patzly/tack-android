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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.BeatMode
import xyz.zedler.patrick.tack.ui.component.core.ScrollableAlertDialog
import xyz.zedler.patrick.tack.ui.component.core.ScrollableAlertDialogContent
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic
import xyz.zedler.patrick.tack.ui.util.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatModeDialog(
  currentBeatMode: BeatMode,
  onBeatModeSelected: (BeatMode) -> Unit,
  onDismissRequest: () -> Unit
) {
  val haptic = LocalHaptic.current

  ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
    BeatModeDialogContent(
      currentBeatMode = currentBeatMode,
      onBeatModeSelected = {
        haptic.click()
        onBeatModeSelected(it)
      },
      onCloseClick = {
        haptic.click()
        onDismissRequest()
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeatModeDialogContent(
  currentBeatMode: BeatMode,
  onBeatModeSelected: (BeatMode) -> Unit = {},
  onCloseClick: () -> Unit = {}
) {
  ScrollableAlertDialogContent(
    title = {
      Text(stringResource(R.string.action_beat_mode))
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
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
      val modes = BeatMode.entries
      val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceBright,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
      )

      modes.forEachIndexed { index, mode ->
        val isSelected = currentBeatMode == mode

        SegmentedListItem(
          onClick = { onBeatModeSelected(mode) },
          shapes = ListItemDefaults.segmentedShapes(index = index, count = modes.size),
          selected = isSelected,
          colors = colors,
          verticalAlignment = Alignment.CenterVertically,
          leadingContent = {
            RadioButton(
              selected = isSelected,
              onClick = null
            )
          },
          content = {
            Text(stringResource(mode.labelRes))
          }
        )
      }
    }
  }
}

@Preview
@Composable
fun BeatModeDialogPreview() {
  TackTheme {
    BeatModeDialogContent(currentBeatMode = BeatMode.ALL)
  }
}
