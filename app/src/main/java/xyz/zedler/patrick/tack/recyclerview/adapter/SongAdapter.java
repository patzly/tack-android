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
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.RowSongBinding;
import xyz.zedler.patrick.tack.util.LocaleUtil;

public class SongAdapter extends ListAdapter<SongWithParts, RecyclerView.ViewHolder> {

  private final static String TAG = SongAdapter.class.getSimpleName();

  private final OnSongClickListener listener;
  private int sortOrder = 0;

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

    binding.linearSongContainer.setOnClickListener(v -> listener.onSongClick(songWithParts));

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

    // last/most played
    boolean sortDetailsEnabled = sortOrder == SONGS_ORDER.LAST_PLAYED_ASC
        || sortOrder == SONGS_ORDER.MOST_PLAYED_ASC;
    binding.textSongSortDetails.setVisibility(sortDetailsEnabled ? View.VISIBLE : View.GONE);
    if (sortOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      long lastPlayed = songWithParts.getSong().getLastPlayed();
      if (lastPlayed != 0) {
        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(LocaleUtil.getLocale());
        LocalDateTime dateTime = Instant.ofEpochMilli(songWithParts.getSong().getLastPlayed())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        binding.textSongSortDetails.setText(
            context.getString(R.string.label_sort_last_played_date, dateTime.format(formatter))
        );
      } else {
        binding.textSongSortDetails.setText(R.string.label_sort_never_played);
      }
    } else if (sortOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
      int playCount = songWithParts.getSong().getPlayCount();
      if (playCount > 0) {
        binding.textSongSortDetails.setText(
            context.getResources().getQuantityString(
                R.plurals.label_sort_most_played_times, playCount, playCount
            )
        );
      } else {
        binding.textSongSortDetails.setText(R.string.label_sort_never_played);
      }
    }
  }

  public void setSortOrder(int sortOrder) {
    if (this.sortOrder != sortOrder) {
      this.sortOrder = sortOrder;
      notifyDataSetChanged();
    }
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
    return oldItem.getSong().getId().equals(newItem.getSong().getId());
  }

  @Override
  public boolean areContentsTheSame(
      @NonNull SongWithParts oldItem, @NonNull SongWithParts newItem
  ) {
    return oldItem.equals(newItem);
  }
}