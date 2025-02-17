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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.RowSongChipBinding;
import xyz.zedler.patrick.tack.databinding.RowSongChipCloseBinding;
import xyz.zedler.patrick.tack.util.ResUtil;

public class SongChipsAdapter extends Adapter<RecyclerView.ViewHolder> {

  private final static String TAG = SongChipsAdapter.class.getSimpleName();

  private final OnSongClickListener listener;
  private List<SongWithParts> songs = new ArrayList<>();
  private String currentSongName;

  public SongChipsAdapter(@NonNull OnSongClickListener listener, @Nullable String currentSongName) {
    this.listener = listener;
    this.currentSongName = currentSongName;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == R.layout.row_song_chip_close) {
      RowSongChipCloseBinding binding = RowSongChipCloseBinding.inflate(
          LayoutInflater.from(parent.getContext()), parent, false
      );
      return new SongChipCloseViewHolder(binding);
    } else {
      RowSongChipBinding binding = RowSongChipBinding.inflate(
          LayoutInflater.from(parent.getContext()), parent, false
      );
      return new SongChipViewHolder(binding);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (getItemViewType(position) == R.layout.row_song_chip_close) {
      SongChipCloseViewHolder closeViewHolder = (SongChipCloseViewHolder) holder;
      closeViewHolder.binding.chipRowSongClose.setOnClickListener(v -> {
        listener.onCloseClick(closeViewHolder.binding.chipRowSongClose.getChipIcon());
        setCurrentSongName(null);
      });
    } else {
      SongWithParts songWithParts = songs.get(position - 1);
      SongChipViewHolder songViewHolder = (SongChipViewHolder) holder;
      songViewHolder.binding.chipRowSong.setText(songWithParts.getSong().getName());
      songViewHolder.binding.chipRowSong.setOnClickListener(v -> {
        listener.onSongClick(
            songViewHolder.binding.chipRowSong.getChipIcon(),
            songWithParts
        );
        setCurrentSongName(songWithParts.getSong().getName());
      });
      boolean isSelected = songWithParts.getSong().getName().equals(currentSongName);
      Context context = songViewHolder.binding.getRoot().getContext();
      songViewHolder.binding.chipRowSong.setChipBackgroundColor(ColorStateList.valueOf(
          ResUtil.getColor(
              context, isSelected ? R.attr.colorTertiaryContainer : R.attr.colorSurface
          )
      ));
      int foregroundColor = ResUtil.getColor(context, isSelected
          ? R.attr.colorOnTertiaryContainer
          : R.attr.colorOnSurfaceVariant
      );
      songViewHolder.binding.chipRowSong.setTextColor(foregroundColor);
      songViewHolder.binding.chipRowSong.setChipIconTint(ColorStateList.valueOf(foregroundColor));
      songViewHolder.binding.chipRowSong.setChipStrokeColor(ColorStateList.valueOf(
          ResUtil.getColor(context, isSelected ? R.attr.colorTertiary : R.attr.colorOutline)
      ));
    }
  }

  @Override
  public int getItemViewType(int position) {
    return position == 0 ? R.layout.row_song_chip_close : R.layout.row_song_chip;
  }

  @Override
  public int getItemCount() {
    return !songs.isEmpty() ? songs.size() + 1 : 0;
  }

  public void setSongs(List<SongWithParts> newSongs) {
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {

      @Override
      public int getOldListSize() {
        return songs != null ? songs.size() : 0;
      }

      @Override
      public int getNewListSize() {
        return newSongs != null ? newSongs.size() : 0;
      }

      @Override
      public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return songs.get(oldItemPosition).getSong().getName()
            .equals(newSongs.get(newItemPosition).getSong().getName());
      }

      @Override
      public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return songs.get(oldItemPosition).equals(newSongs.get(newItemPosition));
      }
    });
    this.songs = newSongs;
    diffResult.dispatchUpdatesTo(this);
  }

  @SuppressLint("NotifyDataSetChanged")
  private void setCurrentSongName(String currentSongName) {
    if (currentSongName == null && this.currentSongName != null) {
      this.currentSongName = null;
      notifyItemRangeChanged(1, getItemCount() - 1);
    }
    if (!Objects.equals(this.currentSongName, currentSongName)) {
      this.currentSongName = currentSongName;
      notifyDataSetChanged();
    }
  }

  public static class SongChipViewHolder extends RecyclerView.ViewHolder {

    private final RowSongChipBinding binding;

    public SongChipViewHolder(RowSongChipBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public static class SongChipCloseViewHolder extends RecyclerView.ViewHolder {

    private final RowSongChipCloseBinding binding;

    public SongChipCloseViewHolder(RowSongChipCloseBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public interface OnSongClickListener {

    void onCloseClick(Drawable icon);
    void onSongClick(Drawable icon, SongWithParts song);
  }
}