package xyz.zedler.patrick.tack.core.model

import xyz.zedler.patrick.tack.core.audio.Constants.TickType
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.Default
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.Unit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class MetronomeConfig(
  val tempo: Int = Default.TEMPO,
  val beats: List<String> = Default.BEATS,
  val subdivisions: List<String> = Default.SUBDIVISIONS,
  val usePolyrhythm: Boolean = Default.USE_POLYRHYTHM,
  val countIn: Int = Default.COUNT_IN,
  val incrementalAmount: Int = Default.INCREMENTAL_AMOUNT,
  val incrementalInterval: Int = Default.INCREMENTAL_INTERVAL,
  val incrementalLimit: Int = Default.INCREMENTAL_LIMIT,
  val incrementalUnit: String = Default.INCREMENTAL_UNIT,
  val incrementalIncrease: Boolean = Default.INCREMENTAL_INCREASE,
  val timerDuration: Int = Default.TIMER_DURATION,
  val timerUnit: String = Default.TIMER_UNIT,
  val mutePlay: Int = Default.MUTE_PLAY,
  val muteMute: Int = Default.MUTE_MUTE,
  val muteUnit: String = Default.MUTE_UNIT,
  val muteRandom: Boolean = Default.MUTE_RANDOM,
) {
  val isCountInActive: Boolean get() = countIn > 0
  val isIncrementalActive: Boolean get() = incrementalAmount > 0
  val isTimerActive: Boolean get() = timerDuration > 0
  val isMuteActive: Boolean get() = if (muteUnit == Unit.BEATS) muteMute > 0 else mutePlay > 0

  val beatsCount: Int get() = beats.size
  val subdivisionsCount: Int get() = subdivisions.size

  val isFirstSubdivisionMuted: Boolean
    get() = subdivisions.isNotEmpty() && subdivisions[0] == TickType.BEAT_SUB_MUTED

  fun getSnappedMuteMute(value: Int): Int {
    val min = if (muteUnit == Unit.BEATS) {
      MetronomeConstants.MUTE_MUTE_MIN_BEATS
    } else {
      MetronomeConstants.MUTE_MUTE_MIN
    }
    val max = if (muteUnit == Unit.BEATS) {
      MetronomeConstants.MUTE_MUTE_MAX_BEATS
    } else {
      MetronomeConstants.MUTE_MUTE_MAX
    }
    val stepSize = if (muteUnit == Unit.BEATS) {
      MetronomeConstants.MUTE_MUTE_STEP_SIZE_BEATS
    } else {
      MetronomeConstants.MUTE_MUTE_STEP_SIZE
    }
    val maxStepIndex = (max - min) / stepSize
    val desiredStepIndex = ceil((value - min).toDouble() / stepSize).toInt()
    val clampedStepIndex = max(0, min(maxStepIndex, desiredStepIndex))
    return min + clampedStepIndex * stepSize
  }

  // Helper for Swing patterns (from legacy)
  companion object {
    fun swing3() = listOf(TickType.BEAT_SUB, TickType.MUTED, TickType.NORMAL)
    fun swing5() =
      listOf(TickType.BEAT_SUB, TickType.MUTED, TickType.MUTED, TickType.NORMAL, TickType.MUTED)

    fun swing7() = listOf(
      TickType.BEAT_SUB,
      TickType.MUTED,
      TickType.MUTED,
      TickType.MUTED,
      TickType.NORMAL,
      TickType.MUTED,
      TickType.MUTED
    )
  }
}
