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

package xyz.zedler.patrick.tack.widget.remote

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.ACTION
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.EXTRA
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.database.SongDatabase
import xyz.zedler.patrick.tack.database.relations.SongWithParts
import xyz.zedler.patrick.tack.util.*
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.UUID

class SongsRemoteViewsFactory(
  private val context: Context,
  intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

  private val prefsUtil = PrefsUtil(context)
  private val appWidgetManager = AppWidgetManager.getInstance(context)
  private val appWidgetId = intent.getIntExtra(
    AppWidgetManager.EXTRA_APPWIDGET_ID,
    AppWidgetManager.INVALID_APPWIDGET_ID
  )
  private var db: SongDatabase? = null
  private var songsWithParts: MutableList<SongWithParts> = mutableListOf()
  private var sortOrder: Int = 0
  private var isListTooBig: Boolean = false
  private var minWidth = -1

  override fun onCreate() {
    db = SongDatabase.getInstance(context)
  }

  override fun onDataSetChanged() {
    db?.let {
      songsWithParts = it.songDao().getAllSongsWithParts()
        .filter { songWithPart -> songWithPart.song.id != Constants.SONG_ID_DEFAULT }
        .toMutableList()
    }
    sortOrder = prefsUtil.sharedPrefs.getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER)
    sortSongsWithParts(songsWithParts, sortOrder)

    isListTooBig = songsWithParts.size > MAX_SONG_COUNT
    if (isListTooBig) {
      songsWithParts = songsWithParts.subList(0, MAX_SONG_COUNT)
    }

    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
      if (options != null && options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
        minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
      }
    }
  }

  override fun onDestroy() {
    songsWithParts.clear()
    db?.close()
  }

  override fun getCount(): Int {
    return songsWithParts.size + if (isListTooBig) 1 else 0
  }

  override fun getViewAt(position: Int): RemoteViews {
    if (isListTooBig && position == songsWithParts.size) {
      val views = RemoteViews(context.packageName, R.layout.row_widget_more)
      val fillInIntent = Intent().apply {
        action = ACTION.SHOW_SONGS
      }
      views.setOnClickFillInIntent(R.id.frame_widget_song_container_more, fillInIntent)
      return views
    }

    val views = RemoteViews(context.packageName, R.layout.row_widget_song)
    val songWithParts = songsWithParts[position]

    val fillInIntentApply = Intent().apply {
      action = ACTION.APPLY_SONG
      putExtra(EXTRA.SONG_ID, songWithParts.song.id)
    }
    views.setOnClickFillInIntent(R.id.linear_widget_song_container, fillInIntentApply)

    views.setTextViewText(R.id.text_widget_song_name, songWithParts.song.name)

    val partCount = songWithParts.parts.size
    views.setTextViewText(
      R.id.text_widget_song_part_count,
      context.resources.getQuantityString(R.plurals.label_parts_count, partCount, partCount)
    )

    val hasDuration = songWithParts.parts.all { it.timerDuration != 0 }
    views.setTextViewText(
      R.id.text_widget_song_duration,
      if (hasDuration) songWithParts.getDurationString() else context.getString(R.string.label_part_no_duration)
    )

    val showLooped = minWidth == -1 || minWidth > 300
    views.setViewVisibility(
      R.id.image_widget_song_looped,
      if (showLooped) View.VISIBLE else View.GONE
    )
    views.setViewVisibility(
      R.id.text_widget_song_looped,
      if (showLooped) View.VISIBLE else View.GONE
    )
    views.setTextViewText(
      R.id.text_widget_song_looped,
      context.getString(
        if (songWithParts.song.isLooped) R.string.label_song_looped else R.string.label_song_not_looped
      )
    )

    val sortDetailsEnabled = sortOrder == SONGS_ORDER.LAST_PLAYED_ASC ||
        sortOrder == SONGS_ORDER.MOST_PLAYED_ASC
    views.setViewVisibility(
      R.id.text_widget_song_sort_details,
      if (sortDetailsEnabled) View.VISIBLE else View.GONE
    )

    when (sortOrder) {
      SONGS_ORDER.LAST_PLAYED_ASC -> {
        val lastPlayed = songWithParts.song.lastPlayed
        if (lastPlayed != 0L) {
          val locale = getLocale()
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val formatter = DateTimeFormatter
              .ofLocalizedDate(FormatStyle.SHORT)
              .withLocale(locale)
            val dateTime = Instant.ofEpochMilli(lastPlayed)
              .atZone(ZoneId.systemDefault())
              .toLocalDateTime()
            views.setTextViewText(
              R.id.text_widget_song_sort_details,
              context.getString(R.string.label_sort_last_played_date, dateTime.format(formatter))
            )
          } else {
            val dateFormat = DateFormat.getDateTimeInstance(
              DateFormat.SHORT, DateFormat.SHORT, locale
            )
            val formattedDate = dateFormat.format(Date(lastPlayed))
            views.setTextViewText(
              R.id.text_widget_song_sort_details,
              context.getString(R.string.label_sort_last_played_date, formattedDate)
            )
          }
        } else {
          views.setTextViewText(
            R.id.text_widget_song_sort_details,
            context.getString(R.string.label_sort_never_played)
          )
        }
      }

      SONGS_ORDER.MOST_PLAYED_ASC -> {
        val playCount = songWithParts.song.playCount
        if (playCount > 0) {
          views.setTextViewText(
            R.id.text_widget_song_sort_details,
            context.resources.getQuantityString(
              R.plurals.label_sort_most_played_times, playCount, playCount
            )
          )
        } else {
          views.setTextViewText(
            R.id.text_widget_song_sort_details,
            context.getString(R.string.label_sort_never_played)
          )
        }
      }
    }

    val showPlay = minWidth == -1 || minWidth > 200
    views.setViewVisibility(
      R.id.frame_widget_song_play,
      if (showPlay) View.VISIBLE else View.GONE
    )
    val isRtl = context.isLayoutRtl()
    val paddingEnd = if (showPlay) 0 else context.dpToPx(12f)
    views.setViewPadding(
      R.id.linear_widget_song_container,
      if (isRtl) paddingEnd else 0, 0, if (isRtl) 0 else paddingEnd, 0
    )
    val fillInIntentPlay = Intent().apply {
      action = ACTION.START_SONG
      putExtra(EXTRA.SONG_ID, songWithParts.song.id)
    }
    views.setOnClickFillInIntent(R.id.frame_widget_song_play, fillInIntentPlay)

    return views
  }

  override fun getLoadingView(): RemoteViews? = null

  override fun getViewTypeCount(): Int = 2

  override fun getItemId(position: Int): Long {
    if (position < 0 || position >= songsWithParts.size) {
      return position.toLong()
    }
    val songId = songsWithParts[position].song.id
    val uuid = UUID.fromString(songId)
    return uuid.mostSignificantBits xor uuid.leastSignificantBits
  }

  override fun hasStableIds(): Boolean = true

  companion object {
    private const val MAX_SONG_COUNT = 20
  }
}
