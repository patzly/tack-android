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

package xyz.zedler.patrick.tack.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.color.DynamicColors;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.THEME;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.service.MetronomeService;

public class NotificationUtil {

  private static final String TAG = NotificationUtil.class.getSimpleName();

  private final static String CHANNEL_ID = "metronome";
  private final static int REQUEST_CODE = 0;
  public final static int NOTIFICATION_ID = 1;

  private final Context context;
  private final SharedPreferences sharedPrefs;
  private final NotificationManagerCompat notificationManager;

  public NotificationUtil(Context context) {
    this.context = context;
    sharedPrefs = new PrefsUtil(context).getSharedPrefs();
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
    if (VERSION.SDK_INT < VERSION_CODES.O) {
      return;
    }
    notificationManager.createNotificationChannel(
        new NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.title_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
    );
  }

  @SuppressLint("MissingPermission")
  public void updateNotification(Notification notification) {
    if (hasPermission()) {
      notificationManager.notify(NOTIFICATION_ID, notification);
    }
  }

  public Notification getNotification(boolean playButton) {
    Intent openIntent = new Intent(context, MainActivity.class);
    openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    PendingIntent activityPendingIntent = PendingIntent.getActivity(
        context, REQUEST_CODE, openIntent, PendingIntent.FLAG_IMMUTABLE
    );

    Intent startIntent = new Intent(context, MetronomeService.class);
    startIntent.setAction(ACTION.START);
    PendingIntent startServicePendingIntent = PendingIntent.getService(
        context, REQUEST_CODE, startIntent, PendingIntent.FLAG_IMMUTABLE
    );
    Action actionStart = new Action(
        R.drawable.ic_rounded_play_arrow_fill,
        context.getString(R.string.action_play),
        startServicePendingIntent
    );

    Intent stopIntent = new Intent(context, MetronomeService.class);
    stopIntent.setAction(ACTION.STOP);
    PendingIntent stopServicePendingIntent = PendingIntent.getService(
        context, REQUEST_CODE, stopIntent, PendingIntent.FLAG_IMMUTABLE
    );
    Action actionStop = new Action(
        R.drawable.ic_rounded_stop_fill,
        context.getString(R.string.action_stop),
        stopServicePendingIntent
    );

    String title = context.getString(R.string.msg_service_running);
    String text = context.getString(R.string.msg_service_running_return);
    return new NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(new NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(text))
        .setContentIntent(activityPendingIntent)
        .addAction(playButton ? actionStart : actionStop)
        .setAutoCancel(true)
        .setSilent(true)
        .setOngoing(true)
        .setColor(getColor())
        .setSmallIcon(R.drawable.ic_logo_notification)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build();
  }

  private int getColor() {
    Context colorContext;
    if (DynamicColors.isDynamicColorAvailable()) {
      colorContext = DynamicColors.wrapContextIfAvailable(context);
    } else {
      int themeResId;
      switch (sharedPrefs.getString(PREF.THEME, DEF.THEME)) {
        case THEME.RED:
          themeResId = R.style.Theme_Tack_Red;
          break;
        case THEME.GREEN:
          themeResId = R.style.Theme_Tack_Green;
          break;
        case THEME.BLUE:
          themeResId = R.style.Theme_Tack_Blue;
          break;
        default:
          themeResId = R.style.Theme_Tack_Yellow;
          break;
      }
      colorContext = new ContextThemeWrapper(context, themeResId);
    }
    return ResUtil.getSysColor(colorContext, R.attr.colorPrimary);
  }
}
