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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.activity.ShortcutActivity;
import xyz.zedler.patrick.tack.database.SongDatabase;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.widget.remote.SongsRemoteViewsService;

public class SongsWidgetProvider extends AppWidgetProvider {

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private List<Song> songs = new ArrayList<>();

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    SongDatabase db = SongDatabase.getInstance(context);
    executorService.execute(() -> {
      songs = db.songDao().getAllSongs();
      for (Song song : songs) {
        if (song.getId().equals(Constants.SONG_ID_DEFAULT)) {
          // Remove default song
          songs.remove(song);
          break;
        }
      }
      for (int appWidgetId : appWidgetIds) {
        updateWidget(context, appWidgetManager, appWidgetId);
      }
    });
  }

  private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_songs);

    Intent intentHeader = new Intent(context, MainActivity.class);
    PendingIntent pendingIntentHeader = PendingIntent.getActivity(
        context, 0, intentHeader,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
    views.setOnClickPendingIntent(R.id.linear_widget_songs_header, pendingIntentHeader);

    Intent intentUpdate = new Intent(context, SongsWidgetProvider.class);
    intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
    PendingIntent pendingIntentUpdate = PendingIntent.getBroadcast(
        context, 0, intentUpdate,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
    views.setOnClickPendingIntent(R.id.frame_widget_songs_update, pendingIntentUpdate);

    views.setViewVisibility(R.id.list_widget_songs, songs.isEmpty() ? View.GONE : View.VISIBLE);
    views.setViewVisibility(
        R.id.frame_widget_songs_empty, songs.isEmpty() ? View.VISIBLE : View.GONE
    );

    if (!songs.isEmpty()) {
      Intent serviceIntentSongs = new Intent(context, SongsRemoteViewsService.class);
      views.setRemoteAdapter(R.id.list_widget_songs, serviceIntentSongs);

      Intent intentSong = new Intent(context, ShortcutActivity.class);
      intentSong.setAction(ACTION.START_SONG);
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
    }

    appWidgetManager.updateAppWidget(appWidgetId, views);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);

    String action = intent.getAction();
    if (action != null && action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
      ComponentName componentName = new ComponentName(context, SongsWidgetProvider.class);
      int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
      if (appWidgetIds == null) {
        appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
      }
      onUpdate(context, appWidgetManager, appWidgetIds);
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_widget_songs);
    }
  }
}