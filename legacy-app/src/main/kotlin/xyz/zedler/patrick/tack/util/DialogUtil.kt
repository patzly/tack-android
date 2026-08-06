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

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.zedler.patrick.tack.R

class DialogUtil(private val context: Context, private val tag: String) {

  private var dialog: AlertDialog? = null

  fun createDialog(listener: OnBuilderReadyListener) {
    val builder = MaterialAlertDialogBuilder(
      context, R.style.ThemeOverlay_Tack_AlertDialog
    )
    listener.onBuilderReady(builder)
    dialog = builder.create()
  }

  fun createDialogError(listener: OnBuilderReadyListener) {
    val builder = MaterialAlertDialogBuilder(
      context, R.style.ThemeOverlay_Tack_AlertDialog_Error
    )
    listener.onBuilderReady(builder)
    dialog = builder.create()
  }

  fun show() {
    val d = dialog
    if (d != null && !d.isShowing) {
      d.show()
    } else if (d == null) {
      throw IllegalStateException("Dialog for $tag not created before showing")
    }
  }

  fun showIfWasShown(state: Bundle?): Boolean {
    val wasShowing = wasShown(state)
    if (wasShowing) {
      Handler(Looper.getMainLooper()).postDelayed({ show() }, 10)
    }
    return wasShowing
  }

  fun wasShown(state: Bundle?): Boolean {
    return state != null && state.getBoolean(IS_SHOWING + tag)
  }

  fun setOnShowListener(listener: DialogInterface.OnShowListener) {
    dialog?.setOnShowListener(listener)
  }

  fun saveState(outState: Bundle) {
    outState.putBoolean(IS_SHOWING + tag, dialog?.isShowing == true)
  }

  /**
   * Must be called in onDestroy, else an exception will be thrown when orientation changes
   */
  fun dismiss() {
    val d = dialog
    if (d != null && d.isShowing) {
      d.dismiss()
    }
  }

  fun getDialog(): AlertDialog? = dialog

  fun interface OnBuilderReadyListener {
    fun onBuilderReady(builder: MaterialAlertDialogBuilder)
  }

  companion object {
    private val TAG = DialogUtil::class.java.simpleName
    private const val IS_SHOWING = "is_showing_dialog_"
  }
}
