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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.tack.database.SongDatabase;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;

public class SongViewModel extends AndroidViewModel {

  private final SongDatabase db;
  private final LiveData<List<SongWithParts>> allSongsWithParts;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  public SongViewModel(Application application) {
    super(application);

    db = SongDatabase.getInstance(application);

    allSongsWithParts = db.songDao().getAllSongsWithPartsLive();
  }

  public LiveData<List<SongWithParts>> getAllSongsWithPartsLive() {
    return allSongsWithParts;
  }

  public void insertSongsWithParts(
      List<SongWithParts> songWithParts, @NonNull Runnable runOnInserted
  ) {
    executorService.execute(() -> {
      for (SongWithParts songWithPart : songWithParts) {
        db.songDao().insertSong(songWithPart.getSong());
        db.songDao().insertParts(songWithPart.getParts());
      }
      runOnInserted.run();
    });
  }

  public void fetchSongWithParts(
      @Nullable String songId, @NonNull OnSongWithPartsFetchedListener listener
  ) {
    if (songId != null) {
      executorService.execute(() -> {
        SongWithParts song = db.songDao().getSongWithPartsById(songId);
        listener.onSongWithPartsFetched(song);
      });
    } else {
      listener.onSongWithPartsFetched(null);
    }
  }

  public void fetchAllSongsWithParts(@NonNull OnSongsWithPartsFetchedListener listener) {
    executorService.execute(() -> {
      List<SongWithParts> songs = db.songDao().getAllSongsWithParts();
      listener.onSongsWithPartsFetched(songs);
    });
  }

  public interface OnSongWithPartsFetchedListener {
    void onSongWithPartsFetched(@Nullable SongWithParts songWithParts);
  }

  public interface OnSongsWithPartsFetchedListener {
    void onSongsWithPartsFetched(List<SongWithParts> songsWithParts);
  }

  public LiveData<List<Song>> getAllSongsLive() {
    return db.songDao().getAllSongsLive();
  }

  public void insertSong(Song song) {
    executorService.execute(() -> db.songDao().insertSong(song));
  }

  public void updateSong(Song song) {
    executorService.execute(() -> db.songDao().updateSong(song));
  }

  public void deleteSong(Song song) {
    executorService.execute(() -> db.songDao().deleteSong(song));
  }

  public void deleteSong(Song song, @NonNull Runnable runOnDeleted) {
    executorService.execute(() -> {
      db.songDao().deleteSong(song);
      runOnDeleted.run();
    });
  }

  public void insertPart(Part part) {
    executorService.execute(() -> db.songDao().insertPart(part));
  }

  public void insertParts(List<Part> parts) {
    executorService.execute(() -> db.songDao().insertParts(parts));
  }

  public void updatePart(Part part) {
    executorService.execute(() -> db.songDao().updatePart(part));
  }

  public void deletePart(Part part) {
    executorService.execute(() -> db.songDao().deletePart(part));
  }

  public void deleteParts(List<Part> parts) {
    executorService.execute(() -> db.songDao().deleteParts(parts));
  }

  public void updateSongAndParts(
      Song song, List<Part> partsNew, List<Part> partsOld, @Nullable Runnable runOnUpdated
  ) {
    executorService.execute(() -> {
      db.songDao().updateSong(song);

      for (Part part : partsNew) {
        boolean isNew = true;
        for (Part partSource : partsOld) {
          if (part.getId().equals(partSource.getId())) {
            isNew = false;
            break;
          }
        }
        if (isNew) {
          db.songDao().insertPart(part);
        } else {
          db.songDao().updatePart(part);
        }
      }
      for (Part part : partsOld) {
        boolean isDeleted = true;
        for (Part partResult : partsNew) {
          if (part.getId().equals(partResult.getId())) {
            isDeleted = false;
            break;
          }
        }
        if (isDeleted) {
          db.songDao().deletePart(part);
        }
      }

      if (runOnUpdated != null) {
        runOnUpdated.run();
      }
    });
  }

  public void deleteAll() {
    executorService.execute(db::clearAllTables);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    executorService.shutdown();
  }
}