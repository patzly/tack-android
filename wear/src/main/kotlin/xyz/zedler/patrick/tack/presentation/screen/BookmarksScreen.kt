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

package xyz.zedler.patrick.tack.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SplitRadioButton
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.dialog.DeletionDialog
import xyz.zedler.patrick.tack.presentation.state.Bookmark
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun BookmarksScreen(
  viewModel: MainViewModel = MainViewModel()
) {
  TackTheme {
    val state by viewModel.state.collectAsState()
    val allowAdding = remember { derivedStateOf {
      state.bookmarks.none {
        it.tempo == state.tempo
            && it.beats == state.beats
            && it.subdivisions == state.subdivisions
      } && state.bookmarks.size < Constants.BOOKMARKS_MAX
    } }
    val scrollableState = rememberScalingLazyListState()
    ScreenScaffold(
      scrollState = scrollableState,
      edgeButton = {
        BottomButton(
          onClick = {
            viewModel.addBookmark()
          },
          enabled = allowAdding.value
        )
      },
      modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
    ) {
      Box(
        modifier = Modifier.fillMaxSize()
      ) {
        var showDeletionDialog by remember { mutableStateOf(false) }
        var deletionBookmark by remember { mutableStateOf<Bookmark?>(null) }
        ScalingLazyColumn(
          state = scrollableState
        ) {
          item {
            ListHeader(
              // Necessary padding to prevent cut-off by time text
              contentPadding = PaddingValues(top = 24.dp, bottom = 8.dp)
            ) {
              Text(
                text = stringResource(id = R.string.wear_title_bookmarks),
                style = MaterialTheme.typography.titleMedium
              )
            }
          }
          if (state.bookmarks.isNotEmpty()) {
            items(state.bookmarks) { bookmark ->
              BookmarkOption(
                bookmark = bookmark,
                selected = state.tempo == bookmark.tempo
                    && state.beats == bookmark.beats
                    && state.subdivisions == bookmark.subdivisions,
                onSelectionClick = {
                  viewModel.updateFromBookmark(bookmark)
                },
                onContainerClick = {
                  deletionBookmark = bookmark
                  showDeletionDialog = true
                }
              )
            }
          } else {
            item {
              Text(
                text = stringResource(id = R.string.wear_msg_empty_bookmarks),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                  .padding(16.dp)
                  .fillMaxWidth()
              )
            }
          }
        }
        DeletionDialog(
          visible = showDeletionDialog,
          bookmark = deletionBookmark,
          onConfirm = {
            viewModel.deleteBookmark(it)
            showDeletionDialog = false
          },
          onDismiss = {
            showDeletionDialog = false
          }
        )
      }
    }
  }
}

@Composable
fun BottomButton(
  onClick: () -> Unit,
  enabled: Boolean = true
) {
  EdgeButton(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.tertiary,
      contentColor = MaterialTheme.colorScheme.onTertiary
    ),
    modifier = Modifier.padding(bottom = 4.dp),
    enabled = enabled
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_rounded_bookmark_add),
      contentDescription = stringResource(id = R.string.wear_action_bookmark)
    )
  }
}

@Composable
fun BookmarkOption(
  bookmark: Bookmark,
  selected: Boolean,
  onSelectionClick: () -> Unit,
  onContainerClick: () -> Unit
) {
  SplitRadioButton(
    selected = selected,
    onSelectionClick = onSelectionClick,
    selectionContentDescription = null,
    onContainerClick = onContainerClick,
    label = {
      Text(
        text = stringResource(id = R.string.wear_label_bpm_value, bookmark.tempo),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    },
    secondaryLabel = {
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = pluralStringResource(
            id = R.plurals.wear_label_beats, bookmark.beats.size, bookmark.beats.size
          ),
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = pluralStringResource(
            id = R.plurals.wear_label_subs, bookmark.subdivisions.size, bookmark.subdivisions.size
          ),
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    },
    modifier = Modifier.fillMaxWidth()
  )
}