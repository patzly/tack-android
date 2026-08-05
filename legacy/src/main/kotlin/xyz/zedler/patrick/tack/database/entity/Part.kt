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

package xyz.zedler.patrick.tack.database.entity

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.UNIT
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.metronome.MetronomeEngine
import xyz.zedler.patrick.tack.model.MetronomeConfig
import java.util.UUID

@kotlinx.parcelize.Parcelize
@Entity(
  tableName = "parts",
  foreignKeys = [ForeignKey(
    entity = Song::class,
    parentColumns = ["id"],
    childColumns = ["songId"],
    onDelete = ForeignKey.CASCADE
  )],
  indices = [Index("songId")]
)
data class Part(
  @PrimaryKey
  var id: String = UUID.randomUUID().toString(),
  var name: String? = null,
  var songId: String = "",
  var partIndex: Int = 0,
  var countIn: Int = DEF.COUNT_IN,
  var tempo: Int = DEF.TEMPO,
  var beats: String? = DEF.BEATS,
  var subdivisions: String? = DEF.SUBDIVISIONS,
  var usePolyrhythm: Boolean = DEF.USE_POLYRHYTHM,
  var incrementalAmount: Int = DEF.INCREMENTAL_AMOUNT,
  var incrementalInterval: Int = DEF.INCREMENTAL_INTERVAL,
  var incrementalLimit: Int = DEF.INCREMENTAL_LIMIT,
  var incrementalUnit: String? = DEF.INCREMENTAL_UNIT,
  var incrementalIncrease: Boolean = DEF.INCREMENTAL_INCREASE,
  var timerDuration: Int = DEF.TIMER_DURATION,
  var timerUnit: String? = DEF.TIMER_UNIT,
  var mutePlay: Int = DEF.MUTE_PLAY,
  var muteMute: Int = DEF.MUTE_MUTE,
  var muteUnit: String? = DEF.MUTE_UNIT,
  var muteRandom: Boolean = DEF.MUTE_RANDOM,
) : Parcelable {

  companion object {
    fun fromConfig(
      name: String?,
      songId: String,
      partIndex: Int,
      config: MetronomeConfig
    ): Part {
      return Part(
        id = UUID.randomUUID().toString(),
        name = name,
        songId = songId,
        partIndex = partIndex,
        countIn = config.countIn,
        tempo = config.tempo,
        beats = config.beats.joinToString(","),
        subdivisions = config.subdivisions.joinToString(","),
        usePolyrhythm = config.usePolyrhythm,
        incrementalAmount = config.incrementalAmount,
        incrementalInterval = config.incrementalInterval,
        incrementalLimit = config.incrementalLimit,
        incrementalUnit = config.incrementalUnit,
        incrementalIncrease = config.incrementalIncrease,
        timerDuration = config.timerDuration,
        timerUnit = config.timerUnit,
        mutePlay = config.mutePlay,
        muteMute = config.muteMute,
        muteUnit = config.muteUnit,
        muteRandom = config.muteRandom
      )
    }
  }

  fun setRandomId() {
    id = UUID.randomUUID().toString()
  }

  val beatsCount: Int
    get() = (beats ?: DEF.BEATS).split(",").size

  fun getTimerDurationString(context: Context): String {
    if (timerDuration == 0) {
      return context.getString(R.string.label_part_no_duration)
    }
    return when (timerUnit) {
      UNIT.SECONDS, UNIT.MINUTES -> {
        var seconds = timerDuration
        if (timerUnit == UNIT.MINUTES) {
          seconds *= 60
        }
        MetronomeEngine.getTimeStringFromSeconds(seconds, false)
      }

      else -> context.resources.getQuantityString(
        R.plurals.options_unit_bars, timerDuration, timerDuration
      )
    }
  }

  fun toConfig(): MetronomeConfig {
    return MetronomeConfig(
      countIn,
      tempo,
      (beats ?: DEF.BEATS).split(",").toTypedArray(),
      (subdivisions ?: DEF.SUBDIVISIONS).split(",").toTypedArray(),
      usePolyrhythm,
      incrementalAmount,
      incrementalInterval,
      incrementalLimit,
      incrementalUnit ?: DEF.INCREMENTAL_UNIT,
      incrementalIncrease,
      timerDuration,
      timerUnit ?: DEF.TIMER_UNIT,
      mutePlay,
      muteMute,
      muteUnit ?: DEF.MUTE_UNIT,
      muteRandom
    )
  }

  fun setConfig(config: MetronomeConfig) {
    countIn = config.countIn
    tempo = config.tempo
    beats = config.beats.joinToString(",")
    subdivisions = config.subdivisions.joinToString(",")
    usePolyrhythm = config.usePolyrhythm
    incrementalAmount = config.incrementalAmount
    incrementalInterval = config.incrementalInterval
    incrementalLimit = config.incrementalLimit
    incrementalUnit = config.incrementalUnit
    incrementalIncrease = config.incrementalIncrease
    timerDuration = config.timerDuration
    timerUnit = config.timerUnit
    mutePlay = config.mutePlay
    muteMute = config.muteMute
    muteUnit = config.muteUnit
    muteRandom = config.muteRandom
  }

  fun equalsConfig(config: MetronomeConfig): Boolean {
    return toConfig().equals(config)
  }
}
