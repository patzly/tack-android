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

package xyz.zedler.patrick.tack.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListenerAdapter;
import xyz.zedler.patrick.tack.util.NotificationUtil;
import xyz.zedler.patrick.tack.util.PrefsUtil;

public class MetronomeService extends Service {

  private static final String TAG = MetronomeService.class.getSimpleName();

  private final IBinder binder = new MetronomeBinder();
  private MetronomeEngine metronomeEngine;
  private NotificationUtil notificationUtil;
  private SharedPreferences sharedPrefs;
  private boolean isBound, configChange, permNotification;

  @Override
  public void onCreate() {
    super.onCreate();

    notificationUtil = new NotificationUtil(this);
    metronomeEngine = new MetronomeEngine(this);
    metronomeEngine.addListener(new MetronomeListenerAdapter() {
      @Override
      public void onMetronomeStart() {
        if (permNotification && hasPermission()) {
          notificationUtil.updateNotification(notificationUtil.getNotification(false));
        }
      }

      @Override
      public void onMetronomeStop() {
        if (permNotification && hasPermission()) {
          notificationUtil.updateNotification(notificationUtil.getNotification(true));
        }
      }
    });

    sharedPrefs = new PrefsUtil(this).getSharedPrefs();

    permNotification = sharedPrefs.getBoolean(PREF.PERM_NOTIFICATION, DEF.PERM_NOTIFICATION);
    if (permNotification && hasPermission()) {
      startForeground(true);
    }
    Log.d(TAG, "onCreate: service created");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    stopForeground();
    metronomeEngine.destroy();
    Log.d(TAG, "onDestroy: service destroyed");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && intent.getAction() != null) {
      String action = intent.getAction();
      switch (action) {
        case ACTION.START:
          metronomeEngine.start();
          break;
        case ACTION.APPLY_SONG:
        case ACTION.START_SONG:
          String songId = intent.getStringExtra(EXTRA.SONG_ID);
          if (songId == null) {
            songId = Constants.SONG_ID_DEFAULT;
          }
          boolean startPlaying = action.equals(ACTION.START_SONG);
          metronomeEngine.setCurrentSong(songId, 0, startPlaying);
          break;
        case ACTION.STOP:
          metronomeEngine.stop();
          if (!permNotification && hasPermission()) {
            stopForeground();
          }
          break;
      }
    }
    return START_NOT_STICKY;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    if (!permNotification && hasPermission()) {
      stopForeground();
    }
    isBound = true;
    return binder;
  }

  @Override
  public void onRebind(Intent intent) {
    super.onRebind(intent);

    if (!permNotification && hasPermission()) {
      stopForeground();
    }
    isBound = true;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    if (canShowNonPermNotification() && !permNotification && hasPermission()) {
      startForeground(false);
    }
    isBound = false;
    return true;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    configChange = true;
  }

  private void startForeground(boolean playButton) {
    if (hasPermission() && !configChange) {
      notificationUtil.createNotificationChannel();
      Notification notification = notificationUtil.getNotification(playButton);
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;
          startForeground(NotificationUtil.NOTIFICATION_ID, notification, type);
        } else {
          startForeground(NotificationUtil.NOTIFICATION_ID, notification);
        }
      } catch (Exception e) {
        Log.e(TAG, "startForeground: could not start foreground", e);
      }
    }
  }

  private void stopForeground() {
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE);
    } else {
      stopForeground(true);
    }
    configChange = false;
  }

  public MetronomeEngine getMetronomeEngine() {
    return metronomeEngine;
  }

  public boolean getPermNotification() {
    return permNotification;
  }

  public boolean setPermNotification(boolean permanent) {
    if (permNotification != permanent) {
      if (permanent) {
        if (hasPermission()) {
          startForeground(!metronomeEngine.isPlaying());
        } else {
          throw new IllegalStateException("Notification permission missing");
        }
      } else {
        if (!isBound && canShowNonPermNotification()) {
          if (hasPermission()) {
            // Only provide stop action in non-permanent notification
            startForeground(false);
          } else {
            throw new IllegalStateException("Notification permission missing");
          }
        } else {
          stopForeground();
        }
      }
      permNotification = permanent;
      sharedPrefs.edit().putBoolean(PREF.PERM_NOTIFICATION, permanent).apply();
    }
    return permNotification;
  }

  private boolean canShowNonPermNotification() {
    boolean realTimeActive =
        metronomeEngine.getConfig().isTimerActive() || metronomeEngine.isElapsedActive();
    return metronomeEngine.isPlaying() || realTimeActive;
  }

  private boolean hasPermission() {
    return notificationUtil.hasPermission();
  }

  public class MetronomeBinder extends Binder {

    public MetronomeService getService() {
      return MetronomeService.this;
    }
  }
}