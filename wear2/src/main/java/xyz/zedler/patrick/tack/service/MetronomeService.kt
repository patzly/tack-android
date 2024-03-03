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

package xyz.zedler.patrick.tack.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import xyz.zedler.patrick.tack.util.MetronomeUtil
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener
import xyz.zedler.patrick.tack.util.NotificationUtil

class MetronomeService : LifecycleService(), MetronomeListener {

  companion object {
    private const val TAG = "MetronomeService"
  }

  private lateinit var metronomeUtil: MetronomeUtil
  private lateinit var notificationUtil: NotificationUtil

  private var configurationChange = false
  private var inForeground = false
  private val binder = MetronomeBinder()

  override fun onCreate() {
    super.onCreate()

    metronomeUtil = MetronomeUtil(this, true)
    metronomeUtil.addListener(this)

    notificationUtil = NotificationUtil(this)
  }

  override fun onDestroy() {
    super.onDestroy()

    notForegroundService()
    metronomeUtil.destroy()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)

    val stop = intent?.getBooleanExtra(NotificationUtil.EXTRA_STOP, false) ?: false
    if (stop) {
      metronomeUtil.stop()
    }
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent): IBinder {
    super.onBind(intent)

    notForegroundService()
    return binder
  }

  override fun onRebind(intent: Intent) {
    super.onRebind(intent)

    notForegroundService()
  }

  override fun onUnbind(intent: Intent): Boolean {
    val hasPermission = NotificationUtil.hasPermission(this)
    if (hasPermission && !configurationChange && metronomeUtil.isPlaying) {
      notificationUtil.createNotificationChannel()
      val notification = notificationUtil.notification
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
          startForeground(NotificationUtil.NOTIFICATION_ID, notification, type)
        } else {
          startForeground(NotificationUtil.NOTIFICATION_ID, notification)
        }
        inForeground = true
        Log.d(TAG, "onUnbind: started foreground service")
      } catch (e: Exception) {
        Log.e(TAG, "startForeground: could not start foreground", e)
      }
    }
    return true
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)

    configurationChange = true
  }

  private fun notForegroundService() {
    stopForeground(STOP_FOREGROUND_REMOVE)
    inForeground = false
    configurationChange = false
  }

  override fun onMetronomeStart() {}

  override fun onMetronomeStop() {
    notForegroundService()
  }

  override fun onMetronomeTick(tick: MetronomeUtil.Tick?) {}

  fun getMetronomeUtil(): MetronomeUtil {
    return metronomeUtil
  }

  inner class MetronomeBinder : Binder() {
    internal val service: MetronomeService
      get() = this@MetronomeService
  }
}