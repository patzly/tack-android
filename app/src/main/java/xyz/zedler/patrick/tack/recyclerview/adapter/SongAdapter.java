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
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.RowSongBinding;
import xyz.zedler.patrick.tack.util.LocaleUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.util.ViewUtil.OnMenuInflatedListener;

public class SongAdapter extends Adapter<SongAdapter.SongViewHolder> {

  private final static String TAG = SongAdapter.class.getSimpleName();

  private final static String PAYLOAD_PLAY = "play";

  private final List<SongWithParts> songsWithParts = new ArrayList<>();
  private final OnSongClickListener listener;
  private int sortOrder = 0;
  private String currentSongId = null;
  private boolean isPlaying = false;

  public SongAdapter(@NonNull OnSongClickListener listener) {
    this.listener = listener;
    setHasStableIds(true);
  }

  @NonNull
  @Override
  public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    RowSongBinding binding = RowSongBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false
    );
    return new SongViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
    int adapterPosition = holder.getBindingAdapterPosition();
    SongWithParts songWithParts = getItem(adapterPosition);
    Context context = holder.binding.getRoot().getContext();
    RowSongBinding binding = holder.binding;

    binding.linearSongContainer.setOnClickListener(v -> listener.onSongClick(songWithParts));
    boolean isSelected = songWithParts.getSong().getId().equals(currentSongId);

    // item background
    if (getItemCount() == 1) {
      binding.linearSongContainer.setBackgroundResource(
          isSelected
              ? R.drawable.ripple_list_item_bg_tertiary_segmented_single
              : R.drawable.ripple_list_item_bg_segmented_single
      );
    } else if (adapterPosition == 0) {
      binding.linearSongContainer.setBackgroundResource(
          isSelected
              ? R.drawable.ripple_list_item_bg_tertiary_segmented_first
              : R.drawable.ripple_list_item_bg_segmented_first
      );
    } else if (adapterPosition == getItemCount() - 1) {
      binding.linearSongContainer.setBackgroundResource(
          isSelected
              ? R.drawable.ripple_list_item_bg_tertiary_segmented_last
              : R.drawable.ripple_list_item_bg_segmented_last
      );
    } else {
      binding.linearSongContainer.setBackgroundResource(
          isSelected
              ? R.drawable.ripple_list_item_bg_tertiary_segmented_middle
              : R.drawable.ripple_list_item_bg_segmented_middle
      );
    }

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

    boolean isPlayingSong = isPlaying && isSelected;
    binding.buttonSongPlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    binding.buttonSongPlay.setChecked(isPlayingSong);
    binding.buttonSongPlay.setIconResource(
        isPlayingSong ? R.drawable.ic_rounded_stop : R.drawable.ic_rounded_play_arrow
    );
    binding.buttonSongPlay.setBackgroundColor(
        ResUtil.getColor(
            context, isSelected ? R.attr.colorTertiary : R.attr.colorSurfaceBright
        )
    );
    binding.buttonSongPlay.setIconTint(
        ColorStateList.valueOf(
            ResUtil.getColor(
                context,
                isSelected ? R.attr.colorOnTertiary : R.attr.colorOnSurfaceVariant
            )
        )
    );
    binding.buttonSongPlay.setOnClickListener(v -> listener.onPlayStopClick());

    binding.buttonSongMenu.setIconTint(
        ColorStateList.valueOf(
            ResUtil.getColor(
                context, isSelected ? R.attr.colorOnTertiaryContainer : R.attr.colorOnSurface
            )
        )
    );
    binding.buttonSongMenu.setOnClickListener(v -> {
      listener.onMoreClick();
      PopupMenu.OnMenuItemClickListener itemClickListener = item -> {
        int id = item.getItemId();
        if (id == R.id.action_play) {
          listener.onPlayClick(songWithParts);
        } else if (id == R.id.action_apply) {
          listener.onApplyClick(songWithParts);
        } else if (id == R.id.action_delete) {
          listener.onDeleteClick(songWithParts);
        }
        return true;
      };
      OnMenuInflatedListener menuInflatedListener = menu -> {
        MenuItem itemPlay = menu.findItem(R.id.action_play);
        itemPlay.setVisible(!isSelected);
        MenuItem itemApply = menu.findItem(R.id.action_apply);
        itemApply.setVisible(!isSelected);
      };
      ViewUtil.showMenu(v, R.menu.menu_song_list, itemClickListener, menuInflatedListener);
    });

    ViewUtil.setTooltipText(binding.buttonSongPlay, R.string.action_play);
    ViewUtil.setTooltipText(binding.buttonSongMenu, R.string.action_more);
  }

  @Override
  public void onBindViewHolder(
      @NonNull SongViewHolder holder, int position, @NonNull List<Object> payloads
  ) {
    if (payloads.contains(PAYLOAD_PLAY)) {
      RowSongBinding binding = holder.binding;
      binding.buttonSongPlay.setChecked(isPlaying);
      binding.buttonSongPlay.setIconResource(
          isPlaying ? R.drawable.ic_rounded_stop : R.drawable.ic_rounded_play_arrow
      );
    } else {
      super.onBindViewHolder(holder, position, payloads);
    }
  }

  @Override
  public int getItemCount() {
    return songsWithParts.size();
  }

  public SongWithParts getItem(int position) {
    return songsWithParts.get(position);
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

  public void setCurrentSongId(@Nullable String currentSongId) {
    String oldSongId = this.currentSongId;
    this.currentSongId = currentSongId;
    for (int i = 0; i < getItemCount(); i++) {
      String id = getItem(i).getSong().getId();
      if (id.equals(oldSongId)) {
        notifyItemChanged(i);
        oldSongId = null;
      }
      if (id.equals(currentSongId)) {
        notifyItemChanged(i);
      }
    }
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

  public void setSongsWithParts(List<SongWithParts> newSongsWithParts) {
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
      @Override
      public int getOldListSize() {
        return songsWithParts.size();
      }

      @Override
      public int getNewListSize() {
        return newSongsWithParts.size();
      }

      @Override
      public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Song oldSong = songsWithParts.get(oldItemPosition).getSong();
        Song newSong = newSongsWithParts.get(newItemPosition).getSong();
        return oldSong.getId().equals(newSong.getId());
      }

      @Override
      public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        SongWithParts oldSongWithParts = songsWithParts.get(oldItemPosition);
        SongWithParts newSongWithParts = newSongsWithParts.get(newItemPosition);

        if (!oldSongWithParts.equals(newSongWithParts)) {
          return false;
        }

        int oldRole = getItemRole(oldItemPosition, songsWithParts.size());
        int newRole = getItemRole(newItemPosition, newSongsWithParts.size());
        return oldRole == newRole;
      }

      private int getItemRole(int position, int size) {
        if (size == 1) return -1;
        if (position == 0) return 0;
        if (position == size - 1) return 2;
        return 1;
      }
    });
    songsWithParts.clear();
    songsWithParts.addAll(newSongsWithParts);
    diffResult.dispatchUpdatesTo(this);
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
    void onMoreClick();
    void onApplyClick(@NonNull SongWithParts song);
    void onDeleteClick(@NonNull SongWithParts song);
  }
}