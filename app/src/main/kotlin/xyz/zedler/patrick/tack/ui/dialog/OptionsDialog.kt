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
import androidx.compose.foundation.layout.fillMaxWidth
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
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.component.core.LeadingContentWrapper
import xyz.zedler.patrick.tack.ui.component.core.ScrollableAlertDialog
import xyz.zedler.patrick.tack.ui.component.core.ScrollableAlertDialogContent
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsDialog(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  onRateClick: () -> Unit = {},
  onRecommendClick: () -> Unit = {},
) {
  val haptic = LocalHaptic.current

  ScrollableAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  ) {
    OptionsDialogContent(
      onRateClick = {
        haptic.click()
        onRateClick()
      },
      onRecommendClick = {
        haptic.click()
        onRecommendClick()
      },
      onCloseClick = {
        haptic.click()
        onDismissRequest()
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsDialogContent(
  modifier: Modifier = Modifier,
  onRateClick: () -> Unit = {},
  onRecommendClick: () -> Unit = {},
  onCloseClick: () -> Unit = {},
) {
  ScrollableAlertDialogContent(
    modifier = modifier,
    title = {
      Text(stringResource(R.string.title_options))
    },
    confirmButton = {
      TextButton(
        onClick = onCloseClick,
        shapes = ButtonDefaults.shapes(),
      ) {
        Text(stringResource(R.string.action_close))
      }
    },
  ) {
    OptionsContent(
      onRateClick = onRateClick,
      onRecommendClick = onRecommendClick,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsContent(
  modifier: Modifier = Modifier,
  onRateClick: () -> Unit = {},
  onRecommendClick: () -> Unit = {},
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
  ) {
    val itemCount = 2

    val colors = ListItemDefaults.colors(
      containerColor = MaterialTheme.colorScheme.surfaceBright,
    )

    SegmentedListItem(
      onClick = onRateClick,
      shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
      colors = colors,
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      leadingContent = {
        LeadingContentWrapper {
          Icon(
            painter = painterResource(R.drawable.ic_rounded_star),
            contentDescription = null,
          )
        }
      },
      supportingContent = {
        Text(stringResource(R.string.action_rate_description))
      },
    ) {
      Text(stringResource(R.string.action_rate))
    }

    SegmentedListItem(
      onClick = onRecommendClick,
      shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
      colors = colors,
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      leadingContent = {
        LeadingContentWrapper {
          Icon(
            painter = painterResource(R.drawable.ic_rounded_group),
            contentDescription = null,
          )
        }
      },
      supportingContent = {
        Text(stringResource(R.string.action_recommend_description))
      },
    ) {
      Text(stringResource(R.string.action_recommend))
    }
  }
}

@Preview
@Composable
private fun OptionsDialogPreview() {
  TackTheme {
    OptionsDialogContent()
  }
}
