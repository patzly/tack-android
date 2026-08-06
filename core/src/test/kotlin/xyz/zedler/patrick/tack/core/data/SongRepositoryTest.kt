package xyz.zedler.patrick.tack.core.data

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import xyz.zedler.patrick.tack.core.database.dao.SongDao
import xyz.zedler.patrick.tack.core.database.entity.Song

class SongRepositoryTest {

  private lateinit var repository: SongRepository
  private lateinit var songDao: SongDao

  @Before
  fun setup() {
    songDao = mockk(relaxed = true)
    repository = SongRepository(songDao)
  }

  @Test
  fun `test insert song delegates to dao`() = runTest {
    val song = Song(name = "Test Song")
    repository.insertSong(song)
    coVerify { songDao.insertSong(song) }
  }

  @Test
  fun `test delete song delegates to dao`() = runTest {
    val song = Song(name = "Test Song")
    repository.deleteSong(song)
    coVerify { songDao.deleteSong(song) }
  }

  @Test
  fun `test update song delegates to dao`() = runTest {
    val song = Song(name = "Test Song")
    repository.updateSong(song)
    coVerify { songDao.updateSong(song) }
  }
}
