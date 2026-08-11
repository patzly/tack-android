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

package xyz.zedler.patrick.tack.core.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import java.util.UUID

private val default = MetronomeConfig()

@Parcelize
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
  val id: String = UUID.randomUUID().toString(),
  var name: String? = null,
  var songId: String = "",
  var partIndex: Int = 0,
  var countIn: Int = default.countIn,
  var tempo: Int = default.tempo,
  var beats: String = default.beats.joinToString(","),
  var subdivisions: String = default.subdivisions.joinToString(","),
  var usePolyrhythm: Boolean = default.usePolyrhythm,
  var incrementalAmount: Int = default.incrementalAmount,
  var incrementalInterval: Int = default.incrementalInterval,
  var incrementalLimit: Int = default.incrementalLimit,
  var incrementalUnit: String = default.incrementalUnit,
  var incrementalIncrease: Boolean = default.incrementalIncrease,
  var timerDuration: Int = default.timerDuration,
  var timerUnit: String = default.timerUnit,
  var mutePlay: Int = default.mutePlay,
  var muteMute: Int = default.muteMute,
  var muteUnit: String = default.muteUnit,
  var muteRandom: Boolean = default.muteRandom,
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

  fun toConfig(): MetronomeConfig {
    return MetronomeConfig(
      tempo = tempo,
      beats = beats.split(","),
      subdivisions = subdivisions.split(","),
      usePolyrhythm = usePolyrhythm,
      countIn = countIn,
      incrementalAmount = incrementalAmount,
      incrementalInterval = incrementalInterval,
      incrementalLimit = incrementalLimit,
      incrementalUnit = incrementalUnit,
      incrementalIncrease = incrementalIncrease,
      timerDuration = timerDuration,
      timerUnit = timerUnit,
      mutePlay = mutePlay,
      muteMute = muteMute,
      muteUnit = muteUnit,
      muteRandom = muteRandom
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
    return toConfig() == config
  }
}
