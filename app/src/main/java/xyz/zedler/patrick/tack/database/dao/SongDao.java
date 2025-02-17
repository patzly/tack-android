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
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.database.dao;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;

@Dao
public interface SongDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertSong(Song song);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  void insertSongs(List<Song> songs);

  @Insert(onConflict = OnConflictStrategy.ABORT)
  void insertPart(Part part);

  @Insert
  void insertParts(List<Part> parts);

  @Transaction
  @Query("SELECT * FROM songs WHERE name = :songName")
  SongWithParts getSongWithPartsByName(String songName);

  @Transaction
  @Query("SELECT * FROM songs")
  LiveData<List<SongWithParts>> getAllSongsWithParts();

  @Query("SELECT * FROM songs")
  LiveData<List<Song>> getAllSongs();

  @Update
  void updateSong(Song song);

  @Update
  void updatePart(Part part);

  @Delete
  void deleteSong(Song song);

  @Delete
  void deletePart(Part part);
}