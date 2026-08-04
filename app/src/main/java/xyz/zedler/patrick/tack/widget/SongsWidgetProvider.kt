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
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.ACTION
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.activity.SongActivity
import xyz.zedler.patrick.tack.database.SongDatabase
import xyz.zedler.patrick.tack.util.PrefsUtil
import xyz.zedler.patrick.tack.widget.remote.SongsRemoteViewsService
import java.util.concurrent.Executors
import androidx.core.content.edit

class SongsWidgetProvider : AppWidgetProvider() {

  override fun onEnabled(context: Context) {
    val sharedPrefs = PrefsUtil(context).sharedPrefs
    sharedPrefs.edit { putInt(PREF.SONGS_VISIT_COUNT, -1) }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    fetchSongs(context) { areSongsEmpty ->
      for (appWidgetId in appWidgetIds) {
        updateWidget(context, appWidgetManager, appWidgetId, null, areSongsEmpty)
      }
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_widget_songs)
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle
  ) {
    fetchSongs(context) { areSongsEmpty ->
      updateWidget(context, appWidgetManager, appWidgetId, newOptions, areSongsEmpty)
      appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list_widget_songs)
    }
  }

  private fun updateWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    options: Bundle?,
    areSongsEmpty: Boolean
  ) {
    val views = RemoteViews(context.packageName, R.layout.widget_songs)

    val intentIcon = Intent(context, MainActivity::class.java)
    val pendingIntentIcon = PendingIntent.getActivity(
      context, 0, intentIcon,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.frame_widget_songs_icon, pendingIntentIcon)

    val intentShowSongs = Intent(context, MainActivity::class.java).apply {
      action = ACTION.SHOW_SONGS
    }
    val pendingIntentShowSongs = PendingIntent.getActivity(
      context, 0, intentShowSongs,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.linear_widget_songs_header, pendingIntentShowSongs)

    val currentOptions = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
    val title = if (currentOptions != null) {
      val minWidth = currentOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
      context.getString(if (minWidth > 200) R.string.title_songs else R.string.title_songs_short)
    } else {
      context.getString(R.string.title_songs)
    }
    views.setTextViewText(R.id.text_widget_songs_title, title)

    val intentUpdate = Intent(
      context, SongsWidgetProvider::class.java
    ).apply {
      action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
    }
    val pendingIntentUpdate = PendingIntent.getBroadcast(
      context, 0, intentUpdate,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.frame_widget_songs_update, pendingIntentUpdate)

    views.setViewVisibility(
      R.id.list_widget_songs, if (areSongsEmpty) View.GONE else View.VISIBLE
    )
    views.setViewVisibility(
      R.id.linear_widget_songs_empty,
      if (areSongsEmpty) View.VISIBLE else View.GONE
    )

    if (!areSongsEmpty) {
      val serviceIntentSongs = Intent(
        context, SongsRemoteViewsService::class.java
      ).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      }
      views.setRemoteAdapter(R.id.list_widget_songs, serviceIntentSongs)

      val intentSong = Intent(context, SongActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      }
      val pendingIntentSong = PendingIntent.getActivity(
        context, 0, intentSong,
        // must be mutable for fillInIntent
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
      )
      views.setPendingIntentTemplate(
        R.id.list_widget_songs, pendingIntentSong
      )

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        views.setViewOutlinePreferredRadiusDimen(
          R.id.list_widget_songs, android.R.dimen.system_app_widget_inner_radius
        )
      }
    } else {
      views.setTextViewText(
        R.id.text_widget_songs_empty,
        context.getString(R.string.msg_songs_empty)
      )
      views.setOnClickPendingIntent(
        R.id.frame_widget_songs_container, pendingIntentShowSongs
      )
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
  }

  private fun fetchSongs(context: Context, listener: (Boolean) -> Unit) {
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
      val db = SongDatabase.getInstance(context)
      val songs = db.songDao().getAllSongs().filter { it.id != Constants.SONG_ID_DEFAULT }
      listener(songs.isEmpty())
    }
    executor.shutdown()
  }
}
