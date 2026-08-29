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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.component.core.ScrollableAlertDialog
import xyz.zedler.patrick.tack.ui.component.core.ScrollableAlertDialogContent
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupDialog(
  onBackup: () -> Unit,
  onRestore: () -> Unit,
  onDismissRequest: () -> Unit
) {
  val haptic = LocalHaptic.current

  ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
    BackupDialogContent(
      onBackupClick = {
        haptic.click()
        onBackup()
      },
      onRestoreClick = {
        haptic.click()
        onRestore()
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
private fun BackupDialogContent(
  onBackupClick: () -> Unit = {},
  onRestoreClick: () -> Unit = {},
  onCloseClick: () -> Unit = {}
) {
  ScrollableAlertDialogContent(
    title = {
      Text(stringResource(R.string.settings_backup))
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
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = stringResource(R.string.msg_backup_description),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        val itemCount = 2
        val colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceBright
        )

        SegmentedListItem(
          onClick = onBackupClick,
          shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
          colors = colors,
          verticalAlignment = Alignment.CenterVertically,
          leadingContent = {
            Box(modifier = Modifier.padding(vertical = 10.dp)) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_download),
                contentDescription = null
              )
            }
          },
          content = { Text(stringResource(R.string.action_backup)) },
          supportingContent = { Text(stringResource(R.string.action_backup_description)) }
        )

        SegmentedListItem(
          onClick = onRestoreClick,
          shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
          colors = colors,
          verticalAlignment = Alignment.CenterVertically,
          leadingContent = {
            Box(modifier = Modifier.padding(vertical = 10.dp)) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_upload),
                contentDescription = null
              )
            }
          },
          content = { Text(stringResource(R.string.action_restore)) },
          supportingContent = { Text(stringResource(R.string.action_restore_description)) }
        )
      }
    }
  }
}

@Preview
@Composable
fun BackupDialogPreview() {
  TackTheme {
    BackupDialogContent()
  }
}
