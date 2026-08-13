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
