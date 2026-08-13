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
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.TackApplication
import xyz.zedler.patrick.tack.core.audio.AudioEngine
import xyz.zedler.patrick.tack.core.data.MetronomeRepository
import xyz.zedler.patrick.tack.core.data.SettingsRepository
import xyz.zedler.patrick.tack.core.data.SongRepository
import xyz.zedler.patrick.tack.core.metronome.MetronomeEngine
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.core.model.TimingUnit
import xyz.zedler.patrick.tack.core.util.TimeUtil
import xyz.zedler.patrick.tack.hardware.FlashlightProviderImpl
import xyz.zedler.patrick.tack.hardware.HapticProviderImpl
import xyz.zedler.patrick.tack.util.NotificationUtil
import java.util.Locale

class MetronomeService : Service() {

  private val binder = MetronomeBinder()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  lateinit var engine: MetronomeEngine
  private lateinit var settingsRepository: SettingsRepository
  private lateinit var metronomeRepository: MetronomeRepository
  private lateinit var songRepository: SongRepository
  private lateinit var hapticProvider: HapticProviderImpl

  private var isBound = false
  private var permNotification = false
  private var metronomeConfig = MetronomeConfig()

  override fun onCreate() {
    super.onCreate()

    val app = application as TackApplication
    settingsRepository = app.settingsRepository
    metronomeRepository = app.metronomeRepository
    songRepository = app.songRepository

    hapticProvider = HapticProviderImpl(this)
    val flashlightProvider = FlashlightProviderImpl(this)
    val audioEngine = AudioEngine(this) { /* stop callback */ }

    engine = MetronomeEngine(audioEngine, hapticProvider, flashlightProvider)

    NotificationUtil.createNotificationChannel(this)

    serviceScope.launch {
      settingsRepository.settings.collect { settings ->
        hapticProvider.isEnabled = settings.haptic
        hapticProvider.intensity = settings.vibrationIntensity
        
        engine.setLatency(settings.latency)
        engine.setBeatMode(settings.beatMode)
        engine.setFlashlight(settings.flashlight)
        permNotification = settings.permNotification
      }
    }

    serviceScope.launch {
      metronomeRepository.metronomeConfig.collect { config ->
        metronomeConfig = config
        engine.setConfig(config)
      }
    }

    serviceScope.launch {
      engine.state.collectLatest { state ->
        if (state.isPlaying || (permNotification && hasPermission())) {
          val notification = getNotification(state)
          if (state.isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              startForeground(
                NotificationUtil.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
              )
            } else {
              startForeground(NotificationUtil.NOTIFICATION_ID, notification)
            }
          } else {
            NotificationUtil.updateNotification(this@MetronomeService, notification)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
              stopForeground(STOP_FOREGROUND_DETACH)
            } else {
              @Suppress("DEPRECATION")
              stopForeground(false)
            }
          }
        } else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
          } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
          }
          if (!isBound) stopSelf()
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> engine.start()
      ACTION_STOP -> engine.stop()
      ACTION_DISMISS -> {
        if (!isBound) {
          engine.stop()
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
          } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
          }
          stopSelf()
        }
      }
      ACTION_APPLY_SONG, ACTION_START_SONG -> {
        val songId = intent.getStringExtra(EXTRA_SONG_ID) ?: "default"
        val startPlaying = intent.action == ACTION_START_SONG
        serviceScope.launch {
          val songWithParts = songRepository.getSongWithPartsAsync(songId)
          songWithParts?.let {
            val part = it.parts.firstOrNull { p -> p.partIndex == 0 }
            part?.let { p ->
              engine.setConfig(p.toConfig())
              if (startPlaying) engine.start()
            }
          }
        }
      }
    }
    return START_STICKY
  }

  override fun onBind(intent: Intent): IBinder {
    isBound = true
    return binder
  }

  override fun onUnbind(intent: Intent?): Boolean {
    isBound = false
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    engine.destroy()
    serviceScope.cancel()
  }

  private fun getNotification(state: MetronomeState): Notification {
    val isTimerActive = metronomeConfig.isTimerActive
    val timerTextLong = if (isTimerActive) {
      getString(
        R.string.label_part_duration_notification,
        getCurrentTimerString(state),
        getTotalTimerString()
      )
    } else {
      getString(R.string.msg_service_running_return)
    }

    return NotificationUtil.getNotification(
      context = this,
      showPlayButton = !state.isPlaying,
      showTimer = isTimerActive,
      showPromotedLiveUpdate = state.isPlaying,
      timerTextLong = timerTextLong,
      timerTextShort = if (isTimerActive) getCurrentTimerString(state) else state.tempo.toString(),
      timerProgress = state.timerProgress,
      timerDuration = metronomeConfig.timerDuration
    )
  }

  private fun getCurrentTimerString(state: MetronomeState): String {
    return when (metronomeConfig.timerUnit) {
      TimingUnit.SECONDS, TimingUnit.MINUTES -> {
        val totalMillis = engine.getTimerInterval()
        val currentMillis = (state.timerProgress * totalMillis).toLong()
        val seconds = (currentMillis / 1000).toInt()
        val totalHours = if (metronomeConfig.timerUnit == TimingUnit.MINUTES) {
          metronomeConfig.timerDuration / 60
        } else {
          metronomeConfig.timerDuration / 3600
        }
        TimeUtil.getTimeStringFromSeconds(seconds, totalHours > 0)
      }
      else -> {
        var format = if (metronomeConfig.beatsCount < 10) "%d.%01d" else "%d.%02d"
        if (metronomeConfig.subdivisionsCount > 1) {
          format += if (metronomeConfig.subdivisionsCount < 10) ".%01d" else ".%02d"
          String.format(
            Locale.ENGLISH,
            format,
            state.timerBarIndex + 1,
            state.timerBeatIndex + 1,
            state.timerSubIndex + 1
          )
        } else {
          String.format(
            Locale.ENGLISH,
            format,
            state.timerBarIndex + 1,
            state.timerBeatIndex + 1
          )
        }
      }
    }
  }

  private fun getTotalTimerString(): String {
    return when (metronomeConfig.timerUnit) {
      TimingUnit.SECONDS, TimingUnit.MINUTES -> {
        val seconds = if (metronomeConfig.timerUnit == TimingUnit.MINUTES) {
          metronomeConfig.timerDuration * 60
        } else {
          metronomeConfig.timerDuration
        }
        TimeUtil.getTimeStringFromSeconds(seconds, false)
      }
      else -> resources.getQuantityString(
        R.plurals.options_unit_bars,
        metronomeConfig.timerDuration,
        metronomeConfig.timerDuration
      )
    }
  }

  private fun hasPermission(): Boolean = NotificationUtil.hasPermission(this)

  inner class MetronomeBinder : Binder() {
    fun getService(): MetronomeService = this@MetronomeService
  }

  companion object {
    const val ACTION_START = "xyz.zedler.patrick.tack.action.START"
    const val ACTION_STOP = "xyz.zedler.patrick.tack.action.STOP"
    const val ACTION_DISMISS = "xyz.zedler.patrick.tack.action.DISMISS"
    const val ACTION_APPLY_SONG = "xyz.zedler.patrick.tack.action.APPLY_SONG"
    const val ACTION_START_SONG = "xyz.zedler.patrick.tack.action.START_SONG"

    const val EXTRA_SONG_ID = "song_id"
  }
}
