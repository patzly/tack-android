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

package xyz.zedler.patrick.tack.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.activity.SongActivity;
import xyz.zedler.patrick.tack.database.SongDatabase;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.util.PrefsUtil;
import xyz.zedler.patrick.tack.widget.remote.SongsRemoteViewsService;

public class SongsWidgetProvider extends AppWidgetProvider {

  @Override
  public void onEnabled(Context context) {
    SharedPreferences sharedPrefs = new PrefsUtil(context).getSharedPrefs();
    sharedPrefs.edit().putInt(PREF.SONGS_VISIT_COUNT, -1).apply();
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    fetchSongs(context, areSongsEmpty -> {
      for (int appWidgetId : appWidgetIds) {
        updateWidget(context, appWidgetManager, appWidgetId, null, areSongsEmpty);
      }
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_widget_songs);
    });
  }

  @Override
  public void onAppWidgetOptionsChanged(
      Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions
  ) {
    fetchSongs(context, areSongsEmpty -> {
      updateWidget(context, appWidgetManager, appWidgetId, newOptions, areSongsEmpty);
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_widget_songs);
    });
  }

  private void updateWidget(
      Context context,
      AppWidgetManager appWidgetManager,
      int appWidgetId,
      @Nullable Bundle options,
      boolean areSongsEmpty // for empty songs placeholder
  ) {
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_songs);

    Intent intentIcon = new Intent(context, MainActivity.class);
    PendingIntent pendingIntentIcon = PendingIntent.getActivity(
        context, 0, intentIcon,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
    views.setOnClickPendingIntent(R.id.frame_widget_songs_icon, pendingIntentIcon);

    Intent intentShowSongs = new Intent(context, MainActivity.class);
    intentShowSongs.setAction(ACTION.SHOW_SONGS);
    PendingIntent pendingIntentShowSongs = PendingIntent.getActivity(
        context, 0, intentShowSongs,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
    views.setOnClickPendingIntent(R.id.linear_widget_songs_header, pendingIntentShowSongs);

    options = options != null ? options : appWidgetManager.getAppWidgetOptions(appWidgetId);
    if (options != null) {
      int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
      views.setTextViewText(
          R.id.text_widget_songs_title,
          context.getString(minWidth > 200 ? R.string.title_songs : R.string.title_songs_short)
      );
    } else {
      views.setTextViewText(R.id.text_widget_songs_title, context.getString(R.string.title_songs));
    }

    Intent intentUpdate = new Intent(context, SongsWidgetProvider.class);
    intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
    PendingIntent pendingIntentUpdate = PendingIntent.getBroadcast(
        context, 0, intentUpdate,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
    views.setOnClickPendingIntent(R.id.frame_widget_songs_update, pendingIntentUpdate);

    views.setViewVisibility(R.id.list_widget_songs, areSongsEmpty ? View.GONE : View.VISIBLE);
    views.setViewVisibility(
        R.id.linear_widget_songs_empty, areSongsEmpty ? View.VISIBLE : View.GONE
    );

    if (!areSongsEmpty) {
      Intent serviceIntentSongs = new Intent(context, SongsRemoteViewsService.class);
      serviceIntentSongs.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      views.setRemoteAdapter(R.id.list_widget_songs, serviceIntentSongs);

      Intent intentSong = new Intent(context, SongActivity.class);
      intentSong.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      PendingIntent pendingIntentSong = PendingIntent.getActivity(
          context, 0, intentSong,
          // must be mutable for fillInIntent
          PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
      );
      views.setPendingIntentTemplate(R.id.list_widget_songs, pendingIntentSong);

      if (VERSION.SDK_INT >= VERSION_CODES.S) {
        views.setViewOutlinePreferredRadiusDimen(
            R.id.list_widget_songs, android.R.dimen.system_app_widget_inner_radius
        );
      }
    } else {
      // for using current language
      views.setTextViewText(
          R.id.text_widget_songs_empty, context.getString(R.string.msg_songs_empty)
      );
      // make container open song library
      views.setOnClickPendingIntent(R.id.frame_widget_songs_container, pendingIntentShowSongs);
    }

    appWidgetManager.updateAppWidget(appWidgetId, views);
  }

  private void fetchSongs(Context context, OnSongsFetchedListener listener) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(() -> {
      SongDatabase db = SongDatabase.getInstance(context);
      List<Song> songs = db.songDao().getAllSongs();
      for (Song song : songs) {
        if (song.getId().equals(Constants.SONG_ID_DEFAULT)) {
          songs.remove(song);
          break;
        }
      }
      listener.onSongsFetched(songs.isEmpty());
    });
    executor.shutdown();
  }

  private interface OnSongsFetchedListener {
    void onSongsFetched(boolean areSongsEmpty);
  }
}