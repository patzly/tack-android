package xyz.zedler.patrick.tack.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import xyz.zedler.patrick.tack.core.database.entity.Part
import xyz.zedler.patrick.tack.core.database.entity.Song
import xyz.zedler.patrick.tack.core.database.relations.SongWithParts

@Dao
interface SongDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSong(song: Song)

  @Update
  suspend fun updateSong(song: Song)

  @Delete
  suspend fun deleteSong(song: Song)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPart(part: Part)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertParts(parts: List<Part>)

  @Update
  suspend fun updatePart(part: Part)

  @Delete
  suspend fun deletePart(part: Part)

  @Delete
  suspend fun deleteParts(parts: List<Part>)

  @Transaction
  @Query("SELECT * FROM songs WHERE id = :songId")
  suspend fun getSongWithPartsById(songId: String): SongWithParts?

  @Transaction
  @Query("SELECT * FROM songs WHERE id = :songId")
  fun getSongWithPartsByIdFlow(songId: String): Flow<SongWithParts?>

  @Transaction
  @Query("SELECT * FROM songs")
  suspend fun getAllSongsWithParts(): List<SongWithParts>

  @Transaction
  @Query("SELECT * FROM songs")
  fun getAllSongsWithPartsFlow(): Flow<List<SongWithParts>>

  @Query("SELECT * FROM songs")
  suspend fun getAllSongs(): List<Song>

  @Query("SELECT * FROM songs")
  fun getAllSongsFlow(): Flow<List<Song>>
}
