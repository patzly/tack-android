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
        db.execSQL("DROP INDEX IF EXISTS index_songs_id")
      }
    }

    fun getInstance(context: Context): SongDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          SongDatabase::class.java,
          "song_database"
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
          .build().also { INSTANCE = it }
      }
    }
  }
}
