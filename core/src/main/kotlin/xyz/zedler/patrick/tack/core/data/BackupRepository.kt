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

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import xyz.zedler.patrick.tack.core.database.relations.SongWithParts

class BackupRepository(
  private val context: Context,
  private val songRepository: SongRepository
) {
  suspend fun exportLibrary(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val songs = songRepository.getAllSongsWithPartsAsync().filter {
        it.song.id != SongRepository.SONG_ID_DEFAULT
      }
      val jsonString = json.encodeToString(songs)
      context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.write(jsonString.toByteArray())
      } ?: throw IllegalStateException("Could not open output stream")
    }
  }

  suspend fun importLibrary(
    uri: Uri,
    formatDuplicateName: (originalName: String, counter: Int) -> String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
      val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("Could not open input stream")

      inputStream.use { stream ->
        val jsonString = stream.bufferedReader().readText()
        val songsWithParts: List<SongWithParts> = json.decodeFromString(jsonString)

        val existingSongs = songRepository.getAllSongsWithPartsAsync()
        val nameCountMap = mutableMapOf<String, Int>()
        val idNameMap = mutableMapOf<String, String>()

        for (existing in existingSongs) {
          idNameMap[existing.song.id] = existing.song.name ?: ""
          val name = existing.song.name ?: continue
          if (name.isNotEmpty()) {
            nameCountMap[name] = (nameCountMap[name] ?: 0) + 1
          }
        }

        for (songWithParts in songsWithParts) {
          val songId = songWithParts.song.id
          if (idNameMap.containsKey(songId)) {
            songWithParts.song.name = idNameMap[songId]
          } else {
            val originalName = songWithParts.song.name ?: ""
            var newName = originalName
            var counter = nameCountMap[originalName] ?: 0
            if (counter > 0) {
              do {
                newName = formatDuplicateName(originalName, counter)
                counter++
              } while (nameCountMap.containsKey(newName))
            }
            songWithParts.song.name = newName
            nameCountMap[newName] = 1
          }
        }

        songRepository.insertSongsWithParts(songsWithParts)
      }
    }
  }

  companion object {
    private val json = Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
      prettyPrint = true
    }
  }
}