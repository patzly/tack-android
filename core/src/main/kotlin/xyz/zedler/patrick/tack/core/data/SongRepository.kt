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

package xyz.zedler.patrick.tack.core.data

import kotlinx.coroutines.flow.Flow
import xyz.zedler.patrick.tack.core.database.dao.SongDao
import xyz.zedler.patrick.tack.core.database.entity.Part
import xyz.zedler.patrick.tack.core.database.entity.Song
import xyz.zedler.patrick.tack.core.database.relations.SongWithParts

class SongRepository(private val songDao: SongDao) {

  val allSongs: Flow<List<Song>> = songDao.getAllSongsFlow()
  val allSongsWithParts: Flow<List<SongWithParts>> = songDao.getAllSongsWithPartsFlow()

  fun getSongWithParts(songId: String): Flow<SongWithParts?> =
    songDao.getSongWithPartsByIdFlow(songId)

  suspend fun getSongWithPartsAsync(songId: String): SongWithParts? =
    songDao.getSongWithPartsById(songId)

  suspend fun getAllSongsWithPartsAsync(): List<SongWithParts> =
    songDao.getAllSongsWithParts()

  suspend fun insertSong(song: Song) = songDao.insertSong(song)
  suspend fun updateSong(song: Song) = songDao.updateSong(song)
  suspend fun deleteSong(song: Song) = songDao.deleteSong(song)

  suspend fun deleteAllSongs() = songDao.deleteAllSongs()

  suspend fun insertSongsWithParts(songs: List<SongWithParts>) =
    songDao.insertSongsWithParts(songs)

  suspend fun insertPart(part: Part) = songDao.insertPart(part)
  suspend fun updatePart(part: Part) = songDao.updatePart(part)
  suspend fun deletePart(part: Part) = songDao.deletePart(part)

  companion object {
    const val SONG_ID_DEFAULT = "default"
  }
}
