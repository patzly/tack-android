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

package xyz.zedler.patrick.tack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.zedler.patrick.tack.database.SongDatabase
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.database.entity.Song
import xyz.zedler.patrick.tack.database.relations.SongWithParts

class SongViewModel(application: Application) : AndroidViewModel(application) {

  private val db: SongDatabase = SongDatabase.getInstance(application)
  val allSongsWithPartsLive: LiveData<List<SongWithParts>> = db.songDao().getAllSongsWithPartsLive()

  fun insertSongsWithParts(
    songWithParts: List<SongWithParts>,
    runOnInserted: Runnable
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      for (songWithPart in songWithParts) {
        db.songDao().insertSong(songWithPart.song)
        db.songDao().insertParts(songWithPart.parts)
      }
      withContext(Dispatchers.Main) {
        runOnInserted.run()
      }
    }
  }

  fun insertSongWithParts(
    song: Song,
    parts: List<Part>,
    runOnInserted: Runnable? = null
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().insertSong(song)
      db.songDao().insertParts(parts)
      runOnInserted?.let {
        withContext(Dispatchers.Main) {
          it.run()
        }
      }
    }
  }

  fun fetchSongWithParts(
    songId: String?,
    listener: OnSongWithPartsFetchedListener
  ) {
    if (songId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        val song = db.songDao().getSongWithPartsById(songId)
        withContext(Dispatchers.Main) {
          listener.onSongWithPartsFetched(song)
        }
      }
    } else {
      listener.onSongWithPartsFetched(null)
    }
  }

  fun fetchAllSongsWithParts(listener: OnSongsWithPartsFetchedListener) {
    viewModelScope.launch(Dispatchers.IO) {
      val songs = db.songDao().getAllSongsWithParts()
      withContext(Dispatchers.Main) {
        listener.onSongsWithPartsFetched(songs)
      }
    }
  }

  fun interface OnSongWithPartsFetchedListener {
    fun onSongWithPartsFetched(songWithParts: SongWithParts?)
  }

  fun interface OnSongsWithPartsFetchedListener {
    fun onSongsWithPartsFetched(songsWithParts: List<SongWithParts>)
  }

  fun getAllSongsLive(): LiveData<List<Song>> = db.songDao().getAllSongsLive()

  fun insertSong(song: Song) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().insertSong(song)
    }
  }

  fun updateSong(song: Song) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().updateSong(song)
    }
  }

  fun deleteSong(song: Song) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().deleteSong(song)
    }
  }

  fun deleteSong(song: Song, runOnDeleted: Runnable) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().deleteSong(song)
      withContext(Dispatchers.Main) {
        runOnDeleted.run()
      }
    }
  }

  fun insertPart(part: Part) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().insertPart(part)
    }
  }

  fun insertParts(parts: List<Part>) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().insertParts(parts)
    }
  }

  fun updatePart(part: Part) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().updatePart(part)
    }
  }

  fun deletePart(part: Part) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().deletePart(part)
    }
  }

  fun deleteParts(parts: List<Part>) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().deleteParts(parts)
    }
  }

  fun updateSongAndParts(
    song: Song,
    partsNew: List<Part>,
    partsOld: List<Part>,
    runOnUpdated: Runnable?
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      db.songDao().updateSong(song)

      for (part in partsNew) {
        val isNew = partsOld.none { it.id == part.id }
        if (isNew) {
          db.songDao().insertPart(part)
        } else {
          db.songDao().updatePart(part)
        }
      }
      for (part in partsOld) {
        val isDeleted = partsNew.none { it.id == part.id }
        if (isDeleted) {
          db.songDao().deletePart(part)
        }
      }

      runOnUpdated?.let {
        withContext(Dispatchers.Main) {
          it.run()
        }
      }
    }
  }

  fun deleteAll() {
    viewModelScope.launch(Dispatchers.IO) {
      db.clearAllTables()
    }
  }
}
