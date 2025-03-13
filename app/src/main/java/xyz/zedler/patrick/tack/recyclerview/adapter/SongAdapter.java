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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.RowSongBinding;
import xyz.zedler.patrick.tack.databinding.RowSongChipBinding;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipAdapter.SongChipViewHolder;

public class SongAdapter extends Adapter<RecyclerView.ViewHolder> {

  private final static String TAG = SongAdapter.class.getSimpleName();

  private final OnSongClickListener listener;
  private List<SongWithParts> songs = new ArrayList<>();

  public SongAdapter(@NonNull OnSongClickListener listener) {
    this.listener = listener;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    RowSongBinding binding = RowSongBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false
    );
    return new SongViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    SongWithParts songWithParts = songs.get(holder.getBindingAdapterPosition());
    SongViewHolder songHolder = (SongViewHolder) holder;
    songHolder.binding.textSongName.setText(songWithParts.getSong().getName());
    songHolder.binding.textSongDescription.setText(songWithParts.getSong().getName());
  }

  @Override
  public int getItemCount() {
    return songs.size();
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

  public static class SongViewHolder extends RecyclerView.ViewHolder {

    private final RowSongBinding binding;

    public SongViewHolder(RowSongBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public interface OnSongClickListener {
    void onSongClick(@NonNull SongWithParts song);
  }
}