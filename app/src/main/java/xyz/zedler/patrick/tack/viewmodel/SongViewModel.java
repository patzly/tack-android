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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.tack.database.SongDatabase;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.model.MetronomeConfig;

public class SongViewModel extends AndroidViewModel {

  private final SongDatabase db;
  private final LiveData<List<SongWithParts>> allSongsWithParts;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  public SongViewModel(Application application) {
    super(application);

    db = SongDatabase.getInstance(application);

    executorService.execute(() -> {
      /*Song song1 = new Song("120 bpm");
      db.songDao().insertSong(song1);

      MetronomeConfig config1 = new MetronomeConfig();
      config1.setTempo(120);
      Part part1 = new Part(null, song1.getId(), 0, config1);
      db.songDao().insertPart(part1);

      Song song2 = new Song("80 bpm");
      db.songDao().insertSong(song2);

      MetronomeConfig config2 = new MetronomeConfig();
      config2.setTempo(80);
      config2.setTimerDuration(4);
      Part part2 = new Part("Adagio", song2.getId(), 0, config2);
      db.songDao().insertPart(part2);

      MetronomeConfig config3 = new MetronomeConfig();
      config3.setTempo(120);
      config3.setTimerDuration(8);
      Part part3 = new Part("Allegro",  song2.getId(), 1, config3);
      db.songDao().insertPart(part3);

      Song song3 = new Song("Eine kleine Nachtmusik");
      db.songDao().insertSong(song3);

      MetronomeConfig config4 = new MetronomeConfig();
      config4.setTempo(120);
      config4.setTimerDuration(20);
      Part part4 = new Part("Allegro", song3.getId(), 0, config4);
      db.songDao().insertPart(part4);

      MetronomeConfig config5 = new MetronomeConfig();
      config5.setTempo(90);
      config5.setTimerDuration(40);
      Part part5 = new Part("Andante", song3.getId(), 1, config5);
      db.songDao().insertPart(part5);

      MetronomeConfig config6 = new MetronomeConfig();
      config6.setTempo(110);
      config6.setTimerDuration(60);
      Part part6 = new Part("Menuetto", song3.getId(), 2, config6);
      db.songDao().insertPart(part6);

      MetronomeConfig config7 = new MetronomeConfig();
      config7.setTempo(120);
      config7.setTimerDuration(30);
      Part part7 = new Part("Keine Angabe", song3.getId(), 3, config7);
      db.songDao().insertPart(part7);*/
    });

    allSongsWithParts = db.songDao().getAllSongsWithParts();
  }

  public LiveData<List<SongWithParts>> getAllSongsWithParts() {
    return allSongsWithParts;
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

  public interface OnSongWithPartsFetchedListener {
    void onSongWithPartsFetched(@Nullable SongWithParts songWithParts);
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

  public void updateSong(Song song, @Nullable Runnable runOnUpdated) {
    executorService.execute(() -> {
      db.songDao().updateSong(song);
      if (runOnUpdated != null) {
        runOnUpdated.run();
      }
    });
  }

  public void deleteSong(Song song) {
    executorService.execute(() -> db.songDao().deleteSong(song));
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

  @Override
  protected void onCleared() {
    super.onCleared();
    executorService.shutdown();
  }
}