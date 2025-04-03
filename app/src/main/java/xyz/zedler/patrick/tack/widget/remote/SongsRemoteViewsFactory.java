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

package xyz.zedler.patrick.tack.widget.remote;

import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.SongDatabase;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.util.LocaleUtil;
import xyz.zedler.patrick.tack.util.PrefsUtil;

public class SongsRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

  private final Context context;
  private final PrefsUtil prefsUtil;
  private SongDatabase db;
  private List<SongWithParts> songsWithParts = new ArrayList<>();
  private int sortOrder;

  public SongsRemoteViewsFactory(Context context) {
    this.context = context;
    this.prefsUtil = new PrefsUtil(context);
  }

  @Override
  public void onCreate() {
    db = SongDatabase.getInstance(context);
  }

  @Override
  public void onDataSetChanged() {
    if (db != null) {
      songsWithParts = db.songDao().getAllSongsWithParts();
      for (SongWithParts songWithPart : songsWithParts) {
        if (songWithPart.getSong().getId().equals(Constants.SONG_ID_DEFAULT)) {
          // Remove default song
          songsWithParts.remove(songWithPart);
          break;
        }
      }
    }

    sortOrder = prefsUtil.getSharedPrefs().getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER);
    if (sortOrder == SONGS_ORDER.NAME_ASC) {
      if (VERSION.SDK_INT >= VERSION_CODES.N) {
        Comparator<String> comparator = Comparator.nullsLast(
            Comparator.comparing(String::toLowerCase, Comparator.naturalOrder())
        );
        Collections.sort(
            songsWithParts,
            Comparator.comparing(
                o -> o.getSong().getName(),
                comparator
            )
        );
      } else {
        Collections.sort(songsWithParts, (s1, s2) -> {
          String name1 = (s1.getSong() != null) ? s1.getSong().getName() : null;
          String name2 = (s2.getSong() != null) ? s2.getSong().getName() : null;
          // Nulls last handling
          if (name1 == null && name2 == null) return 0;
          if (name1 == null) return 1;
          if (name2 == null) return -1;
          return name1.compareToIgnoreCase(name2);
        });
      }
    } else if (sortOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      Collections.sort(
          songsWithParts,
          (s1, s2) -> Long.compare(
              s2.getSong().getLastPlayed(), s1.getSong().getLastPlayed()
          )
      );
    } else if (sortOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
      Collections.sort(
          songsWithParts,
          (s1, s2) -> Integer.compare(
              s2.getSong().getPlayCount(), s1.getSong().getPlayCount()
          )
      );
    }
  }

  @Override
  public void onDestroy() {
    songsWithParts.clear();
    if (db != null) {
      db.close();
    }
  }

  @Override
  public int getCount() {
    return songsWithParts.size();
  }

  @Override
  public RemoteViews getViewAt(int position) {
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.row_widget_song);
    SongWithParts songWithParts = songsWithParts.get(position);

    Intent fillInIntent = new Intent();
    fillInIntent.putExtra(EXTRA.SONG_ID, songWithParts.getSong().getId());
    views.setOnClickFillInIntent(R.id.linear_widget_song_container, fillInIntent);

    views.setTextViewText(R.id.text_widget_song_name, songWithParts.getSong().getName());

    // part count
    int partCount = songWithParts.getParts().size();
    views.setTextViewText(
        R.id.text_widget_song_part_count,
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
      views.setTextViewText(R.id.text_widget_song_duration, songWithParts.getDurationString());
    } else {
      views.setTextViewText(
          R.id.text_widget_song_duration, context.getString(R.string.label_part_no_duration)
      );
    }
    // looped
    views.setTextViewText(
        R.id.text_widget_song_looped,
        context.getString(
            songWithParts.getSong().isLooped()
                ? R.string.label_song_looped
                : R.string.label_song_not_looped
        )
    );

    // last/most played
    boolean sortDetailsEnabled = sortOrder == SONGS_ORDER.LAST_PLAYED_ASC
        || sortOrder == SONGS_ORDER.MOST_PLAYED_ASC;
    views.setViewVisibility(
        R.id.text_widget_song_sort_details, sortDetailsEnabled ? View.VISIBLE : View.GONE
    );
    if (sortOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      long lastPlayed = songWithParts.getSong().getLastPlayed();
      if (lastPlayed != 0) {
        Locale locale = LocaleUtil.getLocale();
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
          DateTimeFormatter formatter = DateTimeFormatter
              .ofLocalizedDateTime(FormatStyle.SHORT)
              .withLocale(locale);
          LocalDateTime dateTime = Instant.ofEpochMilli(lastPlayed)
              .atZone(ZoneId.systemDefault())
              .toLocalDateTime();
          views.setTextViewText(
              R.id.text_widget_song_sort_details,
              context.getString(R.string.label_sort_last_played_date, dateTime.format(formatter))
          );
        } else {
          DateFormat dateFormat = DateFormat.getDateTimeInstance(
              DateFormat.SHORT, DateFormat.SHORT, locale
          );
          String formattedDate = dateFormat.format(new Date(lastPlayed));
          views.setTextViewText(
              R.id.text_widget_song_sort_details,
              context.getString(R.string.label_sort_last_played_date, formattedDate)
          );
        }
      } else {
        views.setTextViewText(
            R.id.text_widget_song_sort_details, context.getString(R.string.label_sort_never_played)
        );
      }
    } else if (sortOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
      int playCount = songWithParts.getSong().getPlayCount();
      if (playCount > 0) {
        views.setTextViewText(
            R.id.text_widget_song_sort_details,
            context.getResources().getQuantityString(
                R.plurals.label_sort_most_played_times, playCount, playCount
            )
        );
      } else {
        views.setTextViewText(
            R.id.text_widget_song_sort_details, context.getString(R.string.label_sort_never_played)
        );
      }
    }
    return views;
  }

  @Override
  public RemoteViews getLoadingView() {
    return null;
  }

  @Override
  public int getViewTypeCount() {
    return 1;
  }

  @Override
  public long getItemId(int position) {
    String songId = songsWithParts.get(position).getSong().getId();
    UUID uuid = UUID.fromString(songId);
    return uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }
}