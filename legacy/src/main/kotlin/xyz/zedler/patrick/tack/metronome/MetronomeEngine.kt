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
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import xyz.zedler.patrick.audio.AudioEngine
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.BEAT_MODE
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.FLASHLIGHT
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER
import xyz.zedler.patrick.tack.Constants.TICK_TYPE
import xyz.zedler.patrick.tack.Constants.UNIT
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.database.SongDatabase
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.database.entity.Song
import xyz.zedler.patrick.tack.database.relations.SongWithParts
import xyz.zedler.patrick.tack.model.MetronomeConfig
import xyz.zedler.patrick.tack.util.FlashlightUtil
import xyz.zedler.patrick.tack.util.HapticUtil
import xyz.zedler.patrick.tack.util.NotificationUtil
import xyz.zedler.patrick.tack.util.ShortcutUtil
import xyz.zedler.patrick.tack.util.sendSongsWidgetUpdate
import xyz.zedler.patrick.tack.util.sortPartsByIndex
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Collections
import java.util.Locale
import java.util.Random
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MetronomeEngine(private val context: Context) {

  private val sharedPrefs: SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)
  private val audioEngine: AudioEngine = AudioEngine(context) { stop() }
  private val hapticUtil: HapticUtil = HapticUtil(context)
  private val shortcutUtil: ShortcutUtil = ShortcutUtil(context)
  private val flashlightUtil: FlashlightUtil = FlashlightUtil(context)
  private val listeners: MutableSet<MetronomeListener> = Collections.synchronizedSet(HashSet())
  private val executorService = Executors.newSingleThreadExecutor()
  private val random = Random()
  val config = MetronomeConfig()
  private val db: SongDatabase = SongDatabase.getInstance(context.applicationContext)

  private var tickThread: HandlerThread? = null
  private var audioThread: HandlerThread? = null
  private var callbackThread: HandlerThread? = null
  private var tickHandler: Handler? = null
  private var latencyHandler: Handler? = null
  private var audioHandler: Handler? = null
  private var countInHandler: Handler? = null
  private var incrementalHandler: Handler? = null
  private var elapsedHandler: Handler? = null
  private var timerHandler: Handler? = null
  private var muteHandler: Handler? = null

  var currentSongWithParts: SongWithParts? = null
    private set

  lateinit var currentSongId: String
    private set

  private var beatMode: String? = null
  private var keepAwake: String? = null
  private var flashScreen: String? = null
  private var flashlight: String? = null
  internal var currentPartIndex = 0
  private var muteCountDown = 0
  private var songsOrder = 0
  private var timerBarIndex = 0
  private var timerBeatIndex = 0
  private var timerSubIndex = 0
  private var tickIndex: Long = 0
  private var tickIndexPoly: Long = 0
  internal var latency: Long = 0
  private var countInStartTime: Long = 0
  private var timerStartTime: Long = 0
  private var elapsedStartTime: Long = 0
  private var elapsedTime: Long = 0
  private var elapsedPrevious: Long = 0
  private var nextScheduleTime: Long = 0
  private var nextPolyScheduleTime: Long = 0
  private var timerProgress = 0f
  private var isPlayingInternal = false
  private var tempPlaying = false
  private var isCountingInInternal = false
  private var isMuted = false
  private var showElapsed = false
  private var resetTimerOnStop = false
  internal var tempoInputKeyboard = false
  internal var tempoTapInstant = false
  private var neverStartedWithGain = true
  private var ignoreTimerCallbacksTemp = false
  var isSongPickerExpanded = false

  init {
    hapticUtil.intensity = sharedPrefs.getString(
      PREF.VIBRATION_INTENSITY, DEF.VIBRATION_INTENSITY
    )
      ?: DEF.VIBRATION_INTENSITY
    resetHandlersIfRequired()
    setToPreferences()
  }

  fun destroy() {
    listeners.clear()
    removeHandlerCallbacks()
    tickThread?.quit()
    audioThread?.quit()
    callbackThread?.quit()
    audioEngine.destroy()
    flashlightUtil.cleanup()
  }

  fun warmUpAudio() {
    audioEngine.warmUp()
  }

  fun setToPreferences() {
    config.setToPreferences(sharedPrefs)
    latency = sharedPrefs.getLong(PREF.LATENCY, DEF.LATENCY)
    showElapsed = sharedPrefs.getBoolean(PREF.SHOW_ELAPSED, DEF.SHOW_ELAPSED)
    resetTimerOnStop = sharedPrefs.getBoolean(
      PREF.RESET_TIMER_ON_STOP, DEF.RESET_TIMER_ON_STOP
    )
    flashScreen = sharedPrefs.getString(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN)
    flashlight = sharedPrefs.getString(PREF.FLASHLIGHT, DEF.FLASHLIGHT)
    keepAwake = sharedPrefs.getString(PREF.KEEP_AWAKE, DEF.KEEP_AWAKE)
    songsOrder = sharedPrefs.getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER)
    tempoInputKeyboard = sharedPrefs.getBoolean(
      PREF.TEMPO_INPUT_KEYBOARD, DEF.TEMPO_INPUT_KEYBOARD
    )
    tempoTapInstant = sharedPrefs.getBoolean(
      PREF.TEMPO_TAP_INSTANT, DEF.TEMPO_TAP_INSTANT
    )

    setSound(sharedPrefs.getString(PREF.SOUND, DEF.SOUND))
    setIgnoreFocus(sharedPrefs.getBoolean(PREF.IGNORE_FOCUS, DEF.IGNORE_FOCUS))
    setGain(sharedPrefs.getInt(PREF.GAIN, DEF.GAIN))
    setBeatMode(
      sharedPrefs.getString(PREF.BEAT_MODE, DEF.BEAT_MODE) ?: DEF.BEAT_MODE
    )
    setCurrentSong(
      sharedPrefs.getString(
        PREF.SONG_CURRENT_ID, DEF.SONG_CURRENT_ID
      ) ?: DEF.SONG_CURRENT_ID,
      sharedPrefs.getInt(
        PREF.PART_CURRENT_INDEX, DEF.PART_CURRENT_INDEX
      ),
      false
    ) {
      for (listener in listeners) {
        listener.onMetronomeConfigChanged()
      }
    }
  }

  fun setConfig(config: MetronomeConfig) {
    setCountIn(config.countIn)

    var tempo = config.tempo
    currentSongWithParts?.let {
      val speed = it.song.speed
      tempo = (tempo * speed / 100.0).roundToInt()
    }
    val tempoDiff = tempo - this.config.tempo
    changeTempo(tempoDiff)

    setBeats(config.beats)
    setSubdivisions(config.subdivisions)
    setUsePolyrhythm(config.usePolyrhythm)

    setIncrementalAmount(config.incrementalAmount)
    setIncrementalInterval(config.incrementalInterval)
    setIncrementalLimit(config.incrementalLimit)
    setIncrementalUnit(config.incrementalUnit)
    setIncrementalIncrease(config.incrementalIncrease)

    setTimerDuration(config.timerDuration)
    setTimerUnit(config.timerUnit)

    setMutePlay(config.mutePlay)
    setMuteMute(config.muteMute)
    setMuteUnit(config.muteUnit)
    setMuteRandom(config.muteRandom)

    maybeUpdateDefaultSong()

    for (listener in listeners) {
      listener.onMetronomeConfigChanged()
    }
  }

  fun setCurrentSong(
    songId: String,
    partIndex: Int,
    startPlaying: Boolean = false,
    onDone: Runnable? = null
  ) {
    currentSongId = songId
    executorService.execute {
      currentSongWithParts = db.songDao().getSongWithPartsById(songId)
      currentSongWithParts?.let {
        sortParts()
        setCurrentPartIndex(partIndex, startPlaying)
      } ?: run {
        if (songId == Constants.SONG_ID_DEFAULT) {
          val songDefault = Song(id = songId)
          db.songDao().insertSong(songDefault)
          val partDefault = Part.fromConfig(
            null, songDefault.id, 0, config
          )
          db.songDao().insertPart(partDefault)
          val parts = mutableListOf(partDefault)
          currentSongWithParts = SongWithParts(songDefault, parts)
        } else {
          Log.e(TAG, "setCurrentSong: song with id='$songId' not found")
        }
      }
      onDone?.run()
    }
    sharedPrefs.edit { putString(PREF.SONG_CURRENT_ID, songId) }
    if (!isSongPickerExpanded) {
      isSongPickerExpanded = songId != Constants.SONG_ID_DEFAULT
    }
  }

  fun reloadCurrentSong() {
    executorService.execute {
      currentSongWithParts = db.songDao().getSongWithPartsById(currentSongId)
      currentSongWithParts?.let {
        sortParts()
        setCurrentPartIndex(currentPartIndex)
      } ?: Log.e(TAG, "reloadCurrentSong: song with id='$currentSongId' not found")
    }
  }

  fun maybeUpdateDefaultSong() {
    executorService.execute {
      if (currentSongWithParts != null && currentSongId == Constants.SONG_ID_DEFAULT) {
        val part = currentSongWithParts!!.parts[0]
        if (!part.equalsConfig(config)) {
          part.setConfig(config)
          db.songDao().updatePart(part)
        }
      }
    }
  }

  fun setSongsOrder(sortOrder: Int) {
    songsOrder = sortOrder
    sharedPrefs.edit { putInt(PREF.SONGS_ORDER, sortOrder) }
  }

  fun getSongsOrder(): Int = songsOrder

  private fun sortParts() {
    currentSongWithParts?.parts?.let { sortPartsByIndex(it.toMutableList()) }
  }

  fun getCurrentPartIndex(): Int = currentPartIndex

  private fun hasNextPart(): Boolean {
    return currentSongWithParts?.let { currentPartIndex < it.parts.size - 1 } ?: false
  }

  fun setCurrentPartIndex(index: Int, startPlaying: Boolean = false) {
    val songWithParts = currentSongWithParts ?: run {
      Log.e(TAG, "setCurrentPartIndex: song with id='$currentSongId' is null")
      return
    }
    val parts = songWithParts.parts
    if (parts.isEmpty()) {
      Log.e(TAG, "setCurrentPartIndex: no part found for song with id='$currentSongId'")
      return
    }
    val boundedIndex = index.coerceIn(0, parts.size - 1)
    currentPartIndex = boundedIndex
    ignoreTimerCallbacksTemp = true
    setConfig(parts[boundedIndex].toConfig())
    ignoreTimerCallbacksTemp = false
    if (!isPlaying() && startPlaying) {
      start(false)
    } else {
      restartIfPlaying(true)
    }
    sharedPrefs.edit { putInt(PREF.PART_CURRENT_INDEX, boundedIndex) }
    synchronized(listeners) {
      for (listener in listeners) {
        listener.onMetronomeSongOrPartChanged(currentSongWithParts, currentPartIndex)
      }
    }
  }

  private fun resetHandlersIfRequired() {
    if (tickThread?.isAlive != true) {
      tickThread = HandlerThread("metronome_ticks").apply { start() }
      removeHandlerCallbacks()
      tickHandler = Handler(tickThread!!.looper)
    }
    if (audioThread?.isAlive != true) {
      audioThread = HandlerThread("metronome_audio").apply { start() }
      removeHandlerCallbacks()
      audioHandler = Handler(audioThread!!.looper)
    }
    if (callbackThread?.isAlive != true) {
      callbackThread = HandlerThread("metronome_callback").apply { start() }
      removeHandlerCallbacks()
      val looper = callbackThread!!.looper
      latencyHandler = Handler(looper)
      countInHandler = Handler(looper)
      incrementalHandler = Handler(looper)
      elapsedHandler = Handler(looper)
      timerHandler = Handler(looper)
      muteHandler = Handler(looper)
    }
  }

  private fun removeHandlerCallbacks() {
    tickHandler?.removeCallbacksAndMessages(null)
    audioHandler?.removeCallbacksAndMessages(null)
    latencyHandler?.removeCallbacksAndMessages(null)
    countInHandler?.removeCallbacksAndMessages(null)
    incrementalHandler?.removeCallbacksAndMessages(null)
    elapsedHandler?.removeCallbacksAndMessages(null)
    timerHandler?.removeCallbacksAndMessages(null)
    muteHandler?.removeCallbacksAndMessages(null)
  }

  fun savePlayingState() {
    tempPlaying = isPlaying()
  }

  fun restorePlayingState() {
    if (tempPlaying) start(false) else stop(false)
  }

  fun setUpLatencyCalibration() {
    config.tempo = 80
    config.beats = DEF.BEATS.split(",").toTypedArray()
    config.subdivisions = DEF.SUBDIVISIONS.split(",").toTypedArray()
    config.countIn = 0
    config.incrementalAmount = 0
    config.timerDuration = 0
    config.mutePlay = 0

    beatMode = BEAT_MODE.ALL
    audioEngine.gain = 0
    audioEngine.isMuted = false
    hapticUtil.setEnabled(true)

    start(true)
  }

  fun addListener(listener: MetronomeListener) {
    listeners.add(listener)
  }

  fun removeListener(listener: MetronomeListener) {
    listeners.remove(listener)
  }

  fun start(ignorePermission: Boolean = false) {
    val permissionDenied = sharedPrefs.getBoolean(PREF.PERMISSION_DENIED, false)
    if (!NotificationUtil.hasPermission(context) && !permissionDenied && !ignorePermission) {
      synchronized(listeners) {
        for (listener in listeners) listener.onMetronomePermissionMissing()
      }
      return
    }
    updateLastPlayedAndPlayCount()

    if (isPlaying()) return
    resetHandlersIfRequired()

    tickIndex = 0
    tickIndexPoly = 0
    isMuted = false
    if (config.isMuteActive()) {
      muteCountDown = calculateMuteCount(false)
    }

    isCountingInInternal = config.isCountInActive()
    countInStartTime = System.currentTimeMillis()
    countInHandler?.postDelayed({
      isCountingInInternal = false
      updateIncrementalHandler()
      elapsedStartTime = System.currentTimeMillis()
      updateElapsedHandler(false)
      timerStartTime = System.currentTimeMillis()
      updateTimerHandler(timerProgress, true)
      updateMuteHandler()
    }, countInInterval)

    if (getGain() > 0) neverStartedWithGain = false

    isPlayingInternal = true
    startTicks()

    synchronized(listeners) {
      for (listener in listeners) listener.onMetronomeStart()
    }
    Log.i(TAG, "start: started metronome handler")
  }

  fun stop(resetTimer: Boolean = resetTimerOnStop) {
    if (!isPlaying()) return
    var isTimerReset = false
    if (resetTimer || isTimerFinished()) {
      timerProgress = 0f
      timerBarIndex = 0
      timerBeatIndex = 0
      timerSubIndex = 0
      isTimerReset = true
    } else {
      timerProgress = getTimerProgress()
    }
    elapsedPrevious = elapsedTime

    removeHandlerCallbacks()
    isPlayingInternal = false
    audioHandler?.post { audioEngine.scheduleDelayedStop() }
    isCountingInInternal = false

    synchronized(listeners) {
      for (listener in listeners) {
        listener.onMetronomeStop()
        if (isTimerReset) listener.onMetronomeTimerProgressOneTime(true)
      }
    }
    Log.i(TAG, "stop: stopped metronome handler")
  }

  fun restartIfPlaying(resetTimer: Boolean) {
    if (isPlaying()) {
      var isTimerReset = false
      if (resetTimer || isTimerFinished()) {
        timerProgress = 0f
        timerBarIndex = 0
        timerBeatIndex = 0
        timerSubIndex = 0
        isTimerReset = true
      } else {
        timerProgress = getTimerProgress()
      }
      elapsedPrevious = elapsedTime
      removeHandlerCallbacks()
      synchronized(listeners) {
        for (listener in listeners) {
          if (isTimerReset) listener.onMetronomeTimerProgressOneTime(true)
        }
      }

      resetHandlersIfRequired()
      val countInTickIndex = if (config.usePolyrhythm) {
        config.countIn * config.getBeatsCount().toLong()
      } else {
        config.countIn * config.getBeatsCount().toLong() * config.getSubdivisionsCount()
      }
      tickIndex = if (config.isCountInActive()) countInTickIndex else 0
      tickIndexPoly = config.countIn.toLong() * config.getSubdivisionsCount()
      isMuted = false
      if (config.isMuteActive()) {
        muteCountDown = calculateMuteCount(false)
      }

      startTicks()
      isCountingInInternal = false
      updateIncrementalHandler()
      elapsedStartTime = System.currentTimeMillis()
      updateElapsedHandler(false)
      timerStartTime = System.currentTimeMillis()
      updateTimerHandler(timerProgress, true)
      updateMuteHandler()
    } else if (resetTimer) {
      timerProgress = 0f
      if (ignoreTimerCallbacksTemp) return
      synchronized(listeners) {
        for (listener in listeners) listener.onMetronomeTimerProgressOneTime(true)
      }
    }
  }

  fun isPlaying(): Boolean = isPlayingInternal

  private fun startTicks() {
    val now = System.currentTimeMillis()
    nextScheduleTime = now
    nextPolyScheduleTime = now

    val tickRunnablePoly = object : Runnable {
      override fun run() {
        if (!isPlaying()) return
        val subdivisionPoly = getCurrentSubdivisionPoly()
        val tickTypePoly = getCurrentTickTypePoly()
        var muted = isMuted
        if (config.isMuteActive() && config.muteUnit == UNIT.BEATS) {
          muted = random.nextInt(100) < config.muteMute
        }
        val tick = Tick(tickIndexPoly, 1, subdivisionPoly, tickTypePoly, muted, true)

        if (subdivisionPoly < config.getSubdivisionsCount()) {
          val barInterval = interval * config.getBeatsCount()
          val step = barInterval / config.getSubdivisionsCount()
          nextPolyScheduleTime += step
          val delay = (nextPolyScheduleTime - System.currentTimeMillis()).coerceAtLeast(0)
          tickHandler?.postDelayed(this, delay)
        }

        performTickPoly(tick)
        audioEngine.playTick(tick.type, tick.isMuted)
        tickIndexPoly++
      }
    }

    val tickRunnable = object : Runnable {
      override fun run() {
        if (!isPlaying()) return
        if (tickIndex == 0L) {
          val currentNow = System.currentTimeMillis()
          nextScheduleTime = currentNow
          nextPolyScheduleTime = currentNow
        }

        val beatIndex =
          if (config.usePolyrhythm) tickIndex else tickIndex / config.getSubdivisionsCount()
        val isBeat = config.usePolyrhythm || (tickIndex % config.getSubdivisionsCount()) == 0L
        val isFirstBeat = isBeat && (beatIndex % config.getBeatsCount()) == 0L
        val barIndex = beatIndex / config.getBeatsCount()
        val isCountIn = barIndex < config.countIn

        if (isFirstBeat && config.isMuteActive() && config.muteUnit == UNIT.BARS && !isCountIn) {
          if (muteCountDown > 0) {
            muteCountDown--
          } else {
            isMuted = !isMuted
            muteCountDown = (calculateMuteCount(isMuted) - 1).coerceAtLeast(0)
          }
        }

        val beat = getCurrentBeat()
        val subdivision = getCurrentSubdivision()
        val tickType = getCurrentTickType()
        var muted = isMuted
        if (config.isMuteActive() && config.muteUnit == UNIT.BEATS) {
          muted = random.nextInt(100) < config.muteMute
        }
        val tick = Tick(tickIndex, beat, subdivision, tickType, muted, false)

        val currentInterval =
          if (config.usePolyrhythm) interval else interval / config.getSubdivisionsCount()
        nextScheduleTime += currentInterval

        if (tick.beat == 1 && tick.subdivision == 1) {
          nextPolyScheduleTime = nextScheduleTime - currentInterval
        }

        val delay = (nextScheduleTime - System.currentTimeMillis()).coerceAtLeast(0)
        tickHandler?.postDelayed(this, delay)

        if (tick.beat == 1 && config.usePolyrhythm) {
          tickHandler?.post(tickRunnablePoly)
        }

        if (performTick(tick)) {
          audioEngine.playTick(tick.type, tick.isMuted)
          tickIndex++
        }
      }
    }
    audioHandler?.post {
      audioEngine.play()
      tickHandler?.post(tickRunnable)
    }
  }

  private fun updateLastPlayedAndPlayCount() {
    executorService.execute {
      currentSongWithParts?.let {
        if (currentSongId != Constants.SONG_ID_DEFAULT) {
          val currentSong = it.song
          currentSong.lastPlayed = System.currentTimeMillis()
          currentSong.incrementPlayCount()
          db.songDao().updateSong(currentSong)
          shortcutUtil.reportUsage(currentSong.id)
          if (songsOrder == SONGS_ORDER.LAST_PLAYED_ASC || songsOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
            sendSongsWidgetUpdate(context)
          }
        }
      }
    }
    updateShortcuts()
  }

  fun updateShortcuts() {
    executorService.execute {
      if (!ShortcutUtil.isSupported) return@execute
      shortcutUtil.removeAllShortcuts()
      val songs = db.songDao().getAllSongs()
      val filteredSongs = songs.toMutableList().apply {
        removeIf { it.id == Constants.SONG_ID_DEFAULT || it.playCount < 1 }
      }
      filteredSongs.sortWith(
        compareByDescending<Song> { it.playCount }.thenBy(
          String.CASE_INSENSITIVE_ORDER
        ) {
          it.name ?: ""
        })
      val maxShortcuts = shortcutUtil.maxShortcutCount
      val shortcuts = filteredSongs.take(maxShortcuts).map {
        shortcutUtil.getShortcutInfo(it.id, it.name ?: "")
      }
      shortcutUtil.addAllShortcuts(shortcuts)
    }
  }

  private fun setBeats(beats: Array<String>) {
    config.beats = beats
    sharedPrefs.edit { putString(PREF.BEATS, beats.joinToString(",")) }
  }

  fun setBeat(beat: Int, tickType: String) {
    config.setBeat(beat, tickType)
    setBeats(config.beats)
  }

  fun addBeat(): Boolean {
    val success = config.addBeat()
    if (success) setBeats(config.beats)
    return success
  }

  fun removeBeat(): Boolean {
    val success = config.removeBeat()
    if (success) setBeats(config.beats)
    return success
  }

  private fun setSubdivisions(subdivisions: Array<String>) {
    config.subdivisions = subdivisions
    sharedPrefs.edit {
      putString(PREF.SUBDIVISIONS, config.subdivisions.joinToString(","))
    }
  }

  fun setSubdivision(subdivision: Int, tickType: String) {
    config.setSubdivision(subdivision, tickType)
    setSubdivisions(config.subdivisions)
  }

  fun addSubdivision(): Boolean {
    val success = config.addSubdivision()
    if (success) setSubdivisions(config.subdivisions)
    return success
  }

  fun removeSubdivision(): Boolean {
    val success = config.removeSubdivision()
    if (success) setSubdivisions(config.subdivisions)
    return success
  }

  fun setSwing3() {
    config.setSwing3()
    setSubdivisions(config.subdivisions)
  }

  fun setSwing5() {
    config.setSwing5()
    setSubdivisions(config.subdivisions)
  }

  fun setSwing7() {
    config.setSwing7()
    setSubdivisions(config.subdivisions)
  }

  fun setTempo(tempo: Int) {
    if (config.tempo != tempo) {
      config.tempo = tempo
      sharedPrefs.edit { putInt(PREF.TEMPO, tempo) }
      if (isPlaying() && config.isTimerActive() && config.timerUnit == UNIT.BARS) {
        updateTimerHandler(startAtFirstBeat = false, performOneTime = true, withTransition = false)
      }
    }
  }

  private fun changeTempo(change: Int) {
    val tempoOld = config.tempo
    val tempoNew = tempoOld + change
    setTempo(tempoNew)
    synchronized(listeners) {
      for (listener in listeners) listener.onMetronomeTempoChanged(tempoOld, tempoNew)
    }
    maybeUpdateDefaultSong()
  }

  val interval: Long
    get() = 1000L * 60 / maxOf(config.tempo, 1)

  fun setUsePolyrhythm(usePolyrhythm: Boolean) {
    config.usePolyrhythm = usePolyrhythm
    sharedPrefs.edit { putBoolean(PREF.USE_POLYRHYTHM, usePolyrhythm) }
  }

  fun setSound(sound: String?) {
    audioEngine.setSound(sound ?: DEF.SOUND)
    sharedPrefs.edit { putString(PREF.SOUND, sound) }
  }

  fun getSound(): String = sharedPrefs.getString(
    PREF.SOUND, DEF.SOUND
  ) ?: DEF.SOUND

  fun setBeatMode(mode: String) {
    var finalMode = mode
    if (!hapticUtil.hasVibrator()) finalMode = BEAT_MODE.SOUND
    beatMode = finalMode
    audioEngine.isMuted = finalMode == BEAT_MODE.VIBRATION
    hapticUtil.setEnabled(finalMode != BEAT_MODE.SOUND)
    sharedPrefs.edit { putString(PREF.BEAT_MODE, finalMode) }
  }

  fun getBeatMode(): String? = beatMode

  fun areHapticEffectsPossible(ignoreIsPlaying: Boolean): Boolean {
    return if (ignoreIsPlaying) beatMode == BEAT_MODE.SOUND
    else !isPlaying() || areHapticEffectsPossible(true)
  }

  fun setVibrationIntensity(intensity: String?) {
    hapticUtil.intensity = intensity ?: DEF.VIBRATION_INTENSITY
    sharedPrefs.edit { putString(PREF.VIBRATION_INTENSITY, intensity) }
  }

  fun setLatency(offset: Long) {
    latency = offset
    sharedPrefs.edit { putLong(PREF.LATENCY, offset) }
  }

  fun getLatency(): Long = latency

  fun setIgnoreFocus(ignore: Boolean) {
    audioEngine.ignoreFocus = ignore
    sharedPrefs.edit { putBoolean(PREF.IGNORE_FOCUS, ignore) }
  }

  fun getIgnoreAudioFocus(): Boolean = audioEngine.ignoreFocus

  fun setGain(gain: Int) {
    audioEngine.gain = gain
    sharedPrefs.edit { putInt(PREF.GAIN, gain) }
  }

  fun getGain(): Int = audioEngine.gain

  fun neverStartedWithGainBefore(): Boolean = neverStartedWithGain

  fun setFlashScreen(flash: String?) {
    flashScreen = flash
    sharedPrefs.edit { putString(PREF.FLASH_SCREEN, flash) }
  }

  fun getFlashScreen(): String? = flashScreen

  fun setFlashlight(strength: String?) {
    flashlight = strength
    sharedPrefs.edit { putString(PREF.FLASHLIGHT, strength) }
  }

  fun getFlashlight(): String? = flashlight

  fun setKeepAwake(keepAwake: String?) {
    this.keepAwake = keepAwake
    sharedPrefs.edit { putString(PREF.KEEP_AWAKE, keepAwake) }
  }

  fun getKeepAwake(): String? = keepAwake

  fun setTempoInputKeyboard(keyboard: Boolean) {
    tempoInputKeyboard = keyboard
    sharedPrefs.edit { putBoolean(PREF.TEMPO_INPUT_KEYBOARD, keyboard) }
  }

  fun getTempoInputKeyboard(): Boolean = tempoInputKeyboard

  fun setTempoTapInstant(instant: Boolean) {
    tempoTapInstant = instant
    sharedPrefs.edit { putBoolean(PREF.TEMPO_TAP_INSTANT, instant) }
  }

  fun getTempoTapInstant(): Boolean = tempoTapInstant

  fun setCountIn(bars: Int) {
    config.countIn = bars
    sharedPrefs.edit { putInt(PREF.COUNT_IN, bars) }
  }

  fun isCountingIn(): Boolean = isCountingInInternal

  val countInInterval: Long
    get() = interval * config.getBeatsCount() * config.countIn

  fun getCountInProgress(): Float {
    if (isPlaying() && isCountingIn()) {
      val countInElapsed = System.currentTimeMillis() - countInStartTime
      return (countInElapsed / countInInterval.toFloat()).coerceIn(0f, 1f)
    }
    return 1f
  }

  fun getCountInIntervalRemaining(): Long {
    if (isPlaying() && isCountingIn()) {
      val countInElapsed = System.currentTimeMillis() - countInStartTime
      return (countInInterval - countInElapsed).coerceAtLeast(0)
    }
    return 0
  }

  fun setIncrementalAmount(bpm: Int) {
    config.incrementalAmount = bpm
    sharedPrefs.edit { putInt(PREF.INCREMENTAL_AMOUNT, bpm) }
    updateIncrementalHandler()
  }

  fun setIncrementalIncrease(increase: Boolean) {
    config.incrementalIncrease = increase
    sharedPrefs.edit { putBoolean(PREF.INCREMENTAL_INCREASE, increase) }
  }

  fun setIncrementalInterval(interval: Int) {
    config.incrementalInterval = interval
    sharedPrefs.edit { putInt(PREF.INCREMENTAL_INTERVAL, interval) }
    updateIncrementalHandler()
  }

  fun setIncrementalUnit(unit: String) {
    if (unit == config.incrementalUnit) return
    config.incrementalUnit = unit
    sharedPrefs.edit { putString(PREF.INCREMENTAL_UNIT, unit) }
    updateIncrementalHandler()
  }

  fun setIncrementalLimit(limit: Int) {
    config.incrementalLimit = limit
    sharedPrefs.edit { putInt(PREF.INCREMENTAL_LIMIT, limit) }
  }

  private fun updateIncrementalHandler() {
    if (!isPlaying()) return
    incrementalHandler?.removeCallbacksAndMessages(null)
    val unit = config.incrementalUnit
    val amount = config.incrementalAmount
    val limit = config.incrementalLimit
    val increase = config.incrementalIncrease
    if (unit != UNIT.BARS && config.isIncrementalActive()) {
      val factor = if (unit == UNIT.SECONDS) 1000L else 60000L
      val intervalMillis = factor * config.incrementalInterval
      incrementalHandler?.postDelayed(object : Runnable {
        override fun run() {
          incrementalHandler?.postDelayed(this, intervalMillis)
          val upperLimit = if (limit != 0) limit else Constants.TEMPO_MAX
          val lowerLimit = if (limit != 0) limit else Constants.TEMPO_MIN
          if (increase && config.tempo + amount <= upperLimit) {
            changeTempo(amount)
          } else if (!increase && config.tempo - amount >= lowerLimit) {
            changeTempo(-amount)
          }
        }
      }, intervalMillis)
    }
  }

  fun setShowElapsed(show: Boolean) {
    showElapsed = show
    sharedPrefs.edit { putBoolean(PREF.SHOW_ELAPSED, show) }
  }

  fun getShowElapsed(): Boolean = showElapsed

  fun isElapsedActive(): Boolean = showElapsed

  fun resetElapsed() {
    elapsedPrevious = 0
    elapsedStartTime = System.currentTimeMillis()
    elapsedTime = 0
    synchronized(listeners) {
      for (listener in listeners) listener.onMetronomeElapsedTimeSecondsChanged()
    }
    updateElapsedHandler(true)
  }

  fun updateElapsedHandler(reset: Boolean) {
    if (!isPlaying()) return
    elapsedHandler?.removeCallbacksAndMessages(null)
    if (!isElapsedActive()) return
    if (reset) elapsedPrevious = 0
    elapsedHandler?.post(object : Runnable {
      override fun run() {
        if (isPlaying()) {
          elapsedTime = System.currentTimeMillis() - elapsedStartTime + elapsedPrevious
          elapsedHandler?.postDelayed(this, 1000)
          synchronized(listeners) {
            for (listener in listeners) listener.onMetronomeElapsedTimeSecondsChanged()
          }
        }
      }
    })
  }

  fun getElapsedTimeString(): String {
    if (!isElapsedActive()) return ""
    val seconds = (elapsedTime / 1000).toInt()
    return getTimeStringFromSeconds(seconds, false)
  }

  fun setTimerDuration(duration: Int) {
    val durationOld = config.timerDuration
    config.timerDuration = duration
    sharedPrefs.edit { putInt(PREF.TIMER_DURATION, duration) }

    if (duration != durationOld && (duration == 0 || durationOld == 0)) {
      synchronized(listeners) {
        for (listener in listeners) listener.onMetronomeTimerActiveStateChanged(config.isTimerActive())
      }
    }

    if (config.timerUnit == UNIT.BARS) {
      updateTimerHandler(startAtFirstBeat = false, performOneTime = true)
    } else {
      updateTimerHandler(0f, false)
    }
  }

  fun getTimerInterval(): Long {
    val factor = when (config.timerUnit) {
      UNIT.SECONDS -> 1000L
      UNIT.MINUTES -> 60000L
      else -> interval * config.getBeatsCount()
    }
    return factor * config.timerDuration
  }

  fun getTimerIntervalRemaining(): Long = (getTimerInterval() * (1 - getTimerProgress())).toLong()

  fun setTimerUnit(unit: String) {
    if (unit == config.timerUnit) return
    config.timerUnit = unit
    sharedPrefs.edit { putString(PREF.TIMER_UNIT, unit) }
    updateTimerHandler(0f, false)
  }

  fun setResetTimerOnStop(reset: Boolean) {
    resetTimerOnStop = reset
    sharedPrefs.edit { putBoolean(PREF.RESET_TIMER_ON_STOP, reset) }
  }

  fun getResetTimerOnStop(): Boolean = resetTimerOnStop

  fun getTimerProgress(): Float {
    return if (config.isTimerActive()) {
      if (config.timerUnit != UNIT.BARS && isPlaying() && !isCountingInInternal) {
        val previousDuration = (timerProgress * getTimerInterval()).toLong()
        val currentElapsedTime = System.currentTimeMillis() - timerStartTime + previousDuration
        (currentElapsedTime / getTimerInterval().toFloat()).coerceIn(0f, 1f)
      } else {
        timerProgress
      }
    } else {
      0f
    }
  }

  fun isTimerFinished(): Boolean {
    return if (config.timerUnit == UNIT.BARS) {
      timerBarIndex >= config.timerDuration - 1 &&
          timerBeatIndex >= config.getBeatsCount() - 1 &&
          timerSubIndex >= config.getSubdivisionsCount() - 1
    } else {
      try {
        val bdProgress = BigDecimal.valueOf(
          getTimerProgress().toDouble()
        ).setScale(
          2, RoundingMode.HALF_UP
        )
        val bdFraction = BigDecimal.valueOf(1).setScale(2, RoundingMode.HALF_UP)
        bdProgress == bdFraction
      } catch (_: NumberFormatException) {
        false
      }
    }
  }

  fun updateTimerHandler(fraction: Float, startAtFirstBeat: Boolean) {
    timerProgress = fraction
    when (config.timerUnit) {
      UNIT.SECONDS, UNIT.MINUTES -> {
        val intervalMillis = maxOf(interval, 1)
        val isUnitSeconds = config.timerUnit == UNIT.SECONDS
        val totalMillis = config.timerDuration * if (isUnitSeconds) 1000L else 60000L
        val elapsedMillis = (fraction * totalMillis).toLong()
        timerBarIndex = (elapsedMillis / intervalMillis / config.getBeatsCount()).toInt()
        timerBeatIndex =
          ((elapsedMillis % (intervalMillis * config.getBeatsCount())) / intervalMillis).toInt()
        val subdivisionMillis = maxOf(intervalMillis / config.getSubdivisionsCount(), 1)
        timerSubIndex = ((elapsedMillis % intervalMillis) / subdivisionMillis).toInt()
      }

      else -> {
        val barIndex = (fraction * config.timerDuration).toInt()
        timerBarIndex = barIndex.coerceAtMost(config.timerDuration - 1)
        if (barIndex <= config.timerDuration - 1) {
          timerBeatIndex = ((fraction * config.timerDuration * config.getBeatsCount())
              % config.getBeatsCount()).toInt()
          timerSubIndex = (fraction * config.timerDuration * config.getBeatsCount()
              * config.getSubdivisionsCount()).toInt() % config.getSubdivisionsCount()
        } else {
          timerBeatIndex = config.getBeatsCount() - 1
          timerSubIndex = config.getSubdivisionsCount() - 1
        }
      }
    }
    updateTimerHandler(startAtFirstBeat, false)
  }

  fun updateTimerHandler(
    startAtFirstBeat: Boolean,
    performOneTime: Boolean,
    withTransition: Boolean = true
  ) {
    if (!isPlaying()) return
    timerHandler?.removeCallbacksAndMessages(null)
    if (!config.isTimerActive()) return

    if (isTimerFinished()) {
      timerProgress = 0f
    } else if (startAtFirstBeat) {
      val barInterval = interval * config.getBeatsCount()
      timerProgress = timerBarIndex * barInterval / getTimerInterval().toFloat()
      timerBeatIndex = 0
      timerSubIndex = 0
    }

    if (config.timerUnit != UNIT.BARS) {
      timerHandler?.postDelayed({
        if (hasNextPart()) {
          setCurrentPartIndex(currentPartIndex + 1)
        } else if (currentSongWithParts?.song?.isLooped == true) {
          setCurrentPartIndex(0)
        } else {
          stop()
          if (currentSongWithParts != null) setCurrentPartIndex(0)
        }
      }, getTimerIntervalRemaining())
      timerHandler?.post(object : Runnable {
        override fun run() {
          if (isPlaying() && config.timerUnit != UNIT.BARS) {
            timerHandler?.postDelayed(this, 1000)
            synchronized(listeners) {
              for (listener in listeners) listener.onMetronomeTimerSecondsChanged()
            }
          }
        }
      })
    }

    if (ignoreTimerCallbacksTemp) return
    synchronized(listeners) {
      for (listener in listeners) {
        if (performOneTime) listener.onMetronomeTimerProgressOneTime(withTransition)
        else listener.onMetronomeTimerStarted()
      }
    }
  }

  fun resetTimerNow() {
    if (config.isTimerActive()) restartIfPlaying(true)
  }

  fun getCurrentTimerString(): String {
    if (!config.isTimerActive()) return ""
    return when (config.timerUnit) {
      UNIT.SECONDS, UNIT.MINUTES -> {
        val currentElapsedTime = (getTimerProgress() * getTimerInterval()).toLong()
        val seconds = (currentElapsedTime / 1000).toInt()
        val totalHours =
          if (config.timerUnit == UNIT.MINUTES) config.timerDuration / 60 else config.timerDuration / 3600
        getTimeStringFromSeconds(seconds, totalHours > 0)
      }

      else -> {
        var format = if (config.getBeatsCount() < 10) "%d.%01d" else "%d.%02d"
        if (config.getSubdivisionsCount() > 1) {
          format += if (config.getSubdivisionsCount() < 10) ".%01d" else ".%02d"
          String.format(
            Locale.ENGLISH,
            format,
            timerBarIndex + 1,
            timerBeatIndex + 1,
            timerSubIndex + 1
          )
        } else {
          String.format(Locale.ENGLISH, format, timerBarIndex + 1, timerBeatIndex + 1)
        }
      }
    }
  }

  fun getTotalTimeString(): String {
    if (!config.isTimerActive()) return ""
    val timerDuration = config.timerDuration
    return when (config.timerUnit) {
      UNIT.SECONDS, UNIT.MINUTES -> {
        val seconds = if (config.timerUnit == UNIT.MINUTES) timerDuration * 60 else timerDuration
        getTimeStringFromSeconds(seconds, false)
      }

      else -> context.resources.getQuantityString(
        R.plurals.options_unit_bars,
        timerDuration,
        timerDuration
      )
    }
  }

  fun setMutePlay(play: Int) {
    config.mutePlay = play
    sharedPrefs.edit { putInt(PREF.MUTE_PLAY, play) }
    updateMuteHandler()
  }

  fun setMuteMute(mute: Int) {
    config.muteMute = mute
    sharedPrefs.edit { putInt(PREF.MUTE_MUTE, config.muteMute) }
    updateMuteHandler()
  }

  fun setMuteUnit(unit: String) {
    if (unit == config.muteUnit) return
    config.muteUnit = unit
    sharedPrefs.edit { putString(PREF.MUTE_UNIT, unit) }
    setMuteMute(config.muteMute)
    updateMuteHandler()
  }

  fun setMuteRandom(random: Boolean) {
    config.muteRandom = random
    sharedPrefs.edit { putBoolean(PREF.MUTE_RANDOM, random) }
    updateMuteHandler()
  }

  private fun updateMuteHandler() {
    if (!isPlaying()) return
    muteHandler?.removeCallbacksAndMessages(null)
    isMuted = false
    if (config.isMuteActive() && config.muteUnit == UNIT.SECONDS) {
      muteHandler?.postDelayed(object : Runnable {
        override fun run() {
          isMuted = !isMuted
          muteHandler?.postDelayed(this, calculateMuteCount(isMuted) * 1000L)
        }
      }, calculateMuteCount(isMuted) * 1000L)
    }
  }

  private fun calculateMuteCount(mute: Boolean): Int {
    val count = if (mute) config.muteMute else config.mutePlay
    return if (config.muteRandom) random.nextInt(count + 1) else count
  }

  private fun performTick(tick: Tick): Boolean {
    val beatIndex =
      if (config.usePolyrhythm) tickIndex else tickIndex / config.getSubdivisionsCount()
    val barIndex = beatIndex / config.getBeatsCount()
    val barIndexWithoutCountIn = barIndex - config.countIn
    val isCountIn = barIndex < config.countIn

    val isBeat = tick.subdivision == 1
    val isFirstBeat = isBeat && (beatIndex % config.getBeatsCount()) == 0L

    if (config.isTimerActive() && config.timerUnit == UNIT.BARS && !isCountIn) {
      val isFirstBeatInFirstBar = barIndexWithoutCountIn == 0L && isFirstBeat
      val increaseTimerProgress = barIndexWithoutCountIn > 0 || !isFirstBeatInFirstBar
      if (increaseTimerProgress) {
        if (isFirstBeat) timerBarIndex++
        if (isBeat) {
          timerBeatIndex++
          if (timerBeatIndex >= config.getBeatsCount()) timerBeatIndex = 0
        }
        timerSubIndex++
        if (timerSubIndex >= config.getSubdivisionsCount()) timerSubIndex = 0
        val barInterval = interval * config.getBeatsCount()
        val subInterval = interval / config.getSubdivisionsCount()
        val progressInterval =
          timerBarIndex * barInterval + timerBeatIndex * interval + timerSubIndex * subInterval
        timerProgress = progressInterval / getTimerInterval().toFloat()
      }
      var isFinished = if (config.timerUnit == UNIT.BARS) {
        timerBarIndex > config.timerDuration - 1
      } else {
        isTimerFinished()
      }
      if (isFinished) {
        if (config.timerUnit == UNIT.BARS) {
          timerBarIndex = config.timerDuration - 1
          timerBeatIndex = config.getBeatsCount() - 1
          timerSubIndex = config.getSubdivisionsCount() - 1
        }
        timerProgress = 1f
        if (hasNextPart()) {
          setCurrentPartIndex(currentPartIndex + 1)
        } else if (currentSongWithParts?.song?.isLooped == true) {
          setCurrentPartIndex(0)
        } else {
          stop()
          if (currentSongWithParts != null) setCurrentPartIndex(0)
        }
        return false
      }
    }

    if (isFirstBeat && config.isIncrementalActive() && config.incrementalUnit == UNIT.BARS
      && !isCountIn
    ) {
      val amount = config.incrementalAmount
      val intervalBars = config.incrementalInterval
      val limit = config.incrementalLimit
      val increase = config.incrementalIncrease
      if (barIndexWithoutCountIn >= intervalBars && barIndexWithoutCountIn % intervalBars == 0L) {
        val upperLimit = if (limit != 0) limit else Constants.TEMPO_MAX
        val lowerLimit = if (limit != 0) limit else Constants.TEMPO_MIN
        if (increase && config.tempo + amount <= upperLimit) {
          changeTempo(amount)
        } else if (!increase && config.tempo - amount >= lowerLimit) {
          changeTempo(-amount)
        }
      }
    }

    latencyHandler?.postDelayed({
      synchronized(listeners) {
        for (listener in listeners) listener.onMetronomePreTick(tick)
      }
    }, (latency - Constants.BEAT_ANIM_OFFSET).coerceAtLeast(0))
    latencyHandler?.postDelayed({
      if (beatMode != BEAT_MODE.SOUND && !tick.isMuted) {
        when (tick.type) {
          TICK_TYPE.STRONG -> hapticUtil.heavyClick(false)
          TICK_TYPE.SUB -> hapticUtil.tick(false)
          TICK_TYPE.MUTED, TICK_TYPE.BEAT_SUB_MUTED -> {}
          else -> hapticUtil.click(false)
        }
      }
      if (flashlight != FLASHLIGHT.OFF) {
        val strength = if (flashlight == FLASHLIGHT.STRONG) 0.8f else 0.15f
        when (tick.type) {
          TICK_TYPE.STRONG -> flashlightUtil.flash(100, strength)
          TICK_TYPE.SUB, TICK_TYPE.MUTED, TICK_TYPE.BEAT_SUB_MUTED -> {}
          else -> flashlightUtil.flash(20, strength)
        }
      }
      synchronized(listeners) {
        for (listener in listeners) listener.onMetronomeTick(tick)
      }
    }, latency)

    return true
  }

  private fun performTickPoly(tick: Tick) {
    latencyHandler?.postDelayed({
      synchronized(listeners) {
        for (listener in listeners) listener.onMetronomePreTick(tick)
      }
    }, (latency - Constants.BEAT_ANIM_OFFSET).coerceAtLeast(0))
    latencyHandler?.postDelayed({
      var shouldVibrate = beatMode != BEAT_MODE.SOUND && !isMuted
      if (shouldVibrate) {
        val product = (tick.subdivision - 1).toLong() * config.getBeatsCount()
        if (product % config.getSubdivisionsCount() == 0L) shouldVibrate = false
      }
      if (shouldVibrate) {
        when (tick.type) {
          TICK_TYPE.STRONG -> hapticUtil.heavyClick()
          TICK_TYPE.SUB -> hapticUtil.tick()
          TICK_TYPE.MUTED, TICK_TYPE.BEAT_SUB_MUTED -> {}
          else -> hapticUtil.click()
        }
      }
    }, latency)
  }

  private fun getCurrentBeat(): Int {
    return if (config.usePolyrhythm) {
      (tickIndex % config.beats.size).toInt() + 1
    } else {
      ((tickIndex / config.getSubdivisionsCount()) % config.beats.size).toInt() + 1
    }
  }

  private fun getCurrentSubdivision(): Int {
    return if (config.usePolyrhythm) 1 else (tickIndex % config.getSubdivisionsCount()).toInt() + 1
  }

  private fun getCurrentSubdivisionPoly(): Int =
    (tickIndexPoly % config.getSubdivisionsCount()).toInt() + 1

  private fun getCurrentTickType(): String {
    return if (config.usePolyrhythm) {
      val beats = config.beats
      val beatIndex = (tickIndex % beats.size).toInt()
      if (beatIndex == 0 && config.isFirstSubdivisionMuted()) TICK_TYPE.BEAT_SUB_MUTED
      else beats[beatIndex]
    } else {
      val subCount = config.getSubdivisionsCount()
      if ((tickIndex % subCount.toLong()) == 0L) {
        if (config.isFirstSubdivisionMuted()) {
          TICK_TYPE.BEAT_SUB_MUTED
        } else {
          val beats = config.beats
          beats[((tickIndex / subCount) % beats.size).toInt()]
        }
      } else {
        val subdivisions = config.subdivisions
        subdivisions[(tickIndex % subCount).toInt()]
      }
    }
  }

  private fun getCurrentTickTypePoly(): String {
    val subCount = config.getSubdivisionsCount()
    return if ((tickIndexPoly % subCount.toLong()) == 0L) {
      TICK_TYPE.BEAT_SUB_MUTED
    } else {
      val subdivisions = config.subdivisions
      subdivisions[(tickIndexPoly % subCount).toInt()]
    }
  }

  interface MetronomeListener {
    fun onMetronomeStart()
    fun onMetronomeStop()
    fun onMetronomePreTick(tick: Tick)
    fun onMetronomeTick(tick: Tick)
    fun onMetronomeTempoChanged(tempoOld: Int, tempoNew: Int)
    fun onMetronomeElapsedTimeSecondsChanged()
    fun onMetronomeTimerStarted()
    fun onMetronomeTimerSecondsChanged()
    fun onMetronomeTimerProgressOneTime(withTransition: Boolean)
    fun onMetronomeTimerActiveStateChanged(active: Boolean)
    fun onMetronomeConfigChanged()
    fun onMetronomeSongOrPartChanged(song: SongWithParts?, partIndex: Int)
    fun onMetronomePermissionMissing()
  }

  open class MetronomeListenerAdapter : MetronomeListener {
    override fun onMetronomeStart() {}
    override fun onMetronomeStop() {}
    override fun onMetronomePreTick(tick: Tick) {}
    override fun onMetronomeTick(tick: Tick) {}
    override fun onMetronomeTempoChanged(tempoOld: Int, tempoNew: Int) {}
    override fun onMetronomeElapsedTimeSecondsChanged() {}
    override fun onMetronomeTimerStarted() {}
    override fun onMetronomeTimerSecondsChanged() {}
    override fun onMetronomeTimerProgressOneTime(withTransition: Boolean) {}
    override fun onMetronomeTimerActiveStateChanged(active: Boolean) {}
    override fun onMetronomeConfigChanged() {}
    override fun onMetronomeSongOrPartChanged(song: SongWithParts?, partIndex: Int) {}
    override fun onMetronomePermissionMissing() {}
  }

  data class Tick(
    val index: Long,
    val beat: Int,
    val subdivision: Int,
    val type: String,
    val isMuted: Boolean,
    val isPoly: Boolean
  ) {
    override fun toString(): String {
      return "Tick{index = $index, beat=$beat, sub=$subdivision, type=$type, " +
          "isPoly=$isPoly, muted=$isMuted}"
    }
  }

  companion object {
    private val TAG = MetronomeEngine::class.java.simpleName

    fun getTimeStringFromSeconds(seconds: Int, forceHours: Boolean): String {
      val minutes = seconds / 60
      val hours = minutes / 60
      return if (hours > 0 || forceHours) {
        String.format(
          Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes % 60, seconds % 60
        )
      } else {
        String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds % 60)
      }
    }
  }
}
