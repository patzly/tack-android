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

package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.tack.R;

public class DialogUtil {

  private static final String TAG = DialogUtil.class.getSimpleName();
  private static final String IS_SHOWING = "is_showing_dialog_";

  private final Context context;
  private final String tag;
  private AlertDialog dialog;

  public DialogUtil(@NonNull Context context, @NonNull String tag) {
    this.context = context;
    this.tag = tag;
  }

  public void createDialog(@NonNull OnBuilderReadyListener listener) {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
        context, R.style.ThemeOverlay_Tack_AlertDialog
    );
    listener.onBuilderReady(builder);
    dialog = builder.create();
  }

  public void createDialogError(@NonNull OnBuilderReadyListener listener) {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
        context, R.style.ThemeOverlay_Tack_AlertDialog_Error
    );
    listener.onBuilderReady(builder);
    dialog = builder.create();
  }

  public void show() {
    if (dialog != null && !dialog.isShowing()) {
      dialog.show();
    } else if (dialog == null) {
      throw new IllegalStateException("Dialog for " + tag + " not created before showing");
    }
  }

  public boolean showIfWasShown(@Nullable Bundle state) {
    boolean wasShowing = wasShown(state);
    if (wasShowing) {
      new Handler(Looper.getMainLooper()).postDelayed(this::show, 10);
    }
    return wasShowing;
  }

  public boolean wasShown(@Nullable Bundle state) {
    return state != null && state.getBoolean(IS_SHOWING + tag);
  }

  public void setOnShowListener(@NonNull DialogInterface.OnShowListener listener) {
    if (dialog != null) {
      dialog.setOnShowListener(listener);
    }
  }

  public void saveState(@NonNull Bundle outState) {
    outState.putBoolean(IS_SHOWING + tag, dialog != null && dialog.isShowing());
  }

  /**
   * Must be called in onDestroy, else an exception will be thrown when orientation changes
   */
  public void dismiss() {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
    }
  }

  @Nullable
  public AlertDialog getDialog() {
    return dialog;
  }

  public interface OnBuilderReadyListener {
    void onBuilderReady(@NonNull MaterialAlertDialogBuilder builder);
  }
}
