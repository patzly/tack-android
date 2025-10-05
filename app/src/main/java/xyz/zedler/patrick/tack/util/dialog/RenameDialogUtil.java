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

package xyz.zedler.patrick.tack.util.dialog;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.databinding.PartialDialogPartRenameBinding;
import xyz.zedler.patrick.tack.fragment.SongFragment;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class RenameDialogUtil {

  private static final String TAG = RenameDialogUtil.class.getSimpleName();

  private static final String PART_ID = "part_id_dialog";
  private static final String PART_NAME_PREV = "part_name_prev_dialog";
  private static final String PART_INDEX = "part_index_dialog";

  private final MainActivity activity;
  private final SongFragment fragment;
  private final PartialDialogPartRenameBinding binding;
  private final DialogUtil dialogUtil;
  private String partId;
  private String partNamePrev;
  private int partIndex;

  public RenameDialogUtil(MainActivity activity, SongFragment fragment) {
    this.activity = activity;
    this.fragment = fragment;

    binding = PartialDialogPartRenameBinding.inflate(activity.getLayoutInflater());
    binding.editTextPartRename.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_DONE) {
            activity.performHapticClick();
            rename();
          }
          return false;
        });

    dialogUtil = new DialogUtil(activity, "part_rename");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.action_rename_part);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(R.string.action_rename, (dialog, which) -> {
        activity.performHapticClick();
        rename();
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> activity.performHapticClick()
      );
    });

    setDividerVisibility(!UiUtil.isOrientationPortrait(activity));
  }

  public void show() {
    update();
    dialogUtil.show();
    showKeyboard();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    partId = state != null ? state.getString(PART_ID) : null;
    partNamePrev = state != null ? state.getString(PART_NAME_PREV) : null;
    partIndex = state != null ? state.getInt(PART_INDEX) : 0;
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
    outState.putString(PART_ID, partId);
    outState.putString(PART_NAME_PREV, partNamePrev);
    outState.putInt(PART_INDEX, partIndex);
  }

  public void update() {
    if (binding == null) {
      return;
    }
    binding.editTextPartRename.setText(partNamePrev);
    Editable text = binding.editTextPartRename.getText();
    binding.editTextPartRename.setSelection(text != null ? text.length() : 0);
    // placeholder
    binding.editTextPartRename.setHint(
        activity.getString(R.string.label_part_unnamed, partIndex + 1)
    );

    measureScrollView();
  }

  public void setPart(@NonNull Part part) {
    partId = part.getId();
    partNamePrev = part.getName();
    partIndex = part.getPartIndex();
    update();
  }

  private void rename() {
    Editable text = binding.editTextPartRename.getText();
    String name = null;
    if (text != null) {
      name = text.toString();
      if (name.trim().isEmpty()) {
        name = null;
      }
    }
    fragment.renamePart(partId, name);
  }

  private void showKeyboard() {
    if (binding != null) {
      binding.editTextPartRename.requestFocus();
      UiUtil.showKeyboard(binding.editTextPartRename);
    }
  }

  private void measureScrollView() {
    binding.scrollPartRename.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.scrollPartRename.canScrollVertically(-1)
                || binding.scrollPartRename.canScrollVertically(1);
            setDividerVisibility(isScrollable);
            binding.scrollPartRename.getViewTreeObserver()
                .removeOnGlobalLayoutListener(this);
          }
        });
  }

  private void setDividerVisibility(boolean visible) {
    binding.dividerPartRenameTop.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.dividerPartRenameBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.linearPartRenameContainer.setPadding(
        binding.linearPartRenameContainer.getPaddingLeft(),
        visible ? UiUtil.dpToPx(activity, 16) : 0,
        binding.linearPartRenameContainer.getPaddingRight(),
        visible ? UiUtil.dpToPx(activity, 16) : 0
    );
  }
}
