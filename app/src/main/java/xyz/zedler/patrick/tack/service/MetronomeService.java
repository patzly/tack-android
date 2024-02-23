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

package xyz.zedler.patrick.tack.service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.util.Objects;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.util.MetronomeUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.NotificationUtil;

public class MetronomeService extends Service implements MetronomeListener {

  private static final String TAG = MetronomeService.class.getSimpleName();

  private final static int NOTIFICATION_ID = 1;

  private final IBinder binder = new MetronomeBinder();
  private MetronomeUtil metronomeUtil;
  private NotificationUtil notificationUtil;
  private StopReceiver stopReceiver;

  @Override
  public void onCreate() {
    super.onCreate();

    notificationUtil = new NotificationUtil(this);
    metronomeUtil = new MetronomeUtil(this, true);
    metronomeUtil.addListener(this);

    stopReceiver = new StopReceiver();
    ContextCompat.registerReceiver(
        this, stopReceiver, new IntentFilter(ACTION.STOP),
        ContextCompat.RECEIVER_EXPORTED
    );
    Log.d(TAG, "onCreate: service created");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    stopForeground();
    metronomeUtil.destroy();
    unregisterReceiver(stopReceiver);
    Log.d(TAG, "onDestroy: service destroyed");
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && intent.getAction() != null) {
      if (intent.getAction().equals(ACTION.START)) {
        metronomeUtil.setTempo(intent.getIntExtra(EXTRA.TEMPO, metronomeUtil.getTempo()));
        metronomeUtil.start();
      }
    }
    return START_STICKY;
  }

  @Override
  public void onMetronomeStart() {
    if (notificationUtil.hasPermission()) {
      startForeground();
    }
  }

  @Override
  public void onMetronomeStop() {}

  @Override
  public void onMetronomePreTick(Tick tick) {}

  @Override
  public void onMetronomeTick(Tick tick) {}

  @Override
  public void onMetronomeTempoChanged(int tempoOld, int tempoNew) {}

  @Override
  public void onMetronomeTimerStarted() {}

  @Override
  public void onElapsedTimeSecondsChanged() {}

  @Override
  public void onTimerSecondsChanged() {}

  @Override
  public void onMetronomeConnectionMissing() {}

  public void startForeground() {
    notificationUtil.createNotificationChannel();
    Notification notification = notificationUtil.getNotification();
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;
        startForeground(NOTIFICATION_ID, notification, type);
      } else {
        startForeground(NOTIFICATION_ID, notification);
      }
    } catch (Exception e) {
      Log.e(TAG, "startForeground: could not start foreground", e);
    }
  }

  private void stopForeground() {
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE);
    } else {
      stopForeground(true);
    }
  }

  public MetronomeUtil getMetronomeUtil() {
    return metronomeUtil;
  }

  public class StopReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent != null && Objects.equals(intent.getAction(), ACTION.STOP)) {
        metronomeUtil.stop();
        stopForeground();
        Log.d(TAG, "onReceive: stopped foreground");
      }
    }
  }

  public class MetronomeBinder extends Binder {

    public MetronomeService getService() {
      return MetronomeService.this;
    }
  }
}