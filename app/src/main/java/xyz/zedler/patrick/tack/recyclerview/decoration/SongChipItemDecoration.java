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

package xyz.zedler.patrick.tack.recyclerview.decoration;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SongChipItemDecoration extends RecyclerView.ItemDecoration {

  private final int outerPadding, innerPadding;
  private final boolean isRtl;

  public SongChipItemDecoration(int outerPadding, int innerPadding, boolean isRtl) {
    this.outerPadding = outerPadding;
    this.innerPadding = innerPadding;
    this.isRtl = isRtl;
  }

  @Override
  public void getItemOffsets(
      @NonNull Rect outRect, @NonNull View view,
      @NonNull RecyclerView parent, @NonNull RecyclerView.State state
  ) {
    int position = parent.getChildAdapterPosition(view);
    if (position == 0) {
      outRect.left = isRtl ? innerPadding : outerPadding;
      outRect.right = isRtl ? outerPadding : innerPadding;
    } else {
      outRect.left = innerPadding;
      outRect.right = innerPadding;
    }

    int count = state.getItemCount();
    if (position == count - 1) {
      outRect.right = isRtl ? innerPadding : outerPadding;
      outRect.left = isRtl ? outerPadding : innerPadding;
    }
    if (count == 1 && position == 0) {
      outRect.left = outerPadding;
      outRect.right = outerPadding;
    }
  }
}
