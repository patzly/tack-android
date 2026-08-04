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

package xyz.zedler.patrick.tack.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.Constants.UNIT
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.database.entity.Song
import xyz.zedler.patrick.tack.metronome.MetronomeEngine

data class SongWithParts(
  @Embedded
  val song: Song,

  @Relation(
    parentColumn = "id",
    entityColumn = "songId"
  )
  val parts: List<Part>
) {

  fun getDurationString(): String {
    var seconds = 0f
    parts.forEach {
      when (it.timerUnit) {
        UNIT.SECONDS -> seconds += it.timerDuration
        UNIT.MINUTES -> seconds += 60 * it.timerDuration
        else -> { // Bars
          val incrementalAmount = it.incrementalAmount
          if (incrementalAmount > 0) {
            // complex duration calculation with incremental tempo changes
            val incrementalUnit = it.incrementalUnit
            val interval = it.incrementalInterval
            if (incrementalUnit == UNIT.BARS) {
              var tempo = it.tempo
              for (i in 0 until it.timerDuration) {
                val factor = (60f / tempo) * it.beatsCount
                seconds += factor * interval
                if (i % interval == 0) {
                  val incrementalLimit = it.incrementalLimit
                  if (it.incrementalIncrease) {
                    val upperLimit = if (incrementalLimit != 0) {
                      incrementalLimit
                    } else {
                      Constants.TEMPO_MAX
                    }
                    if (tempo + incrementalAmount <= upperLimit) {
                      tempo += incrementalAmount
                    }
                  } else {
                    val lowerLimit = if (incrementalLimit != 0) {
                      incrementalLimit
                    } else {
                      Constants.TEMPO_MIN
                    }
                    if (tempo - incrementalAmount >= lowerLimit) {
                      tempo -= incrementalAmount
                    }
                  }
                }
              }
            } else {
              // TODO: implement incremental tempo changes for seconds and minutes
              val factor = (60f / it.tempo) * it.beatsCount
              seconds += factor * it.timerDuration
            }
          } else {
            val factor = (60f / it.tempo) * it.beatsCount
            seconds += factor * it.timerDuration
          }
        }
      }
    }
    return MetronomeEngine.getTimeStringFromSeconds(seconds.toInt(), false)
  }
}
