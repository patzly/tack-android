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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.isSmallScreen

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun TackDialog(
  show: Boolean = true,
  icon: Int = R.drawable.ic_rounded_error,
  title: Int = R.string.wear_msg_notification_permission_denied,
  text: Int = R.string.wear_msg_notification_permission_denied_description,
  caution: Boolean = false,
  confirmIcon: Int = R.drawable.ic_rounded_repeat,
  confirmString: Int = R.string.wear_action_retry,
  dismissIcon: Int = R.drawable.ic_rounded_close,
  dismissString: Int = R.string.wear_action_cancel,
  onDismissRequest: () -> Unit = {},
  onConfirm: () -> Unit = {},
  onDismiss: () -> Unit = {}
) {
  TackTheme {
    AlertDialog(
      show = show,
      confirmButton = {
        ConfirmButton(
          iconResId = confirmIcon,
          stringResId = confirmString,
          onClick = onConfirm,
        )
      },
      dismissButton = {
        DismissButton(
          iconResId = dismissIcon,
          stringResId = dismissString,
          onClick = onDismiss
        )
      },
      onDismissRequest = onDismiss,
      icon = {
        Icon(
          painter = painterResource(id = icon),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onBackground
        )
      },
      title = {
        Text(
          text = stringResource(id = title),
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      text = {
        Text(
          text = stringResource(id = text),
          style = MaterialTheme.typography.bodySmall,
          color = if (caution) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.onSurface
          },
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

@Composable
fun ConfirmButton(
  iconResId: Int,
  stringResId: Int,
  onClick: () -> Unit
) {
  val confirmWidth = if (isSmallScreen()) 56.dp else 64.dp
  val confirmHeight = if (isSmallScreen()) 48.dp else 56.dp

  val confirmShape = CircleShape

  FilledIconButton(
    onClick = onClick,
    modifier = Modifier.rotate(-45f).size(confirmWidth, confirmHeight),
    shape = confirmShape
  ) {
    Row(modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationZ = 45f }) {
      Icon(
        painter = painterResource(id = iconResId),
        contentDescription = stringResource(id = stringResId),
        tint = IconButtonDefaults.filledIconButtonColors().contentColor
      )
    }
  }
}

@Composable
fun DismissButton(
  iconResId: Int,
  stringResId: Int,
  onClick: () -> Unit
) {
  val dismissSize = if (isSmallScreen()) 56.dp else 64.dp
  val dismissShape = MaterialTheme.shapes.medium

  Box(modifier = Modifier.size(dismissSize)) {
    FilledTonalIconButton(
      onClick = onClick,
      modifier = Modifier.size(dismissSize).align(Alignment.BottomEnd),
      shape = dismissShape,
    ) {
      Icon(
        painter = painterResource(id = iconResId),
        contentDescription = stringResource(id = stringResId),
        tint = IconButtonDefaults.filledTonalIconButtonColors().contentColor
      )
    }
  }
}