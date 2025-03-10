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

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogPartsBinding;
import xyz.zedler.patrick.tack.fragment.MainFragment;
import xyz.zedler.patrick.tack.recyclerview.adapter.PartsDialogAdapter;

public class PartsDialogUtil {

  private static final String TAG = PartsDialogUtil.class.getSimpleName();

  private final MainActivity activity;
  private final MainFragment fragment;
  private final PartialDialogPartsBinding binding;
  private final DialogUtil dialogUtil;
  private final PartsDialogAdapter adapter;

  public PartsDialogUtil(MainActivity activity, MainFragment fragment) {
    this.activity = activity;
    this.fragment = fragment;

    binding = PartialDialogPartsBinding.inflate(activity.getLayoutInflater());
    dialogUtil = new DialogUtil(activity, "parts");

    binding.recyclerParts.setLayoutManager(new LinearLayoutManager(activity));
    adapter = new PartsDialogAdapter((partIndex, fromUser) -> {
      if (fromUser) {
        fragment.performHapticClick();
        getMetronomeUtil().setCurrentPartIndex(partIndex, true);
      }
    });
    binding.recyclerParts.setAdapter(adapter);

    dialogUtil.createCloseCustom(getMetronomeUtil().getCurrentSong(), binding.getRoot());
  }

  public void show() {
    update();
    dialogUtil.show();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    update();
    dialogUtil.showIfWasShown(state);
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
    dialogUtil.setTitle(getMetronomeUtil().getCurrentSong());
    adapter.setSongWithParts(getMetronomeUtil().getCurrentSongWithParts());
    adapter.setPartIndex(getMetronomeUtil().getCurrentPartIndex());
    maybeShowDividers();
  }

  private void maybeShowDividers() {
    binding.recyclerParts.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.recyclerParts.canScrollVertically(-1)
                || binding.recyclerParts.canScrollVertically(1);
            binding.dividerDialogPartsTop.setVisibility(isScrollable ? View.VISIBLE : View.GONE);
            binding.dividerDialogPartsBottom.setVisibility(isScrollable ? View.VISIBLE : View.GONE);
            binding.recyclerParts.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  private MetronomeUtil getMetronomeUtil() {
    return activity.getMetronomeUtil();
  }
}