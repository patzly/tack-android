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

package xyz.zedler.patrick.tack.recyclerview.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.RowSongChipBinding;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class SongChipAdapter extends ListAdapter<SongWithParts, ViewHolder> {

  private final static String TAG = SongChipAdapter.class.getSimpleName();

  private final OnSongClickListener listener;
  private final int colorStroke;
  private boolean clickable;

  public SongChipAdapter(
      @NonNull Context context, @NonNull OnSongClickListener listener, boolean clickable
  ) {
    super(new SongWithPartsDiffCallback());
    this.listener = listener;
    this.clickable = clickable;

    boolean isDark = UiUtil.isDarkModeActive(context);
    colorStroke = ResUtil.getColor(
        context, isDark ? R.attr.colorSurfaceBright : R.attr.colorOutlineVariant
    );
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    RowSongChipBinding binding = RowSongChipBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false
    );
    return new SongChipViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    SongWithParts songWithParts = getItem(holder.getBindingAdapterPosition());
    SongChipViewHolder songHolder = (SongChipViewHolder) holder;
    songHolder.binding.textSong.setText(songWithParts.getSong().getName());
    songHolder.binding.cardSong.setStrokeColor(colorStroke);
    songHolder.binding.cardSong.setClickable(clickable);
    if (clickable) {
      songHolder.binding.frameSong.setOnClickListener(
          v -> listener.onSongClick(songWithParts)
      );
      songHolder.binding.cardSong.setOnClickListener(
          v -> songHolder.binding.frameSong.callOnClick()
      );
      songHolder.binding.frameSong.setOnLongClickListener(v -> {
        listener.onSongLongClick(songWithParts);
        return true;
      });
      songHolder.binding.cardSong.setOnLongClickListener(v -> {
        listener.onSongLongClick(songWithParts);
        return true;
      });
    } else {
      songHolder.binding.frameSong.setOnClickListener(null);
      songHolder.binding.frameSong.setOnLongClickListener(null);
      songHolder.binding.cardSong.setOnClickListener(null);
      songHolder.binding.cardSong.setOnLongClickListener(null);
    }
  }

  @SuppressLint("NotifyDataSetChanged")
  public void setClickable(boolean clickable) {
    if ((this.clickable && !clickable) || (!this.clickable && clickable)) {
      this.clickable = clickable;
      notifyDataSetChanged();
    }
  }

  public static class SongChipViewHolder extends ViewHolder {

    private final RowSongChipBinding binding;

    public SongChipViewHolder(RowSongChipBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public interface OnSongClickListener {
    void onSongClick(@NonNull SongWithParts song);
    void onSongLongClick(@NonNull SongWithParts song);
  }

  static class SongWithPartsDiffCallback extends DiffUtil.ItemCallback<SongWithParts> {

    @Override
    public boolean areItemsTheSame(
        @NonNull SongWithParts oldItem,
        @NonNull SongWithParts newItem
    ) {
      return oldItem.getSong().getId().equals(newItem.getSong().getId());
    }

    @Override
    public boolean areContentsTheSame(
        @NonNull SongWithParts oldItem,
        @NonNull SongWithParts newItem
    ) {
      return oldItem.equals(newItem);
    }
  }
}