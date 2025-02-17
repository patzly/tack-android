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
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.TickType
import xyz.zedler.patrick.tack.presentation.state.MainState

class MetronomeUtil(
  private val context: Context,
  private val fromService: Boolean
) {

  companion object {
    private const val TAG = "MetronomeUtil"
  }

  private val audioUtil = AudioUtil(context, ::stop)
  private val hapticUtil = HapticUtil(context)

  private var tickThread: HandlerThread? = null
  private var callbackThread: HandlerThread? = null
  private var tickHandler: Handler? = null
  private var latencyHandler: Handler? = null
  private var flashHandler: Handler? = null

  private var tickIndex: Long = 0
  private var latency: Long = 0
  private var beatModeVibrate: Boolean = false
  private var alwaysVibrate: Boolean = false
  private var flashScreen: Boolean = false

  val listeners: MutableSet<MetronomeListener> = mutableSetOf()
  var beats: MutableList<String> = mutableListOf()
    set(value) {
      field = value.toMutableList()
    }
  var subdivisions: MutableList<String> = mutableListOf()
    set(value) {
      field = value.toMutableList()
    }
  var tempo: Int = 0
  var isPlaying: Boolean = false
    private set

  init {
    resetHandlersIfRequired()
  }

  fun updateFromState(state: MainState) {
    tempo = state.tempo
    beats = state.beats.toMutableList()
    subdivisions = state.subdivisions.toMutableList()
    latency = state.latency
    flashScreen = state.flashScreen
    setSound(state.sound)
    setIgnoreFocus(state.ignoreFocus)
    setGain(state.gain)
    setBeatModeVibrate(state.beatModeVibrate)
    setAlwaysVibrate(state.alwaysVibrate)
    setStrongVibration(state.strongVibration)
  }

  private fun resetHandlersIfRequired() {
    if (!fromService) return

    if (tickThread == null || tickThread?.isAlive == false) {
      tickThread = HandlerThread("metronome_ticks").apply { start() }
      removeHandlerCallbacks()
      tickHandler = Handler(tickThread!!.looper)
    }
    if (callbackThread == null || callbackThread?.isAlive == false) {
      callbackThread = HandlerThread("metronome_callback").apply { start() }
      removeHandlerCallbacks()
      latencyHandler = Handler(callbackThread!!.looper)
    }
    flashHandler = Handler(Looper.getMainLooper())
  }

  private fun removeHandlerCallbacks() {
    tickHandler?.removeCallbacksAndMessages(null)
    latencyHandler?.removeCallbacksAndMessages(null)
  }

  fun destroy() {
    listeners.clear()
    if (fromService) {
      removeHandlerCallbacks()
      tickThread?.quitSafely()
      callbackThread?.quit()
      audioUtil.destroy()
    }
  }

  fun addListener(listener: MetronomeListener) {
    listeners.add(listener)
  }

  fun addListeners(newListeners: Set<MetronomeListener>) {
    listeners.addAll(newListeners)
  }

  fun start() {
    if (isPlaying) return
    if (!fromService) return

    resetHandlersIfRequired()
    isPlaying = true
    audioUtil.play()
    tickIndex = 0
    tickHandler?.post(object : Runnable {
      override fun run() {
        if (isPlaying) {
          tickHandler?.postDelayed(this, getInterval() / getSubdivisionsCount())
          val tick = performTick()
          audioUtil.writeTickPeriod(tick, tempo, getSubdivisionsCount())
          tickIndex++
        }
      }
    })

    listeners.forEach { it.onMetronomeStart() }
    Log.i(TAG, "start: started metronome handler")
  }

  fun stop() {
    if (!isPlaying) return
    isPlaying = false
    audioUtil.stop()

    if (fromService) {
      removeHandlerCallbacks()
    }

    listeners.forEach { it.onMetronomeStop() }
    Log.i(TAG, "stop: stopped metronome handler")
  }

  fun setPlayback(playing: Boolean) {
    if (playing) {
      if (NotificationUtil.hasPermission(context)) {
        start()
      } else {
        listeners.forEach { it.onPermissionMissing() }
      }
    } else {
      stop()
    }
  }

  fun changeBeat(beat: Int, tickType: String) {
    beats[beat] = tickType
  }

  fun addBeat() {
    if (beats.size < Constants.BEATS_MAX) {
      beats.add(TickType.NORMAL)
    }
  }

  fun removeBeat() {
    if (beats.size > 1) {
      beats.removeAt(beats.size - 1)
    }
  }

  fun getSubdivisionsCount(): Int = subdivisions.size

  fun changeSubdivision(subdivision: Int, tickType: String) {
    subdivisions[subdivision] = tickType
  }

  fun addSubdivision() {
    if (subdivisions.size < Constants.SUBS_MAX) {
      subdivisions.add(TickType.SUB)
    }
  }

  fun removeSubdivision() {
    if (subdivisions.size > 1) {
      subdivisions.removeAt(subdivisions.size - 1)
    }
  }

  fun setSwing3() {
    subdivisions = mutableListOf(TickType.MUTED, TickType.MUTED, TickType.NORMAL)
  }

  fun setSwing5() {
    subdivisions = mutableListOf(
      TickType.MUTED, TickType.MUTED, TickType.MUTED, TickType.NORMAL, TickType.MUTED
    )
  }

  fun setSwing7() {
    subdivisions = mutableListOf(
      TickType.MUTED, TickType.MUTED, TickType.MUTED, TickType.MUTED,
      TickType.NORMAL, TickType.MUTED, TickType.MUTED
    )
  }

  fun getInterval(): Long = (1000 * 60 / tempo).toLong()

  private fun setSound(sound: String) {
    audioUtil.setSound(sound)
  }

  private fun setBeatModeVibrate(vibrate: Boolean) {
    beatModeVibrate = vibrate && hapticUtil.hasVibrator()
    audioUtil.muted = beatModeVibrate
    hapticUtil.enabled = beatModeVibrate || alwaysVibrate
  }

  private fun setAlwaysVibrate(always: Boolean) {
    alwaysVibrate = always
    hapticUtil.enabled = always || beatModeVibrate
  }

  private fun setStrongVibration(strong: Boolean) {
    hapticUtil.strong = strong
  }

  private fun setIgnoreFocus(ignore: Boolean) {
    audioUtil.ignoreFocus = ignore
  }

  private fun setGain(gain: Int) {
    audioUtil.gain = gain
  }

  private fun performTick(): Tick {
    val tick = Tick(tickIndex, getCurrentBeat(), getCurrentSubdivision(), getCurrentTickType())

    latencyHandler?.postDelayed({
      listeners.forEach { it.onMetronomePreTick(tick) }
    }, maxOf(0, latency - Constants.BEAT_ANIM_OFFSET))

    latencyHandler?.postDelayed({
      if (beatModeVibrate || alwaysVibrate) {
        when (tick.type) {
          TickType.STRONG -> hapticUtil.heavyClick()
          TickType.SUB -> hapticUtil.tick()
          TickType.MUTED -> {}
          else -> hapticUtil.click()
        }
      }
      listeners.forEach { it.onMetronomeTick(tick) }
    }, latency)

    if (flashScreen) {
      flashHandler?.postDelayed({
        listeners.forEach { it.onFlashScreenEnd() }
      }, latency + Constants.FLASH_SCREEN_DURATION)
    }
    return tick
  }

  private fun getCurrentBeat(): Int =
    ((tickIndex / getSubdivisionsCount()) % beats.size + 1).toInt()

  private fun getCurrentSubdivision(): Int = (tickIndex % getSubdivisionsCount() + 1).toInt()

  private fun getCurrentTickType(): String {
    val subdivisionsCount = getSubdivisionsCount()
    return if ((tickIndex % subdivisionsCount) == 0L) {
      beats[((tickIndex / subdivisionsCount) % beats.size).toInt()]
    } else {
      subdivisions[(tickIndex % subdivisionsCount).toInt()]
    }
  }

  interface MetronomeListener {
    fun onMetronomeStart()
    fun onMetronomeStop()
    fun onMetronomePreTick(tick: Tick)
    fun onMetronomeTick(tick: Tick)
    fun onFlashScreenEnd()
    fun onPermissionMissing()
  }

  open class MetronomeListenerAdapter : MetronomeListener {
    override fun onMetronomeStart() {}
    override fun onMetronomeStop() {}
    override fun onMetronomePreTick(tick: Tick) {}
    override fun onMetronomeTick(tick: Tick) {}
    override fun onFlashScreenEnd() {}
    override fun onPermissionMissing() {}
  }

  data class Tick(
    val index: Long,
    val beat: Int,
    val subdivision: Int,
    val type: String
  ) {
    override fun toString(): String {
      return "Tick{index=$index, beat=$beat, sub=$subdivision, type=$type}"
    }
  }
}