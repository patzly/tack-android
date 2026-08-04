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

package xyz.zedler.patrick.tack.util

import android.content.SharedPreferences
import android.view.View
import android.view.ViewTreeObserver

inline fun SharedPreferences.edit(action: SharedPreferences.Editor.() -> Unit) {
  val editor = edit()
  action(editor)
  editor.apply()
}

fun View.onGlobalLayout(action: () -> Unit) {
  viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
    override fun onGlobalLayout() {
      action()
      viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
  })
}

fun View.setDividerVisibility(
  visible: Boolean,
  dividerTop: View,
  dividerBottom: View,
  container: View
) {
  dividerTop.visibility = if (visible) View.VISIBLE else View.GONE
  dividerBottom.visibility = if (visible) View.VISIBLE else View.GONE
  val padding = if (visible) container.context.dpToPx(16f) else 0
  container.setPadding(
    container.paddingLeft,
    padding,
    container.paddingRight,
    padding
  )
}
