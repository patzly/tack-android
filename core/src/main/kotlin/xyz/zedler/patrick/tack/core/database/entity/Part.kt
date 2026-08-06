package xyz.zedler.patrick.tack.core.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.Default
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import java.util.UUID

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
  var countIn: Int = Default.COUNT_IN,
  var tempo: Int = Default.TEMPO,
  var beats: String = Default.BEATS.joinToString(","),
  var subdivisions: String = Default.SUBDIVISIONS.joinToString(","),
  var usePolyrhythm: Boolean = Default.USE_POLYRHYTHM,
  var incrementalAmount: Int = Default.INCREMENTAL_AMOUNT,
  var incrementalInterval: Int = Default.INCREMENTAL_INTERVAL,
  var incrementalLimit: Int = Default.INCREMENTAL_LIMIT,
  var incrementalUnit: String = Default.INCREMENTAL_UNIT,
  var incrementalIncrease: Boolean = Default.INCREMENTAL_INCREASE,
  var timerDuration: Int = Default.TIMER_DURATION,
  var timerUnit: String = Default.TIMER_UNIT,
  var mutePlay: Int = Default.MUTE_PLAY,
  var muteMute: Int = Default.MUTE_MUTE,
  var muteUnit: String = Default.MUTE_UNIT,
  var muteRandom: Boolean = Default.MUTE_RANDOM,
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
