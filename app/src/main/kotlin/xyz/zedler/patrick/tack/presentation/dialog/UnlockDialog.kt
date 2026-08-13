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

package xyz.zedler.patrick.tack.presentation.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockDialog(
  onDismissRequest: () -> Unit = {}
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = {
      Text(stringResource(R.string.msg_unlock))
    },
    text = {
      Text(stringResource(R.string.msg_unlock_description))
    },
    confirmButton = {
      TextButton(
        onClick = {
          onDismissRequest()
        },
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_open_play_store))
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          onDismissRequest()
        },
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    }
  )
}

@Preview
@Composable
fun UnlockDialogPreview() {
  TackTheme {
    UnlockDialog()
  }
}
