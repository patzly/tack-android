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

package xyz.zedler.patrick.tack.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDestination
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.TEMPO_MAX
import xyz.zedler.patrick.tack.Constants.TEMPO_MIN
import xyz.zedler.patrick.tack.Constants.TICK_TYPE
import xyz.zedler.patrick.tack.presentation.navigation.Screen
import xyz.zedler.patrick.tack.util.MetronomeUtil
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick
import xyz.zedler.patrick.tack.util.TempoTapUtil

class MainViewModel(
  var metronomeUtil: MetronomeUtil? = null,
  private val keepAwakeListener: KeepAwakeListener? = null
) : ViewModel() {
  val mutableIsPlaying = MutableLiveData(metronomeUtil?.isPlaying ?: false)
  private val tempoTapUtil = TempoTapUtil()
  private var currentRoute = Screen.Main.route
  private val _tempo = MutableLiveData(metronomeUtil?.tempo ?: DEF.TEMPO)
  private val _beats = MutableLiveData(
    metronomeUtil?.beats ?: DEF.BEATS.split(",")
  )
  private val _beatTriggers: MutableList<MutableLiveData<Boolean>> =
    MutableList((metronomeUtil?.beats ?: DEF.BEATS.split(",")).size) {
      MutableLiveData(false)
    }
  private val _subdivisions = MutableLiveData(
    metronomeUtil?.subdivisions ?: DEF.SUBDIVISIONS.split(",")
  )
  private val _subdivisionTriggers: MutableList<MutableLiveData<Boolean>> =
    MutableList((metronomeUtil?.subdivisions ?: DEF.SUBDIVISIONS.split(",")).size) {
      MutableLiveData(false)
    }
  private val _beatModeVibrate = MutableLiveData(
    metronomeUtil?.isBeatModeVibrate ?: DEF.BEAT_MODE_VIBRATE
  )
  private val _alwaysVibrate = MutableLiveData(
    metronomeUtil?.isAlwaysVibrate ?: DEF.ALWAYS_VIBRATE
  )
  private val _strongVibration = MutableLiveData(
    metronomeUtil?.isStrongVibration ?: DEF.STRONG_VIBRATION
  )
  private val _gain = MutableLiveData(metronomeUtil?.gain ?: DEF.GAIN)
  private val _sound = MutableLiveData(metronomeUtil?.sound ?: DEF.SOUND)
  private val _ignoreFocus = MutableLiveData(
    metronomeUtil?.ignoreFocus ?: DEF.IGNORE_FOCUS
  )
  private val _latency = MutableLiveData(metronomeUtil?.latency ?: DEF.LATENCY)
  private val _keepAwake = MutableLiveData(metronomeUtil?.keepAwake ?: DEF.KEEP_AWAKE)
  private val _wristGestures = MutableLiveData(
    metronomeUtil?.wristGestures ?: DEF.WRIST_GESTURES
  )
  private val _flashScreen = MutableLiveData(metronomeUtil?.flashScreen ?: DEF.FLASH_SCREEN)
  private val _flashTrigger = MutableLiveData(false)
  private val _flashStrongTrigger = MutableLiveData(false)
  private val _showPermissionDialog = MutableLiveData(false)

  val tempo: LiveData<Int> = _tempo
  val isPlaying: LiveData<Boolean> = mutableIsPlaying
  val beats: LiveData<List<String>> = _beats
  val beatTriggers: List<LiveData<Boolean>> = _beatTriggers
  val subdivisions: LiveData<List<String>> = _subdivisions
  val subdivisionTriggers: List<LiveData<Boolean>> = _subdivisionTriggers
  val beatModeVibrate: LiveData<Boolean> = _beatModeVibrate
  val alwaysVibrate: LiveData<Boolean> = _alwaysVibrate
  val strongVibration: LiveData<Boolean> = _strongVibration
  val gain: LiveData<Int> = _gain
  val sound: LiveData<String> = _sound
  val ignoreFocus: LiveData<Boolean> = _ignoreFocus
  val latency: LiveData<Long> = _latency
  val keepAwake: LiveData<Boolean> = _keepAwake
  val wristGestures: LiveData<Boolean> = _wristGestures
  val flashScreen: LiveData<Boolean> = _flashScreen
  val flashTrigger: LiveData<Boolean> = _flashTrigger
  val flashStrongTrigger: LiveData<Boolean> = _flashStrongTrigger
  val showPermissionDialog: LiveData<Boolean> = _showPermissionDialog
  var tempoChangedByPicker: Boolean = false
  var animateTempoChange: Boolean = true

  fun changeTempo(tempo: Int, picker: Boolean = false, animate: Boolean = true) {
    if (tempo in TEMPO_MIN..TEMPO_MAX) {
      metronomeUtil?.tempo = tempo
      tempoChangedByPicker = picker
      animateTempoChange = animate
      _tempo.value = tempo
    }
  }

  fun tempoTap(): Int {
    if (tempoTapUtil.tap()) {
      val tempo = tempoTapUtil.tempo
      changeTempo(tempo)
      return tempo
    }
    return tempo.value ?: DEF.TEMPO
  }

  fun onPreTick(tick: Tick) {
    if (tick.subdivision == 1) {
      onBeat(tick)
    }
    onSubdivision(tick.subdivision - 1)
  }

  fun onTick(tick: Tick) {
    if (tick.subdivision == 1) {
      if (metronomeUtil?.flashScreen == true && tick.type == TICK_TYPE.STRONG) {
        _flashStrongTrigger.value = true
      } else if (metronomeUtil?.flashScreen == true && tick.type == TICK_TYPE.NORMAL) {
        _flashTrigger.value = true
      }
    }
  }

  private fun onBeat(tick: Tick) {
    val index = tick.beat - 1
    if (index < _beatTriggers.size) {
      val current = _beatTriggers[index].value ?: false
      _beatTriggers[index].value = !current
    }
  }

  private fun onSubdivision(index: Int) {
    if (index < _subdivisionTriggers.size) {
      val current = _subdivisionTriggers[index].value ?: false
      _subdivisionTriggers[index].value = !current
    }
  }

  fun onFlashScreenEnd() {
    _flashTrigger.value = false
    _flashStrongTrigger.value = false
  }

  fun togglePlaying(): Boolean {
    val playing = metronomeUtil?.isPlaying ?: true
    if (metronomeUtil?.setPlaying(!playing) == true) {
      mutableIsPlaying.value = !playing
      keepAwakeListener?.onKeepAwakeChanged(keepAwake())
      return true
    }
    return false
  }

  fun toggleBeatModeVibrate() {
    val beatModeVibrate = metronomeUtil?.isBeatModeVibrate ?: DEF.BEAT_MODE_VIBRATE
    metronomeUtil?.isBeatModeVibrate = !beatModeVibrate
    _beatModeVibrate.value = !beatModeVibrate
  }

  fun changeAlwaysVibrate(alwaysVibrate: Boolean) {
    metronomeUtil?.isAlwaysVibrate = alwaysVibrate
    _alwaysVibrate.value = alwaysVibrate
  }

  fun changeStrongVibration(strong: Boolean) {
    metronomeUtil?.isStrongVibration = strong
    _strongVibration.value = strong
  }

  fun toggleBookmark() {
    changeTempo(metronomeUtil?.toggleBookmark() ?: DEF.TEMPO)
  }

  fun changeGain(gain: Int) {
    metronomeUtil?.gain = gain
    _gain.value = gain
  }

  fun changeSound(sound: String) {
    metronomeUtil?.sound = sound
    _sound.value = sound
  }

  fun changeIgnoreFocus(ignore: Boolean) {
    metronomeUtil?.ignoreFocus = ignore
    _ignoreFocus.value = ignore
  }

  fun changeLatency(latency: Long) {
    metronomeUtil?.latency = latency
    _latency.value = latency
  }

  fun changeFlashScreen(flash: Boolean) {
    metronomeUtil?.flashScreen = flash
    _flashScreen.value = flash
  }

  fun changeKeepAwake(awake: Boolean) {
    metronomeUtil?.keepAwake = awake
    _keepAwake.value = awake
    keepAwakeListener?.onKeepAwakeChanged(keepAwake())
  }

  fun onDestinationChanged(destination: NavDestination) {
    currentRoute = destination.route.toString()
    keepAwakeListener?.onKeepAwakeChanged(keepAwake())
  }

  private fun keepAwake(): Boolean {
    return _keepAwake.value == true
        && mutableIsPlaying.value == true
        && currentRoute == Screen.Main.route
  }

  fun changeWristGestures(gestures: Boolean) {
    metronomeUtil?.wristGestures = gestures
    _wristGestures.value = gestures
  }

  fun changeShowPermissionDialog(show: Boolean) {
    _showPermissionDialog.value = show
  }

  fun addBeat() {
    val success = metronomeUtil?.addBeat()
    if (success == true) {
      val updated = metronomeUtil?.beats?.toList()
      // toList required for a new list instead of the old mutated, would not be handled as changed
      updateBeatTriggers(updated?.size ?: DEF.BEATS.split(",").size)
      _beats.value = updated
    }
  }

  fun removeBeat() {
    val success = metronomeUtil?.removeBeat()
    if (success == true) {
      val updated = metronomeUtil?.beats?.toList()
      updateBeatTriggers(updated?.size ?: DEF.BEATS.split(",").size)
      _beats.value = updated
    }
  }

  fun changeBeat(beat: Int, tickType: String) {
    metronomeUtil?.setBeat(beat, tickType)
    val updated = metronomeUtil?.beats?.toList()
    updateBeatTriggers(updated?.size ?: DEF.BEATS.split(",").size)
    _beats.value = updated
  }

  private fun updateBeatTriggers(count: Int) {
    while (_beatTriggers.size < count) {
      _beatTriggers.add(MutableLiveData(false))
    }
    while (_beatTriggers.size > count) {
      _beatTriggers.removeLast()
    }
  }

  fun addSubdivision() {
    val success = metronomeUtil?.addSubdivision()
    if (success == true) {
      val updated = metronomeUtil?.subdivisions?.toList()
      updateSubdivisionTriggers(updated?.size ?: DEF.SUBDIVISIONS.split(",").size)
      _subdivisions.value = updated
    }
  }

  fun removeSubdivision() {
    val success = metronomeUtil?.removeSubdivision()
    if (success == true) {
      val updated = metronomeUtil?.subdivisions?.toList()
      updateSubdivisionTriggers(updated?.size ?: DEF.SUBDIVISIONS.split(",").size)
      _subdivisions.value = updated
    }
  }

  fun changeSubdivision(subdivision: Int, tickType: String) {
    metronomeUtil?.setSubdivision(subdivision, tickType)
    val updated = metronomeUtil?.subdivisions?.toList()
    updateSubdivisionTriggers(updated?.size ?: DEF.SUBDIVISIONS.split(",").size)
    _subdivisions.value = updated
  }

  private fun updateSubdivisionTriggers(count: Int) {
    while (_subdivisionTriggers.size < count) {
      _subdivisionTriggers.add(MutableLiveData(false))
    }
    while (_subdivisionTriggers.size > count) {
      _subdivisionTriggers.removeLast()
    }
  }

  fun setSwing(swing: Int) {
    when (swing) {
      3 -> metronomeUtil?.setSwing3()
      5 -> metronomeUtil?.setSwing5()
      7 -> metronomeUtil?.setSwing7()
    }
    val updated = metronomeUtil?.subdivisions?.toList()
    updateSubdivisionTriggers(updated?.size ?: DEF.SUBDIVISIONS.split(",").size)
    _subdivisions.value = updated
  }

  interface KeepAwakeListener {
    fun onKeepAwakeChanged(keepAwake: Boolean)
  }
}