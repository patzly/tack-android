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

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.component.core.ScrollableAlertDialog
import xyz.zedler.patrick.tack.ui.component.core.ScrollableAlertDialogContent
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionDialog(
  onNext: () -> Unit,
  onDismissRequest: () -> Unit
) {
  val haptic = LocalHaptic.current

  ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
    NotificationPermissionContent(
      onNextClick = {
        haptic.click()
        onNext()
        onDismissRequest()
      },
      onCancelClick = {
        haptic.click()
        onDismissRequest()
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationPermissionContent(
  onNextClick: () -> Unit = {},
  onCancelClick: () -> Unit = {}
) {
  ScrollableAlertDialogContent(
    title = {
      Text(stringResource(R.string.msg_notification_permission))
    },
    confirmButton = {
      TextButton(
        onClick = onNextClick,
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_next))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onCancelClick,
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    }
  ) {
    Text(stringResource(R.string.msg_notification_permission_description))
  }
}

@Preview
@Composable
fun NotificationPermissionDialogPreview() {
  TackTheme {
    NotificationPermissionContent()
  }
}
