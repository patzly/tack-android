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

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.component.ScrollableAlertDialog
import xyz.zedler.patrick.tack.ui.component.ScrollableAlertDialogContent
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic

private data class HelpItem(
  @get:StringRes val question: Int,
  @get:StringRes val answer: Int,
  val showTranslate: Boolean = false
)

@Composable
fun HelpDialog(onDismissRequest: () -> Unit) {
  val context = LocalContext.current
  val haptic = LocalHaptic.current
  val appTranslate = stringResource(R.string.app_translate)

  ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
    HelpDialogContent(
      onItemClick = {
        haptic.click()
      },
      onTranslateClick = {
        context.startActivity(Intent(Intent.ACTION_VIEW, appTranslate.toUri()))
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
private fun HelpDialogContent(
  onItemClick: () -> Unit = {},
  onTranslateClick: () -> Unit = {},
  onCloseClick: () -> Unit = {}
) {
  val helpItems = remember {
    listOf(
      HelpItem(R.string.help_tempo_change, R.string.help_tempo_change_answer),
      HelpItem(R.string.help_time_signature, R.string.help_time_signature_answer),
      HelpItem(R.string.help_swing, R.string.help_swing_answer),
      HelpItem(R.string.help_song_library, R.string.help_song_library_answer),
      HelpItem(R.string.help_song_picker, R.string.help_song_picker_answer),
      HelpItem(R.string.help_song_part, R.string.help_song_part_answer),
      HelpItem(R.string.help_notification_displays, R.string.help_notification_displays_answer),
      HelpItem(R.string.help_notification_disappears, R.string.help_notification_disappears_answer),
      HelpItem(R.string.help_translation, R.string.help_translation_answer, true)
    )
  }

  var expandedIndices by rememberSaveable { mutableStateOf(setOf<Int>()) }

  ScrollableAlertDialogContent(
    title = { Text(stringResource(R.string.title_help)) },
    confirmButton = {
      TextButton(
        onClick = onCloseClick,
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_close))
      }
    },
    isScrollableControlledByContent = true
  ) {
    val motionScheme = MaterialTheme.motionScheme

    val colors = ListItemDefaults.segmentedColors(
      containerColor = MaterialTheme.colorScheme.surfaceBright
    )

    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
      itemsIndexed(helpItems) { index, item ->
        val expanded = expandedIndices.contains(index)
        SegmentedListItem(
          onClick = {
            onItemClick()
            expandedIndices = if (expanded) {
              expandedIndices - index
            } else {
              expandedIndices + index
            }
          },
          shapes = ListItemDefaults.segmentedShapes(index = index, count = helpItems.size),
          colors = colors,
          content = { Text(stringResource(item.question)) },
          contentPadding = PaddingValues(16.dp),
          supportingContent = {
            // Using AnimatedVisibility with height-only transitions for a physical reveal effect.
            // The content is revealed/concealed as the container expands/shrinks.
            AnimatedVisibility(
              visible = expanded,
              enter = expandVertically(
                animationSpec = motionScheme.defaultSpatialSpec(),
                expandFrom = Alignment.Top
              ),
              exit = shrinkVertically(
                animationSpec = motionScheme.defaultSpatialSpec(),
                shrinkTowards = Alignment.Top
              )
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                Text(
                  text = stringResource(item.answer),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.showTranslate) {
                  TextButton(
                    onClick = {
                      onItemClick()
                      onTranslateClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                      containerColor = MaterialTheme.colorScheme.secondaryContainer,
                      contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shapes = ButtonDefaults.shapes()
                  ) {
                    Text(stringResource(R.string.about_translation))
                  }
                }
              }
            }
          },
          modifier = Modifier.heightIn(min = 48.dp)
        )
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun HelpDialogPreview() {
  TackTheme {
    HelpDialogContent()
  }
}
