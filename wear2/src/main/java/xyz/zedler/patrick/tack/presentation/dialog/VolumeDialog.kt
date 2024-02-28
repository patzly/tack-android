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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme

@Composable
fun VolumeDialog(
  showDialog: Boolean,
  onDismissRequest: () -> Unit,
  onPositiveClick: () -> Unit,
  onNegativeClick: () -> Unit
) {
  val scrollState = rememberScalingLazyListState()
  Dialog(
    showDialog = showDialog,
    onDismissRequest = onDismissRequest,
    scrollState = scrollState,
  ) {
    VolumeAlert(
      onPositiveClick = onPositiveClick,
      onNegativeClick = onNegativeClick
    )
  }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun VolumeAlert(
  onPositiveClick: () -> Unit = {},
  onNegativeClick: () -> Unit = {}
) {
  TackTheme {
    Alert(
      icon = {
        Icon(
          painter = painterResource(id = R.drawable.ic_round_speaker),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onBackground
        )
      },
      title = {
        Text(
          text = stringResource(id = R.string.msg_gain),
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      },
      positiveButton = {
        FilledIconButton(
          onClick = onPositiveClick,
          modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_round_check),
            contentDescription = stringResource(id = R.string.wear_title_settings),
            tint = IconButtonDefaults.filledIconButtonColors().contentColor
          )
        }
      },
      negativeButton = {
        FilledTonalIconButton(
          onClick = onNegativeClick,
          modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.DefaultButtonSize)
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_round_close),
            contentDescription = stringResource(id = R.string.wear_title_settings),
            tint = IconButtonDefaults.filledTonalIconButtonColors().contentColor
          )
        }
      },
      contentPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 24.dp,
        bottom = 24.dp
      )
    ) {
      Text(
        text = stringResource(id = R.string.msg_gain_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}