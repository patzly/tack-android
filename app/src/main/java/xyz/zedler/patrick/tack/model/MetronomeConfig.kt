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

package xyz.zedler.patrick.tack.model

import android.content.SharedPreferences
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.Constants.TICK_TYPE
import xyz.zedler.patrick.tack.Constants.UNIT
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class MetronomeConfig(
  var countIn: Int = DEF.COUNT_IN,
  var tempo: Int = DEF.TEMPO,
  var beats: Array<String> = DEF.BEATS.split(",").toTypedArray(),
  subdivisions: Array<String> = DEF.SUBDIVISIONS.split(",").toTypedArray(),
  var usePolyrhythm: Boolean = DEF.USE_POLYRHYTHM,
  var incrementalAmount: Int = DEF.INCREMENTAL_AMOUNT,
  var incrementalInterval: Int = DEF.INCREMENTAL_INTERVAL,
  var incrementalLimit: Int = DEF.INCREMENTAL_LIMIT,
  var incrementalUnit: String = DEF.INCREMENTAL_UNIT,
  var incrementalIncrease: Boolean = DEF.INCREMENTAL_INCREASE,
  var timerDuration: Int = DEF.TIMER_DURATION,
  var timerUnit: String = DEF.TIMER_UNIT,
  var mutePlay: Int = DEF.MUTE_PLAY,
  muteMute: Int = DEF.MUTE_MUTE,
  muteUnit: String = DEF.MUTE_UNIT,
  var muteRandom: Boolean = DEF.MUTE_RANDOM,
) {
  var subdivisions: Array<String> = subdivisions
    set(value) {
      field = value
      maybeMigrateOldSubdivision()
    }

  var muteUnit: String = muteUnit
    set(value) {
      val unitPrev = field
      if (unitPrev == value) return
      field = value
      if (unitPrev == UNIT.BEATS) {
        val ratio = (muteMute - Constants.MUTE_MUTE_MIN_BEATS).toFloat() /
            (Constants.MUTE_MUTE_MAX_BEATS - Constants.MUTE_MUTE_MIN_BEATS)
        muteMute = (Constants.MUTE_MUTE_MIN + ratio *
            (Constants.MUTE_MUTE_MAX - Constants.MUTE_MUTE_MIN)).toInt()
      } else {
        val ratio = (muteMute - Constants.MUTE_MUTE_MIN).toFloat() /
            (Constants.MUTE_MUTE_MAX - Constants.MUTE_MUTE_MIN)
        muteMute = (Constants.MUTE_MUTE_MIN_BEATS + ratio *
            (Constants.MUTE_MUTE_MAX_BEATS - Constants.MUTE_MUTE_MIN_BEATS)).toInt()
      }
    }

  var muteMute: Int = muteMute
    set(value) {
      field = getSnappedMuteMute(value)
    }

  init {
    maybeMigrateOldSubdivision()
  }

  constructor(sharedPrefs: SharedPreferences) : this() {
    setToPreferences(sharedPrefs)
  }

  constructor(other: MetronomeConfig) : this() {
    setToConfig(other)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MetronomeConfig) return false

    if (countIn != other.countIn) return false
    if (tempo != other.tempo) return false
    if (!beats.contentEquals(other.beats)) return false
    if (!subdivisions.contentEquals(other.subdivisions)) return false
    if (usePolyrhythm != other.usePolyrhythm) return false
    if (incrementalAmount != other.incrementalAmount) return false
    if (incrementalInterval != other.incrementalInterval) return false
    if (incrementalLimit != other.incrementalLimit) return false
    if (incrementalUnit != other.incrementalUnit) return false
    if (incrementalIncrease != other.incrementalIncrease) return false
    if (timerDuration != other.timerDuration) return false
    if (timerUnit != other.timerUnit) return false
    if (mutePlay != other.mutePlay) return false
    if (muteMute != other.muteMute) return false
    if (muteUnit != other.muteUnit) return false
    if (muteRandom != other.muteRandom) return false

    return true
  }

  override fun hashCode(): Int {
    var result = countIn
    result = 31 * result + tempo
    result = 31 * result + beats.contentHashCode()
    result = 31 * result + subdivisions.contentHashCode()
    result = 31 * result + usePolyrhythm.hashCode()
    result = 31 * result + incrementalAmount
    result = 31 * result + incrementalInterval
    result = 31 * result + incrementalLimit
    result = 31 * result + incrementalUnit.hashCode()
    result = 31 * result + incrementalIncrease.hashCode()
    result = 31 * result + timerDuration
    result = 31 * result + timerUnit.hashCode()
    result = 31 * result + mutePlay
    result = 31 * result + muteMute
    result = 31 * result + muteUnit.hashCode()
    result = 31 * result + muteRandom.hashCode()
    return result
  }

  fun setToConfig(other: MetronomeConfig) {
    this.countIn = other.countIn
    this.tempo = other.tempo
    this.beats = other.beats.copyOf()
    this.subdivisions = other.subdivisions.copyOf()
    this.usePolyrhythm = other.usePolyrhythm
    this.incrementalAmount = other.incrementalAmount
    this.incrementalInterval = other.incrementalInterval
    this.incrementalLimit = other.incrementalLimit
    this.incrementalUnit = other.incrementalUnit
    this.incrementalIncrease = other.incrementalIncrease
    this.timerDuration = other.timerDuration
    this.timerUnit = other.timerUnit
    this.mutePlay = other.mutePlay
    this.muteMute = other.muteMute
    this.muteUnit = other.muteUnit
    this.muteRandom = other.muteRandom
  }

  fun setToPreferences(sharedPrefs: SharedPreferences) {
    countIn = sharedPrefs.getInt(PREF.COUNT_IN, DEF.COUNT_IN)
    tempo = sharedPrefs.getInt(PREF.TEMPO, DEF.TEMPO)
    beats = (sharedPrefs.getString(PREF.BEATS, DEF.BEATS) ?: DEF.BEATS)
      .split(",").toTypedArray()
    subdivisions = (sharedPrefs.getString(PREF.SUBDIVISIONS, DEF.SUBDIVISIONS)
      ?: DEF.SUBDIVISIONS).split(",").toTypedArray()
    usePolyrhythm = sharedPrefs.getBoolean(PREF.USE_POLYRHYTHM, DEF.USE_POLYRHYTHM)
    incrementalAmount = sharedPrefs.getInt(
      PREF.INCREMENTAL_AMOUNT, DEF.INCREMENTAL_AMOUNT
    )
    incrementalInterval = sharedPrefs.getInt(
      PREF.INCREMENTAL_INTERVAL, DEF.INCREMENTAL_INTERVAL
    )
    incrementalLimit = sharedPrefs.getInt(
      PREF.INCREMENTAL_LIMIT, DEF.INCREMENTAL_LIMIT
    )
    incrementalUnit = sharedPrefs.getString(
      PREF.INCREMENTAL_UNIT, DEF.INCREMENTAL_UNIT
    ) ?: DEF.INCREMENTAL_UNIT
    incrementalIncrease = sharedPrefs.getBoolean(
      PREF.INCREMENTAL_INCREASE, DEF.INCREMENTAL_INCREASE
    )
    timerDuration = sharedPrefs.getInt(PREF.TIMER_DURATION, DEF.TIMER_DURATION)
    timerUnit = sharedPrefs.getString(
      PREF.TIMER_UNIT, DEF.TIMER_UNIT
    ) ?: DEF.TIMER_UNIT
    mutePlay = sharedPrefs.getInt(PREF.MUTE_PLAY, DEF.MUTE_PLAY)
    muteMute = sharedPrefs.getInt(PREF.MUTE_MUTE, DEF.MUTE_MUTE)
    muteUnit = sharedPrefs.getString(
      PREF.MUTE_UNIT, DEF.MUTE_UNIT
    ) ?: DEF.MUTE_UNIT
    muteRandom = sharedPrefs.getBoolean(PREF.MUTE_RANDOM, DEF.MUTE_RANDOM)
  }

  fun isCountInActive(): Boolean = countIn > 0

  fun setBeats(beats: String) {
    this.beats = beats.split(",").toTypedArray()
  }

  fun setBeat(beat: Int, tickType: String) {
    beats[beat] = tickType
  }

  fun getBeatsCount(): Int = beats.size

  fun addBeat(): Boolean {
    if (beats.size >= Constants.BEATS_MAX) {
      return false
    }
    this.beats = Array(beats.size + 1) { i ->
      if (i < beats.size) beats[i] else TICK_TYPE.NORMAL
    }
    return true
  }

  fun removeBeat(): Boolean {
    if (beats.size <= 1) {
      return false
    }
    beats = Array(beats.size - 1) { i -> beats[i] }
    return true
  }

  fun setSubdivisions(subdivisions: String) {
    this.subdivisions = subdivisions.split(",").toTypedArray()
  }

  fun setSubdivision(subdivision: Int, tickType: String) {
    subdivisions[subdivision] = tickType
    maybeMigrateOldSubdivision()
  }

  fun getSubdivisionsCount(): Int = subdivisions.size

  fun isSubdivisionActive(): Boolean = subdivisions.size > 1

  fun isFirstSubdivisionMuted(): Boolean {
    return subdivisions.isNotEmpty() && subdivisions[0] == TICK_TYPE.BEAT_SUB_MUTED
  }

  fun addSubdivision(): Boolean {
    if (subdivisions.size >= Constants.SUBS_MAX) {
      return false
    }
    this.subdivisions = Array(subdivisions.size + 1) { i ->
      if (i < subdivisions.size) subdivisions[i] else TICK_TYPE.SUB
    }
    return true
  }

  fun removeSubdivision(): Boolean {
    if (subdivisions.size <= 1) {
      return false
    }
    subdivisions = Array(subdivisions.size - 1) { i -> subdivisions[i] }
    return true
  }

  fun setSwing3() {
    subdivisions = arrayOf(TICK_TYPE.BEAT_SUB, TICK_TYPE.MUTED, TICK_TYPE.NORMAL)
  }

  fun isSwing3(): Boolean {
    val triplet = listOf(TICK_TYPE.BEAT_SUB, TICK_TYPE.MUTED, TICK_TYPE.SUB)
    val tripletAlt = listOf(TICK_TYPE.BEAT_SUB, TICK_TYPE.MUTED, TICK_TYPE.NORMAL)
    val current = subdivisions.toList()
    return current == triplet || current == tripletAlt
  }

  fun setSwing5() {
    subdivisions = arrayOf(
      TICK_TYPE.BEAT_SUB, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL, TICK_TYPE.MUTED
    )
  }

  fun isSwing5(): Boolean {
    val quintuplet = listOf(
      TICK_TYPE.BEAT_SUB, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.SUB, TICK_TYPE.MUTED
    )
    val quintupletAlt = listOf(
      TICK_TYPE.BEAT_SUB, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL, TICK_TYPE.MUTED
    )
    val current = subdivisions.toList()
    return current == quintuplet || current == quintupletAlt
  }

  fun setSwing7() {
    subdivisions = arrayOf(
      TICK_TYPE.BEAT_SUB, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED,
      TICK_TYPE.NORMAL, TICK_TYPE.MUTED, TICK_TYPE.MUTED
    )
  }

  fun isSwing7(): Boolean {
    val septuplet = listOf(
      TICK_TYPE.BEAT_SUB, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED,
      TICK_TYPE.SUB, TICK_TYPE.MUTED, TICK_TYPE.MUTED
    )
    val septupletAlt = listOf(
      TICK_TYPE.BEAT_SUB, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED,
      TICK_TYPE.NORMAL, TICK_TYPE.MUTED, TICK_TYPE.MUTED
    )
    val current = subdivisions.toList()
    return current == septuplet || current == septupletAlt
  }

  fun isSwingActive(): Boolean = isSwing3() || isSwing5() || isSwing7()

  fun isIncrementalActive(): Boolean = incrementalAmount > 0

  fun isTimerActive(): Boolean = timerDuration > 0

  fun isMuteActive(): Boolean {
    return if (muteUnit == UNIT.BEATS) muteMute > 0 else mutePlay > 0
  }

  private fun getSnappedMuteMute(value: Int): Int {
    val min = if (muteUnit == UNIT.BEATS) {
      Constants.MUTE_MUTE_MIN_BEATS
    } else {
      Constants.MUTE_MUTE_MIN
    }
    val max = if (muteUnit == UNIT.BEATS) {
      Constants.MUTE_MUTE_MAX_BEATS
    } else {
      Constants.MUTE_MUTE_MAX
    }
    val stepSize = if (muteUnit == UNIT.BEATS) {
      Constants.MUTE_MUTE_STEP_SIZE_BEATS
    } else {
      Constants.MUTE_MUTE_STEP_SIZE
    }
    val maxStepIndex = (max - min) / stepSize
    val desiredStepIndex = ceil((value - min).toDouble() / stepSize).toInt()
    val clampedStepIndex = max(0, min(maxStepIndex, desiredStepIndex))
    return min + clampedStepIndex * stepSize
  }

  private fun maybeMigrateOldSubdivision() {
    if (subdivisions.isNotEmpty() && subdivisions[0] == TICK_TYPE.MUTED) {
      // Migrate from old muted subdivision type
      subdivisions[0] = TICK_TYPE.BEAT_SUB
    }
  }
}
