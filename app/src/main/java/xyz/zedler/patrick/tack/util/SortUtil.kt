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

@file:JvmName("SortUtil")

package xyz.zedler.patrick.tack.util

import xyz.zedler.patrick.tack.Constants.SONGS_ORDER
import xyz.zedler.patrick.tack.database.entity.Part
import xyz.zedler.patrick.tack.database.relations.SongWithParts

fun sortSongsWithParts(songsWithParts: MutableList<SongWithParts>, sortOrder: Int) {
  when (sortOrder) {
    SONGS_ORDER.NAME_ASC -> {
      songsWithParts.sortWith(
        compareBy(nullsLast(String.CASE_INSENSITIVE_ORDER)) {
          it.song.name
        }
      )
    }

    SONGS_ORDER.LAST_PLAYED_ASC -> {
      songsWithParts.sortByDescending { it.song.lastPlayed }
    }

    SONGS_ORDER.MOST_PLAYED_ASC -> {
      songsWithParts.sortByDescending { it.song.playCount }
    }
  }
}

fun sortPartsByIndex(parts: MutableList<Part>) {
  parts.sortBy { it.partIndex }
}
