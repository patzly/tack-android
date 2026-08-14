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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.presentation.util.LocalHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockDialog(onDismissRequest: () -> Unit = {}) {
  val haptic = LocalHaptic.current

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
          haptic.click()
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
          haptic.click()
          onDismissRequest()
        },
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnlockDialogContent(
  onCloseClick: () -> Unit = {}
) {
  Surface(
    shape = AlertDialogDefaults.shape,
    color = AlertDialogDefaults.containerColor,
    tonalElevation = AlertDialogDefaults.TonalElevation,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(max = 600.dp)
      .padding(vertical = 24.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = stringResource(R.string.msg_unlock),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)
      )

      val scrollState = rememberScrollState()

      HorizontalDivider()

      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .verticalScroll(scrollState)
          .padding(horizontal = 24.dp)
      ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
          Text(
            text = stringResource(R.string.msg_unlock_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      HorizontalDivider()

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(all = 24.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = onCloseClick) {
          Text(stringResource(R.string.action_close))
        }
      }
    }
  }
}

@Preview
@Composable
fun UnlockDialogPreview() {
  TackTheme {
    UnlockDialogContent()
  }
}
