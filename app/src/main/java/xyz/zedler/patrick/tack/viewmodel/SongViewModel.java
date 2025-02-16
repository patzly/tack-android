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

package xyz.zedler.patrick.tack.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.tack.database.SongDatabase;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.model.MetronomeConfig;

public class SongViewModel extends AndroidViewModel {

  private final SongDatabase db;
  private final LiveData<List<SongWithParts>> allSongsWithParts;

  public SongViewModel(Application application) {
    super(application);

    db = SongDatabase.getInstance(application);

    Executors.newSingleThreadExecutor().execute(() -> {
      Song song1 = new Song("120 bpm");
      db.songDao().insertSong(song1);

      MetronomeConfig config1 = new MetronomeConfig();
      config1.setTempo(120);
      Part part1 = new Part(null, "120 bpm", config1);
      db.songDao().insertPart(part1);
    });

    allSongsWithParts = db.songDao().getAllSongsWithParts();
  }

  public LiveData<List<SongWithParts>> getAllSongsWithParts() {
    return allSongsWithParts;
  }
}