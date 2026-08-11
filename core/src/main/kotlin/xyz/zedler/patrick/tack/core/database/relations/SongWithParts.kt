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

package xyz.zedler.patrick.tack.core.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import xyz.zedler.patrick.tack.core.database.entity.Part
import xyz.zedler.patrick.tack.core.database.entity.Song
import xyz.zedler.patrick.tack.core.model.MetronomeConstants
import xyz.zedler.patrick.tack.core.model.TimingUnit
import xyz.zedler.patrick.tack.core.util.TimeUtil

data class SongWithParts(
  @Embedded val song: Song,
  @Relation(
    parentColumn = "id",
    entityColumn = "songId"
  )
  val parts: List<Part>
) {
  fun getTotalDuration(): Int {
    var seconds = 0
    parts.forEach {
      when (it.timerUnit) {
        TimingUnit.SECONDS.key -> seconds += it.timerDuration
        TimingUnit.MINUTES.key -> seconds += 60 * it.timerDuration
        else -> { // Bars
          val incrementalAmount = it.incrementalAmount
          if (incrementalAmount != 0) {
            val incrementalUnit = it.incrementalUnit
            val interval = it.incrementalInterval
            if (incrementalUnit == TimingUnit.BARS.key) {
              var tempo = it.tempo
              for (i in 0 until it.timerDuration) {
                if (i > 0 && i % interval == 0) {
                  tempo = (tempo + incrementalAmount).coerceIn(
                    MetronomeConstants.TEMPO_MIN,
                    MetronomeConstants.TEMPO_MAX
                  )
                }
                seconds += (60.0 / tempo * it.beats.split(",").size).toInt()
              }
            } else {
              // TODO: implement time based incremental
            }
          } else {
            seconds += (60.0 / it.tempo * it.beats.split(",").size
                * it.timerDuration).toInt()
          }
        }
      }
    }
    return seconds
  }

  fun getTotalDurationString(): String {
    return TimeUtil.getTimeStringFromSeconds(getTotalDuration(), forceHours = false)
  }
}
