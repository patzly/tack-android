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

package xyz.zedler.patrick.tack.presentation.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.R

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun PermissionDialog(
  show: Boolean = true,
  onDismissRequest: () -> Unit = {},
  onRetry: () -> Unit = {},
  onDismiss: () -> Unit = {}
) {
  TackDialog(
    show = show,
    icon = R.drawable.ic_rounded_error,
    title = R.string.wear_msg_notification_permission_denied,
    text = R.string.wear_msg_notification_permission_denied_description,
    confirmIcon = R.drawable.ic_rounded_repeat,
    confirmString = R.string.wear_action_retry,
    dismissIcon = R.drawable.ic_rounded_close,
    dismissString = R.string.wear_action_cancel,
    onDismissRequest = onDismissRequest,
    onConfirm = onRetry,
    onDismiss = onDismiss
  )
}