package xyz.zedler.patrick.tack.core.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import xyz.zedler.patrick.tack.core.database.entity.Part
import xyz.zedler.patrick.tack.core.database.entity.Song
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.Unit
import xyz.zedler.patrick.tack.core.util.TimeUtil

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
        Unit.SECONDS -> seconds += it.timerDuration
        Unit.MINUTES -> seconds += 60 * it.timerDuration
        else -> { // Bars
          val incrementalAmount = it.incrementalAmount
          if (incrementalAmount > 0) {
            val incrementalUnit = it.incrementalUnit
            val interval = it.incrementalInterval
            if (incrementalUnit == Unit.BARS) {
              var tempo = it.tempo
              for (i in 0 until it.timerDuration) {
                val factor = (60f / tempo) * it.beats.split(",").size
                seconds += factor * interval
                if (i % interval == 0) {
                  val incrementalLimit = it.incrementalLimit
                  if (it.incrementalIncrease) {
                    val upperLimit = if (incrementalLimit != 0) {
                      incrementalLimit
                    } else {
                      MetronomeConstants.TEMPO_MAX
                    }
                    if (tempo + incrementalAmount <= upperLimit) {
                      tempo += incrementalAmount
                    }
                  } else {
                    val lowerLimit = if (incrementalLimit != 0) {
                      incrementalLimit
                    } else {
                      MetronomeConstants.TEMPO_MIN
                    }
                    if (tempo - incrementalAmount >= lowerLimit) {
                      tempo -= incrementalAmount
                    }
                  }
                }
              }
            } else {
              val factor = (60f / it.tempo) * it.beats.split(",").size
              seconds += factor * it.timerDuration
            }
          } else {
            val factor = (60f / it.tempo) * it.beats.split(",").size
            seconds += factor * it.timerDuration
          }
        }
      }
    }
    return TimeUtil.getTimeStringFromSeconds(seconds.toInt(), false)
  }
}
