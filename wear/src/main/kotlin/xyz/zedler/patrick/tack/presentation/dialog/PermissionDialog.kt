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

package xyz.zedler.patrick.tack.presentation.dialog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme

@Composable
fun PermissionDialog(
  visible: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  TackTheme {
    AlertDialog(
      visible = visible,
      confirmButton = {
        AlertDialogDefaults.ConfirmButton(
          onClick = onConfirm
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_rounded_repeat),
            contentDescription = stringResource(id = R.string.wear_action_retry)
          )
        }
      },
      dismissButton = {
        AlertDialogDefaults.DismissButton(
          onClick = onDismiss,
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
          )
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_rounded_close),
            contentDescription = stringResource(id = R.string.wear_action_cancel)
          )
        }
      },
      onDismissRequest = onDismiss,
      icon = {
        Icon(
          painter = painterResource(id = R.drawable.ic_rounded_error),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onBackground
        )
      },
      title = {
        Text(
          text = stringResource(id = R.string.wear_msg_notification_permission_denied),
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      text = {
        Text(
          text = stringResource(id = R.string.wear_msg_notification_permission_denied_description),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      contentPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 24.dp,
        bottom = 32.dp
      )
    )
  }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun PermissionDialogPreview() {
  PermissionDialog(
    visible = true,
    onConfirm = {},
    onDismiss = {}
  )
}