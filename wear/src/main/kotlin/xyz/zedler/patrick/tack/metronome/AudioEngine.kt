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

package xyz.zedler.patrick.tack.metronome

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.RawRes
import androidx.core.content.getSystemService
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.Tick
import xyz.zedler.patrick.tack.util.AudioUtil
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.pow

@Keep
class AudioEngine(
  private val context: Context,
  private val listener: AudioListener
) : OnAudioFocusChangeListener {

  private val audioManager: AudioManager? = context.getSystemService()
  private var engineHandle: Long = 0
  private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  private var delayedStopTask: ScheduledFuture<*>? = null
  private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
    .setAudioAttributes(AudioUtil.getAttributes())
    .setWillPauseWhenDucked(true)
    .setOnAudioFocusChangeListener(this)
    .build()

  @Volatile
  private var isPlaying: Boolean = false
  @Volatile
  private var isStreamRunning: Boolean = false
  var ignoreFocus: Boolean = false

  private val isInitialized: Boolean
    get() = engineHandle != 0L

  // Properties with custom setters to update native engine immediately
  var gain: Int = Constants.Def.GAIN
    set(value) {
      field = value
      if (isInitialized) {
        val dbToLinear = 10.0.pow(value / 20.0).toFloat()
        nativeSetMasterVolume(engineHandle, dbToLinear)
      }
    }

  var isMuted: Boolean = false
    set(value) {
      field = value
      if (isInitialized) {
        nativeSetMuted(engineHandle, value)
      }
    }

  init {
    engineHandle = nativeCreate()
    if (engineHandle == 0L) {
      Log.e(TAG, "Failed to create Oboe engine")
    } else {
      val initSuccess = nativeInit(engineHandle)
      if (!initSuccess) {
        Log.e(TAG, "Failed to init Oboe audio stream")
      } else {
        // Initialize defaults
        setSound(Constants.Def.SOUND)
        // Force update gain on native side
        val currentGain = gain
        gain = currentGain
      }
    }
  }

  fun destroy() {
    executor.shutdownNow()
    stop()
    if (isInitialized) {
      nativeDestroy(engineHandle)
      engineHandle = 0
    }
  }

  fun warmUp() {
    if (!isInitialized) return
    cancelDelayedStop()

    if (!isStreamRunning) {
      val success = nativeStart(engineHandle)
      if (success) {
        isStreamRunning = true
      } else {
        Log.e(TAG, "Failed to warm up Oboe audio stream")
      }
    }

    scheduleStreamShutdown()
  }

  fun play() {
    if (!isInitialized) return

    cancelDelayedStop()

    if (!isStreamRunning) {
      val success = nativeStart(engineHandle)
      if (success) {
        isStreamRunning = true
      } else {
        Log.e(TAG, "Failed to start Oboe audio stream")
        return
      }
    }

    if (!isPlaying) {
      isPlaying = true
      requestAudioFocus()
    }
  }

  fun stop() {
    if (!isInitialized) return
    cancelDelayedStop()

    val success = nativeStop(engineHandle)
    if (success) {
      isStreamRunning = false
      isPlaying = false

      if (!ignoreFocus) {
        audioManager!!.abandonAudioFocusRequest(audioFocusRequest)
      }
    } else {
      Log.e(TAG, "Failed to stop Oboe engine")
    }
  }

  fun scheduleDelayedStop() {
    if (!isPlaying) return
    isPlaying = false

    if (!ignoreFocus) {
      audioManager!!.abandonAudioFocusRequest(audioFocusRequest)
    }

    listener.onAudioStop()
    scheduleStreamShutdown()
  }

  override fun onAudioFocusChange(focusChange: Int) {
    if (!isInitialized) return

    when (focusChange) {
      AudioManager.AUDIOFOCUS_GAIN -> {
        nativeSetDuckingVolume(engineHandle, 1.0f)
      }

      AudioManager.AUDIOFOCUS_LOSS -> {
        stop()
      }

      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        nativeSetDuckingVolume(engineHandle, 0.25f)
      }
    }
  }

  fun setSound(sound: String) {
    if (!isInitialized) return

    val config = when (sound) {
      Constants.Sound.WOOD -> SoundConfig(
        normal = R.raw.wood, strong = R.raw.wood, sub = R.raw.wood
      )

      Constants.Sound.MECHANICAL -> SoundConfig(
        normal = R.raw.mechanical_tick,
        strong = R.raw.mechanical_ding,
        sub = R.raw.mechanical_knock,
        pitchStrong = Pitch.NORMAL,
        pitchSub = Pitch.NORMAL
      )

      Constants.Sound.BEATBOXING_1 -> SoundConfig(
        normal = R.raw.beatbox_snare1,
        strong = R.raw.beatbox_kick1,
        sub = R.raw.beatbox_hihat1,
        pitchStrong = Pitch.NORMAL,
        pitchSub = Pitch.NORMAL
      )

      Constants.Sound.BEATBOXING_2 -> SoundConfig(
        normal = R.raw.beatbox_snare2,
        strong = R.raw.beatbox_kick2,
        sub = R.raw.beatbox_hihat2,
        pitchStrong = Pitch.NORMAL,
        pitchSub = Pitch.NORMAL
      )

      Constants.Sound.HANDS -> SoundConfig(
        normal = R.raw.hands_hit,
        strong = R.raw.hands_clap,
        sub = R.raw.hands_snap,
        pitchStrong = Pitch.NORMAL,
        pitchSub = Pitch.NORMAL
      )

      Constants.Sound.FOLDING -> SoundConfig(
        normal = R.raw.folding_knock,
        strong = R.raw.folding_fold,
        sub = R.raw.folding_tap,
        pitchStrong = Pitch.NORMAL,
        pitchSub = Pitch.NORMAL
      )

      else -> SoundConfig(
        normal = R.raw.sine, strong = R.raw.sine, sub = R.raw.sine
      )
    }

    nativeSetTickData(
      engineHandle,
      NATIVE_TICK_TYPE_NORMAL,
      loadAudio(config.normal, config.pitchNormal)
    )
    nativeSetTickData(
      engineHandle,
      NATIVE_TICK_TYPE_STRONG,
      loadAudio(config.strong, config.pitchStrong)
    )
    nativeSetTickData(
      engineHandle,
      NATIVE_TICK_TYPE_SUB,
      loadAudio(config.sub, config.pitchSub)
    )
  }

  fun playTick(tick: Tick) {
    if (!isPlaying || !isInitialized || isMuted || tick.isMuted) return

    val nativeTickType = when (tick.type) {
      Constants.TickType.STRONG -> NATIVE_TICK_TYPE_STRONG
      Constants.TickType.SUB -> NATIVE_TICK_TYPE_SUB
      Constants.TickType.MUTED, Constants.TickType.BEAT_SUB_MUTED -> return // Silence
      else -> NATIVE_TICK_TYPE_NORMAL
    }
    nativePlayTick(engineHandle, nativeTickType)
  }

  private fun loadAudio(@RawRes resId: Int, pitch: Pitch): FloatArray {
    return try {
      context.resources.openRawResource(resId).use { stream ->
        adjustPitch(AudioUtil.readDataFromWavFloat(stream), pitch)
      }
    } catch (e: IOException) {
      throw RuntimeException(e)
    }
  }

  private fun adjustPitch(originalData: FloatArray, pitch: Pitch): FloatArray {
    return when (pitch) {
      Pitch.HIGH -> {
        FloatArray(originalData.size / 2) { i -> originalData[i * 2] }
      }

      Pitch.LOW -> {
        FloatArray(originalData.size * 2).apply {
          for (i in originalData.indices) {
            val j = i * 2
            this[j] = originalData[i]
            this[j + 1] = originalData[i]
          }
        }
      }

      Pitch.NORMAL -> originalData
    }
  }

  private fun scheduleStreamShutdown() {
    if (!isStreamRunning) return

    try {
      delayedStopTask = executor.schedule({
        if (!isPlaying && isStreamRunning) {
          stop()
        }
      }, STREAM_DELAY_SECONDS, TimeUnit.SECONDS)
    } catch (e: Exception) {
      Log.e(TAG, "scheduleStreamShutdown: failed to schedule stream stop", e)
    }
  }

  private fun cancelDelayedStop() {
    if (delayedStopTask != null && !delayedStopTask!!.isDone) {
      delayedStopTask!!.cancel(false)
    }
  }

  private fun requestAudioFocus() {
    if (ignoreFocus || audioManager == null) return

    audioManager.requestAudioFocus(audioFocusRequest)
  }

  // --- Native Methods ---
  private external fun nativeCreate(): Long
  private external fun nativeDestroy(handle: Long)
  private external fun nativeInit(handle: Long): Boolean
  private external fun nativeStart(handle: Long): Boolean
  private external fun nativeStop(handle: Long): Boolean
  private external fun nativeSetTickData(handle: Long, tickType: Int, data: FloatArray)
  private external fun nativePlayTick(handle: Long, tickType: Int)
  private external fun nativeSetMasterVolume(handle: Long, volume: Float)
  private external fun nativeSetDuckingVolume(handle: Long, volume: Float)
  private external fun nativeSetMuted(handle: Long, muted: Boolean)

  // --- Helper Types ---

  private enum class Pitch {
    NORMAL, HIGH, LOW
  }

  private data class SoundConfig(
    @get:RawRes val normal: Int,
    @get:RawRes val strong: Int,
    @get:RawRes val sub: Int,
    val pitchNormal: Pitch = Pitch.NORMAL,
    val pitchStrong: Pitch = Pitch.HIGH,
    val pitchSub: Pitch = Pitch.LOW
  )

  fun interface AudioListener {

    fun onAudioStop()
  }

  companion object Companion {

    private const val TAG = "AudioEngine"

    private const val NATIVE_TICK_TYPE_STRONG: Int = 1
    private const val NATIVE_TICK_TYPE_NORMAL: Int = 2
    private const val NATIVE_TICK_TYPE_SUB: Int = 3
    private const val STREAM_DELAY_SECONDS: Long = 60

    init {
      System.loadLibrary("oboe-audio-engine")
    }
  }
}