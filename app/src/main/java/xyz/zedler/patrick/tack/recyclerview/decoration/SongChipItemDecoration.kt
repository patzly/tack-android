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

package xyz.zedler.patrick.tack.recyclerview.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SongChipItemDecoration(
  private val outerPadding: Int,
  private val innerPadding: Int,
  private val isRtl: Boolean
) : RecyclerView.ItemDecoration() {

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    val position = parent.getChildAdapterPosition(view)
    if (position == 0) {
      outRect.left = if (isRtl) innerPadding else outerPadding
      outRect.right = if (isRtl) outerPadding else innerPadding
    } else {
      outRect.left = innerPadding
      outRect.right = innerPadding
    }

    val count = state.itemCount
    if (position == count - 1) {
      outRect.right = if (isRtl) innerPadding else outerPadding
      outRect.left = if (isRtl) outerPadding else innerPadding
    }
    if (count == 1 && position == 0) {
      outRect.left = outerPadding
      outRect.right = outerPadding
    }
  }
}
