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

package xyz.zedler.patrick.tack.core.metronome

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.zedler.patrick.tack.core.audio.AudioProvider
import xyz.zedler.patrick.tack.core.hardware.FlashlightProvider
import xyz.zedler.patrick.tack.core.hardware.HapticProvider
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.core.model.BeatMode
import xyz.zedler.patrick.tack.core.model.FlashStrength
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import xyz.zedler.patrick.tack.core.model.MetronomeConstants
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.core.model.Tick
import xyz.zedler.patrick.tack.core.model.TickType
import xyz.zedler.patrick.tack.core.model.TimingUnit
import xyz.zedler.patrick.tack.core.util.Clock
import xyz.zedler.patrick.tack.core.util.SystemClockImpl
import java.util.Random

class MetronomeEngine(
  private val audioProvider: AudioProvider,
  private val hapticProvider: HapticProvider? = null,
  private val flashlightProvider: FlashlightProvider? = null,
  private val clock: Clock = SystemClockImpl(),
  private val tickLooper: Looper? = null,
  private val callbackLooper: Looper? = null
) {
  private val _state = MutableStateFlow(MetronomeState())
  val state: StateFlow<MetronomeState> = _state.asStateFlow()

  private val _tickEvent = MutableSharedFlow<Tick>(
    extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val tickEvent: SharedFlow<Tick> = _tickEvent.asSharedFlow()

  private val _preTickEvent = MutableSharedFlow<Tick>(
    extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val preTickEvent: SharedFlow<Tick> = _preTickEvent.asSharedFlow()

  private val _engineEvent = MutableSharedFlow<EngineEvent>(
    extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val engineEvent: SharedFlow<EngineEvent> = _engineEvent.asSharedFlow()

  sealed interface EngineEvent {
    data class AutoTempoChange(val newTempo: Int) : EngineEvent
    data class PartChange(val partIndex: Int, val config: MetronomeConfig) : EngineEvent
    data object PlaylistEnd : EngineEvent
  }

  var config = MetronomeConfig()
    private set
  private var appSettings = AppSettings()
  private val random = Random()

  private var playlist: List<MetronomeConfig> = emptyList()
  private var isPlaylistLooped = false
  private var ignoreTimerCallbacksTemp = false
  private var tempPlaying = false

  private var tickThread: HandlerThread? = null
  private var tickHandler: Handler? = null
  private var callbackThread: HandlerThread? = null

  private var timerHandler: Handler? = null
  private var incrementalHandler: Handler? = null
  private var muteHandler: Handler? = null
  private var elapsedHandler: Handler? = null

  private var nextScheduleTime: Long = 0
  private var nextPolyScheduleTime: Long = 0
  private var tickIndex: Long = 0
  private var tickIndexPoly: Long = 0

  private var timerStartTime: Long = 0
  private var timerPreviousElapsed: Long = 0
  private var elapsedStartTime: Long = 0
  private var elapsedPrevious: Long = 0

  private var muteCountDown = 0

  // Settings and config

  fun updateSettings(settings: AppSettings) {
    val oldSettings = appSettings
    appSettings = settings

    audioProvider.isMuted = settings.beatMode == BeatMode.VIBRATION
    audioProvider.ignoreFocus = settings.ignoreFocus
    updateHapticPossible()

    if (oldSettings.showElapsed != settings.showElapsed) {
      updateElapsedHandler(false)
    }
  }

  fun applyConfig(newConfig: MetronomeConfig) {
    val oldConfig = config
    config = newConfig
    _state.update { it.copy(tempo = config.tempo) }

    if (state.value.isPlaying) {
      if (oldConfig.timerDuration != config.timerDuration
        || oldConfig.timerUnit != config.timerUnit
      ) {
        updateTimerHandler(startAtFirstBeat = config.timerUnit == TimingUnit.BARS)
      }
      if (oldConfig.incrementalAmount != config.incrementalAmount
        || oldConfig.incrementalUnit != config.incrementalUnit
      ) {
        updateIncrementalHandler()
      }
      if (oldConfig.mutePlay != config.mutePlay
        || oldConfig.muteMute != config.muteMute
        || oldConfig.muteUnit != config.muteUnit
      ) {
        updateMuteHandler()
      }
    }
  }

  // Playlist and parts

  fun setPlaylist(
    configs: List<MetronomeConfig>,
    isLooped: Boolean,
    partIndex: Int = 0,
    startPlaying: Boolean = false
  ) {
    playlist = configs
    isPlaylistLooped = isLooped

    if (configs.isNotEmpty()) {
      setPartIndex(partIndex, startPlaying)
    } else {
      _state.update { it.copy(currentPartIndex = 0) }
    }
  }

  fun setPartIndex(index: Int, startPlaying: Boolean = false) {
    if (playlist.isEmpty()) return

    val boundedIndex = index.coerceIn(0, playlist.size - 1)
    _state.update { it.copy(currentPartIndex = boundedIndex) }

    val partConfig = playlist[boundedIndex]

    ignoreTimerCallbacksTemp = true
    applyConfig(partConfig)
    ignoreTimerCallbacksTemp = false

    if (state.value.isPlaying) {
      restartInternal(resetTimer = true)
    } else if (startPlaying) {
      start()
    } else if (appSettings.resetTimerOnStop) {
      resetTimerState()
    }

    _engineEvent.tryEmit(EngineEvent.PartChange(boundedIndex, partConfig))
  }

  private fun handlePartTransition() {
    if (playlist.isNotEmpty() && state.value.currentPartIndex < playlist.size - 1) {
      setPartIndex(state.value.currentPartIndex + 1, startPlaying = true)
    } else if (isPlaylistLooped) {
      setPartIndex(0, startPlaying = true)
    } else {
      stop(resetTimer = true)
      _engineEvent.tryEmit(EngineEvent.PlaylistEnd)
    }
  }

  // Playback control

  fun start() {
    if (state.value.isPlaying) return
    resetHandlers()

    tickIndex = 0
    tickIndexPoly = 0
    _state.update {
      it.copy(
        isPlaying = true,
        isCountingIn = config.isCountInActive,
        isMuted = false
      )
    }
    updateHapticPossible()

    if (config.isMuteActive) muteCountDown = calculateMuteCount(false)

    if (config.isCountInActive) {
      timerHandler?.postDelayed({
        _state.update { it.copy(isCountingIn = false) }
        startLifeCycleHandlers()
      }, getCountInInterval())
    } else {
      startLifeCycleHandlers()
    }

    startTicks()
  }

  fun stop(resetTimer: Boolean = appSettings.resetTimerOnStop) {
    if (!state.value.isPlaying) return

    if (resetTimer || isTimerFinished()) {
      resetTimerState()
    } else {
      timerPreviousElapsed = (state.value.timerProgress * getTimerInterval()).toLong()
    }
    elapsedPrevious = state.value.elapsedTime

    _state.update { it.copy(isPlaying = false, isCountingIn = false) }
    updateHapticPossible()

    audioProvider.scheduleDelayedStop()
    removeHandlerCallbacks()

    tickThread?.quit()
    callbackThread?.quit()
    tickThread = null
    callbackThread = null

    flashlightProvider?.cleanup()
  }

  fun savePlayingState() {
    tempPlaying = state.value.isPlaying
  }

  fun restorePlayingState() {
    if (tempPlaying) start() else stop(false)
  }

  fun resetTimerNow() {
    if (config.isTimerActive) restartInternal(resetTimer = true)
  }

  fun resetElapsed() {
    elapsedPrevious = 0
    elapsedStartTime = clock.uptimeMillis()
    _state.update { it.copy(elapsedTime = 0) }
    updateElapsedHandler(true)
  }

  fun setUpLatencyCalibration() {
    val calibrationConfig = MetronomeConfig(
      tempo = 80, countIn = 0, incrementalAmount = 0, timerDuration = 0, mutePlay = 0
    )
    applyConfig(calibrationConfig)
    updateSettings(appSettings.copy(beatMode = BeatMode.ALL))
    audioProvider.isMuted = false
    audioProvider.gain = 0
    start()
  }

  fun destroy() {
    stop()
    flashlightProvider?.cleanup()
  }

  // internal state management

  private fun restartInternal(resetTimer: Boolean) {
    if (!state.value.isPlaying) return

    if (resetTimer || isTimerFinished()) {
      resetTimerState()
    } else {
      timerPreviousElapsed = (state.value.timerProgress * getTimerInterval()).toLong()
    }
    elapsedPrevious = state.value.elapsedTime

    removeHandlerCallbacks()
    resetHandlers()

    val countInTickIndex = if (config.usePolyrhythm) {
      (config.countIn * config.beatsCount).toLong()
    } else {
      (config.countIn * config.beatsCount * config.subdivisionsCount).toLong()
    }

    tickIndex = if (config.isCountInActive) countInTickIndex else 0L
    tickIndexPoly = (config.countIn * config.subdivisionsCount).toLong()

    _state.update { it.copy(isCountingIn = false, isMuted = false) }
    if (config.isMuteActive) muteCountDown = calculateMuteCount(false)

    startLifeCycleHandlers()
    startTicks()
  }

  private fun resetTimerState() {
    timerPreviousElapsed = 0L
    _state.update {
      it.copy(timerProgress = 0f, timerBarIndex = 0, timerBeatIndex = 0, timerSubIndex = 0)
    }
  }

  private fun isTimerFinished(): Boolean {
    return if (config.timerUnit == TimingUnit.BARS) {
      state.value.timerBarIndex >= config.timerDuration - 1 &&
          state.value.timerBeatIndex >= config.beatsCount - 1 &&
          state.value.timerSubIndex >= config.subdivisionsCount - 1
    } else {
      state.value.timerProgress >= 1f
    }
  }

  private fun startLifeCycleHandlers() {
    val now = clock.uptimeMillis()
    elapsedStartTime = now
    timerStartTime = now
    updateIncrementalHandler()
    updateElapsedHandler(false)
    updateTimerHandler(false)
    updateMuteHandler()
  }

  private fun updateHapticPossible() {
    _state.update {
      it.copy(isHapticPossible = !state.value.isPlaying || appSettings.beatMode == BeatMode.SOUND)
    }
  }

  private fun resetHandlers() {
    tickThread?.quit()
    callbackThread?.quit()

    val tLooper = tickLooper ?: HandlerThread("metronome_ticks").also {
      it.start(); tickThread = it
    }.looper
    tickHandler = Handler(tLooper)

    val cLooper = callbackLooper ?: HandlerThread("metronome_callbacks").also {
      it.start(); callbackThread = it
    }.looper
    timerHandler = Handler(cLooper)
    incrementalHandler = Handler(cLooper)
    muteHandler = Handler(cLooper)
    elapsedHandler = Handler(cLooper)
  }

  private fun removeHandlerCallbacks() {
    tickHandler?.removeCallbacksAndMessages(null)
    timerHandler?.removeCallbacksAndMessages(null)
    incrementalHandler?.removeCallbacksAndMessages(null)
    muteHandler?.removeCallbacksAndMessages(null)
    elapsedHandler?.removeCallbacksAndMessages(null)
  }

  // ticks and rhythm

  private fun startTicks() {
    val now = clock.uptimeMillis()
    nextScheduleTime = now
    nextPolyScheduleTime = now

    val tickRunnablePoly = object : Runnable {
      override fun run() {
        if (!state.value.isPlaying) return

        val subdivisionPoly = (tickIndexPoly % config.subdivisionsCount).toInt() + 1
        val type = if (subdivisionPoly == 1) {
          TickType.BEAT_SUB_MUTED
        } else {
          config.subdivisions[subdivisionPoly - 1]
        }

        var muted = state.value.isMuted
        if (config.isMuteActive && config.muteUnit == TimingUnit.BEATS) {
          muted = random.nextInt(100) < config.muteMute
        }

        val tick = Tick(
          tickIndexPoly, 1, subdivisionPoly, type, muted, true
        )

        if (subdivisionPoly < config.subdivisionsCount) {
          val step = getInterval() * config.beatsCount / config.subdivisionsCount
          nextPolyScheduleTime += step
          tickHandler?.postAtTime(this, nextPolyScheduleTime)
        }

        performTickPoly(tick)
        audioProvider.playTick(tick.type, tick.isMuted)
        tickIndexPoly++
      }
    }

    val tickRunnable = object : Runnable {
      override fun run() {
        if (!state.value.isPlaying) return

        val beatIndex =
          if (config.usePolyrhythm) tickIndex else tickIndex / config.subdivisionsCount
        val isBeat = config.usePolyrhythm || (tickIndex % config.subdivisionsCount) == 0L
        val isFirstBeat = isBeat && (beatIndex % config.beatsCount) == 0L
        val isCountIn = (beatIndex / config.beatsCount) < config.countIn

        if (isFirstBeat && config.isMuteActive
          && config.muteUnit == TimingUnit.BARS && !isCountIn
        ) {
          if (muteCountDown > 0) {
            muteCountDown--
          } else {
            _state.update { it.copy(isMuted = !it.isMuted) }
            muteCountDown =
              (calculateMuteCount(state.value.isMuted) - 1).coerceAtLeast(0)
          }
        }

        val beat = ((beatIndex % config.beatsCount).toInt() + 1)
        val subdivision =
          if (config.usePolyrhythm) 1 else (tickIndex % config.subdivisionsCount).toInt() + 1
        val type = if (config.usePolyrhythm) {
          val beatIndex = (tickIndex % config.beatsCount).toInt()
          if (beatIndex == 0 && config.isFirstSubdivisionMuted) {
            TickType.BEAT_SUB_MUTED
          } else {
            config.beats[beatIndex]
          }
        } else {
          if (isBeat) {
            if (config.isFirstSubdivisionMuted) TickType.BEAT_SUB_MUTED else config.beats[beat - 1]
          } else config.subdivisions[subdivision - 1]
        }

        var muted = state.value.isMuted
        if (config.isMuteActive && config.muteUnit == TimingUnit.BEATS) {
          muted = random.nextInt(100) < config.muteMute
        }

        val tick = Tick(tickIndex, beat, subdivision, type, muted, false)
        val scheduledTime = nextScheduleTime

        val currentInterval =
          if (config.usePolyrhythm) getInterval() else getInterval() / config.subdivisionsCount
        nextScheduleTime += currentInterval

        if (tick.beat == 1 && tick.subdivision == 1) {
          nextPolyScheduleTime = nextScheduleTime - currentInterval
        }

        tickHandler?.postAtTime(this, nextScheduleTime)
        if (tick.beat == 1 && config.usePolyrhythm) tickHandler?.post(tickRunnablePoly)

        if (performTick(tick, scheduledTime)) {
          audioProvider.playTick(tick.type, tick.isMuted)
          tickIndex++
        }
      }
    }

    audioProvider.play()
    tickHandler?.post(tickRunnable)
  }

  private fun performTick(tick: Tick, scheduledTime: Long): Boolean {
    val beatIndex = if (config.usePolyrhythm) tickIndex else tickIndex / config.subdivisionsCount
    val barIndexNoCountIn = (beatIndex / config.beatsCount) - config.countIn
    val isCountIn = barIndexNoCountIn < 0

    val isBeat = tick.subdivision == 1
    val isFirstBeat = isBeat && (beatIndex % config.beatsCount) == 0L

    if (config.isTimerActive && config.timerUnit == TimingUnit.BARS && !isCountIn) {
      if (barIndexNoCountIn > 0 || !isFirstBeat) {
        _state.update { s ->
          var newBar = s.timerBarIndex
          var newBeat = s.timerBeatIndex
          var newSub = s.timerSubIndex

          if (isFirstBeat) newBar++
          if (isBeat) newBeat = (newBeat + 1) % config.beatsCount
          newSub = (newSub + 1) % config.subdivisionsCount

          val barInterval = getInterval() * config.beatsCount
          val subInterval = getInterval() / config.subdivisionsCount
          val progressInterval =
            newBar * barInterval + newBeat * getInterval() + newSub * subInterval

          s.copy(
            timerBarIndex = newBar,
            timerBeatIndex = newBeat,
            timerSubIndex = newSub,
            timerProgress = (progressInterval / getTimerInterval().toFloat()).coerceIn(0f, 1f)
          )
        }
      }

      if (state.value.timerBarIndex >= config.timerDuration) {
        _state.update { it.copy(timerProgress = 1f) }
        handlePartTransition()
        return false
      }
    }

    if (isFirstBeat && config.isIncrementalActive
      && config.incrementalUnit == TimingUnit.BARS && !isCountIn
    ) {
      if (barIndexNoCountIn > 0 && barIndexNoCountIn % config.incrementalInterval == 0L) {
        val limit = config.incrementalLimit
        val upperLimit = if (limit != 0) limit else MetronomeConstants.TEMPO_MAX
        val lowerLimit = if (limit != 0) limit else MetronomeConstants.TEMPO_MIN

        if (config.incrementalIncrease && config.tempo + config.incrementalAmount <= upperLimit) {
          changeTempo(config.incrementalAmount)
        } else if (!config.incrementalIncrease
          && config.tempo - config.incrementalAmount >= lowerLimit
        ) {
          changeTempo(-config.incrementalAmount)
        }
      }
    }

    tickHandler?.postAtTime(
      {
        _preTickEvent.tryEmit(tick)
      }, scheduledTime + (appSettings.latency - MetronomeConstants.BEAT_ANIM_OFFSET)
        .coerceAtLeast(0)
    )

    tickHandler?.postAtTime({
      if (appSettings.beatMode != BeatMode.SOUND && !tick.isMuted) {
        when (tick.type) {
          TickType.STRONG -> hapticProvider?.heavyClick(false)
          TickType.SUB -> hapticProvider?.tick(false)
          else -> hapticProvider?.click(false)
        }
      }
      if (appSettings.flashlight != FlashStrength.OFF && !tick.isMuted) {
        val strength = if (appSettings.flashlight == FlashStrength.STRONG) 0.8f else 0.15f
        when (tick.type) {
          TickType.STRONG -> flashlightProvider?.flash(100, strength)
          TickType.SUB, TickType.MUTED, TickType.BEAT_SUB_MUTED -> {}
          else -> flashlightProvider?.flash(20, strength)
        }
      }
      _tickEvent.tryEmit(tick)
    }, scheduledTime + appSettings.latency)

    return true
  }

  private fun performTickPoly(tick: Tick) {
    tickHandler?.postAtTime(
      {
        _preTickEvent.tryEmit(tick)
      },
      nextPolyScheduleTime
          + (appSettings.latency - MetronomeConstants.BEAT_ANIM_OFFSET).coerceAtLeast(
        0
      )
    )

    tickHandler?.postAtTime({
      var shouldVibrate = appSettings.beatMode != BeatMode.SOUND && !tick.isMuted
      if (shouldVibrate) {
        val product = (tick.subdivision - 1).toLong() * config.beatsCount
        if (product % config.subdivisionsCount == 0L) shouldVibrate = false
      }
      if (shouldVibrate) {
        when (tick.type) {
          TickType.STRONG -> hapticProvider?.heavyClick(true)
          TickType.SUB -> hapticProvider?.tick(true)
          else -> hapticProvider?.click(true)
        }
      }
      _tickEvent.tryEmit(tick)
    }, nextPolyScheduleTime + appSettings.latency)
  }

  // callbacks and automations

  private fun changeTempo(amount: Int) {
    val newTempo =
      (config.tempo + amount).coerceIn(MetronomeConstants.TEMPO_MIN, MetronomeConstants.TEMPO_MAX)
    if (newTempo != config.tempo) {
      config = config.copy(tempo = newTempo)
      _state.update { it.copy(tempo = newTempo) }
      _engineEvent.tryEmit(EngineEvent.AutoTempoChange(newTempo))
    }
  }

  private fun updateTimerHandler(startAtFirstBeat: Boolean) {
    timerHandler?.removeCallbacksAndMessages(null)
    if (!config.isTimerActive || ignoreTimerCallbacksTemp) return

    if (isTimerFinished()) {
      _state.update { it.copy(timerProgress = 0f) }
    } else if (startAtFirstBeat) {
      val barInterval = getInterval() * config.beatsCount
      val total = getTimerInterval()
      val progress =
        if (total > 0) state.value.timerBarIndex * barInterval / total.toFloat() else 0f
      _state.update {
        it.copy(
          timerProgress = progress.coerceIn(0f, 1f),
          timerBeatIndex = 0, timerSubIndex = 0
        )
      }
    }

    if (config.timerUnit != TimingUnit.BARS) {
      timerStartTime = clock.uptimeMillis()

      timerHandler?.postDelayed({
        handlePartTransition()
      }, getTimerIntervalRemaining())

      timerHandler?.post(object : Runnable {
        override fun run() {
          if (state.value.isPlaying) {
            val elapsed = clock.uptimeMillis() - timerStartTime + timerPreviousElapsed
            val total = getTimerInterval()
            _state.update { it.copy(timerProgress = (elapsed.toFloat() / total).coerceIn(0f, 1f)) }
            timerHandler?.postDelayed(this, 1000)
          }
        }
      })
    }
  }

  private fun updateIncrementalHandler() {
    incrementalHandler?.removeCallbacksAndMessages(null)
    if (!config.isIncrementalActive || config.incrementalUnit == TimingUnit.BARS) return

    val unitMillis = if (config.incrementalUnit == TimingUnit.SECONDS) 1000L else 60000L
    val intervalMillis = unitMillis * config.incrementalInterval

    incrementalHandler?.postDelayed(object : Runnable {
      override fun run() {
        val limit = config.incrementalLimit
        val upperLimit = if (limit != 0) limit else MetronomeConstants.TEMPO_MAX
        val lowerLimit = if (limit != 0) limit else MetronomeConstants.TEMPO_MIN

        if (config.incrementalIncrease && config.tempo + config.incrementalAmount <= upperLimit) {
          changeTempo(config.incrementalAmount)
        } else if (!config.incrementalIncrease
          && config.tempo - config.incrementalAmount >= lowerLimit
        ) {
          changeTempo(-config.incrementalAmount)
        }
        incrementalHandler?.postDelayed(this, intervalMillis)
      }
    }, intervalMillis)
  }

  private fun updateElapsedHandler(reset: Boolean) {
    elapsedHandler?.removeCallbacksAndMessages(null)
    if (!appSettings.showElapsed) return

    if (reset) elapsedPrevious = 0
    elapsedHandler?.post(object : Runnable {
      override fun run() {
        if (state.value.isPlaying) {
          val time = clock.uptimeMillis() - elapsedStartTime + elapsedPrevious
          _state.update { it.copy(elapsedTime = time) }
          elapsedHandler?.postDelayed(this, 1000)
        }
      }
    })
  }

  private fun updateMuteHandler() {
    muteHandler?.removeCallbacksAndMessages(null)
    _state.update { it.copy(isMuted = false) }
    if (config.isMuteActive && config.muteUnit == TimingUnit.SECONDS) {
      muteHandler?.postDelayed(object : Runnable {
        override fun run() {
          _state.update { it.copy(isMuted = !it.isMuted) }
          muteHandler?.postDelayed(
            this, calculateMuteCount(state.value.isMuted) * 1000L
          )
        }
      }, calculateMuteCount(false) * 1000L)
    }
  }

  fun calculateMuteCount(isMuted: Boolean): Int {
    val count = if (isMuted) config.muteMute else config.mutePlay
    return if (config.muteRandom) random.nextInt(count + 1) else count
  }

  fun getInterval(): Long = 1000L * 60 / maxOf(config.tempo, 1)

  fun getCountInInterval(): Long = getInterval() * config.beatsCount * config.countIn

  fun getTimerInterval(): Long {
    val factor = when (config.timerUnit) {
      TimingUnit.SECONDS -> 1000L
      TimingUnit.MINUTES -> 60000L
      else -> getInterval() * config.beatsCount
    }
    return factor * config.timerDuration
  }

  fun getTimerIntervalRemaining(): Long =
    (getTimerInterval() * (1 - state.value.timerProgress)).toLong().coerceAtLeast(0)

  fun warmUpAudio() = audioProvider.warmUp()
}
