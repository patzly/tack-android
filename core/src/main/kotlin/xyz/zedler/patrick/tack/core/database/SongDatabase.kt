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

package xyz.zedler.patrick.tack.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import xyz.zedler.patrick.tack.core.database.dao.SongDao
import xyz.zedler.patrick.tack.core.database.entity.Part
import xyz.zedler.patrick.tack.core.database.entity.Song

@Database(
  entities = [Song::class, Part::class],
  version = 4,
  exportSchema = true
)
abstract class SongDatabase : RoomDatabase() {

  abstract fun songDao(): SongDao

  companion object {
    @Volatile
    private var INSTANCE: SongDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE parts ADD COLUMN usePolyrhythm INTEGER NOT NULL DEFAULT 0")
      }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE songs ADD COLUMN speed INTEGER NOT NULL DEFAULT 100")
      }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS `index_songs_id`")

        // create temporary table "parts_new" with new schema
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `parts_new` (
            `id` TEXT NOT NULL, 
            `name` TEXT, 
            `songId` TEXT NOT NULL, 
            `partIndex` INTEGER NOT NULL, 
            `countIn` INTEGER NOT NULL, 
            `tempo` INTEGER NOT NULL, 
            `beats` TEXT NOT NULL, 
            `subdivisions` TEXT NOT NULL, 
            `usePolyrhythm` INTEGER NOT NULL, 
            `incrementalAmount` INTEGER NOT NULL, 
            `incrementalInterval` INTEGER NOT NULL, 
            `incrementalLimit` INTEGER NOT NULL, 
            `incrementalUnit` TEXT NOT NULL, 
            `incrementalIncrease` INTEGER NOT NULL, 
            `timerDuration` INTEGER NOT NULL, 
            `timerUnit` TEXT NOT NULL, 
            `mutePlay` INTEGER NOT NULL, 
            `muteMute` INTEGER NOT NULL, 
            `muteUnit` TEXT NOT NULL, 
            `muteRandom` INTEGER NOT NULL, 
            PRIMARY KEY(`id`), 
            FOREIGN KEY(`songId`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
          )
          """.trimIndent()
        )

        // migrate data to new table
        db.execSQL(
          """
          INSERT INTO `parts_new` (
            `id`, `name`, `songId`, `partIndex`, `countIn`, `tempo`, 
            `beats`, `subdivisions`, `usePolyrhythm`, `incrementalAmount`, 
            `incrementalInterval`, `incrementalLimit`, `incrementalUnit`, 
            `incrementalIncrease`, `timerDuration`, `timerUnit`, 
            `mutePlay`, `muteMute`, `muteUnit`, `muteRandom`
          )
          SELECT 
            `id`, `name`, `songId`, `partIndex`, `countIn`, `tempo`, 
            COALESCE(`beats`, ''), 
            COALESCE(`subdivisions`, ''), 
            `usePolyrhythm`, `incrementalAmount`, 
            `incrementalInterval`, `incrementalLimit`, 
            COALESCE(`incrementalUnit`, 'BARS'), 
            `incrementalIncrease`, `timerDuration`, 
            COALESCE(`timerUnit`, 'BARS'), 
            `mutePlay`, `muteMute`, 
            COALESCE(`muteUnit`, 'BARS'), 
            `muteRandom`
          FROM `parts`
          """.trimIndent()
        )

        // remove old table and rename new table
        db.execSQL("DROP TABLE `parts`")
        db.execSQL("ALTER TABLE `parts_new` RENAME TO `parts`")

        // create foreign key index on parts
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_parts_songId` ON `parts` (`songId`)")
      }
    }

    fun getInstance(context: Context): SongDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          SongDatabase::class.java,
          "song_database"
        ).addMigrations(
          MIGRATION_1_2,
          MIGRATION_2_3,
          MIGRATION_3_4
        ).build().also { INSTANCE = it }
      }
    }
  }
}
