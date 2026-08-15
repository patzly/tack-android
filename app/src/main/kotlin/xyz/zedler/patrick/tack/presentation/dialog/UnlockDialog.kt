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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.component.AlertDialogFlowRow
import xyz.zedler.patrick.tack.presentation.component.ProvideContentColorTextStyle
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.presentation.util.LocalHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockDialog(onDismissRequest: () -> Unit = {}) {
  val haptic = LocalHaptic.current

  BasicAlertDialog(onDismissRequest = onDismissRequest) {
    UnlockDialogContent(
      onOpenClick = {
        haptic.click()
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
private fun UnlockDialogContent(
  onOpenClick: () -> Unit = {},
  onCancelClick: () -> Unit = {}
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
      val isScrollable by remember { derivedStateOf { scrollState.maxValue > 0 } }

      if (isScrollable) {
        HorizontalDivider()
      }

      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .verticalScroll(scrollState)
          .padding(horizontal = 24.dp)
      ) {
        Text(
          text = stringResource(R.string.msg_unlock_description),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = if (isScrollable) 16.dp else 0.dp)
        )
      }

      if (isScrollable) {
        HorizontalDivider()
      }

      Box(
        modifier = Modifier
          .align(Alignment.End)
          .padding(24.dp)
      ) {
        ProvideContentColorTextStyle(
          contentColor = MaterialTheme.colorScheme.primary,
          textStyle = MaterialTheme.typography.labelLarge,
          content = {
            val buttonPaddingFromMICS =
              LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } -
                  ButtonDefaults.MinHeight
            AlertDialogFlowRow(
              mainAxisSpacing = 8.dp,
              crossAxisSpacing = (8.dp - buttonPaddingFromMICS).coerceIn(0.dp, 8.dp)
            ) {
              TextButton(
                onClick = onOpenClick,
                shapes = ButtonDefaults.shapes()
              ) {
                Text(stringResource(R.string.action_open_play_store))
              }

              TextButton(
                onClick = onCancelClick,
                shapes = ButtonDefaults.shapes()
              ) {
                Text(stringResource(R.string.action_cancel))
              }
            }
          }
        )
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
