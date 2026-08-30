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

package xyz.zedler.patrick.tack.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.ACTION
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.EXTRA
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.Constants.UNIT
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.metronome.MetronomeEngine
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListenerAdapter
import xyz.zedler.patrick.tack.util.NotificationUtil
import xyz.zedler.patrick.tack.util.PrefsUtil

class MetronomeService : Service() {

  private val binder = MetronomeBinder()
  private val mainHandler = Handler(Looper.getMainLooper())

  val metronomeEngine: MetronomeEngine by lazy { MetronomeEngine(this) }
  private val notificationUtil: NotificationUtil by lazy { NotificationUtil(this) }
  private val sharedPrefs: SharedPreferences by lazy { PrefsUtil(this).sharedPrefs }

  private var isBound = false
  private var configChange = false
  private var permNotification = false
  private var showPlayButton = false

  override fun onCreate() {
    super.onCreate()

    metronomeEngine.addListener(object : MetronomeListenerAdapter() {
      override fun onMetronomeStart() {
        mainHandler.post {
          if (permNotification && hasPermission()) {
            showPlayButton = false
            notificationUtil.updateNotification(getNotification())
          }
        }
      }

      override fun onMetronomeStop() {
        mainHandler.post {
          if (permNotification && hasPermission()) {
            showPlayButton = true
            notificationUtil.updateNotification(getNotification())
          }
        }
      }

      override fun onMetronomeTick(tick: MetronomeEngine.Tick) {
        mainHandler.post {
          if (metronomeEngine.config.isTimerActive() &&
            metronomeEngine.config.timerUnit == UNIT.BARS
          ) {
            updateTimerNotification()
          }
        }
      }

      override fun onMetronomeTimerSecondsChanged() {
        mainHandler.post { updateTimerNotification() }
      }

      override fun onMetronomeTimerProgressOneTime(withTransition: Boolean) {
        mainHandler.post { updateTimerNotification() }
      }

      override fun onMetronomeTimerActiveStateChanged(active: Boolean) {
        mainHandler.post { updateTimerNotification() }
      }

      private fun updateTimerNotification() {
        val isTimerActive = metronomeEngine.config.isTimerActive()
        if (isTimerActive && (permNotification || !isBound) && hasPermission()) {
          notificationUtil.updateNotification(getNotification())
        }
      }
    })

    permNotification = sharedPrefs.getBoolean(
      PREF.PERM_NOTIFICATION, DEF.PERM_NOTIFICATION
    )
    if (permNotification && hasPermission()) {
      showPlayButton = true
      startForeground()
    }
    Log.d(TAG, "onCreate: service created")
  }

  override fun onDestroy() {
    super.onDestroy()
    stopForeground()
    metronomeEngine.destroy()
    Log.d(TAG, "onDestroy: service destroyed")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action
    if (action != null) {
      when (action) {
        ACTION.START -> metronomeEngine.start()
        ACTION.APPLY_SONG, ACTION.START_SONG -> {
          var songId = intent.getStringExtra(EXTRA.SONG_ID)
          if (songId == null) {
            songId = Constants.SONG_ID_DEFAULT
          }
          val startPlaying = action == ACTION.START_SONG
          metronomeEngine.setCurrentSong(songId, 0, startPlaying)
        }

        ACTION.STOP -> {
          metronomeEngine.stop()
          if (!permNotification && hasPermission()) {
            stopForeground()
            stopSelf()
          }
        }

        ACTION.DISMISS -> {
          if (!isBound) {
            metronomeEngine.stop()
            stopForeground()
            stopSelf()
          }
        }
      }
    }
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent): IBinder {
    if (!permNotification && hasPermission()) {
      stopForeground()
    }
    isBound = true
    return binder
  }

  override fun onRebind(intent: Intent) {
    super.onRebind(intent)
    if (!permNotification && hasPermission()) {
      stopForeground()
    }
    isBound = true
  }

  override fun onUnbind(intent: Intent): Boolean {
    isBound = false
    if (hasPermission()) {
      if (!permNotification && canShowNonPermNotification()) {
        showPlayButton = false
        startForeground()
      } else if (permNotification) {
        showPlayButton = !metronomeEngine.isPlaying()
        notificationUtil.updateNotification(getNotification())
      }
    }
    return true
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    configChange = true
  }

  private fun startForeground() {
    if (hasPermission() && !configChange) {
      notificationUtil.createNotificationChannel()
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
          startForeground(
            NotificationUtil.NOTIFICATION_ID,
            getNotification(),
            type
          )
        } else {
          startForeground(NotificationUtil.NOTIFICATION_ID, getNotification())
        }
      } catch (e: Exception) {
        Log.e(TAG, "startForeground: could not start foreground", e)
      }
    }
  }

  private fun stopForeground() {
    stopForeground(STOP_FOREGROUND_REMOVE)
    configChange = false
  }

  fun usePermNotification(): Boolean = permNotification

  fun setPermNotification(permanent: Boolean): Boolean {
    if (permNotification != permanent) {
      if (permanent) {
        showPlayButton = !metronomeEngine.isPlaying()
        if (hasPermission()) {
          startForeground()
        } else {
          throw IllegalStateException("Notification permission missing")
        }
      } else {
        if (!isBound && canShowNonPermNotification()) {
          if (hasPermission()) {
            // Only provide stop action in non-permanent notification
            showPlayButton = false
            startForeground()
          } else {
            throw IllegalStateException("Notification permission missing")
          }
        } else {
          stopForeground()
        }
      }
      permNotification = permanent
      sharedPrefs.edit { putBoolean(PREF.PERM_NOTIFICATION, permanent) }
    }
    return permNotification
  }

  private fun canShowNonPermNotification(): Boolean {
    val realTimeActive = metronomeEngine.config.isTimerActive() || metronomeEngine.isElapsedActive()
    return metronomeEngine.isPlaying() || realTimeActive
  }

  private fun getNotification(): Notification {
    val isTimerActive = metronomeEngine.config.isTimerActive()
    return notificationUtil.getNotification(
      showPlayButton,
      isTimerActive,
      isTimerActive && metronomeEngine.isPlaying(),
      getString(
        R.string.label_part_duration_notification,
        metronomeEngine.getCurrentTimerString(),
        metronomeEngine.getTotalTimeString()
      ),
      metronomeEngine.getCurrentTimerString(),
      metronomeEngine.getTimerProgress(),
      metronomeEngine.config.timerDuration
    )
  }

  private fun hasPermission(): Boolean = notificationUtil.hasPermission()

  inner class MetronomeBinder : Binder() {
    fun getService(): MetronomeService = this@MetronomeService
  }

  companion object {
    private val TAG = MetronomeService::class.java.simpleName
  }
}
