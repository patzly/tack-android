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
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.RowSongBinding;
import xyz.zedler.patrick.tack.util.LocaleUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class SongAdapter extends ListAdapter<SongWithParts, ViewHolder> {

  private final static String TAG = SongAdapter.class.getSimpleName();

  private final static String PAYLOAD_PLAY = "play";

  private final OnSongClickListener listener;
  private int sortOrder = 0;
  private String currentSongId = null;
  private boolean isPlaying = false;

  public SongAdapter(@NonNull OnSongClickListener listener) {
    super(new SongDiffCallback());
    this.listener = listener;
    setHasStableIds(true);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    RowSongBinding binding = RowSongBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false
    );
    return new SongViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    SongWithParts songWithParts = getItem(holder.getBindingAdapterPosition());
    SongViewHolder songHolder = (SongViewHolder) holder;
    Context context = songHolder.binding.getRoot().getContext();
    RowSongBinding binding = songHolder.binding;

    binding.linearSongContainer.setOnClickListener(v -> listener.onSongClick(songWithParts));
    boolean isSelected = songWithParts.getSong().getId().equals(currentSongId);
    if (isSelected) {
      binding.linearSongContainer.setBackground(
          ViewUtil.getBgListItemSelected(
              context, R.attr.colorTertiaryContainer, 8, 8
          )
      );
    } else {
      binding.linearSongContainer.setBackgroundResource(R.drawable.ripple_list_item_bg);
    }

    binding.imageSongIcon.setColorFilter(
        ResUtil.getColor(
            context, isSelected ? R.attr.colorOnTertiaryContainer : R.attr.colorPrimary
        )
    );

    binding.textSongName.setText(songWithParts.getSong().getName());
    binding.textSongName.setTextColor(
        ResUtil.getColor(
            context, isSelected ? R.attr.colorOnTertiaryContainer : R.attr.colorOnSurface
        )
    );

    int colorFgSecondary = ResUtil.getColor(
        context, isSelected ? R.attr.colorOnTertiaryContainer : R.attr.colorOnSurfaceVariant
    );
    binding.textSongPartCount.setTextColor(colorFgSecondary);
    binding.imageSongDivider1.setColorFilter(colorFgSecondary);
    binding.textSongDuration.setTextColor(colorFgSecondary);
    binding.imageSongDivider2.setColorFilter(colorFgSecondary);
    binding.textSongLooped.setTextColor(colorFgSecondary);
    binding.textSongSortDetails.setTextColor(colorFgSecondary);

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
        Locale locale = LocaleUtil.getLocale();
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
          DateTimeFormatter formatter = DateTimeFormatter
              .ofLocalizedDate(FormatStyle.SHORT)
              .withLocale(locale);
          LocalDateTime dateTime = Instant.ofEpochMilli(lastPlayed)
              .atZone(ZoneId.systemDefault())
              .toLocalDateTime();
          binding.textSongSortDetails.setText(
              context.getString(R.string.label_sort_last_played_date, dateTime.format(formatter))
          );
        } else {
          DateFormat dateFormat = DateFormat.getDateTimeInstance(
              DateFormat.SHORT, DateFormat.SHORT, locale
          );
          String formattedDate = dateFormat.format(new Date(lastPlayed));
          binding.textSongSortDetails.setText(
              context.getString(R.string.label_sort_last_played_date, formattedDate)
          );
        }
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

    binding.buttonSongPlay.setChecked(isPlaying);
    binding.buttonSongPlay.setIconResource(
        isPlaying ? R.drawable.ic_rounded_stop : R.drawable.ic_rounded_play_arrow
    );
    binding.buttonSongPlay.setOnClickListener(v -> {
      if (isPlaying) {
        listener.onPlayStopClick();
      } else {
        isPlaying = true;
        listener.onPlayClick(songWithParts);
      }
    });

    binding.buttonSongCloseSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    binding.buttonSongCloseSelected.setOnClickListener(v -> listener.onCloseClick());
  }

  @Override
  public void onBindViewHolder(
      @NonNull ViewHolder holder, int position, @NonNull List<Object> payloads
  ) {
    if (payloads.contains(PAYLOAD_PLAY)) {
      SongViewHolder songHolder = (SongViewHolder) holder;
      RowSongBinding binding = songHolder.binding;

      binding.buttonSongPlay.setChecked(isPlaying);
      binding.buttonSongPlay.setIconResource(
          isPlaying ? R.drawable.ic_rounded_stop : R.drawable.ic_rounded_play_arrow
      );
    } else {
      onBindViewHolder(holder, position);
    }
  }

  @Override
  public long getItemId(int position) {
    String songId = getItem(position).getSong().getId();
    UUID uuid = UUID.fromString(songId);
    return uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
  }

  @SuppressLint("NotifyDataSetChanged")
  public void setSortOrder(int sortOrder) {
    if (this.sortOrder != sortOrder) {
      this.sortOrder = sortOrder;
      notifyDataSetChanged();
    }
  }

  @SuppressLint("NotifyDataSetChanged")
  public void setCurrentSongId(@Nullable String currentSongId) {
    this.currentSongId = currentSongId;
    // We don't know which previous item was selected, so we need to notify the whole list
    notifyDataSetChanged();
  }

  public void setPlaying(boolean isPlaying) {
    this.isPlaying = isPlaying;
    for (int i = 0; i < getItemCount(); i++) {
      if (getItem(i).getSong().getId().equals(currentSongId)) {
        notifyItemChanged(i, PAYLOAD_PLAY);
        break;
      }
    }
  }

  public static class SongViewHolder extends ViewHolder {

    private final RowSongBinding binding;

    public SongViewHolder(RowSongBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public interface OnSongClickListener {
    void onSongClick(@NonNull SongWithParts song);
    void onPlayClick(@NonNull SongWithParts song);
    void onPlayStopClick();
    void onCloseClick();
  }

  static class SongDiffCallback extends DiffUtil.ItemCallback<SongWithParts> {

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
}