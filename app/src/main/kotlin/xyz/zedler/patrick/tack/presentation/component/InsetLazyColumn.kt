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

package xyz.zedler.patrick.tack.presentation.component

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A [LazyColumn] that applies symmetric horizontal padding based on window insets
 * and limits the width of its items to a maximum of 640.dp.
 */
@Composable
fun InsetLazyColumn(
  modifier: Modifier = Modifier,
  state: LazyListState = rememberLazyListState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
  horizontalBasePadding: Dp = 16.dp,
  reverseLayout: Boolean = false,
  verticalArrangement: Arrangement.Vertical =
    if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
  flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
  userScrollEnabled: Boolean = true,
  content: LazyListScope.() -> Unit
) {
  val layoutDirection = LocalLayoutDirection.current
  val safePadding = WindowInsets.safeDrawing.asPaddingValues()
  val maxInset = maxOf(
    safePadding.calculateStartPadding(layoutDirection),
    safePadding.calculateEndPadding(layoutDirection)
  )
  val horizontalPadding = maxInset + horizontalBasePadding

  LazyColumn(
    modifier = modifier,
    state = state,
    contentPadding = PaddingValues(
      start = horizontalPadding,
      end = horizontalPadding,
      top = contentPadding.calculateTopPadding(),
      bottom = contentPadding.calculateBottomPadding()
    ),
    reverseLayout = reverseLayout,
    verticalArrangement = verticalArrangement,
    horizontalAlignment = Alignment.CenterHorizontally,
    flingBehavior = flingBehavior,
    userScrollEnabled = userScrollEnabled,
    content = content
  )
}

/**
 * An item in an [InsetLazyColumn] that is automatically constrained to 640.dp.
 */
fun LazyListScope.insetItem(
  key: Any? = null,
  contentType: Any? = null,
  maxWidth: Dp = 640.dp,
  content: @Composable LazyItemScope.() -> Unit
) {
  item(key = key, contentType = contentType) {
    Box(
      modifier = Modifier
        .widthIn(max = maxWidth)
        .fillMaxWidth(),
      contentAlignment = Alignment.CenterStart
    ) {
      content()
    }
  }
}

/**
 * Multiple items in an [InsetLazyColumn] that are automatically constrained to 640.dp.
 */
fun <T> LazyListScope.insetItems(
  items: List<T>,
  key: ((item: T) -> Any)? = null,
  contentType: (item: T) -> Any? = { null },
  maxWidth: Dp = 640.dp,
  itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
  items(
    items = items,
    key = key,
    contentType = contentType,
    itemContent = { item ->
      Box(
        modifier = Modifier
          .widthIn(max = maxWidth)
          .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
      ) {
        itemContent(item)
      }
    }
  )
}
