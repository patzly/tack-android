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
