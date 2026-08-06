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

  suspend fun insertSong(song: Song) = songDao.insertSong(song)
  suspend fun updateSong(song: Song) = songDao.updateSong(song)
  suspend fun deleteSong(song: Song) = songDao.deleteSong(song)

  suspend fun insertPart(part: Part) = songDao.insertPart(part)
  suspend fun updatePart(part: Part) = songDao.updatePart(part)
  suspend fun deletePart(part: Part) = songDao.deletePart(part)
}
