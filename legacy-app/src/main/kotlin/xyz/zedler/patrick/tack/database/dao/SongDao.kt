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

package xyz.zedler.patrick.tack.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.database.entity.Song
import xyz.zedler.patrick.tack.database.relations.SongWithParts

@Dao
interface SongDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertSong(song: Song)

  @Update
  fun updateSong(song: Song)

  @Delete
  fun deleteSong(song: Song)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertPart(part: Part)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertParts(parts: List<Part>)

  @Update
  fun updatePart(part: Part)

  @Delete
  fun deletePart(part: Part)

  @Delete
  fun deleteParts(parts: List<Part>)

  @Transaction
  @Query("SELECT * FROM songs WHERE id = :songId")
  fun getSongWithPartsById(songId: String): SongWithParts?

  @Transaction
  @Query("SELECT * FROM songs")
  fun getAllSongsWithParts(): List<SongWithParts>

  @Transaction
  @Query("SELECT * FROM songs")
  fun getAllSongsWithPartsLive(): LiveData<List<SongWithParts>>

  @Query("SELECT * FROM songs")
  fun getAllSongs(): List<Song>

  @Query("SELECT * FROM songs")
  fun getAllSongsLive(): LiveData<List<Song>>
}
