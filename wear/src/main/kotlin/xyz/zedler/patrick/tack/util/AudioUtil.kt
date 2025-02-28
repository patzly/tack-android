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

package xyz.zedler.patrick.tack.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RawRes
import xyz.zedler.patrick.tack.Constants.Sound
import xyz.zedler.patrick.tack.Constants.TickType
import xyz.zedler.patrick.tack.R
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.math.max

class AudioUtil(
  private val context: Context,
  private val onAudioStop: () -> Unit
) : AudioManager.OnAudioFocusChangeListener {

  companion object {
    private const val TAG = "AudioUtil"
    private const val SAMPLE_RATE_IN_HZ = 48000
    private const val SILENCE_CHUNK_SIZE = 8000
    private const val DATA_CHUNK_SIZE = 8
  }

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val silence = FloatArray(SILENCE_CHUNK_SIZE)
  private val dataMarker = "data".toByteArray(StandardCharsets.US_ASCII)

  private var audioThread: HandlerThread? = null
  private var audioHandler: Handler? = null
  private var audioTrack: AudioTrack? = null
  private var loudnessEnhancer: LoudnessEnhancer? = null
  private var tickNormal: FloatArray? = null
  private var tickStrong: FloatArray? = null
  private var tickSub: FloatArray? = null
  private var playing = false
  var gain = 0
    set(value) {
      field = value
      loudnessEnhancer?.let {
        try {
          it.setTargetGain(field * 100)
          it.setEnabled(field > 0)
        } catch (e: RuntimeException) {
          Log.e(TAG, "Failed to set target gain: ", e)
        }
      }
    }
  var muted = false
  var ignoreFocus = false

  fun destroy() {
    removeHandlerCallbacks()
    audioThread?.quitSafely()
  }

  fun resetHandlersIfRequired() {
    if (audioThread == null || audioThread?.isAlive == false) {
      audioThread = HandlerThread("audio").apply { start() }
      removeHandlerCallbacks()
      audioHandler = Handler(audioThread!!.looper)
    }
  }

  fun removeHandlerCallbacks() {
    audioHandler?.removeCallbacksAndMessages(null)
  }

  fun play() {
    resetHandlersIfRequired()

    playing = true
    audioTrack = getTrack().apply {
      try {
        loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
          setTargetGain(gain * 100)
          setEnabled(gain > 0)
        }
      } catch (e: RuntimeException) {
        Log.e(TAG, "Failed to initialize LoudnessEnhancer: ", e)
      }
      if (state == AudioTrack.STATE_INITIALIZED) {
        play()
      } else {
        Log.e(TAG, "Failed to start AudioTrack")
      }
    }

    if (!ignoreFocus) {
      audioManager.requestAudioFocus(getAudioFocusRequest())
    }
  }

  fun stop() {
    playing = false
    removeHandlerCallbacks()

    audioTrack?.apply {
      if (state == AudioTrack.STATE_INITIALIZED) {
        stop()
      }
      flush()
      release()
    }
    loudnessEnhancer?.apply {
      try {
        release()
      } catch (e: RuntimeException) {
        Log.e(TAG, "stop: failed to release LoudnessEnhancer resources: ", e)
      }
    }
    if (!ignoreFocus) {
      audioManager.abandonAudioFocusRequest(getAudioFocusRequest())
    }
    onAudioStop()
  }

  override fun onAudioFocusChange(focusChange: Int) {
    when (focusChange) {
      AudioManager.AUDIOFOCUS_GAIN -> {
        audioTrack?.setVolume(1f)
      }
      AudioManager.AUDIOFOCUS_LOSS -> stop()
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        audioTrack?.setVolume(0.25f)
      }
    }
  }

  fun setSound(sound: String) {
    var pitchStrong = Pitch.HIGH
    var pitchSub = Pitch.LOW
    val (resIdNormal, resIdStrong, resIdSub) = when (sound) {
      Sound.WOOD -> Triple(R.raw.wood, R.raw.wood, R.raw.mechanical_knock).also {
        pitchSub = Pitch.NORMAL
      }
      Sound.MECHANICAL -> Triple(
        R.raw.mechanical_tick, R.raw.mechanical_ding, R.raw.mechanical_knock
      ).also {
        pitchStrong = Pitch.NORMAL
        pitchSub = Pitch.NORMAL
      }
      Sound.BEATBOXING_1 -> Triple(
        R.raw.beatbox_snare1, R.raw.beatbox_kick1, R.raw.beatbox_hihat1
      ).also {
        pitchStrong = Pitch.NORMAL
        pitchSub = Pitch.NORMAL
      }
      Sound.BEATBOXING_2 -> Triple(
        R.raw.beatbox_snare2, R.raw.beatbox_kick2, R.raw.beatbox_hihat2
      ).also {
        pitchStrong = Pitch.NORMAL
        pitchSub = Pitch.NORMAL
      }
      Sound.HANDS -> Triple(R.raw.hands_hit, R.raw.hands_clap, R.raw.hands_snap).also {
        pitchStrong = Pitch.NORMAL
        pitchSub = Pitch.NORMAL
      }
      Sound.FOLDING -> Triple(R.raw.folding_knock, R.raw.folding_fold, R.raw.folding_tap).also {
        pitchStrong = Pitch.NORMAL
        pitchSub = Pitch.NORMAL
      }
      else -> Triple(R.raw.sine, R.raw.sine, R.raw.sine)
    }
    tickNormal = loadAudio(resIdNormal)
    tickStrong = loadAudio(resIdStrong, pitchStrong)
    tickSub = loadAudio(resIdSub, pitchSub)
  }

  fun writeTickPeriod(tick: MetronomeUtil.Tick, tempo: Int, subdivisionCount: Int) {
    val periodSize = 60 * SAMPLE_RATE_IN_HZ / tempo / subdivisionCount
    val expectedTime = SystemClock.elapsedRealtime()
    audioHandler?.post {
      var periodSizeTrimmed = periodSize
      if (tick.subdivision == 1) {
        val currentTime = SystemClock.elapsedRealtime()
        val delay = currentTime - expectedTime
        if (delay > 1) {
          val trimSize = delay.coerceAtLeast(10) * (SAMPLE_RATE_IN_HZ / 1000)
          periodSizeTrimmed = max(0, periodSize - trimSize).toInt()
        }
      }
      val tickSound = if (muted) silence else getTickSound(tick.type) ?: return@post
      val sizeWritten = writeNextAudioData(tickSound, periodSizeTrimmed, 0)
      writeSilenceUntilPeriodFinished(sizeWritten, periodSizeTrimmed)
    }
  }

  private fun writeSilenceUntilPeriodFinished(previousSizeWritten: Int, periodSize: Int) {
    var sizeWritten = previousSizeWritten
    while (sizeWritten < periodSize) {
      sizeWritten += writeNextAudioData(silence, periodSize, sizeWritten)
    }
  }

  private fun writeNextAudioData(data: FloatArray, periodSize: Int, sizeWritten: Int): Int {
    val size = minOf(data.size, periodSize - sizeWritten)
    if (playing) {
      writeAudio(data, size)
    }
    return size
  }

  private fun getTickSound(tickType: String): FloatArray? {
    return when (tickType) {
      TickType.STRONG -> tickStrong
      TickType.SUB -> tickSub
      TickType.MUTED -> silence
      else -> tickNormal
    }
  }

  private fun loadAudio(@RawRes resId: Int, pitch: Pitch = Pitch.NORMAL): FloatArray {
    return context.resources.openRawResource(resId).use {
      adjustPitch(readDataFromWavFloat(it), pitch)
    }
  }

  private fun adjustPitch(originalData: FloatArray, pitch: Pitch): FloatArray {
    return when (pitch) {
      Pitch.HIGH -> originalData.filterIndexed { index, _ -> index % 2 == 0 }.toFloatArray()
      Pitch.LOW -> originalData.flatMap { listOf(it, it) }.toFloatArray()
      else -> originalData
    }
  }

  private fun writeAudio(data: FloatArray, size: Int) {
    try {
      audioTrack?.write(data, 0, size, AudioTrack.WRITE_BLOCKING)?.takeIf { it < 0 }?.let {
        stop()
        throw RuntimeException("Error code: $it")
      }
    } catch (e: RuntimeException) {
      Log.e(TAG, "writeAudio: failed to play audion data", e)
    }
  }

  private fun readDataFromWavFloat(input: InputStream): FloatArray {
    val content = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      input.readBytes()
    } else {
      readInputStreamToBytes(input)
    }
    val indexOfDataMarker = getIndexOfDataMarker(content)
    val startOfSound = indexOfDataMarker + DATA_CHUNK_SIZE
    return ByteBuffer.wrap(content, startOfSound, content.size - startOfSound).apply {
      order(ByteOrder.LITTLE_ENDIAN)
    }.asFloatBuffer().let {
      FloatArray(it.remaining()).also { data -> it.get(data) }
    }
  }

  private fun readInputStreamToBytes(input: InputStream): ByteArray {
    return ByteArrayOutputStream().use { buffer ->
      val data = ByteArray(4096)
      var read: Int
      while (input.read(data).also { read = it } != -1) {
        buffer.write(data, 0, read)
      }
      buffer.toByteArray()
    }
  }

  private fun getIndexOfDataMarker(array: ByteArray): Int {
    if (dataMarker.isEmpty()) {
      return 0
    }
    outer@ for (i in 0..array.size - dataMarker.size) {
      for (j in dataMarker.indices) {
        if (array[i + j] != dataMarker[j]) {
          continue@outer
        }
      }
      return i
    }
    return -1
  }

  private fun getTrack(): AudioTrack {
    val audioFormat = AudioFormat.Builder()
      .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
      .setSampleRate(SAMPLE_RATE_IN_HZ)
      .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
      .build()
    return AudioTrack(
      getAttributes(),
      audioFormat,
      AudioTrack.getMinBufferSize(
        audioFormat.sampleRate, audioFormat.channelMask, audioFormat.encoding
      ),
      AudioTrack.MODE_STREAM,
      AudioManager.AUDIO_SESSION_ID_GENERATE
    )
  }

  private fun getAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_MEDIA)
      .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
      .build()
  }

  private fun getAudioFocusRequest(): AudioFocusRequest {
    return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
      .setAudioAttributes(getAttributes())
      .setWillPauseWhenDucked(true)
      .setOnAudioFocusChangeListener(this)
      .build()
  }

  private enum class Pitch {
    NORMAL, HIGH, LOW
  }
}