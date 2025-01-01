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

package xyz.zedler.patrick.tack.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import xyz.zedler.patrick.tack.Constants.Action
import xyz.zedler.patrick.tack.util.MetronomeUtil
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListenerAdapter
import xyz.zedler.patrick.tack.util.NotificationUtil

class MetronomeService : LifecycleService() {

  companion object {
    private const val TAG = "MetronomeService"
  }

  private lateinit var metronomeUtil: MetronomeUtil
  private lateinit var notificationUtil: NotificationUtil

  private var configChange = false
  private val binder = MetronomeBinder()

  override fun onCreate() {
    super.onCreate()

    metronomeUtil = MetronomeUtil(this, true)
    metronomeUtil.addListener(object : MetronomeListenerAdapter() {
      override fun onMetronomeStop() {
        stopForeground()
      }
    })

    notificationUtil = NotificationUtil(this)
  }

  override fun onDestroy() {
    super.onDestroy()

    stopForeground()
    metronomeUtil.destroy()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)

    val action = intent?.action ?: ""
    if (action == Action.STOP) {
      metronomeUtil.stop()
    }
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent): IBinder {
    super.onBind(intent)

    stopForeground()
    return binder
  }

  override fun onRebind(intent: Intent) {
    super.onRebind(intent)

    stopForeground()
  }

  override fun onUnbind(intent: Intent): Boolean {
    startForeground()
    return true
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)

    configChange = true
  }

  private fun startForeground() {
    val hasPermission = NotificationUtil.hasPermission(this)
    if (hasPermission && !configChange && metronomeUtil.isPlaying) {
      notificationUtil.createNotificationChannel()
      val notification = notificationUtil.notification
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
          startForeground(NotificationUtil.NOTIFICATION_ID, notification, type)
        } else {
          startForeground(NotificationUtil.NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "startForeground: started foreground service")
      } catch (e: Exception) {
        Log.e(TAG, "startForeground: could not start foreground", e)
      }
    }
  }

  private fun stopForeground() {
    stopForeground(STOP_FOREGROUND_REMOVE)
    configChange = false
  }

  fun getMetronomeUtil(): MetronomeUtil {
    return metronomeUtil
  }

  inner class MetronomeBinder : Binder() {
    internal val service: MetronomeService
      get() = this@MetronomeService
  }
}