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
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.util.MetronomeUtil
import xyz.zedler.patrick.tack.util.TempoTapUtil

class MainViewModel(var metronomeUtil: MetronomeUtil? = null) : ViewModel() {

  val mutableIsPlaying = MutableLiveData(metronomeUtil?.isPlaying ?: false)
  private val tempoTapUtil = TempoTapUtil()
  private val _tempo = MutableLiveData(metronomeUtil?.tempo ?: DEF.TEMPO)
  private val _beatModeVibrate = MutableLiveData(
    metronomeUtil?.isBeatModeVibrate ?: DEF.BEAT_MODE_VIBRATE
  )
  private val _alwaysVibrate = MutableLiveData(
    metronomeUtil?.isAlwaysVibrate ?: DEF.ALWAYS_VIBRATE
  )
  private val _gain = MutableLiveData(metronomeUtil?.gain ?: DEF.GAIN)
  private val _sound = MutableLiveData(metronomeUtil?.sound ?: DEF.SOUND)
  private val _ignoreFocus = MutableLiveData(
    metronomeUtil?.ignoreFocus ?: DEF.IGNORE_FOCUS
  )
  private val _latency = MutableLiveData(metronomeUtil?.latency ?: DEF.LATENCY)
  private val _keepAwake = MutableLiveData(metronomeUtil?.keepAwake ?: DEF.KEEP_AWAKE)
  private val _showPermissionDialog = MutableLiveData(false)

  val tempo: LiveData<Int> = _tempo
  val isPlaying: LiveData<Boolean> = mutableIsPlaying
  val beatModeVibrate: LiveData<Boolean> = _beatModeVibrate
  val alwaysVibrate: LiveData<Boolean> = _alwaysVibrate
  val gain: LiveData<Int> = _gain
  val sound: LiveData<String> = _sound
  val ignoreFocus: LiveData<Boolean> = _ignoreFocus
  val latency: LiveData<Long> = _latency
  val keepAwake: LiveData<Boolean> = _keepAwake
  val showPermissionDialog: LiveData<Boolean> = _showPermissionDialog

  fun changeTempo(tempo: Int) {
    metronomeUtil?.tempo = tempo
    _tempo.value = tempo
  }

  fun onTempoTap(): Int {
    if (tempoTapUtil.tap()) {
      val tempo = tempoTapUtil.tempo
      changeTempo(tempo)
      return tempo
    }
    return tempo.value ?: DEF.TEMPO
  }

  fun onTempoChange(tempo: Int) {
    metronomeUtil?.tempo = tempo
  }

  fun onPlayingChange(playing: Boolean) {
    metronomeUtil?.isPlaying = playing
    mutableIsPlaying.value = playing
  }

  fun togglePlaying() {
    val playing = metronomeUtil?.isPlaying ?: true
    metronomeUtil?.isPlaying = !playing
    mutableIsPlaying.value = !playing
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

  fun changeKeepAwake(awake: Boolean) {
    metronomeUtil?.keepAwake = awake
    _keepAwake.value = awake;
  }

  fun changeShowPermissionDialog(show: Boolean) {
    _showPermissionDialog.value = show
  }
}