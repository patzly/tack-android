package xyz.zedler.patrick.tack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.TackApplication
import xyz.zedler.patrick.tack.core.audio.AudioEngine
import xyz.zedler.patrick.tack.core.data.SettingsRepository
import xyz.zedler.patrick.tack.core.data.SongRepository
import xyz.zedler.patrick.tack.core.metronome.MetronomeEngine
import xyz.zedler.patrick.tack.util.FlashlightUtilImpl
import xyz.zedler.patrick.tack.util.HapticUtilImpl

class MetronomeService : Service() {

  private val binder = MetronomeBinder()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  lateinit var engine: MetronomeEngine
  private lateinit var settingsRepository: SettingsRepository
  private lateinit var songRepository: SongRepository

  override fun onCreate() {
    super.onCreate()

    val app = application as TackApplication
    settingsRepository = app.settingsRepository
    songRepository = app.songRepository

    val hapticProvider = HapticUtilImpl(this)
    val flashlightProvider = FlashlightUtilImpl(this)
    val audioEngine = AudioEngine(this) { /* stop */ }

    engine = MetronomeEngine(audioEngine, hapticProvider, flashlightProvider)

    createNotificationChannel()

    serviceScope.launch {
      settingsRepository.metronomeConfig.collect { config ->
        engine.setConfig(config)
      }
    }

    serviceScope.launch {
      settingsRepository.latency.collect { latency ->
        engine.setLatency(latency)
      }
    }

    serviceScope.launch {
      settingsRepository.beatMode.collect { mode ->
        engine.setBeatMode(mode)
      }
    }

    serviceScope.launch {
      engine.state.collectLatest { state ->
        if (state.isPlaying) {
          val notification = getNotification(state.tempo.toString())
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
              NOTIFICATION_ID,
              notification,
              ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
          } else {
            startForeground(NOTIFICATION_ID, notification)
          }
        } else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
          } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
          }
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> engine.start()
      ACTION_STOP -> engine.stop()
      ACTION_APPLY_SONG, ACTION_START_SONG -> {
        val songId = intent.getStringExtra(EXTRA_SONG_ID) ?: "default"
        val startPlaying = intent.action == ACTION_START_SONG
        serviceScope.launch {
          val songWithParts = songRepository.getSongWithPartsAsync(songId)
          songWithParts?.let {
            val part = it.parts.firstOrNull { p -> p.partIndex == 0 }
            part?.let { p ->
              // Here we should update the engine config from the part
              // For simplicity, just update the tempo for now or full config
              engine.setConfig(p.toConfig())
              if (startPlaying) engine.start()
            }
          }
        }
      }
    }
    return START_STICKY
  }

  override fun onBind(intent: Intent): IBinder = binder

  override fun onDestroy() {
    super.onDestroy()
    engine.stop()
    serviceScope.cancel()
  }

  private fun getNotification(content: String): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Metronome")
      .setContentText(content)
      .setSmallIcon(R.drawable.ic_logo_notification)
      .setOngoing(true)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Metronome Service",
        NotificationManager.IMPORTANCE_LOW
      )
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  inner class MetronomeBinder : Binder() {
    fun getService(): MetronomeService = this@MetronomeService
  }

  companion object {
    private const val NOTIFICATION_ID = 1
    private const val CHANNEL_ID = "metronome_channel"

    const val ACTION_START = "xyz.zedler.patrick.tack.action.START"
    const val ACTION_STOP = "xyz.zedler.patrick.tack.action.STOP"
    const val ACTION_APPLY_SONG = "xyz.zedler.patrick.tack.action.APPLY_SONG"
    const val ACTION_START_SONG = "xyz.zedler.patrick.tack.action.START_SONG"

    const val EXTRA_SONG_ID = "song_id"
  }
}
