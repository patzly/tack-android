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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.wear.ongoing.OngoingActivity;
import androidx.wear.ongoing.Status;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.presentation.MainActivity;
import xyz.zedler.patrick.tack.service.MetronomeService;

public class NotificationUtil {

  private final static String CHANNEL_ID = "metronome";
  private final static int REQUEST_CODE = 0;
  public final static int NOTIFICATION_ID = 1;

  private final Context context;
  private final NotificationManagerCompat notificationManager;

  public NotificationUtil(Context context) {
    this.context = context;
    notificationManager = NotificationManagerCompat.from(context);
  }

  public boolean hasPermission() {
    return hasPermission(context);
  }

  public static boolean hasPermission(@NonNull Context context) {
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
      int status = ContextCompat.checkSelfPermission(
          context, Manifest.permission.POST_NOTIFICATIONS
      );
      return status == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  public void createNotificationChannel() {
    notificationManager.createNotificationChannel(
        new NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.wear_title_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
    );
  }

  public Notification getNotification() {
    Intent openIntent = new Intent(context, MainActivity.class);
    openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    PendingIntent activityPendingIntent = PendingIntent.getActivity(
        context, REQUEST_CODE, openIntent, PendingIntent.FLAG_IMMUTABLE
    );

    Intent stopIntent = new Intent(context, MetronomeService.class);
    stopIntent.setAction(Constants.Action.STOP);
    PendingIntent servicePendingIntent = PendingIntent.getService(
        context, REQUEST_CODE, stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
    Action actionStop = new Action(
        R.drawable.ic_rounded_stop, context.getString(R.string.wear_action_stop), servicePendingIntent
    );

    String title = context.getString(R.string.wear_msg_service_running);
    String text = context.getString(R.string.wear_msg_service_running_return);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(new NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(text))
        .setContentIntent(activityPendingIntent)
        .addAction(actionStop)
        .setAutoCancel(true)
        .setSilent(true)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setColor(0xFFDAC66F)
        .setSmallIcon(R.drawable.ic_logo_notification)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
    Status ongoingStatus = new Status.Builder()
        .addTemplate(context.getString(R.string.wear_msg_service_running))
        .build();
    OngoingActivity ongoingActivity = new OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
        .setAnimatedIcon(R.drawable.ic_logo_ongoing)
        .setStaticIcon(R.drawable.ic_logo_notification)
        .setTouchIntent(activityPendingIntent)
        .setStatus(ongoingStatus)
        .build();
    ongoingActivity.apply(context);
    return builder.build();
  }
}
