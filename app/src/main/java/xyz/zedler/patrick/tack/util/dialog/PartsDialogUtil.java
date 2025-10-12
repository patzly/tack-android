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
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.PartialDialogPartsTitleBinding;
import xyz.zedler.patrick.tack.databinding.PartialDialogRecyclerBinding;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.recyclerview.adapter.PartDialogAdapter;
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager;
import xyz.zedler.patrick.tack.util.DialogUtil;

public class PartsDialogUtil {

  private static final String TAG = PartsDialogUtil.class.getSimpleName();

  private final MainActivity activity;
  private final PartialDialogPartsTitleBinding titleBinding;
  private final PartialDialogRecyclerBinding binding;
  private final DialogUtil dialogUtil;
  private final PartDialogAdapter adapter;

  public PartsDialogUtil(MainActivity activity) {
    this.activity = activity;

    titleBinding = PartialDialogPartsTitleBinding.inflate(activity.getLayoutInflater());

    binding = PartialDialogRecyclerBinding.inflate(activity.getLayoutInflater());
    dialogUtil = new DialogUtil(activity, "parts");

    binding.recyclerDialog.setLayoutManager(new WrapperLinearLayoutManager(activity));
    adapter = new PartDialogAdapter((partIndex, fromUser) -> {
      if (fromUser && getMetronomeEngine() != null) {
        activity.performHapticClick();
        getMetronomeEngine().setCurrentPartIndex(partIndex);
      }
    });
    binding.recyclerDialog.setAdapter(adapter);

    dialogUtil.createDialog(builder -> {
      builder.setCustomTitle(titleBinding.getRoot());
      builder.setView(binding.getRoot());
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> activity.performHapticClick()
      );
    });
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
    MetronomeEngine metronomeEngine = getMetronomeEngine();
    if (binding == null || titleBinding == null || metronomeEngine == null) {
      return;
    }
    SongWithParts songWithParts = metronomeEngine.getCurrentSongWithParts();
    if (songWithParts != null) {
      titleBinding.textDialogPartsTitle.setText(songWithParts.getSong().getName());
      // part count
      int partCount = songWithParts.getParts().size();
      titleBinding.textDialogPartsCount.setText(
          activity.getResources().getQuantityString(
              R.plurals.label_parts_count, partCount, partCount
          )
      );
      // song duration
      boolean hasDuration = true;
      for (Part part : songWithParts.getParts()) {
        if (part.getTimerDuration() == 0) {
          hasDuration = false;
          break;
        }
      }
      if (hasDuration) {
        titleBinding.textDialogPartsDuration.setText(songWithParts.getDurationString());
      } else {
        titleBinding.textDialogPartsDuration.setText(R.string.label_part_no_duration);
      }
      // looped
      titleBinding.textDialogPartsLooped.setText(
          activity.getString(
              songWithParts.getSong().isLooped()
                  ? R.string.label_song_looped
                  : R.string.label_song_not_looped
          )
      );
    } else {
      // Don't show dialog if no song is selected
      dismiss();
    }

    adapter.setSongWithParts(songWithParts);
    adapter.setPartIndex(metronomeEngine.getCurrentPartIndex());
    maybeShowDividers();
  }

  private void maybeShowDividers() {
    binding.recyclerDialog.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.recyclerDialog.canScrollVertically(-1)
                || binding.recyclerDialog.canScrollVertically(1);
            binding.dividerDialogTop.setVisibility(isScrollable ? View.VISIBLE : View.GONE);
            binding.dividerDialogBottom.setVisibility(isScrollable ? View.VISIBLE : View.GONE);
            binding.recyclerDialog.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  @Nullable
  private MetronomeEngine getMetronomeEngine() {
    return activity.getMetronomeEngine();
  }
}