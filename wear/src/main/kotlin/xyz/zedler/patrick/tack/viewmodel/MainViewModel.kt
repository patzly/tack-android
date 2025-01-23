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

package xyz.zedler.patrick.tack.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.Def
import xyz.zedler.patrick.tack.Constants.Pref
import xyz.zedler.patrick.tack.Constants.TickType
import xyz.zedler.patrick.tack.presentation.navigation.Screen
import xyz.zedler.patrick.tack.presentation.state.Bookmark
import xyz.zedler.patrick.tack.presentation.state.MainState
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick
import xyz.zedler.patrick.tack.util.TempoTapUtil

class MainViewModel(
  private val sharedPrefs: SharedPreferences? = null,
  private val listener: StateListener? = null
) : ViewModel() {
  private val tempoTapUtil = TempoTapUtil()
  private val _state = MutableStateFlow(MainState())
  val state: StateFlow<MainState> = _state.asStateFlow()

  init {
    if (sharedPrefs != null) {
      val bookmarksString = sharedPrefs.getString(Pref.BOOKMARKS, Def.BOOKMARKS)!!
      val bookmarks = if (bookmarksString.isEmpty()) {
        listOf()
      } else {
        bookmarksString.split("|").map { item ->
          val parts = item.split("&")
          Bookmark(parts[0].toInt(), parts[1].split(","), parts[2].split(","))
        }.sorted()
      }
      val beats = sharedPrefs.getString(Pref.BEATS, Def.BEATS)!!.split(",")
      val subs = sharedPrefs.getString(Pref.SUBDIVISIONS, Def.SUBDIVISIONS)!!.split(",")
      _state.update { it.copy(
        tempo = sharedPrefs.getInt(Pref.TEMPO, Def.TEMPO),
        beats = beats,
        beatTriggers = beatTriggers(beats.size),
        subdivisions = subs,
        subdivisionTriggers = subdivisionTriggers(subs.size),
        bookmarks = bookmarks,
        beatModeVibrate = sharedPrefs
          .getBoolean(Pref.BEAT_MODE_VIBRATE, Def.BEAT_MODE_VIBRATE),
        alwaysVibrate = sharedPrefs
          .getBoolean(Pref.ALWAYS_VIBRATE, Def.ALWAYS_VIBRATE),
        strongVibration = sharedPrefs
          .getBoolean(Pref.STRONG_VIBRATION, Def.STRONG_VIBRATION),
        gain = sharedPrefs.getInt(Pref.GAIN, Def.GAIN),
        sound = sharedPrefs.getString(Pref.SOUND, Def.SOUND)!!,
        ignoreFocus = sharedPrefs.getBoolean(Pref.IGNORE_FOCUS, Def.IGNORE_FOCUS),
        latency = sharedPrefs.getLong(Pref.LATENCY, Def.LATENCY),
        keepAwake = sharedPrefs.getBoolean(Pref.KEEP_AWAKE, Def.KEEP_AWAKE),
        reduceAnim = sharedPrefs.getBoolean(Pref.REDUCE_ANIM, Def.REDUCE_ANIM),
        flashScreen = sharedPrefs.getBoolean(Pref.FLASH_SCREEN, Def.FLASH_SCREEN),
      ) }
      val subdivisions = sharedPrefs
        .getString(Pref.SUBDIVISIONS, Def.SUBDIVISIONS)!!.split(",")
      updateSubdivisions(subdivisions)
    }
  }

  fun updateCurrentRoute(route: String) {
    _state.update { it.copy(currentRoute = route) }
    listener?.onKeepAwakeChanged(keepAwake())
  }

  fun updatePlaying(playing: Boolean) {
    if (_state.value.isPlaying != playing) {

      _state.update { it.copy(
        isPlaying = playing,
        startedWithGain = if (playing) _state.value.gain > 0 else _state.value.startedWithGain
      ) }
    }
  }

  fun togglePlaying() {
    listener?.onPlayingToggleRequest()
  }

  fun updateTempo(tempo: Int, picker: Boolean = false, animate: Boolean = true) {
    if (tempo in Constants.TEMPO_MIN..Constants.TEMPO_MAX && _state.value.tempo != tempo) {
      _state.update { it.copy(
        tempo = tempo,
        tempoChangedByPicker = picker,
        animateTempoChange = animate
      ) }
      listener?.onMetronomeConfigChanged(_state.value)
      sharedPrefs?.edit()?.putInt(Pref.TEMPO, tempo)?.apply()
    }
  }

  fun tempoTap(): Int {
    if (tempoTapUtil.tap()) {
      val tempo = tempoTapUtil.tempo
      updateTempo(tempo)
      return tempo
    }
    return _state.value.tempo
  }

  fun updateBeats(beats: List<String>) {
    _state.update { it.copy(
      beats = beats,
      beatTriggers = beatTriggers(beats.size)
    ) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putString(Pref.BEATS, beats.joinToString(","))?.apply()
  }

  private fun beatTriggers(count: Int): List<Boolean> {
    val triggers = _state.value.beatTriggers
    return when {
      triggers.size < count -> triggers + List(count - triggers.size) { false }
      triggers.size > count -> triggers.take(count)
      else -> triggers
    }
  }

  fun addBeat() {
    listener?.onAddBeatRequest()
  }

  fun removeBeat() {
    listener?.onRemoveBeatRequest()
  }

  fun changeBeat(beat: Int, tickType: String) {
    listener?.onChangeBeatRequest(beat, tickType)
  }

  fun updateSubdivisions(subdivisions: List<String>) {
    _state.update { it.copy(
      subdivisions = subdivisions,
      subdivisionTriggers = subdivisionTriggers(subdivisions.size)
    ) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putString(
      Pref.SUBDIVISIONS, subdivisions.joinToString(",")
    )?.apply()
  }

  private fun subdivisionTriggers(count: Int): List<Boolean> {
    val triggers = _state.value.subdivisionTriggers
    return when {
      triggers.size < count -> triggers + List(count - triggers.size) { false }
      triggers.size > count -> triggers.take(count)
      else -> triggers
    }
  }

  fun addSubdivision() {
    listener?.onAddSubdivisionRequest()
  }

  fun removeSubdivision() {
    listener?.onRemoveSubdivisionRequest()
  }

  fun changeSubdivision(subdivision: Int, tickType: String) {
    listener?.onChangeSubdivisionRequest(subdivision, tickType)
  }

  fun updateSwing(swing: Int) {
    listener?.onSwingChangeRequest(swing)
  }

  fun addBookmark() {
    val existing = _state.value.bookmarks.find {
      it.tempo == _state.value.tempo
          && it.beats == _state.value.beats
          && it.subdivisions == _state.value.subdivisions
    }
    if (existing == null && _state.value.bookmarks.size < Constants.BOOKMARKS_MAX) {
      val bookmarks = _state.value.bookmarks.toMutableList()
      bookmarks.add(Bookmark(_state.value.tempo, _state.value.beats, _state.value.subdivisions))
      updateBookmarks(bookmarks)
    }
  }

  fun deleteBookmark(bookmark: Bookmark) {
    val bookmarks = _state.value.bookmarks.toMutableList()
    if (bookmarks.remove(bookmark)) {
      updateBookmarks(bookmarks)
    }
  }

  fun updateFromBookmark(bookmark: Bookmark) {
    updateTempo(bookmark.tempo)
    updateBeats(bookmark.beats)
    updateSubdivisions(bookmark.subdivisions)
  }

  fun circulateThroughBookmarks() {
    val bookmarks = _state.value.bookmarks
    if (bookmarks.isNotEmpty()) {
      val index = bookmarks.indexOfFirst {
        it.tempo == _state.value.tempo
            && it.beats == _state.value.beats
            && it.subdivisions == _state.value.subdivisions
      }
      if (index == -1) {
        updateFromBookmark(bookmarks[0])
      } else {
        val nextIndex = (index + 1) % bookmarks.size
        updateFromBookmark(bookmarks[nextIndex])
      }
    }
  }

  private fun updateBookmarks(bookmarks: List<Bookmark>) {
    _state.update { it.copy(bookmarks = bookmarks.sorted().toList()) }
    sharedPrefs?.edit()?.putString(
      Pref.BOOKMARKS,
      bookmarks.joinToString("|") {
        "${it.tempo}" +
            "&${it.beats.joinToString(",")}" +
            "&${it.subdivisions.joinToString(",")}"
      }
    )?.apply()
  }

  fun updateBeatModeVibrate(vibrate: Boolean) {
    _state.update { it.copy(beatModeVibrate = vibrate) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putBoolean(Pref.BEAT_MODE_VIBRATE, vibrate)?.apply()
  }

  fun updateAlwaysVibrate(alwaysVibrate: Boolean) {
    _state.update { it.copy(alwaysVibrate = alwaysVibrate) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putBoolean(Pref.ALWAYS_VIBRATE, alwaysVibrate)?.apply()
  }

  fun updateStrongVibration(strongVibration: Boolean) {
    _state.update { it.copy(strongVibration = strongVibration) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putBoolean(Pref.STRONG_VIBRATION, strongVibration)?.apply()
  }

  fun updateGain(gain: Int) {
    val startedWithGain = if (gain == 0) {
      true
    } else if (gain > 0) {
      false
    } else {
      _state.value.startedWithGain
    }
    _state.update { it.copy(
      gain = gain,
      startedWithGain = startedWithGain
    ) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putInt(Pref.GAIN, gain)?.apply()
  }

  fun updateSound(sound: String) {
    _state.update { it.copy(sound = sound) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putString(Pref.SOUND, sound)?.apply()
  }

  fun updateIgnoreFocus(ignoreFocus: Boolean) {
    _state.update { it.copy(ignoreFocus = ignoreFocus) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putBoolean(Pref.IGNORE_FOCUS, ignoreFocus)?.apply()
  }

  fun updateLatency(latency: Long) {
    _state.update { it.copy(latency = latency) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putLong(Pref.LATENCY, latency)?.apply()
  }

  fun updateKeepAwake(awake: Boolean) {
    _state.update { it.copy(keepAwake = awake) }
    listener?.onKeepAwakeChanged(keepAwake())
    sharedPrefs?.edit()?.putBoolean(Pref.KEEP_AWAKE, awake)?.apply()
  }

  private fun keepAwake(): Boolean {
    val currentRoute = _state.value.currentRoute
    return _state.value.keepAwake
        && _state.value.isPlaying
        && (currentRoute == Screen.Main.route
        || currentRoute == Screen.Tempo.route
        || currentRoute == Screen.Beats.route)
  }

  fun updateReduceAnim(reduceAnim: Boolean) {
    _state.update { it.copy(reduceAnim = reduceAnim) }
    sharedPrefs?.edit()?.putBoolean(Pref.REDUCE_ANIM, reduceAnim)?.apply()
  }

  fun updateFlashScreen(flashScreen: Boolean) {
    _state.update { it.copy(flashScreen = flashScreen) }
    listener?.onMetronomeConfigChanged(_state.value)
    sharedPrefs?.edit()?.putBoolean(Pref.FLASH_SCREEN, flashScreen)?.apply()
  }

  fun onPreTick(tick: Tick) {
    if (tick.subdivision == 1) {
      onBeat(tick.beat - 1)
    }
    onSubdivision(tick.subdivision - 1)
  }

  fun onTick(tick: Tick) {
    if (tick.subdivision == 1) {
      if (_state.value.flashScreen && tick.type == TickType.STRONG) {
        _state.update { it.copy(flashStrong = true) }
      } else if (_state.value.flashScreen && tick.type == TickType.NORMAL) {
        _state.update { it.copy(flash = true) }
      }
    }
  }

  private fun onBeat(index: Int) {
    if (index < _state.value.beatTriggers.size) {
      _state.update { it.copy(
        beatTriggers = _state.value.beatTriggers.mapIndexed { i, value ->
          if (i == index) !value else value
        }
      ) }
    }
  }

  private fun onSubdivision(index: Int) {
    if (index < _state.value.subdivisionTriggers.size) {
      _state.update { it.copy(
        subdivisionTriggers = _state.value.subdivisionTriggers.mapIndexed { i, value ->
          if (i == index) !value else value
        }
      ) }
    }
  }

  fun onFlashScreenEnd() {
    _state.update { it.copy(
      flash = false,
      flashStrong = false
    ) }
  }

  fun updateShowPermissionDialog(show: Boolean) {
    _state.update { it.copy(showPermissionDialog = show) }
  }

  interface StateListener {
    fun onMetronomeConfigChanged(state: MainState)
    fun onKeepAwakeChanged(keepAwake: Boolean)
    fun onPlayingToggleRequest()
    fun onAddBeatRequest()
    fun onRemoveBeatRequest()
    fun onChangeBeatRequest(beat: Int, tickType: String)
    fun onAddSubdivisionRequest()
    fun onRemoveSubdivisionRequest()
    fun onChangeSubdivisionRequest(subdivision: Int, tickType: String)
    fun onSwingChangeRequest(swing: Int)
  }

  companion object {
    private const val TAG = "MainViewModel"
  }
}