/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it under the terms of the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors
import xyz.zedler.patrick.tack.Constants.ACTION
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.Constants.THEME
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.service.MetronomeService

class NotificationUtil(private val context: Context) {

  private val sharedPrefs: SharedPreferences by lazy {
    PrefsUtil(context).sharedPrefs
  }
  private val notificationManager: NotificationManagerCompat by lazy {
    NotificationManagerCompat.from(context)
  }

  fun hasPermission(): Boolean = hasPermission(context)

  fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    notificationManager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.title_notification_channel),
        NotificationManager.IMPORTANCE_HIGH
      )
    )
  }

  @SuppressLint("MissingPermission")
  fun updateNotification(notification: Notification) {
    if (hasPermission()) {
      notificationManager.notify(NOTIFICATION_ID, notification)
    }
  }

  fun getNotification(
    showPlayButton: Boolean,
    showTimer: Boolean,
    showPromotedLiveUpdate: Boolean,
    timerTextLong: String?,
    timerTextShort: String?,
    timerProgress: Float,
    timerDuration: Int
  ): Notification {
    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    }
    val activityPendingIntent = PendingIntent.getActivity(
      context, REQUEST_CODE, openIntent, PendingIntent.FLAG_IMMUTABLE
    )

    val startIntent = Intent(context, MetronomeService::class.java).apply {
      action = ACTION.START
    }
    val startServicePendingIntent = PendingIntent.getService(
      context, REQUEST_CODE, startIntent, PendingIntent.FLAG_IMMUTABLE
    )
    val actionStart = Action(
      R.drawable.ic_rounded_play_arrow_fill,
      context.getString(R.string.action_play),
      startServicePendingIntent
    )

    val stopIntent = Intent(context, MetronomeService::class.java).apply {
      action = ACTION.STOP
    }
    val stopServicePendingIntent = PendingIntent.getService(
      context, REQUEST_CODE, stopIntent, PendingIntent.FLAG_IMMUTABLE
    )
    val actionStop = Action(
      R.drawable.ic_rounded_stop_fill,
      context.getString(R.string.action_stop),
      stopServicePendingIntent
    )

    val dismissIntent = Intent(context, MetronomeService::class.java).apply {
      action = ACTION.DISMISS
    }
    val dismissPendingIntent = PendingIntent.getService(
      context, REQUEST_CODE, dismissIntent, PendingIntent.FLAG_IMMUTABLE
    )

    val title = context.getString(R.string.msg_service_running)
    val text = context.getString(R.string.msg_service_running_return)

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setContentTitle(title)
      .setContentText(text)
      .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(text))
      .setContentIntent(activityPendingIntent)
      .addAction(if (showPlayButton) actionStart else actionStop)
      .setAutoCancel(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setOngoing(true)
      .setShowWhen(false)
      .setDeleteIntent(dismissPendingIntent)
      .setColor(getColor())
      .setSmallIcon(R.drawable.ic_logo_notification)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

    if (showTimer) {
      val progressStyle = NotificationCompat.ProgressStyle()
      val safeDuration = if (timerDuration > 0) timerDuration else 1

      if (safeDuration > 10) {
        progressStyle.setProgressSegments(
          listOf(NotificationCompat.ProgressStyle.Segment(100))
        )
      } else {
        val points = mutableListOf<NotificationCompat.ProgressStyle.Point>()
        val segments = mutableListOf<NotificationCompat.ProgressStyle.Segment>()
        var previousOffset = 0

        for (i in 1..safeDuration) {
          val currentOffset = Math.round((i * 100f) / safeDuration)
          val segmentLength = currentOffset - previousOffset
          segments.add(NotificationCompat.ProgressStyle.Segment(segmentLength))
          if (safeDuration <= 5) {
            points.add(NotificationCompat.ProgressStyle.Point(currentOffset))
          }
          previousOffset = currentOffset
        }
        progressStyle.setProgressSegments(segments)
        if (points.isNotEmpty()) {
          progressStyle.setProgressPoints(points)
        }
      }
      progressStyle.setProgress((timerProgress * 100).toInt())

      builder.setContentText(timerTextLong)
        .setShortCriticalText(timerTextShort)
        .setStyle(progressStyle)
        .setRequestPromotedOngoing(showPromotedLiveUpdate)
    }
    return builder.build()
  }

  private fun getColor(): Int {
    val colorContext = if (DynamicColors.isDynamicColorAvailable()) {
      DynamicColors.wrapContextIfAvailable(context)
    } else {
      val themeResId = when (sharedPrefs.getString(PREF.THEME, DEF.THEME)) {
        THEME.RED -> R.style.Theme_Tack_Red
        THEME.GREEN -> R.style.Theme_Tack_Green
        THEME.BLUE -> R.style.Theme_Tack_Blue
        else -> R.style.Theme_Tack_Yellow
      }
      ContextThemeWrapper(context, themeResId)
    }
    return colorContext.getSysColor(R.attr.colorPrimary)
  }

  companion object {
    private const val CHANNEL_ID = "metronome"
    private const val REQUEST_CODE = 0
    const val NOTIFICATION_ID: Int = 1

    @JvmStatic
    fun hasPermission(context: Context): Boolean {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
          context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    }
  }
}
