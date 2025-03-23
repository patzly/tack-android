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

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.databinding.PartialDialogPartRenameBinding;
import xyz.zedler.patrick.tack.databinding.PartialDialogTempoBinding;
import xyz.zedler.patrick.tack.fragment.MainFragment;
import xyz.zedler.patrick.tack.fragment.SongFragment;

public class RenameDialogUtil {

  private static final String TAG = RenameDialogUtil.class.getSimpleName();

  private final MainActivity activity;
  private final PartialDialogPartRenameBinding binding;
  private final DialogUtil dialogUtil;
  private Part part;
  private OnRenameListener onRenameListener;

  public RenameDialogUtil(MainActivity activity) {
    this.activity = activity;

    binding = PartialDialogPartRenameBinding.inflate(activity.getLayoutInflater());
    binding.editTextName.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        activity.performHapticClick();
        rename();
      }
      return false;
    });

    dialogUtil = new DialogUtil(activity, "part_rename");
    dialogUtil.createApplyCustom(R.string.action_rename_part, binding.getRoot(), () -> {
      activity.performHapticClick();
      rename();
    });
  }

  public void show() {
    update();
    dialogUtil.show();
    showKeyboard();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    update();
    boolean showing = dialogUtil.showIfWasShown(state);
    if (showing) {
      showKeyboard();
    }
  }

  public void dismiss() {
    dialogUtil.dismiss();
  }

  public void saveState(@NonNull Bundle outState) {
    if (dialogUtil != null) {
      dialogUtil.saveState(outState);
    }
  }

  public void update() {
    if (binding == null) {
      return;
    }
    binding.editTextName.setText(part != null ? part.getName() : null);
    Editable text = binding.editTextName.getText();
    binding.editTextName.setSelection(text != null ? text.length() : 0);
    // placeholder
    if (part != null) {
      binding.editTextName.setHint(
          activity.getString(R.string.label_part_unnamed, part.getPartIndex() + 1)
      );
    }
  }

  public void setPart(@Nullable Part part, @Nullable OnRenameListener onRenameListener) {
    this.part = part;
    this.onRenameListener = onRenameListener;
    update();
  }

  private void rename() {
    Editable text = binding.editTextName.getText();
    String name = null;
    if (text != null) {
      name = text.toString();
      if (name.trim().isEmpty()) {
        name = null;
      }
    }
    if (onRenameListener != null) {
      onRenameListener.onRename(name);
    }
  }

  private void showKeyboard() {
    if (binding != null) {
      binding.editTextName.requestFocus();
      UiUtil.showKeyboard(activity, binding.editTextName);
    }
  }

  public interface OnRenameListener {
    void onRename(@Nullable String name);
  }
}
