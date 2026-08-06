package xyz.zedler.patrick.tack.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.service.MetronomeService
import kotlin.math.roundToInt

object NotificationUtil {

  const val NOTIFICATION_ID: Int = 1
  private const val CHANNEL_ID = "metronome"
  private const val REQUEST_CODE = 0

  fun hasPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.title_notification_channel),
        NotificationManager.IMPORTANCE_HIGH
      )
    )
  }

  @SuppressLint("MissingPermission")
  fun updateNotification(context: Context, notification: Notification) {
    if (hasPermission(context)) {
      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
  }

  fun getNotification(
    context: Context,
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
      action = MetronomeService.ACTION_START
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
      action = MetronomeService.ACTION_STOP
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
      action = MetronomeService.ACTION_DISMISS
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
      .setColor(getColor(context))
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
          val currentOffset = ((i * 100f) / safeDuration).roundToInt()
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

  private fun getColor(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.getColor(context, android.R.color.system_accent1_600)
    } else {
      ContextCompat.getColor(context, R.color.yellow_theme_primary)
    }
  }
}
