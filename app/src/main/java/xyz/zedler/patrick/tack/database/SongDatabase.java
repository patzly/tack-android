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

package xyz.zedler.patrick.tack.database;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import xyz.zedler.patrick.tack.database.dao.SongDao;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;

@Database(entities = {Song.class, Part.class}, version = 2)
public abstract class SongDatabase extends RoomDatabase {

  public abstract SongDao songDao();

  private static volatile SongDatabase INSTANCE;

  private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
      database.execSQL("ALTER TABLE parts ADD COLUMN usePolyrhythm INTEGER NOT NULL DEFAULT 0");
    }
  };

  public static SongDatabase getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (SongDatabase.class) {
        if (INSTANCE == null) {
          INSTANCE = Room.databaseBuilder(
              context.getApplicationContext(),
              SongDatabase.class,
              "song_database"
          )
          .addMigrations(MIGRATION_1_2)
          .build();
        }
      }
    }
    return INSTANCE;
  }
}