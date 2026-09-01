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
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.TackApplication
import xyz.zedler.patrick.tack.core.audio.AudioEngine
import xyz.zedler.patrick.tack.core.data.MetronomeRepository
import xyz.zedler.patrick.tack.core.data.SettingsRepository
import xyz.zedler.patrick.tack.core.data.SongRepository
import xyz.zedler.patrick.tack.core.metronome.MetronomeEngine
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import xyz.zedler.patrick.tack.core.model.MetronomeConstants
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.core.model.TimingUnit
import xyz.zedler.patrick.tack.core.util.TimeUtil
import xyz.zedler.patrick.tack.hardware.FlashlightProviderImpl
import xyz.zedler.patrick.tack.hardware.HapticProviderImpl
import xyz.zedler.patrick.tack.util.NotificationUtil
import java.util.Locale
import kotlin.math.roundToInt

class MetronomeService : Service() {

  private val binder = MetronomeBinder()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  lateinit var engine: MetronomeEngine
  private lateinit var settingsRepository: SettingsRepository
  private lateinit var metronomeRepository: MetronomeRepository
  private lateinit var songRepository: SongRepository
  private lateinit var hapticProvider: HapticProviderImpl

  private var isBound = false
  private var appSettings = AppSettings()

  private val _externalSongLoad = MutableSharedFlow<SongLoadEvent>(
    extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val externalSongLoad: SharedFlow<SongLoadEvent> = _externalSongLoad.asSharedFlow()

  data class SongLoadEvent(val songId: String?, val partIndex: Int, val config: MetronomeConfig)

  override fun onCreate() {
    super.onCreate()

    val app = application as TackApplication
    settingsRepository = app.settingsRepository
    metronomeRepository = app.metronomeRepository
    songRepository = app.songRepository

    hapticProvider = HapticProviderImpl(this)
    val flashlightProvider = FlashlightProviderImpl(this)
    val audioEngine = AudioEngine(this) { engine.stop() }

    engine = MetronomeEngine(audioEngine, hapticProvider, flashlightProvider)

    NotificationUtil.createNotificationChannel(this)

    serviceScope.launch {
      settingsRepository.settings.collect { settings ->
        appSettings = settings
        hapticProvider.isEnabled = settings.haptic
        hapticProvider.intensity = settings.vibrationIntensity

        engine.updateSettings(settings)
        updateServiceState()
      }
    }

    serviceScope.launch {
      engine.state.collect { state ->
        updateServiceState(state)
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
          stopForegroundCompat(true)
          stopSelf()
        }
      }
      ACTION_APPLY_SONG, ACTION_START_SONG -> {
        val songId = intent.getStringExtra(EXTRA_SONG_ID)
        val startPlaying = intent.action == ACTION_START_SONG

        serviceScope.launch {
          if (songId == null || songId == "default") {
            val config = metronomeRepository.metronomeConfig.first()
            engine.setPlaylist(
              emptyList(), false, 0, false
            )
            engine.applyConfig(config)
            if (startPlaying) engine.start()

            metronomeRepository.updateActiveSong(null)
            _externalSongLoad.tryEmit(
              SongLoadEvent(null, 0, config)
            )
          } else {
            val songWithParts = songRepository.getSongWithPartsAsync(songId)
            if (songWithParts != null && songWithParts.parts.isNotEmpty()) {
              val speedModifier = songWithParts.song.speed / 100.0
              val playlist = songWithParts.parts.map { part ->
                val c = part.toConfig()
                val effectiveTempo = (c.tempo * speedModifier).roundToInt()
                  .coerceIn(MetronomeConstants.TEMPO_MIN, MetronomeConstants.TEMPO_MAX)
                c.copy(tempo = effectiveTempo)
              }

              engine.setPlaylist(
                playlist, songWithParts.song.isLooped, 0, startPlaying
              )
              metronomeRepository.updateActiveSong(songId, 0)
              _externalSongLoad.tryEmit(
                SongLoadEvent(songId, 0, playlist[0])
              )
            }
          }
        }
      }
    }
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent): IBinder {
    isBound = true
    updateServiceState()
    return binder
  }

  override fun onRebind(intent: Intent) {
    super.onRebind(intent)
    isBound = true
    updateServiceState()
  }

  override fun onUnbind(intent: Intent?): Boolean {
    isBound = false
    updateServiceState()
    return true
  }

  override fun onDestroy() {
    super.onDestroy()
    stopForegroundCompat(true)
    engine.destroy()
    serviceScope.cancel()
  }

  private fun updateServiceState(state: MetronomeState = engine.state.value) {
    if (!NotificationUtil.hasPermission(this)) {
      stopForegroundCompat(true)
      if (!isBound) stopSelf()
      return
    }

    val config = engine.config
    val isTimerActive = config.isTimerActive
    val isElapsedActive = appSettings.showElapsed
    val realTimeActive = isTimerActive || isElapsedActive
    val shouldShowNonPermanent = state.isPlaying || realTimeActive

    val showNotification = appSettings.permanentNotification || (!isBound && shouldShowNonPermanent)

    if (showNotification) {
      val notification = getNotification(state)
      if (state.isPlaying) {
        startForegroundCompat(notification)
      } else {
        NotificationUtil.updateNotification(this, notification)
        stopForegroundCompat(false)
      }
    } else {
      stopForegroundCompat(true)
      if (!isBound) {
        stopSelf()
      }
    }
  }

  private fun startForegroundCompat(notification: Notification) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          NotificationUtil.NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
      } else {
        startForeground(NotificationUtil.NOTIFICATION_ID, notification)
      }
    } catch (e: Exception) {
      Log.e(TAG, "startForegroundCompat", e)
    }
  }

  private fun stopForegroundCompat(removeNotification: Boolean) {
    stopForeground(
      if (removeNotification) {
        STOP_FOREGROUND_REMOVE
      } else {
        STOP_FOREGROUND_DETACH
      }
    )
  }

  private fun getNotification(state: MetronomeState): Notification {
    val config = engine.config
    val isTimerActive = config.isTimerActive

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
      timerDuration = config.timerDuration
    )
  }

  private fun getCurrentTimerString(state: MetronomeState): String {
    val config = engine.config
    return when (config.timerUnit) {
      TimingUnit.SECONDS, TimingUnit.MINUTES -> {
        val totalMillis = engine.getTimerInterval()
        val currentMillis = (state.timerProgress * totalMillis).toLong()
        val seconds = (currentMillis / 1000).toInt()
        val totalHours = if (config.timerUnit == TimingUnit.MINUTES) {
          config.timerDuration / 60
        } else {
          config.timerDuration / 3600
        }
        TimeUtil.getTimeStringFromSeconds(seconds, totalHours > 0)
      }
      else -> {
        var format = if (config.beatsCount < 10) "%d.%01d" else "%d.%02d"
        if (config.subdivisionsCount > 1) {
          format += if (config.subdivisionsCount < 10) ".%01d" else ".%02d"
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
    val config = engine.config
    return when (config.timerUnit) {
      TimingUnit.SECONDS, TimingUnit.MINUTES -> {
        val seconds = if (config.timerUnit == TimingUnit.MINUTES) {
          config.timerDuration * 60
        } else {
          config.timerDuration
        }
        TimeUtil.getTimeStringFromSeconds(seconds, false)
      }
      else -> resources.getQuantityString(
        R.plurals.options_unit_bars,
        config.timerDuration,
        config.timerDuration
      )
    }
  }

  inner class MetronomeBinder : Binder() {
    fun getService(): MetronomeService = this@MetronomeService
  }

  companion object {
    private val TAG = MetronomeService::class.java.simpleName

    const val ACTION_START = "xyz.zedler.patrick.tack.action.START"
    const val ACTION_STOP = "xyz.zedler.patrick.tack.action.STOP"
    const val ACTION_DISMISS = "xyz.zedler.patrick.tack.action.DISMISS"
    const val ACTION_APPLY_SONG = "xyz.zedler.patrick.tack.action.APPLY_SONG"
    const val ACTION_START_SONG = "xyz.zedler.patrick.tack.action.START_SONG"

    const val EXTRA_SONG_ID = "song_id"
  }
}
