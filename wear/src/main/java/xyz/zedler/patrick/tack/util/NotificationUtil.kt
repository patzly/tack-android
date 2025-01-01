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
package xyz.zedler.patrick.tack.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.service.MetronomeService

class NotificationUtil(private val context: Context) {

  private val notificationManager = NotificationManagerCompat.from(context)

  fun createNotificationChannel() {
    notificationManager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.wear_title_notification_channel),
        NotificationManager.IMPORTANCE_HIGH
      )
    )
  }

  val notification: Notification
    get() {
      val openIntent = Intent(context, MainActivity::class.java)
      openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
      val activityPendingIntent = PendingIntent.getActivity(
        context, REQUEST_CODE, openIntent, PendingIntent.FLAG_IMMUTABLE
      )

      val stopIntent = Intent(context, MetronomeService::class.java)
      stopIntent.setAction(Constants.Action.STOP)
      val servicePendingIntent = PendingIntent.getService(
        context, REQUEST_CODE, stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val actionStop = NotificationCompat.Action(
        R.drawable.ic_rounded_stop,
        context.getString(R.string.wear_action_stop),
        servicePendingIntent
      )

      val title = context.getString(R.string.wear_msg_service_running)
      val text = context.getString(R.string.wear_msg_service_running_return)

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(text))
        .setContentIntent(activityPendingIntent)
        .addAction(actionStop)
        .setAutoCancel(true)
        .setSilent(true)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setColor(-0x253991)
        .setSmallIcon(R.drawable.ic_logo_notification)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
      val ongoingStatus = Status.Builder()
        .addTemplate(context.getString(R.string.wear_msg_service_running))
        .build()
      val ongoingActivity = OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
        .setAnimatedIcon(R.drawable.ic_logo_ongoing)
        .setStaticIcon(R.drawable.ic_logo_notification)
        .setTouchIntent(activityPendingIntent)
        .setStatus(ongoingStatus)
        .build()
      ongoingActivity.apply(context)
      return builder.build()
    }

  companion object {
    private const val CHANNEL_ID = "metronome"
    private const val REQUEST_CODE = 0
    const val NOTIFICATION_ID: Int = 1

    fun hasPermission(context: Context): Boolean {
      if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        val status = ContextCompat.checkSelfPermission(
          context, Manifest.permission.POST_NOTIFICATIONS
        )
        return status == PackageManager.PERMISSION_GRANTED
      } else {
        return true
      }
    }
  }
}
