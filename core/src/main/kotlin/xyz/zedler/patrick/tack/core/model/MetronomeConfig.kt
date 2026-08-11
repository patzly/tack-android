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

package xyz.zedler.patrick.tack.core.model

import xyz.zedler.patrick.tack.core.audio.Constants.TickType
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.DurationUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class MetronomeConfig(
  val tempo: Int = 120,
  val beats: List<String> = listOf(
    TickType.STRONG, TickType.NORMAL, TickType.NORMAL, TickType.NORMAL
  ),
  val subdivisions: List<String> = listOf(TickType.BEAT_SUB),
  val usePolyrhythm: Boolean = false,
  val countIn: Int = 0,
  val incrementalAmount: Int = 0,
  val incrementalInterval: Int = 1,
  val incrementalLimit: Int = 0,
  val incrementalUnit: String = DurationUnit.BARS,
  val incrementalIncrease: Boolean = true,
  val timerDuration: Int = 0,
  val timerUnit: String = DurationUnit.BARS,
  val mutePlay: Int = 1,
  val muteMute: Int = 0,
  val muteUnit: String = DurationUnit.BEATS,
  val muteRandom: Boolean = false
) {
  val isCountInActive: Boolean get() = countIn > 0
  val isIncrementalActive: Boolean get() = incrementalAmount > 0
  val isTimerActive: Boolean get() = timerDuration > 0
  val isMuteActive: Boolean get() = if (muteUnit == DurationUnit.BEATS) muteMute > 0 else mutePlay > 0

  val beatsCount: Int get() = beats.size
  val subdivisionsCount: Int get() = subdivisions.size

  val isFirstSubdivisionMuted: Boolean
    get() = subdivisions.isNotEmpty() && subdivisions[0] == TickType.BEAT_SUB_MUTED

  fun getSnappedMuteMute(value: Int): Int {
    val min = if (muteUnit == DurationUnit.BEATS) {
      MetronomeConstants.MUTE_MUTE_MIN_BEATS
    } else {
      MetronomeConstants.MUTE_MUTE_MIN
    }
    val max = if (muteUnit == DurationUnit.BEATS) {
      MetronomeConstants.MUTE_MUTE_MAX_BEATS
    } else {
      MetronomeConstants.MUTE_MUTE_MAX
    }
    val stepSize = if (muteUnit == DurationUnit.BEATS) {
      MetronomeConstants.MUTE_MUTE_STEP_SIZE_BEATS
    } else {
      MetronomeConstants.MUTE_MUTE_STEP_SIZE
    }
    val maxStepIndex = (max - min) / stepSize
    val desiredStepIndex = ceil((value - min).toDouble() / stepSize).toInt()
    val clampedStepIndex = max(0, min(maxStepIndex, desiredStepIndex))
    return min + clampedStepIndex * stepSize
  }

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
