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

package xyz.zedler.patrick.tack.metronome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.BEAT_MODE;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.FLASHLIGHT;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.SongDatabase;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.model.MetronomeConfig;
import xyz.zedler.patrick.tack.util.FlashlightUtil;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.NotificationUtil;
import xyz.zedler.patrick.tack.util.ShortcutUtil;
import xyz.zedler.patrick.tack.util.SortUtil;
import xyz.zedler.patrick.tack.util.WidgetUtil;

public class MetronomeEngine {

  private static final String TAG = MetronomeEngine.class.getSimpleName();

  private final Context context;
  private final SharedPreferences sharedPrefs;
  private final AudioEngine audioEngine;
  private final HapticUtil hapticUtil;
  private final ShortcutUtil shortcutUtil;
  private final FlashlightUtil flashlightUtil;
  private final Set<MetronomeListener> listeners = Collections.synchronizedSet(new HashSet<>());
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Random random = new Random();
  private final MetronomeConfig config = new MetronomeConfig();
  private final SongDatabase db;
  private HandlerThread tickThread, audioThread, callbackThread;
  private Handler tickHandler, latencyHandler, audioHandler;
  private Handler countInHandler, incrementalHandler, elapsedHandler, timerHandler, muteHandler;
  private SongWithParts currentSongWithParts;
  private String beatMode, currentSongId, keepAwake, flashScreen, flashlight;
  private int currentPartIndex, muteCountDown, songsOrder;
  private int timerBarIndex, timerBeatIndex, timerSubIndex;
  private long tickIndex, tickIndexPoly, latency, countInStartTime, timerStartTime;
  private long elapsedStartTime, elapsedTime, elapsedPrevious;
  private long nextScheduleTime, nextPolyScheduleTime;
  private float timerProgress;
  private boolean playing, tempPlaying, isCountingIn, isMuted;
  private boolean showElapsed, resetTimerOnStop, tempoInputKeyboard, tempoTapInstant;
  private boolean neverStartedWithGain = true;
  private boolean ignoreTimerCallbacksTemp, isSongPickerExpanded;

  public MetronomeEngine(@NonNull Context context) {
    this.context = context;

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    audioEngine = new AudioEngine(context, this::stop);

    hapticUtil = new HapticUtil(context);
    hapticUtil.setIntensity(
        sharedPrefs.getString(PREF.VIBRATION_INTENSITY, DEF.VIBRATION_INTENSITY)
    );

    flashlightUtil = new FlashlightUtil(context);

    shortcutUtil = new ShortcutUtil(context);

    db = SongDatabase.getInstance(context.getApplicationContext());

    resetHandlersIfRequired();
    setToPreferences();
  }

  public void destroy() {
    listeners.clear();
    removeHandlerCallbacks();
    tickThread.quit();
    audioThread.quit();
    callbackThread.quit();
    audioEngine.destroy();
    flashlightUtil.cleanup();
  }

  public void setToPreferences() {
    config.setToPreferences(sharedPrefs);

    latency = sharedPrefs.getLong(PREF.LATENCY, DEF.LATENCY);
    showElapsed = sharedPrefs.getBoolean(PREF.SHOW_ELAPSED, DEF.SHOW_ELAPSED);
    resetTimerOnStop = sharedPrefs.getBoolean(PREF.RESET_TIMER_ON_STOP, DEF.RESET_TIMER_ON_STOP);
    flashScreen = sharedPrefs.getString(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN);
    flashlight = sharedPrefs.getString(PREF.FLASHLIGHT, DEF.FLASHLIGHT);
    keepAwake = sharedPrefs.getString(PREF.KEEP_AWAKE, DEF.KEEP_AWAKE);
    songsOrder = sharedPrefs.getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER);
    tempoInputKeyboard = sharedPrefs.getBoolean(
        PREF.TEMPO_INPUT_KEYBOARD, DEF.TEMPO_INPUT_KEYBOARD
    );
    tempoTapInstant = sharedPrefs.getBoolean(PREF.TEMPO_TAP_INSTANT, DEF.TEMPO_TAP_INSTANT);

    setSound(sharedPrefs.getString(PREF.SOUND, DEF.SOUND));
    setIgnoreFocus(sharedPrefs.getBoolean(PREF.IGNORE_FOCUS, DEF.IGNORE_FOCUS));
    setGain(sharedPrefs.getInt(PREF.GAIN, DEF.GAIN));
    setBeatMode(sharedPrefs.getString(PREF.BEAT_MODE, DEF.BEAT_MODE));
    setCurrentSong(
        sharedPrefs.getString(PREF.SONG_CURRENT_ID, DEF.SONG_CURRENT_ID),
        sharedPrefs.getInt(PREF.PART_CURRENT_INDEX, DEF.PART_CURRENT_INDEX),
        false,
        () -> {
          for (MetronomeListener listener : listeners) {
            listener.onMetronomeConfigChanged();
          }
        }
    );
  }

  public MetronomeConfig getConfig() {
    return config;
  }

  public void setConfig(MetronomeConfig config) {
    setCountIn(config.getCountIn());

    int tempoDiff = config.getTempo() - this.config.getTempo();
    changeTempo(tempoDiff);

    setBeats(config.getBeats());
    setSubdivisions(config.getSubdivisions());
    setUsePolyrhythm(config.usePolyrhythm());

    setIncrementalAmount(config.getIncrementalAmount());
    setIncrementalInterval(config.getIncrementalInterval());
    setIncrementalLimit(config.getIncrementalLimit());
    setIncrementalUnit(config.getIncrementalUnit());
    setIncrementalIncrease(config.isIncrementalIncrease());

    setTimerDuration(config.getTimerDuration());
    setTimerUnit(config.getTimerUnit());

    setMutePlay(config.getMutePlay());
    setMuteMute(config.getMuteMute());
    setMuteUnit(config.getMuteUnit());
    setMuteRandom(config.isMuteRandom());

    maybeUpdateDefaultSong();

    for (MetronomeListener listener : listeners) {
      listener.onMetronomeConfigChanged();
    }
  }

  @Nullable
  public SongWithParts getCurrentSongWithParts() {
    return currentSongWithParts;
  }

  @NonNull
  public String getCurrentSongId() {
    return currentSongId;
  }

  public void setCurrentSong(@NonNull String songId, int partIndex) {
    setCurrentSong(songId, partIndex, false, null);
  }

  public void setCurrentSong(@NonNull String songId, int partIndex, boolean startPlaying) {
    setCurrentSong(songId, partIndex, startPlaying, null);
  }

  public void setCurrentSong(
      @NonNull String songId, int partIndex, boolean startPlaying, Runnable onDone
  ) {
    currentSongId = songId;
    executorService.execute(() -> {
      currentSongWithParts = db.songDao().getSongWithPartsById(songId);
      if (currentSongWithParts != null) {
        sortParts();
        setCurrentPartIndex(partIndex, startPlaying);
      } else if (songId.equals(Constants.SONG_ID_DEFAULT)) {
        // default song not created yet
        Song songDefault = new Song(songId, null, 0, 0, false);
        db.songDao().insertSong(songDefault);
        Part partDefault = new Part(null, songDefault.getId(), 0, config);
        db.songDao().insertPart(partDefault);
        List<Part> parts = new ArrayList<>();
        parts.add(partDefault);
        currentSongWithParts = new SongWithParts(songDefault, parts);
      } else {
        Log.e(TAG, "setCurrentSong: song with id='" + songId + "' not found");
      }
      if (onDone != null) {
        onDone.run();
      }
    });
    sharedPrefs.edit().putString(PREF.SONG_CURRENT_ID, songId).apply();
    if(!isSongPickerExpanded) {
      isSongPickerExpanded = !songId.equals(Constants.SONG_ID_DEFAULT);
    }
  }

  public void reloadCurrentSong() {
    executorService.execute(() -> {
      currentSongWithParts = db.songDao().getSongWithPartsById(currentSongId);
      if (currentSongWithParts != null) {
        sortParts();
        setCurrentPartIndex(currentPartIndex);
      } else {
        Log.e(TAG, "reloadCurrentSong: song with id='" + currentSongId + "' not found");
      }
    });
  }

  public void maybeUpdateDefaultSong() {
    executorService.execute(() -> {
      if (currentSongWithParts != null && currentSongId.equals(Constants.SONG_ID_DEFAULT)) {
        Part part = currentSongWithParts.getParts().get(0);
        if (part.equalsConfig(config)) {
          return;
        }
        part.setConfig(config);
        db.songDao().updatePart(part);
      }
    });
  }

  public void setSongsOrder(int sortOrder) {
    songsOrder = sortOrder;
    sharedPrefs.edit().putInt(PREF.SONGS_ORDER, sortOrder).apply();
  }

  public int getSongsOrder() {
    return songsOrder;
  }

  public void setSongPickerExpanded(boolean songPickerExpanded) {
    isSongPickerExpanded = songPickerExpanded;
  }

  public boolean isSongPickerExpanded() {
    return isSongPickerExpanded;
  }

  private void sortParts() {
    if (currentSongWithParts == null) {
      return;
    }
    SortUtil.sortPartsByIndex(currentSongWithParts.getParts());
  }

  public int getCurrentPartIndex() {
    return currentPartIndex;
  }

  private boolean hasNextPart() {
    return currentSongWithParts != null && currentPartIndex
        < currentSongWithParts.getParts().size() - 1;
  }

  public void setCurrentPartIndex(int index) {
    setCurrentPartIndex(index, false);
  }

  private void setCurrentPartIndex(int index, boolean startPlaying) {
    if (currentSongWithParts == null) {
      Log.e(TAG, "setCurrentPartIndex: song with id='" + currentSongId + "' is null");
      return;
    }
    List<Part> parts = currentSongWithParts.getParts();
    index = Math.max(0, Math.min(index, parts.size() - 1));
    currentPartIndex = index;
    if (!parts.isEmpty()) {
      // ignore timer callbacks temporary
      // else the timer transition would be laggy
      ignoreTimerCallbacksTemp = true;
      setConfig(parts.get(index).toConfig());
      ignoreTimerCallbacksTemp = false;
      if (!isPlaying() && startPlaying) {
        start(false);
      } else {
        restartIfPlaying(true);
      }
      sharedPrefs.edit().putInt(PREF.PART_CURRENT_INDEX, index).apply();
    } else {
      Log.e(
          TAG, "setCurrentPartIndex: no part found for song with id='" + currentSongId + "'"
      );
      return;
    }
    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeSongOrPartChanged(currentSongWithParts, currentPartIndex);
      }
    }
  }

  private void resetHandlersIfRequired() {
    if (tickThread == null || !tickThread.isAlive()) {
      tickThread = new HandlerThread("metronome_ticks");
      tickThread.start();
      removeHandlerCallbacks();
      tickHandler = new Handler(tickThread.getLooper());
    }
    if (audioThread == null || !audioThread.isAlive()) {
      audioThread = new HandlerThread("metronome_audio");
      audioThread.start();
      removeHandlerCallbacks();
      audioHandler = new Handler(audioThread.getLooper());
    }
    if (callbackThread == null || !callbackThread.isAlive()) {
      callbackThread = new HandlerThread("metronome_callback");
      callbackThread.start();
      removeHandlerCallbacks();
      latencyHandler = new Handler(callbackThread.getLooper());
      countInHandler = new Handler(callbackThread.getLooper());
      incrementalHandler = new Handler(callbackThread.getLooper());
      elapsedHandler = new Handler(callbackThread.getLooper());
      timerHandler = new Handler(callbackThread.getLooper());
      muteHandler = new Handler(callbackThread.getLooper());
    }
  }

  private void removeHandlerCallbacks() {
    if (tickHandler != null) {
      tickHandler.removeCallbacksAndMessages(null);
    }
    if (audioHandler != null) {
      audioHandler.removeCallbacksAndMessages(null);
    }
    if (latencyHandler != null) {
      latencyHandler.removeCallbacksAndMessages(null);
      countInHandler.removeCallbacksAndMessages(null);
      incrementalHandler.removeCallbacksAndMessages(null);
      elapsedHandler.removeCallbacksAndMessages(null);
      timerHandler.removeCallbacksAndMessages(null);
      muteHandler.removeCallbacksAndMessages(null);
    }
  }

  public void savePlayingState() {
    tempPlaying = isPlaying();
  }

  public void restorePlayingState() {
    if (tempPlaying) {
      start(false);
    } else {
      stop(false);
    }
  }

  public void setUpLatencyCalibration() {
    config.setTempo(80);
    config.setBeats(DEF.BEATS);
    config.setSubdivisions(DEF.SUBDIVISIONS);
    config.setCountIn(0);
    config.setIncrementalAmount(0);
    config.setTimerDuration(0);
    config.setMutePlay(0);

    beatMode = BEAT_MODE.ALL;
    audioEngine.setGain(0);
    audioEngine.setMuted(false);
    hapticUtil.setEnabled(true);

    start(true);
  }

  public void addListener(MetronomeListener listener) {
    listeners.add(listener);
  }

  public void removeListener(MetronomeListener listener) {
    listeners.remove(listener);
  }

  public void start() {
    start(false);
  }

  private void start(boolean ignorePermission) {
    // isRestarted should suppress onStop/onStart callbacks and count-in
    boolean permissionDenied = sharedPrefs.getBoolean(PREF.PERMISSION_DENIED, false);
    if (!NotificationUtil.hasPermission(context) && !permissionDenied && !ignorePermission) {
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomePermissionMissing();
        }
      }
      return;
    }
    updateLastPlayedAndPlayCount();

    if (isPlaying()) {
      return;
    }
    resetHandlersIfRequired();

    tickIndex = 0;
    tickIndexPoly = 0;
    isMuted = false;
    if (config.isMuteActive()) {
      // updateMuteHandler would be too late
      muteCountDown = calculateMuteCount(false);
    }

    isCountingIn = config.isCountInActive();
    countInStartTime = System.currentTimeMillis();
    countInHandler.postDelayed(() -> {
      isCountingIn = false;
      updateIncrementalHandler();
      elapsedStartTime = System.currentTimeMillis();
      updateElapsedHandler(false);
      timerStartTime = System.currentTimeMillis();
      updateTimerHandler(timerProgress, true);
      updateMuteHandler();
    }, getCountInInterval()); // already 0 if count-in is disabled

    if (getGain() > 0) {
      neverStartedWithGain = false;
    }

    playing = true;
    startTicks();

    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeStart();
      }
    }
    Log.i(TAG, "start: started metronome handler");
  }

  public void stop() {
    stop(resetTimerOnStop);
  }

  private void stop(boolean resetTimer) {
    if (!isPlaying()) {
      return;
    }
    boolean isTimerReset = false;
    if (resetTimer || isTimerFinished()) {
      timerProgress = 0;
      timerBarIndex = 0;
      timerBeatIndex = 0;
      timerSubIndex = 0;
      isTimerReset = true;
    } else {
      timerProgress = getTimerProgress(); // must be called before playing is set to false
    }
    elapsedPrevious = elapsedTime;

    removeHandlerCallbacks();

    playing = false;
    audioHandler.post(audioEngine::stop);
    isCountingIn = false;

    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeStop();
        if (isTimerReset) {
          listener.onMetronomeTimerProgressOneTime(true);
        }
      }
    }
    Log.i(TAG, "stop: stopped metronome handler");
  }

  public void restartIfPlaying(boolean resetTimer) {
    if (isPlaying()) {
      // stop-like logic
      boolean isTimerReset = false;
      if (resetTimer || isTimerFinished()) {
        timerProgress = 0;
        timerBarIndex = 0;
        timerBeatIndex = 0;
        timerSubIndex = 0;
        isTimerReset = true;
      } else {
        timerProgress = getTimerProgress(); // must be called before playing is set to false
      }
      elapsedPrevious = elapsedTime;
      removeHandlerCallbacks();
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          if (isTimerReset) {
            listener.onMetronomeTimerProgressOneTime(true);
          }
        }
      }

      // start-like logic
      resetHandlersIfRequired();
      int countInTickIndex = config.getCountIn() *
          config.getBeatsCount() * config.getSubdivisionsCount();
      if (config.usePolyrhythm()) {
        countInTickIndex = config.getCountIn() *
            config.getBeatsCount(); // subdivisions are handled in polyrhythm
      }
      tickIndex = config.isCountInActive() ? countInTickIndex : 0;
      tickIndexPoly = (long) config.getCountIn() * config.getSubdivisionsCount();
      isMuted = false;
      if (config.isMuteActive()) {
        // updateMuteHandler would be too late
        muteCountDown = calculateMuteCount(false);
      }

      startTicks();

      isCountingIn = false;
      updateIncrementalHandler();
      elapsedStartTime = System.currentTimeMillis();
      updateElapsedHandler(false);
      timerStartTime = System.currentTimeMillis();
      updateTimerHandler(timerProgress, true);
      updateMuteHandler();
    } else if (resetTimer) {
      timerProgress = 0;
      if (ignoreTimerCallbacksTemp) {
        return;
      }
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomeTimerProgressOneTime(true);
        }
      }
    }
  }

  public boolean isPlaying() {
    return playing;
  }

  private void startTicks() {
    long now = System.currentTimeMillis();
    nextScheduleTime = now;
    nextPolyScheduleTime = now;

    Runnable tickRunnablePoly = new Runnable() {
      @Override
      public void run() {
        if (!isPlaying()) {
          return;
        }
        int subdivisionPoly = getCurrentSubdivisionPoly();
        String tickTypePoly = getCurrentTickTypePoly();
        boolean muted = isMuted;
        if (config.isMuteActive() && config.getMuteUnit().equals(UNIT.BEATS)) {
          muted = random.nextInt(100) < config.getMuteMute();
        }
        Tick tick = new Tick(
            tickIndexPoly, 1, subdivisionPoly, tickTypePoly, muted, true
        );

        if (subdivisionPoly < config.getSubdivisionsCount()) {
          // first poly subdivision handled in main tick runnable to keep poly in sync
          long barInterval = getInterval() * config.getBeatsCount();
          long step = barInterval / config.getSubdivisionsCount();
          nextPolyScheduleTime += step;
          long delay = nextPolyScheduleTime - System.currentTimeMillis();
          if (delay < 0) {
            delay = 0;
          }
          tickHandler.postDelayed(this, delay);
        }

        performTickPoly(tick);
        audioEngine.playTick(tick);
        tickIndexPoly++;
      }
    };
    Runnable tickRunnable = new Runnable() {
      @Override
      public void run() {
        if (!isPlaying()) {
          return;
        }

        // first calculate muted state
        long beatIndex = config.usePolyrhythm()
            ? tickIndex
            : tickIndex / config.getSubdivisionsCount();
        boolean isBeat = config.usePolyrhythm()
            || (tickIndex % config.getSubdivisionsCount()) == 0;
        boolean isFirstBeat = isBeat && (beatIndex % config.getBeatsCount()) == 0;
        long barIndex = beatIndex / config.getBeatsCount();
        boolean isCountIn = barIndex < config.getCountIn();
        if (isFirstBeat
            && config.isMuteActive() && config.getMuteUnit().equals(UNIT.BARS) && !isCountIn) {
          if (muteCountDown > 0) {
            muteCountDown--;
          } else {
            isMuted = !isMuted;
            // Minus 1 because it's already the next bar
            muteCountDown = Math.max(calculateMuteCount(isMuted) - 1, 0);
          }
        }
        // now calculate tick
        int beat = getCurrentBeat();
        int subdivision = getCurrentSubdivision();
        String tickType = getCurrentTickType();
        boolean muted = isMuted;
        if (config.isMuteActive() && config.getMuteUnit().equals(UNIT.BEATS)) {
          muted = random.nextInt(100) < config.getMuteMute();
        }
        Tick tick = new Tick(tickIndex, beat, subdivision, tickType, muted, false);

        long interval = config.usePolyrhythm()
            ? getInterval()
            : getInterval() / config.getSubdivisionsCount();
        nextScheduleTime += interval;

        if (tick.beat == 1 && tick.subdivision == 1) {
          // first beat and subdivision is handled in main tick runnable to keep poly in sync
          nextPolyScheduleTime = nextScheduleTime - interval;
        }

        long delay = nextScheduleTime - System.currentTimeMillis();
        if (delay < 0) {
          delay = 0;
        }
        tickHandler.postDelayed(this, delay);

        if (tick.beat == 1 && config.usePolyrhythm()) {
          // start polyrhythm subdivisions every new bar
          tickHandler.post(tickRunnablePoly);
        }

        boolean playing = performTick(tick); // don't play next tick if timer finished
        if (playing) {
          audioEngine.playTick(tick);
          tickIndex++;
        }
      }
    };
    audioHandler.post(() -> {
      audioEngine.play();
      // wait for audio engine to be started
      tickHandler.post(tickRunnable);
    });
  }

  private void updateLastPlayedAndPlayCount() {
    executorService.execute(() -> {
      if (currentSongWithParts != null && !currentSongId.equals(Constants.SONG_ID_DEFAULT)) {
        // update last played and play count except for default song
        Song currentSong = currentSongWithParts.getSong();
        currentSong.setLastPlayed(System.currentTimeMillis());
        currentSong.incrementPlayCount();
        db.songDao().updateSong(currentSong);
        shortcutUtil.reportUsage(currentSong.getId());
        // update widget only if songs are sorted by last played or most played
        if (songsOrder == SONGS_ORDER.LAST_PLAYED_ASC
            || songsOrder == SONGS_ORDER.MOST_PLAYED_ASC
        ) {
          WidgetUtil.sendSongsWidgetUpdate(context);
        }
      }
    });
    updateShortcuts();
  }

  public void updateShortcuts() {
    executorService.execute(() -> {
      if (!ShortcutUtil.isSupported()) {
        return;
      }
      shortcutUtil.removeAllShortcuts();
      List<Song> songs = db.songDao().getAllSongs();
      List<Song> filteredSongs = new ArrayList<>(songs);
      filteredSongs.removeIf(
          song -> song.getId().equals(Constants.SONG_ID_DEFAULT) || song.getPlayCount() < 1
      );
      filteredSongs.removeIf(song -> song.getPlayCount() < 1);
      Collections.sort(filteredSongs, Comparator
          .comparingInt(Song::getPlayCount).reversed()
          .thenComparing(Song::getName, String.CASE_INSENSITIVE_ORDER)
      );
      filteredSongs.subList(0, Math.min(filteredSongs.size(), shortcutUtil.getMaxShortcutCount()));
      List <ShortcutInfo> shortcuts = new ArrayList<>();
      for (Song song : filteredSongs) {
        if (shortcuts.size() < shortcutUtil.getMaxShortcutCount()) {
          shortcuts.add(shortcutUtil.getShortcutInfo(song.getId(), song.getName()));
        } else {
          break;
        }
      }
      shortcutUtil.addAllShortcuts(shortcuts);
    });
  }

  private void setBeats(String[] beats) {
    config.setBeats(beats);
    sharedPrefs.edit().putString(PREF.BEATS, String.join(",", beats)).apply();
  }

  public void setBeat(int beat, String tickType) {
    config.setBeat(beat, tickType);
    setBeats(config.getBeats());
  }

  public boolean addBeat() {
    boolean success = config.addBeat();
    if (success) {
      setBeats(config.getBeats());
    }
    return success;
  }

  public boolean removeBeat() {
    boolean success = config.removeBeat();
    if (success) {
      setBeats(config.getBeats());
    }
    return success;
  }

  private void setSubdivisions(String[] subdivisions) {
    config.setSubdivisions(subdivisions);
    sharedPrefs.edit()
        .putString(PREF.SUBDIVISIONS, String.join(",", config.getSubdivisions()))
        .apply();
  }

  public void setSubdivision(int subdivision, String tickType) {
    config.setSubdivision(subdivision, tickType);
    setSubdivisions(config.getSubdivisions());
  }

  public boolean addSubdivision() {
    boolean success = config.addSubdivision();
    if (success) {
      setSubdivisions(config.getSubdivisions());
    }
    return success;
  }

  public boolean removeSubdivision() {
    boolean success = config.removeSubdivision();
    if (success) {
      setSubdivisions(config.getSubdivisions());
    }
    return success;
  }

  public void setSwing3() {
    config.setSwing3();
    setSubdivisions(config.getSubdivisions());
  }

  public void setSwing5() {
    config.setSwing5();
    setSubdivisions(config.getSubdivisions());
  }

  public void setSwing7() {
    config.setSwing7();
    setSubdivisions(config.getSubdivisions());
  }

  public void setTempo(int tempo) {
    if (config.getTempo() != tempo) {
      config.setTempo(tempo);
      sharedPrefs.edit().putInt(PREF.TEMPO, tempo).apply();
      if (isPlaying() && config.isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
        updateTimerHandler(false, true, false);
      }
    }
  }

  private void changeTempo(int change) {
    int tempoOld = config.getTempo();
    int tempoNew = tempoOld + change;
    setTempo(tempoNew);
    // setTempo will only be called by callback below, else we would break timer animation
    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeTempoChanged(tempoOld, tempoNew);
      }
    }
    maybeUpdateDefaultSong();
  }

  public long getInterval() {
    return 1000 * 60 / Math.max(config.getTempo(), 1);
  }

  public void setUsePolyrhythm(boolean usePolyrhythm) {
    config.setUsePolyrhythm(usePolyrhythm);
    sharedPrefs.edit().putBoolean(PREF.USE_POLYRHYTHM, usePolyrhythm).apply();
    // no restart here, may cause race condition during parts change
  }

  public void setSound(String sound) {
    audioEngine.setSound(sound);
    sharedPrefs.edit().putString(PREF.SOUND, sound).apply();
  }

  public String getSound() {
    return sharedPrefs.getString(PREF.SOUND, DEF.SOUND);
  }

  public void setBeatMode(@NonNull String mode) {
    if (!hapticUtil.hasVibrator()) {
      mode = BEAT_MODE.SOUND;
    }
    beatMode = mode;
    audioEngine.setMuted(mode.equals(BEAT_MODE.VIBRATION));
    hapticUtil.setEnabled(!mode.equals(BEAT_MODE.SOUND));
    sharedPrefs.edit().putString(PREF.BEAT_MODE, mode).apply();
  }

  public String getBeatMode() {
    return beatMode;
  }

  public boolean areHapticEffectsPossible(boolean ignoreIsPlaying) {
    if (ignoreIsPlaying) {
      return beatMode.equals(BEAT_MODE.SOUND);
    } else {
      return !isPlaying() || areHapticEffectsPossible(true);
    }
  }

  public void setVibrationIntensity(String intensity) {
    hapticUtil.setIntensity(intensity);
    sharedPrefs.edit().putString(PREF.VIBRATION_INTENSITY, intensity).apply();
  }

  public void setLatency(long offset) {
    latency = offset;
    sharedPrefs.edit().putLong(PREF.LATENCY, offset).apply();
  }

  public long getLatency() {
    return latency;
  }

  public void setIgnoreFocus(boolean ignore) {
    audioEngine.setIgnoreFocus(ignore);
    sharedPrefs.edit().putBoolean(PREF.IGNORE_FOCUS, ignore).apply();
  }

  public boolean getIgnoreAudioFocus() {
    return audioEngine.getIgnoreFocus();
  }

  public void setGain(int gain) {
    audioEngine.setGain(gain);
    sharedPrefs.edit().putInt(PREF.GAIN, gain).apply();
  }

  public int getGain() {
    return audioEngine.getGain();
  }

  public boolean neverStartedWithGainBefore() {
    return neverStartedWithGain;
  }

  public void setFlashScreen(String flash) {
    flashScreen = flash;
    sharedPrefs.edit().putString(PREF.FLASH_SCREEN, flash).apply();
  }

  public String getFlashScreen() {
    return flashScreen;
  }

  public void setFlashlight(String strength) {
    flashlight = strength;
    sharedPrefs.edit().putString(PREF.FLASHLIGHT, strength).apply();
  }

  public String getFlashlight() {
    return flashlight;
  }

  public void setKeepAwake(String keepAwake) {
    this.keepAwake = keepAwake;
    sharedPrefs.edit().putString(PREF.KEEP_AWAKE, keepAwake).apply();
  }

  public String getKeepAwake() {
    return keepAwake;
  }

  public void setTempoInputKeyboard(boolean keyboard) {
    tempoInputKeyboard = keyboard;
    sharedPrefs.edit().putBoolean(PREF.TEMPO_INPUT_KEYBOARD, keyboard).apply();
  }

  public boolean getTempoInputKeyboard() {
    return tempoInputKeyboard;
  }

  public void setTempoTapInstant(boolean instant) {
    tempoTapInstant = instant;
    sharedPrefs.edit().putBoolean(PREF.TEMPO_TAP_INSTANT, instant).apply();
  }

  public boolean getTempoTapInstant() {
    return tempoTapInstant;
  }

  public void setCountIn(int bars) {
    config.setCountIn(bars);
    sharedPrefs.edit().putInt(PREF.COUNT_IN, bars).apply();
  }

  public boolean isCountingIn() {
    return isCountingIn;
  }

  public long getCountInInterval() {
    return getInterval() * config.getBeatsCount() * config.getCountIn();
  }

  public float getCountInProgress() {
    if (isPlaying() && isCountingIn()) {
      long countInElapsed = System.currentTimeMillis() - countInStartTime;
      return Math.max(0, Math.min(1, countInElapsed / (float) getCountInInterval()));
    }
    return 1;
  }

  public long getCountInIntervalRemaining() {
    if (isPlaying() && isCountingIn()) {
      long countInElapsed = System.currentTimeMillis() - countInStartTime;
      return Math.max(0, getCountInInterval() - countInElapsed);
    }
    return 0;
  }

  public void setIncrementalAmount(int bpm) {
    config.setIncrementalAmount(bpm);
    sharedPrefs.edit().putInt(PREF.INCREMENTAL_AMOUNT, bpm).apply();
    updateIncrementalHandler();
  }

  public void setIncrementalIncrease(boolean increase) {
    config.setIncrementalIncrease(increase);
    sharedPrefs.edit().putBoolean(PREF.INCREMENTAL_INCREASE, increase).apply();
  }

  public void setIncrementalInterval(int interval) {
    config.setIncrementalInterval(interval);
    sharedPrefs.edit().putInt(PREF.INCREMENTAL_INTERVAL, interval).apply();
    updateIncrementalHandler();
  }

  public void setIncrementalUnit(String unit) {
    if (unit.equals(config.getIncrementalUnit())) {
      return;
    }
    config.setIncrementalUnit(unit);
    sharedPrefs.edit().putString(PREF.INCREMENTAL_UNIT, unit).apply();
    updateIncrementalHandler();
  }

  public void setIncrementalLimit(int limit) {
    config.setIncrementalLimit(limit);
    sharedPrefs.edit().putInt(PREF.INCREMENTAL_LIMIT, limit).apply();
  }

  private void updateIncrementalHandler() {
    if (!isPlaying()) {
      return;
    }
    incrementalHandler.removeCallbacksAndMessages(null);
    String unit = config.getIncrementalUnit();
    int amount = config.getIncrementalAmount();
    int limit = config.getIncrementalLimit();
    boolean increase = config.isIncrementalIncrease();
    if (!unit.equals(UNIT.BARS) && config.isIncrementalActive()) {
      long factor = unit.equals(UNIT.SECONDS) ? 1000L : 60000L;
      long interval = factor * config.getIncrementalInterval();
      incrementalHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          incrementalHandler.postDelayed(this, interval);
          int upperLimit = limit != 0 ? limit : Constants.TEMPO_MAX;
          int lowerLimit = limit != 0 ? limit : Constants.TEMPO_MIN;
          if (increase && config.getTempo() + amount <= upperLimit) {
            changeTempo(amount);
          } else if (!increase && config.getTempo() - amount >= lowerLimit) {
            changeTempo(-amount);
          }
        }
      }, interval);
    }
  }

  public void setShowElapsed(boolean show) {
    showElapsed = show;
    sharedPrefs.edit().putBoolean(PREF.SHOW_ELAPSED, show).apply();
  }

  public boolean getShowElapsed() {
    return showElapsed;
  }

  public boolean isElapsedActive() {
    return showElapsed;
  }

  public void resetElapsed() {
    elapsedPrevious = 0;
    elapsedStartTime = System.currentTimeMillis();
    elapsedTime = 0;
    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeElapsedTimeSecondsChanged();
      }
    }
    updateElapsedHandler(true);
  }

  public void updateElapsedHandler(boolean reset) {
    if (!isPlaying()) {
      return;
    }
    elapsedHandler.removeCallbacksAndMessages(null);
    if (!isElapsedActive()) {
      return;
    }
    if (reset) {
      elapsedPrevious = 0;
    }
    elapsedHandler.post(new Runnable() {
      @Override
      public void run() {
        if (isPlaying()) {
          elapsedTime = System.currentTimeMillis() - elapsedStartTime + elapsedPrevious;
          elapsedHandler.postDelayed(this, 1000);
          synchronized (listeners) {
            for (MetronomeListener listener : listeners) {
              listener.onMetronomeElapsedTimeSecondsChanged();
            }
          }
        }
      }
    });
  }

  public String getElapsedTimeString() {
    if (!isElapsedActive()) {
      return "";
    }
    int seconds = (int) (elapsedTime / 1000);
    return getTimeStringFromSeconds(seconds, false);
  }

  public void setTimerDuration(int duration) {
    config.setTimerDuration(duration);
    sharedPrefs.edit().putInt(PREF.TIMER_DURATION, duration).apply();
    if (config.getTimerUnit().equals(UNIT.BARS)) {
      updateTimerHandler(false, true);
    } else {
      updateTimerHandler(0, false);
    }
  }

  public long getTimerInterval() {
    long factor;
    switch (config.getTimerUnit()) {
      case UNIT.SECONDS:
        factor = 1000L;
        break;
      case UNIT.MINUTES:
        factor = 60000L;
        break;
      default:
        factor = getInterval() * config.getBeatsCount();
        break;
    }
    return factor * config.getTimerDuration();
  }

  public long getTimerIntervalRemaining() {
    return (long) (getTimerInterval() * (1 - getTimerProgress()));
  }

  public void setTimerUnit(String unit) {
    if (unit.equals(config.getTimerUnit())) {
      return;
    }
    config.setTimerUnit(unit);
    sharedPrefs.edit().putString(PREF.TIMER_UNIT, unit).apply();
    updateTimerHandler(0, false);
  }

  public void setResetTimerOnStop(boolean reset) {
    resetTimerOnStop = reset;
    sharedPrefs.edit().putBoolean(PREF.RESET_TIMER_ON_STOP, reset).apply();
  }

  public boolean getResetTimerOnStop() {
    return resetTimerOnStop;
  }

  public float getTimerProgress() {
    if (config.isTimerActive()) {
      if (!config.getTimerUnit().equals(UNIT.BARS) && isPlaying() && !isCountingIn) {
        long previousDuration = (long) (timerProgress * getTimerInterval());
        long elapsedTime = System.currentTimeMillis() - timerStartTime + previousDuration;
        float fraction = elapsedTime / (float) getTimerInterval();
        return Math.min(1, Math.max(0, fraction));
      } else {
        return timerProgress;
      }
    } else {
      return 0;
    }
  }

  public boolean isTimerFinished() {
    if (config.getTimerUnit().equals(UNIT.BARS)) {
      return timerBarIndex >= config.getTimerDuration() - 1 &&
          timerBeatIndex >= config.getBeatsCount() - 1 &&
          timerSubIndex >= config.getSubdivisionsCount() - 1;
    } else {
      try {
        BigDecimal bdProgress = BigDecimal.valueOf(getTimerProgress()).setScale(
            2, RoundingMode.HALF_UP
        );
        BigDecimal bdFraction = new BigDecimal(1).setScale(2, RoundingMode.HALF_UP);
        return bdProgress.equals(bdFraction);
      } catch (NumberFormatException e) {
        return false;
      }
    }
  }

  public void updateTimerHandler(float fraction, boolean startAtFirstBeat) {
    timerProgress = fraction;
    switch (config.getTimerUnit()) {
      case UNIT.SECONDS:
      case UNIT.MINUTES:
        long intervalMillis = Math.max(getInterval(), 1);
        boolean isUnitSeconds = config.getTimerUnit().equals(UNIT.SECONDS);
        long totalMillis = config.getTimerDuration() * (isUnitSeconds ? 1000L : 60000L);
        long elapsedMillis = (long) (fraction * totalMillis);
        timerBarIndex = (int) (elapsedMillis / intervalMillis / config.getBeatsCount());
        timerBeatIndex =
            (int) ((elapsedMillis % (intervalMillis * config.getBeatsCount())) / intervalMillis);
        long subdivisionMillis = intervalMillis / config.getSubdivisionsCount();
        if (subdivisionMillis == 0) { // maybe caused division by zero
          subdivisionMillis = 1;
        }
        timerSubIndex = (int) ((elapsedMillis % intervalMillis) / subdivisionMillis);
        break;
      default:
        int barIndex = (int) (fraction * config.getTimerDuration());
        timerBarIndex = Math.min(barIndex, config.getTimerDuration() - 1);

        if (barIndex <= config.getTimerDuration() - 1) {
          timerBeatIndex = (int) ((fraction * config.getTimerDuration() * config.getBeatsCount())
              % config.getBeatsCount());
          timerSubIndex = (int) (fraction * config.getTimerDuration() * config.getBeatsCount()
              * config.getSubdivisionsCount()) % config.getSubdivisionsCount();
        } else {
          timerBeatIndex = config.getBeatsCount() - 1;
          timerSubIndex = config.getSubdivisionsCount() - 1;
        }
        break;
    }
    updateTimerHandler(startAtFirstBeat, false);
  }

  public void updateTimerHandler(boolean startAtFirstBeat, boolean performOneTime) {
    updateTimerHandler(startAtFirstBeat, performOneTime, true);
  }

  public void updateTimerHandler(
      boolean startAtFirstBeat, boolean performOneTime, boolean withTransition
  ) {
    // withTransition is only relevant for tempo changes while playing (in setTempo)
    // transitions make these changes laggy
    if (!isPlaying()) {
      return;
    }
    timerHandler.removeCallbacksAndMessages(null);
    if (!config.isTimerActive()) {
      return;
    }

    if (isTimerFinished()) {
      timerProgress = 0;
    } else if (startAtFirstBeat) {
      // set timer progress on start of this bar
      long barInterval = getInterval() * config.getBeatsCount();
      timerProgress = timerBarIndex * barInterval / (float) getTimerInterval();
      timerBeatIndex = 0;
      timerSubIndex = 0;
    }

    if (!config.getTimerUnit().equals(UNIT.BARS)) {
      timerHandler.postDelayed(() -> {
        if (hasNextPart()) {
          setCurrentPartIndex(currentPartIndex + 1);
        } else if (currentSongWithParts != null && currentSongWithParts.getSong().isLooped()) {
          // Restart song
          setCurrentPartIndex(0);
        } else {
          stop();
          if (currentSongWithParts != null) {
            setCurrentPartIndex(0);
          }
        }
      }, getTimerIntervalRemaining());
      timerHandler.post(new Runnable() {
        @Override
        public void run() {
          if (isPlaying() && !config.getTimerUnit().equals(UNIT.BARS)) {
            timerHandler.postDelayed(this, 1000);
            synchronized (listeners) {
              for (MetronomeListener listener : listeners) {
                listener.onMetronomeTimerSecondsChanged();
              }
            }
          }
        }
      });
    }

    if (ignoreTimerCallbacksTemp) {
      return;
    }
    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        if (performOneTime) {
          listener.onMetronomeTimerProgressOneTime(withTransition);
        } else {
          listener.onMetronomeTimerStarted();
        }
      }
    }
  }

  public void resetTimerNow() {
    if (config.isTimerActive()) {
      restartIfPlaying(true);
    }
  }

  public String getCurrentTimerString() {
    if (!config.isTimerActive()) {
      return "";
    }
    switch (config.getTimerUnit()) {
      case UNIT.SECONDS:
      case UNIT.MINUTES:
        long elapsedTime = (long) (getTimerProgress() * getTimerInterval());
        int seconds = (int) (elapsedTime / 1000);
        // Decide whether to force hours for consistency with total time
        int totalHours = config.getTimerDuration() / 3600;
        if (config.getTimerUnit().equals(UNIT.MINUTES)) {
          totalHours = config.getTimerDuration() / 60;
        }
        return getTimeStringFromSeconds(seconds, totalHours > 0);
      default:
        String format = config.getBeatsCount() < 10 ? "%d.%01d" : "%d.%02d";
        if (config.getSubdivisionsCount() > 1) {
          format += config.getSubdivisionsCount() < 10 ? ".%01d" : ".%02d";
          return String.format(
              Locale.ENGLISH, format,
              timerBarIndex + 1, timerBeatIndex + 1, timerSubIndex + 1
          );
        } else {
          return String.format(
              Locale.ENGLISH, format, timerBarIndex + 1, timerBeatIndex + 1
          );
        }
    }
  }

  public String getTotalTimeString() {
    if (!config.isTimerActive()) {
      return "";
    }
    int timerDuration = config.getTimerDuration();
    switch (config.getTimerUnit()) {
      case UNIT.SECONDS:
      case UNIT.MINUTES:
        int seconds = timerDuration;
        if (config.getTimerUnit().equals(UNIT.MINUTES)) {
          seconds *= 60;
        }
        return getTimeStringFromSeconds(seconds, false);
      default:
        return context.getResources().getQuantityString(
            R.plurals.options_unit_bars, timerDuration, timerDuration
        );
    }
  }

  public static String getTimeStringFromSeconds(int seconds, boolean forceHours) {
    int minutes = seconds / 60;
    int hours = minutes / 60;
    if (hours > 0 || forceHours) {
      return String.format(
          Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes % 60, seconds % 60
      );
    }
    return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds % 60);
  }

  public void setMutePlay(int play) {
    config.setMutePlay(play);
    sharedPrefs.edit().putInt(PREF.MUTE_PLAY, play).apply();
    updateMuteHandler();
  }

  public void setMuteMute(int mute) {
    config.setMuteMute(mute);
    sharedPrefs.edit().putInt(PREF.MUTE_MUTE, config.getMuteMute()).apply();
    updateMuteHandler();
  }

  public void setMuteUnit(String unit) {
    if (unit.equals(config.getMuteUnit())) {
      return;
    }
    config.setMuteUnit(unit);
    sharedPrefs.edit().putString(PREF.MUTE_UNIT, unit).apply();
    // Trigger snapping to correct step size
    setMuteMute(config.getMuteMute());

    updateMuteHandler();
  }

  public void setMuteRandom(boolean random) {
    config.setMuteRandom(random);
    sharedPrefs.edit().putBoolean(PREF.MUTE_RANDOM, random).apply();
    updateMuteHandler();
  }

  private void updateMuteHandler() {
    if (!isPlaying()) {
      return;
    }
    muteHandler.removeCallbacksAndMessages(null);
    isMuted = false;
    if (config.isMuteActive() && config.getMuteUnit().equals(UNIT.SECONDS)) {
      muteHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          isMuted = !isMuted;
          muteHandler.postDelayed(this, calculateMuteCount(isMuted) * 1000L);
        }
      }, calculateMuteCount(isMuted) * 1000L);
    }
  }

  private int calculateMuteCount(boolean mute) {
    int count = mute ? config.getMuteMute() : config.getMutePlay();
    if (config.isMuteRandom()) {
      return random.nextInt(count + 1);
    } else {
      return count;
    }
  }

  private boolean performTick(Tick tick) {
    long beatIndex = config.usePolyrhythm() ? tickIndex : tickIndex / config.getSubdivisionsCount();
    long barIndex = beatIndex / config.getBeatsCount();
    long barIndexWithoutCountIn = barIndex - config.getCountIn();
    boolean isCountIn = barIndex < config.getCountIn();

    boolean isBeat = tick.subdivision == 1;
    boolean isFirstBeat = isBeat && (beatIndex % config.getBeatsCount()) == 0;

    if (config.isTimerActive() && config.getTimerUnit().equals(UNIT.BARS) && !isCountIn) {
      boolean isFirstBeatInFirstBar = barIndexWithoutCountIn == 0 && isFirstBeat;
      boolean increaseTimerProgress = barIndexWithoutCountIn > 0 || !isFirstBeatInFirstBar;
      // Play the first beat without increasing
      if (increaseTimerProgress) {
        if (isFirstBeat) {
          timerBarIndex++;
        }
        if (isBeat) {
          timerBeatIndex++;
          if (timerBeatIndex >= config.getBeatsCount()) {
            timerBeatIndex = 0;
          }
        }
        timerSubIndex++;
        if (timerSubIndex >= config.getSubdivisionsCount()) {
          timerSubIndex = 0;
        }
        long barInterval = getInterval() * config.getBeatsCount();
        long subInterval = getInterval() / config.getSubdivisionsCount();
        long progressInterval = timerBarIndex * barInterval
            + timerBeatIndex * getInterval()
            + timerSubIndex * subInterval;
        timerProgress = progressInterval / (float) getTimerInterval();
      }
      boolean isTimerFinished = isTimerFinished();
      if (config.getTimerUnit().equals(UNIT.BARS)) {
        // Cannot use isTimerFinished() here because last beat has to be still played
        isTimerFinished = timerBarIndex > config.getTimerDuration() - 1;
        if (isTimerFinished) {
          timerBarIndex = config.getTimerDuration() - 1;
          timerBeatIndex = config.getBeatsCount() - 1;
          timerSubIndex = config.getSubdivisionsCount() - 1;
        }
      }
      if (isTimerFinished) {
        timerProgress = 1;
        if (hasNextPart()) {
          setCurrentPartIndex(currentPartIndex + 1);
        } else if (currentSongWithParts != null && currentSongWithParts.getSong().isLooped()) {
          // Restart song
          setCurrentPartIndex(0);
        } else {
          stop();
          if (currentSongWithParts != null) {
            setCurrentPartIndex(0);
          }
        }
        return false;
      }
    }

    if (isFirstBeat) {
      if (config.isIncrementalActive()
          && config.getIncrementalUnit().equals(UNIT.BARS)
          && !isCountIn
      ) {
        int incrementalAmount = config.getIncrementalAmount();
        int incrementalInterval = config.getIncrementalInterval();
        int incrementalLimit = config.getIncrementalLimit();
        boolean incrementalIncrease = config.isIncrementalIncrease();
        if (barIndexWithoutCountIn >= incrementalInterval
            && barIndexWithoutCountIn % incrementalInterval == 0) {
          int upperLimit = incrementalLimit != 0 ? incrementalLimit : Constants.TEMPO_MAX;
          int lowerLimit = incrementalLimit != 0 ? incrementalLimit : Constants.TEMPO_MIN;
          if (incrementalIncrease && config.getTempo() + incrementalAmount <= upperLimit) {
            changeTempo(incrementalAmount);
          } else if (!incrementalIncrease && config.getTempo() - incrementalAmount >= lowerLimit) {
            changeTempo(-incrementalAmount);
          }
        }
      }
    }

    latencyHandler.postDelayed(() -> {
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomePreTick(tick);
        }
      }
    }, Math.max(0, latency - Constants.BEAT_ANIM_OFFSET));
    latencyHandler.postDelayed(() -> {
      if (!beatMode.equals(BEAT_MODE.SOUND) && !tick.isMuted) {
        switch (tick.type) {
          case TICK_TYPE.STRONG:
            hapticUtil.heavyClick(false);
            if (!flashlight.equals(FLASHLIGHT.OFF)) {
              float strength = flashlight.equals(FLASHLIGHT.STRONG) ? 1 : 0.15f;
              flashlightUtil.flash(100, strength);
            }
            break;
          case TICK_TYPE.SUB:
            hapticUtil.tick(false);
            break;
          case TICK_TYPE.MUTED:
          case TICK_TYPE.BEAT_SUB_MUTED:
            break;
          default:
            hapticUtil.click(false);
            if (!flashlight.equals(FLASHLIGHT.OFF)) {
              float strength = flashlight.equals(FLASHLIGHT.STRONG) ? 1 : 0.15f;
              flashlightUtil.flash(20, strength);
            }
        }
      }
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomeTick(tick);
        }
      }
    }, latency);

    return true;
  }

  private void performTickPoly(Tick tick) {
    latencyHandler.postDelayed(() -> {
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomePreTick(tick);
        }
      }
    }, Math.max(0, latency - Constants.BEAT_ANIM_OFFSET));
    latencyHandler.postDelayed(() -> {
      boolean shouldVibrate = !beatMode.equals(BEAT_MODE.SOUND) && !isMuted;
      if (shouldVibrate) {
        // check whether any poly subdivision collides with a beat
        long product = (long) (tick.subdivision - 1) * config.getBeatsCount();
        if (product % config.getSubdivisionsCount() == 0) {
          shouldVibrate = false;
        }
      }
      if (shouldVibrate) {
        switch (tick.type) {
          case TICK_TYPE.STRONG:
            hapticUtil.heavyClick();
            break;
          case TICK_TYPE.SUB:
            hapticUtil.tick();
            break;
          case TICK_TYPE.MUTED:
          case TICK_TYPE.BEAT_SUB_MUTED:
            break;
          default:
            hapticUtil.click();
        }
      }
    }, latency);
  }

  private int getCurrentBeat() {
    if (config.usePolyrhythm()) {
      return (int) (tickIndex % config.getBeats().length) + 1;
    }
    return (int) ((tickIndex / config.getSubdivisionsCount()) % config.getBeats().length) + 1;
  }

  private int getCurrentSubdivision() {
    if (config.usePolyrhythm()) {
      return 1;
    }
    return (int) (tickIndex % config.getSubdivisionsCount()) + 1;
  }

  private int getCurrentSubdivisionPoly() {
    return (int) (tickIndexPoly % config.getSubdivisionsCount()) + 1;
  }

  private String getCurrentTickType() {
    if (config.usePolyrhythm()) {
      String[] beats = config.getBeats();
      int beatIndex = (int) (tickIndex % beats.length);
      if (beatIndex == 0 && config.isFirstSubdivisionMuted()) {
        // mute first beat if first subdivision is muted because user expects that with polyrhythm
        return TICK_TYPE.BEAT_SUB_MUTED;
      } else {
        return beats[(int) (tickIndex % beats.length)];
      }
    } else {
      int subdivisionsCount = config.getSubdivisionsCount();
      if ((tickIndex % subdivisionsCount) == 0) {
        if (config.isFirstSubdivisionMuted()) {
          return TICK_TYPE.BEAT_SUB_MUTED;
        } else {
          String[] beats = config.getBeats();
          return beats[(int) ((tickIndex / subdivisionsCount) % beats.length)];
        }
      } else {
        String[] subdivisions = config.getSubdivisions();
        return subdivisions[(int) (tickIndex % subdivisionsCount)];
      }
    }
  }

  private String getCurrentTickTypePoly() {
    int subdivisionsCount = config.getSubdivisionsCount();
    if ((tickIndexPoly % subdivisionsCount) == 0) {
      return TICK_TYPE.BEAT_SUB_MUTED;
    } else {
      String[] subdivisions = config.getSubdivisions();
      return subdivisions[(int) (tickIndexPoly % subdivisionsCount)];
    }
  }

  public interface MetronomeListener {
    void onMetronomeStart();
    void onMetronomeStop();
    void onMetronomePreTick(Tick tick);
    void onMetronomeTick(Tick tick);
    void onMetronomeTempoChanged(int tempoOld, int tempoNew);
    void onMetronomeElapsedTimeSecondsChanged();
    void onMetronomeTimerStarted();
    void onMetronomeTimerSecondsChanged();
    void onMetronomeTimerProgressOneTime(boolean withTransition);
    void onMetronomeConfigChanged();
    void onMetronomeSongOrPartChanged(@Nullable SongWithParts song, int partIndex);
    void onMetronomePermissionMissing();
  }

  public static class MetronomeListenerAdapter implements MetronomeListener {
    public void onMetronomeStart() {}
    public void onMetronomeStop() {}
    public void onMetronomePreTick(Tick tick) {}
    public void onMetronomeTick(Tick tick) {}
    public void onMetronomeTempoChanged(int tempoOld, int tempoNew) {}
    public void onMetronomeElapsedTimeSecondsChanged() {}
    public void onMetronomeTimerStarted() {}
    public void onMetronomeTimerSecondsChanged() {}
    public void onMetronomeTimerProgressOneTime(boolean withTransition) {}
    public void onMetronomeConfigChanged() {}
    public void onMetronomeSongOrPartChanged(@Nullable SongWithParts song, int partIndex) {}
    public void onMetronomePermissionMissing() {}
  }

  public static class Tick {
    public final long index;
    public final int beat, subdivision;
    @NonNull
    public final String type;
    public final boolean isMuted, isPoly;

    public Tick(
        long index,
        int beat,
        int subdivision,
        @NonNull String type,
        boolean isMuted,
        boolean isPoly
    ) {
      this.index = index;
      this.beat = beat;
      this.subdivision = subdivision;
      this.type = type;
      this.isMuted = isMuted;
      this.isPoly = isPoly;
    }

    @NonNull
    @Override
    public String toString() {
      return "Tick{index = " + index +
          ", beat=" + beat +
          ", sub=" + subdivision +
          ", type=" + type +
          ", isPoly=" + isPoly +
          ", muted=" + isMuted + '}';
    }
  }
}