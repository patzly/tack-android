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

package xyz.zedler.patrick.tack.recyclerview.layoutmanager

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WrapperLinearLayoutManager : LinearLayoutManager {

  companion object {
    private val TAG = WrapperLinearLayoutManager::class.java.simpleName
  }

  constructor(context: Context?) : super(context)

  constructor(
    context: Context?,
    orientation: Int,
    reverseLayout: Boolean
  ) : super(context, orientation, reverseLayout)

  override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
    // Fix for IndexOutOfBoundsException: Inconsistency detected. Invalid item position
    try {
      super.onLayoutChildren(recycler, state)
    } catch (e: IndexOutOfBoundsException) {
      Log.e(TAG, "onLayoutChildren: ", e)
    }
  }
}
