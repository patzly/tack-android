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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.RowSongBinding;

public class SongAdapter extends ListAdapter<SongWithParts, RecyclerView.ViewHolder> {

  private final static String TAG = SongAdapter.class.getSimpleName();

  private final OnSongClickListener listener;

  public SongAdapter(@NonNull OnSongClickListener listener) {
    super(new SongDiffCallback());
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
    SongWithParts songWithParts = getItem(holder.getBindingAdapterPosition());
    SongViewHolder songHolder = (SongViewHolder) holder;
    Context context = songHolder.binding.getRoot().getContext();
    RowSongBinding binding = songHolder.binding;

    binding.textSongName.setText(songWithParts.getSong().getName());

    // part count
    int partCount = songWithParts.getParts().size();
    binding.textSongPartCount.setText(
        context.getResources().getQuantityString(R.plurals.label_parts_count, partCount, partCount)
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
      binding.textSongDuration.setText(songWithParts.getDurationString());
    } else {
      binding.textSongDuration.setText(R.string.label_part_no_duration);
    }
    // looped
    binding.textSongLooped.setText(
        context.getString(
            songWithParts.getSong().isLooped()
                ? R.string.label_song_looped
                : R.string.label_song_not_looped
        )
    );
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

class SongDiffCallback extends DiffUtil.ItemCallback<SongWithParts> {

  @Override
  public boolean areItemsTheSame(
      @NonNull SongWithParts oldItem, @NonNull SongWithParts newItem
  ) {
    return oldItem.getSong().getName().equals(newItem.getSong().getName());
  }

  @Override
  public boolean areContentsTheSame(
      @NonNull SongWithParts oldItem, @NonNull SongWithParts newItem
  ) {
    return oldItem.equals(newItem);
  }
}