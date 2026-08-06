package xyz.zedler.patrick.tack.core.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.UUID

@Parcelize
@Entity(tableName = "songs")
data class Song(
  @PrimaryKey
  val id: String = UUID.randomUUID().toString(),
  var name: String? = null,
  var lastPlayed: Long = 0,
  var playCount: Int = 0,
  var isLooped: Boolean = false,
  var speed: Int = 100,
) : Parcelable {

  fun incrementPlayCount() {
    playCount++
  }

  override fun toString(): String {
    val lastPlayedDate = Date(lastPlayed)
    return "Song{" +
        "id='$id'" +
        ", name='$name'" +
        ", lastPlayed=$lastPlayedDate" +
        ", isLooped=$isLooped" +
        ", speed=$speed" +
        '}'
  }
}
