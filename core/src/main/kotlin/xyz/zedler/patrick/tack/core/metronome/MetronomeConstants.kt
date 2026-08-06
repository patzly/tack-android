package xyz.zedler.patrick.tack.core.metronome

import xyz.zedler.patrick.tack.core.audio.Constants.TickType

object MetronomeConstants {
  const val TEMPO_MIN = 1
  const val TEMPO_MAX = 600
  const val BEATS_MAX = 20
  const val SUBS_MAX = 10
  const val COUNT_IN_MAX = 4

  const val MUTE_MUTE_MIN = 1
  const val MUTE_MUTE_MIN_BEATS = 0
  const val MUTE_MUTE_MAX = 20
  const val MUTE_MUTE_MAX_BEATS = 100
  const val MUTE_MUTE_STEP_SIZE = 1
  const val MUTE_MUTE_STEP_SIZE_BEATS = 5

  object Default {
    const val TEMPO = 120
    const val COUNT_IN = 0
    const val USE_POLYRHYTHM = false
    val BEATS = listOf(
      TickType.STRONG, TickType.NORMAL, TickType.NORMAL, TickType.NORMAL
    )
    val SUBDIVISIONS = listOf(TickType.BEAT_SUB)
    const val INCREMENTAL_AMOUNT = 0
    const val INCREMENTAL_INTERVAL = 1
    const val INCREMENTAL_LIMIT = 0
    const val INCREMENTAL_UNIT = Unit.BARS
    const val INCREMENTAL_INCREASE = true
    const val TIMER_DURATION = 0
    const val TIMER_UNIT = Unit.BARS
    const val MUTE_PLAY = 1
    const val MUTE_MUTE = 0
    const val MUTE_UNIT = Unit.BEATS
    const val MUTE_RANDOM = false
  }

  object Unit {
    const val BEATS = "beats"
    const val BARS = "bars"
    const val SECONDS = "seconds"
    const val MINUTES = "minutes"
  }
}
