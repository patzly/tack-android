package xyz.zedler.patrick.tack.core.metronome

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.zedler.patrick.tack.core.audio.AudioProvider
import xyz.zedler.patrick.tack.core.audio.Constants.TickType
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.Unit
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.core.model.Tick
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

  private var config = MetronomeConfig()
  private val random = Random()

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

  private var countInStartTime: Long = 0
  private var timerStartTime: Long = 0
  private var elapsedStartTime: Long = 0
  private var elapsedPrevious: Long = 0

  private var muteCountDown = 0
  private var latency: Long = 0
  private var beatMode: String = "all"

  fun setConfig(newConfig: MetronomeConfig) {
    val oldConfig = config
    config = newConfig
    _state.update { it.copy(tempo = config.tempo) }

    if (state.value.isPlaying) {
      if (oldConfig.timerDuration != config.timerDuration || oldConfig.timerUnit != config.timerUnit) {
        updateTimerHandler(false)
      }
      if (oldConfig.incrementalAmount != config.incrementalAmount || oldConfig.incrementalUnit != config.incrementalUnit) {
        updateIncrementalHandler()
      }
      if (oldConfig.mutePlay != config.mutePlay || oldConfig.muteMute != config.muteMute || oldConfig.muteUnit != config.muteUnit) {
        updateMuteHandler()
      }
    }
  }

  fun setLatency(ms: Long) {
    latency = ms
  }

  fun setBeatMode(mode: String) {
    beatMode = mode
    audioProvider.isMuted = mode == "vibration"
  }

  fun start() {
    if (state.value.isPlaying) return

    resetHandlers()

    tickIndex = 0
    tickIndexPoly = 0
    _state.update {
      it.copy(
        isPlaying = true,
        isCountingIn = config.isCountInActive,
        isMuted = false,
        timerProgress = 0f,
        timerBarIndex = 0,
        timerBeatIndex = 0,
        timerSubIndex = 0
      )
    }

    if (config.isMuteActive) {
      muteCountDown = calculateMuteCount(false)
    }

    val now = clock.uptimeMillis()
    countInStartTime = now

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

  private fun startLifeCycleHandlers() {
    val now = clock.uptimeMillis()
    elapsedStartTime = now
    timerStartTime = now
    updateIncrementalHandler()
    updateElapsedHandler()
    updateTimerHandler(true)
    updateMuteHandler()
  }

  fun stop() {
    if (!state.value.isPlaying) return

    _state.update { it.copy(isPlaying = false, isCountingIn = false) }

    audioProvider.scheduleDelayedStop()
    removeHandlerCallbacks()

    tickThread?.quit()
    callbackThread?.quit()
    tickThread = null
    callbackThread = null
  }

  private fun resetHandlers() {
    tickThread?.quit()
    callbackThread?.quit()

    val tLooper = tickLooper ?: HandlerThread("metronome_ticks").also {
      tickThread = it
      it.start()
    }.looper
    tickHandler = Handler(tLooper)

    val cLooper = callbackLooper ?: HandlerThread("metronome_callbacks").also {
      callbackThread = it
      it.start()
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

  private fun startTicks() {
    val now = clock.uptimeMillis()
    nextScheduleTime = now
    nextPolyScheduleTime = now

    val tickRunnablePoly = object : Runnable {
      override fun run() {
        if (!state.value.isPlaying) return

        val subdivisionPoly = (tickIndexPoly % config.subdivisionsCount).toInt() + 1
        val isBeat = subdivisionPoly == 1
        val type = if (isBeat) TickType.BEAT_SUB_MUTED else config.subdivisions[subdivisionPoly - 1]

        var muted = state.value.isMuted
        if (config.isMuteActive && config.muteUnit == Unit.BEATS) {
          muted = random.nextInt(100) < config.muteMute
        }

        val tick = Tick(tickIndexPoly, 1, subdivisionPoly, type, muted, true)

        if (subdivisionPoly < config.subdivisionsCount) {
          val barInterval = getInterval() * config.beatsCount
          val step = barInterval / config.subdivisionsCount
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
        val barIndex = beatIndex / config.beatsCount
        val isCountIn = barIndex < config.countIn

        if (isFirstBeat && config.isMuteActive && config.muteUnit == Unit.BARS && !isCountIn) {
          if (muteCountDown > 0) {
            muteCountDown--
          } else {
            _state.update { it.copy(isMuted = !it.isMuted) }
            muteCountDown = (calculateMuteCount(state.value.isMuted) - 1).coerceAtLeast(0)
          }
        }

        val beat = ((beatIndex % config.beatsCount).toInt() + 1)
        val subdivision =
          if (config.usePolyrhythm) 1 else (tickIndex % config.subdivisionsCount).toInt() + 1

        val type = if (config.usePolyrhythm) {
          val bIndex = (tickIndex % config.beatsCount).toInt()
          if (bIndex == 0 && config.isFirstSubdivisionMuted) TickType.BEAT_SUB_MUTED
          else config.beats[bIndex]
        } else {
          if (isBeat) {
            if (config.isFirstSubdivisionMuted) TickType.BEAT_SUB_MUTED
            else config.beats[beat - 1]
          } else {
            config.subdivisions[subdivision - 1]
          }
        }

        var muted = state.value.isMuted
        if (config.isMuteActive && config.muteUnit == Unit.BEATS) {
          muted = random.nextInt(100) < config.muteMute
        }

        val tick = Tick(tickIndex, beat, subdivision, type, muted, false)

        val currentInterval =
          if (config.usePolyrhythm) getInterval() else getInterval() / config.subdivisionsCount
        nextScheduleTime += currentInterval

        if (tick.beat == 1 && tick.subdivision == 1) {
          nextPolyScheduleTime = nextScheduleTime - currentInterval
        }

        tickHandler?.postAtTime(this, nextScheduleTime)

        if (tick.beat == 1 && config.usePolyrhythm) {
          tickHandler?.post(tickRunnablePoly)
        }

        if (performTick(tick)) {
          audioProvider.playTick(tick.type, tick.isMuted)
          tickIndex++
        }
      }
    }

    audioProvider.play()
    tickHandler?.post(tickRunnable)
  }

  private fun performTick(tick: Tick): Boolean {
    val beatIndex = if (config.usePolyrhythm) tickIndex else tickIndex / config.subdivisionsCount
    val barIndex = beatIndex / config.beatsCount
    val barIndexNoCountIn = barIndex - config.countIn
    val isCountIn = barIndex < config.countIn

    val isBeat = tick.subdivision == 1
    val isFirstBeat = isBeat && (beatIndex % config.beatsCount) == 0L

    if (config.isTimerActive && config.timerUnit == Unit.BARS && !isCountIn) {
      val isFirstBeatInFirstBar = barIndexNoCountIn == 0L && isFirstBeat
      if (barIndexNoCountIn > 0 || !isFirstBeatInFirstBar) {
        _state.update { s ->
          var newBar = s.timerBarIndex
          var newBeat = s.timerBeatIndex
          var newSub = s.timerSubIndex

          if (isFirstBeat) newBar++
          if (isBeat) {
            newBeat++
            if (newBeat >= config.beatsCount) newBeat = 0
          }
          newSub++
          if (newSub >= config.subdivisionsCount) newSub = 0

          val barInterval = getInterval() * config.beatsCount
          val subInterval = getInterval() / config.subdivisionsCount
          val progressInterval =
            newBar * barInterval + newBeat * getInterval() + newSub * subInterval
          val progress = progressInterval / getTimerInterval().toFloat()

          s.copy(
            timerBarIndex = newBar,
            timerBeatIndex = newBeat,
            timerSubIndex = newSub,
            timerProgress = progress.coerceIn(0f, 1f)
          )
        }
      }

      if (state.value.timerBarIndex >= config.timerDuration) {
        stop()
        return false
      }
    }

    if (isFirstBeat && config.isIncrementalActive && config.incrementalUnit == Unit.BARS && !isCountIn) {
      if (barIndexNoCountIn > 0 && barIndexNoCountIn % config.incrementalInterval == 0L) {
        changeTempo(config.incrementalAmount)
      }
    }

    // Haptics and Flashlight with latency
    tickHandler?.postAtTime({
      if (beatMode != "sound" && !tick.isMuted) {
        when (tick.type) {
          TickType.STRONG -> hapticProvider?.heavyClick(false)
          TickType.SUB -> hapticProvider?.tick(false)
          else -> hapticProvider?.click(false)
        }
      }
      if (flashlightProvider != null && !tick.isMuted) {
        // Implementation similar to legacy
      }
    }, nextScheduleTime + latency)

    return true
  }

  private fun performTickPoly(tick: Tick) {
    tickHandler?.postAtTime({
      var shouldVibrate = beatMode != "sound" && !tick.isMuted
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
    }, nextPolyScheduleTime + latency)
  }

  private fun changeTempo(amount: Int) {
    val newTempo = (config.tempo + if (config.incrementalIncrease) amount else -amount)
      .coerceIn(MetronomeConstants.TEMPO_MIN, MetronomeConstants.TEMPO_MAX)
    if (newTempo != config.tempo) {
      config = config.copy(tempo = newTempo)
      _state.update { it.copy(tempo = newTempo) }
    }
  }

  private fun updateTimerHandler(reset: Boolean) {
    timerHandler?.removeCallbacksAndMessages(null)
    if (!config.isTimerActive) return

    if (config.timerUnit != Unit.BARS) {
      timerHandler?.postDelayed({
        stop()
      }, getTimerIntervalRemaining())

      timerHandler?.post(object : Runnable {
        override fun run() {
          if (state.value.isPlaying) {
            updateTimerProgress()
            timerHandler?.postDelayed(this, 100)
          }
        }
      })
    }
  }

  private fun updateTimerProgress() {
    val now = clock.uptimeMillis()
    val elapsed = now - timerStartTime
    val total = getTimerInterval()
    _state.update { it.copy(timerProgress = (elapsed.toFloat() / total).coerceIn(0f, 1f)) }
  }

  private fun updateIncrementalHandler() {
    incrementalHandler?.removeCallbacksAndMessages(null)
    if (!config.isIncrementalActive || config.incrementalUnit == Unit.BARS) return

    val factor = if (config.incrementalUnit == Unit.SECONDS) 1000L else 60000L
    val intervalMillis = factor * config.incrementalInterval

    incrementalHandler?.postDelayed(object : Runnable {
      override fun run() {
        changeTempo(config.incrementalAmount)
        incrementalHandler?.postDelayed(this, intervalMillis)
      }
    }, intervalMillis)
  }

  private fun updateElapsedHandler() {
    elapsedHandler?.removeCallbacksAndMessages(null)
    elapsedHandler?.post(object : Runnable {
      override fun run() {
        if (state.value.isPlaying) {
          val now = clock.uptimeMillis()
          val time = now - elapsedStartTime + elapsedPrevious
          _state.update { it.copy(elapsedTime = time) }
          elapsedHandler?.postDelayed(this, 1000)
        }
      }
    })
  }

  private fun updateMuteHandler() {
    muteHandler?.removeCallbacksAndMessages(null)
    _state.update { it.copy(isMuted = false) }
    if (config.isMuteActive && config.muteUnit == Unit.SECONDS) {
      muteHandler?.postDelayed(object : Runnable {
        override fun run() {
          _state.update { it.copy(isMuted = !it.isMuted) }
          muteHandler?.postDelayed(this, calculateMuteCount(state.value.isMuted) * 1000L)
        }
      }, calculateMuteCount(false) * 1000L)
    }
  }

  private fun calculateMuteCount(isMuted: Boolean): Int {
    val count = if (isMuted) config.muteMute else config.mutePlay
    return if (config.muteRandom) random.nextInt(count + 1) else count
  }

  private fun getInterval(): Long = 1000L * 60 / maxOf(config.tempo, 1)

  private fun getCountInInterval(): Long = getInterval() * config.beatsCount * config.countIn

  private fun getTimerInterval(): Long {
    val factor = when (config.timerUnit) {
      Unit.SECONDS -> 1000L
      Unit.MINUTES -> 60000L
      else -> getInterval() * config.beatsCount
    }
    return factor * config.timerDuration
  }

  private fun getTimerIntervalRemaining(): Long {
    val total = getTimerInterval()
    val elapsed = clock.uptimeMillis() - timerStartTime
    return (total - elapsed).coerceAtLeast(0)
  }

  fun warmUpAudio() = audioProvider.warmUp()
}
