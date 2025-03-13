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

package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.SongDatabase;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.model.MetronomeConfig;

public class MetronomeUtil {

  private static final String TAG = MetronomeUtil.class.getSimpleName();

  private final Context context;
  private final SharedPreferences sharedPrefs;
  private final AudioUtil audioUtil;
  private final HapticUtil hapticUtil;
  private final ShortcutUtil shortcutUtil;
  private final Set<MetronomeListener> listeners = Collections.synchronizedSet(new HashSet<>());
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Random random = new Random();
  private final MetronomeConfig config = new MetronomeConfig();
  private final SongDatabase db;
  private final boolean fromService;
  private HandlerThread tickThread, callbackThread;
  private Handler tickHandler, latencyHandler;
  private Handler countInHandler, incrementalHandler, elapsedHandler, timerHandler, muteHandler;
  private SongWithParts currentSongWithParts;
  private String currentSongName;
  private int currentPartIndex, muteCountDown;
  private long tickIndex, latency, elapsedStartTime, elapsedTime, elapsedPrevious, timerStartTime;
  private float timerProgress;
  private boolean playing, tempPlaying, beatModeVibrate, isCountingIn, isMuted;
  private boolean showElapsed, resetTimerOnStop, alwaysVibrate, flashScreen, keepAwake;
  private boolean neverStartedWithGain = true;

  public MetronomeUtil(@NonNull Context context, boolean fromService) {
    this.context = context;
    this.fromService = fromService;

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    audioUtil = new AudioUtil(context, this::stop);
    hapticUtil = new HapticUtil(context);
    shortcutUtil = new ShortcutUtil(context);

    db = SongDatabase.getInstance(context.getApplicationContext());

    resetHandlersIfRequired();
    setToPreferences();
  }

  public void setToPreferences() {
    config.setToPreferences(sharedPrefs);

    latency = sharedPrefs.getLong(PREF.LATENCY, DEF.LATENCY);
    alwaysVibrate = sharedPrefs.getBoolean(PREF.ALWAYS_VIBRATE, DEF.ALWAYS_VIBRATE);
    showElapsed = sharedPrefs.getBoolean(PREF.SHOW_ELAPSED, DEF.SHOW_ELAPSED);
    resetTimerOnStop = sharedPrefs.getBoolean(PREF.RESET_TIMER_ON_STOP, DEF.RESET_TIMER_ON_STOP);
    flashScreen = sharedPrefs.getBoolean(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN);
    keepAwake = sharedPrefs.getBoolean(PREF.KEEP_AWAKE, DEF.KEEP_AWAKE);

    setSound(sharedPrefs.getString(PREF.SOUND, DEF.SOUND));
    setIgnoreFocus(sharedPrefs.getBoolean(PREF.IGNORE_FOCUS, DEF.IGNORE_FOCUS));
    setGain(sharedPrefs.getInt(PREF.GAIN, DEF.GAIN));
    setBeatModeVibrate(sharedPrefs.getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE));
    setCurrentSong(
        sharedPrefs.getString(PREF.SONG_CURRENT, DEF.SONG_CURRENT),
        sharedPrefs.getInt(PREF.PART_CURRENT, DEF.PART_CURRENT),
        false
    );
  }

  public MetronomeConfig getConfig() {
    return config;
  }

  public void setConfig(MetronomeConfig config, boolean restart) {
    setCountIn(config.getCountIn());

    int tempoDiff = config.getTempo() - getTempo();
    changeTempo(tempoDiff);

    setBeats(config.getBeats(), restart);
    setSubdivisions(config.getSubdivisions());

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

    for (MetronomeListener listener : listeners) {
      listener.onMetronomeConfigChanged();
    }
  }

  @Nullable
  public SongWithParts getCurrentSongWithParts() {
    return currentSongWithParts;
  }

  @Nullable
  public String getCurrentSong() {
    if (currentSongName != null) {
      return currentSongName;
    } else {
      return null;
    }
  }

  public void setCurrentSong(@Nullable String songName, int partIndex, boolean restart) {
    currentSongName = songName;
    if (songName != null) {
      executorService.execute(() -> {
        currentSongWithParts = db.songDao().getSongWithPartsByName(songName);
        if (currentSongWithParts != null) {
          setCurrentPartIndex(partIndex, restart);
        } else {
          Log.e(TAG, "setCurrentSong: song '" + songName + "' not found");
        }
      });
    } else {
      currentSongWithParts = null;
    }
    sharedPrefs.edit().putString(PREF.SONG_CURRENT, songName).apply();
  }

  public int getCurrentPartIndex() {
    return currentPartIndex;
  }

  private boolean hasNextPart() {
    return currentSongWithParts != null && currentPartIndex
        < currentSongWithParts.getParts().size() - 1;
  }

  public void setCurrentPartIndex(int index, boolean restart) {
    currentPartIndex = index;
    if (currentSongWithParts == null) {
      Log.e(TAG, "setCurrentPartIndex: song '" + currentSongName + "' is null");
      return;
    }
    List<Part> parts = currentSongWithParts.getParts();
    if (!parts.isEmpty()) {
      setConfig(parts.get(index).toConfig(), restart);
      if (restart) {
        restartIfPlaying(true);
      }
      sharedPrefs.edit().putInt(PREF.PART_CURRENT, index).apply();
    } else {
      Log.e(TAG, "setCurrentPartIndex: no part found for song '" + currentSongName + "'");
      return;
    }
    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeSongOrPartChanged(currentSongWithParts, currentPartIndex);
      }
    }
  }

  private void resetHandlersIfRequired() {
    if (!fromService) {
      return;
    }
    if (tickThread == null || !tickThread.isAlive()) {
      tickThread = new HandlerThread("metronome_ticks");
      tickThread.start();
      removeHandlerCallbacks();
      tickHandler = new Handler(tickThread.getLooper());
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
      stop(false, false);
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

    alwaysVibrate = true;
    setGain(0);
    setBeatModeVibrate(false);
    start(false);
  }

  public void destroy() {
    listeners.clear();
    if (fromService) {
      removeHandlerCallbacks();
      tickThread.quit();
      callbackThread.quit();
      audioUtil.destroy();
    }
  }

  public void addListener(MetronomeListener listener) {
    listeners.add(listener);
  }

  public void addListeners(Set<MetronomeListener> listeners) {
    this.listeners.addAll(listeners);
  }

  public void removeListener(MetronomeListener listener) {
    listeners.remove(listener);
  }

  public Set<MetronomeListener> getListeners() {
    return Collections.unmodifiableSet(listeners);
  }

  public void start() {
    start(false);
  }

  public void start(boolean isRestarted) {
    if (!NotificationUtil.hasPermission(context)) {
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomePermissionMissing();
        }
      }
      return;
    }
    // notify system for shortcut usage prediction
    shortcutUtil.reportUsage(currentSongName);
    // update last played for song
    if (currentSongWithParts != null) {
      executorService.execute(() -> {
        currentSongWithParts.getSong().setLastPlayed(System.currentTimeMillis());
        db.songDao().updateSong(currentSongWithParts.getSong());
      });
    }

    if (isPlaying() && !isRestarted) {
      return;
    }
    if (!fromService) {
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomeConnectionMissing();
        }
      }
      return;
    } else {
      resetHandlersIfRequired();
    }

    playing = true;
    audioUtil.play();
    tickIndex = 0;
    isMuted = false;
    if (isMuteActive()) {
      // updateMuteHandler would be too late
      muteCountDown = calculateMuteCount(false);
    }
    tickHandler.post(new Runnable() {
      @Override
      public void run() {
        if (isPlaying()) {
          tickHandler.postDelayed(this, getInterval() / getSubdivisionsCount());
          Tick tick = performTick();
          if (tick != null) {
            audioUtil.writeTickPeriod(tick, config.getTempo(), getSubdivisionsCount());
            tickIndex++;
          }
        }
      }
    });

    isCountingIn = isCountInActive();
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

    if (!isRestarted) {
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomeStart();
        }
      }
    }
    Log.i(TAG, "start: started metronome handler");
  }

  public void stop() {
    stop(resetTimerOnStop, false);
  }

  public void stop(boolean resetTimer, boolean isRestarted) {
    if (!isPlaying()) {
      return;
    }
    timerProgress = getTimerProgress(); // must be called before playing is set to false
    boolean isTimerReset = false;
    if (resetTimer || equalsTimerProgress(1)) {
      timerProgress = 0;
      isTimerReset = true;
    }
    elapsedPrevious = elapsedTime;

    playing = false;
    audioUtil.stop();
    isCountingIn = false;

    if (fromService) {
      removeHandlerCallbacks();
    }

    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        if (!isRestarted) {
          listener.onMetronomeStop();
        }
        if (isTimerReset) {
          listener.onMetronomeTimerProgressOneTime();
        }
      }
    }
    Log.i(TAG, "stop: stopped metronome handler");
  }

  public void restartIfPlaying(boolean resetTimer) {
    if (isPlaying()) {
      stop(resetTimer, true);
      start(true);
    } else if (resetTimer) {
      timerProgress = 0;
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomeTimerProgressOneTime();
        }
      }
    }
  }

  public boolean isPlaying() {
    return playing;
  }

  public void setBeats(String[] beats, boolean restart) {
    config.setBeats(beats);
    sharedPrefs.edit().putString(PREF.BEATS, String.join(",", beats)).apply();
    if (restart && isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
      restartIfPlaying(false);
    }
  }

  public String[] getBeats() {
    return config.getBeats();
  }

  public int getBeatsCount() {
    return config.getBeats().length;
  }

  public void setBeat(int beat, String tickType) {
    String[] beats = getBeats();
    beats[beat] = tickType;
    setBeats(beats, true);
  }

  public boolean addBeat() {
    if (getBeatsCount() >= Constants.BEATS_MAX) {
      return false;
    }
    String[] beats = Arrays.copyOf(config.getBeats(), getBeatsCount() + 1);
    beats[beats.length - 1] = TICK_TYPE.NORMAL;
    setBeats(beats, true);
    return true;
  }

  public boolean removeBeat() {
    if (getBeatsCount() <= 1) {
      return false;
    }
    setBeats(Arrays.copyOf(config.getBeats(), getBeatsCount() - 1), true);
    return true;
  }

  public void setSubdivisions(String[] subdivisions) {
    config.setSubdivisions(subdivisions);
    sharedPrefs.edit()
        .putString(PREF.SUBDIVISIONS, String.join(",", getSubdivisions()))
        .apply();
  }

  public String[] getSubdivisions() {
    return config.getSubdivisions();
  }

  public int getSubdivisionsCount() {
    return config.getSubdivisions().length;
  }

  public boolean isSubdivisionActive() {
    return getSubdivisionsCount() > 1;
  }

  public void setSubdivision(int subdivision, String tickType) {
    String[] subdivisions = getSubdivisions();
    subdivisions[subdivision] = tickType;
    setSubdivisions(subdivisions);
  }

  public boolean addSubdivision() {
    if (getSubdivisionsCount() >= Constants.SUBS_MAX) {
      return false;
    }
    String[] subdivisions = Arrays.copyOf(
        config.getSubdivisions(), getSubdivisionsCount() + 1
    );
    subdivisions[subdivisions.length - 1] = TICK_TYPE.SUB;
    setSubdivisions(subdivisions);
    return true;
  }

  public boolean removeSubdivision() {
    if (getSubdivisionsCount() <= 1) {
      return false;
    }
    setSubdivisions(Arrays.copyOf(config.getSubdivisions(), getSubdivisionsCount() - 1));
    return true;
  }

  public void setSwing3() {
    setSubdivisions(String.join(
        ",", TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL).split(","));
  }

  public boolean isSwing3() {
    String triplet = String.join(",", TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.SUB);
    String tripletAlt = String.join(
        ",", TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL
    );
    String subdivisions = String.join(",", getSubdivisions());
    return subdivisions.equals(triplet) || subdivisions.equals(tripletAlt);
  }

  public void setSwing5() {
    setSubdivisions(String.join(
        ",",
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL, TICK_TYPE.MUTED
    ).split(","));
  }

  public boolean isSwing5() {
    String quintuplet = String.join(
        ",",
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.SUB, TICK_TYPE.MUTED
    );
    String quintupletAlt = String.join(
        ",",
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL, TICK_TYPE.MUTED
    );
    String subdivisions = String.join(",", getSubdivisions());
    return subdivisions.equals(quintuplet) || subdivisions.equals(quintupletAlt);
  }

  public void setSwing7() {
    setSubdivisions(String.join(
        ",",
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED,
        TICK_TYPE.NORMAL, TICK_TYPE.MUTED, TICK_TYPE.MUTED
    ).split(","));
  }

  public boolean isSwing7() {
    String septuplet = String.join(
        ",",
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED,
        TICK_TYPE.SUB, TICK_TYPE.MUTED, TICK_TYPE.MUTED
    );
    String septupletAlt = String.join(
        ",",
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED,
        TICK_TYPE.NORMAL, TICK_TYPE.MUTED, TICK_TYPE.MUTED
    );
    String subdivisions = String.join(",", getSubdivisions());
    return subdivisions.equals(septuplet) || subdivisions.equals(septupletAlt);
  }

  public boolean isSwingActive() {
    return isSwing3() || isSwing5() || isSwing7();
  }

  public void setTempo(int tempo) {
    if (config.getTempo() != tempo) {
      config.setTempo(tempo);
      sharedPrefs.edit().putInt(PREF.TEMPO, tempo).apply();
      if (isPlaying() && isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
        updateTimerHandler(false, true);
      }
    }
  }

  public int getTempo() {
    return config.getTempo();
  }

  private void changeTempo(int change) {
    int tempoOld = getTempo();
    int tempoNew = tempoOld + change;
    // setTempo will only be called by callback below, else we would break timer animation
    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeTempoChanged(tempoOld, tempoNew);
      }
    }
  }

  public long getInterval() {
    return 1000 * 60 / config.getTempo();
  }

  public void setSound(String sound) {
    audioUtil.setSound(sound);
    sharedPrefs.edit().putString(PREF.SOUND, sound).apply();
  }

  public String getSound() {
    return sharedPrefs.getString(PREF.SOUND, DEF.SOUND);
  }

  public void setBeatModeVibrate(boolean vibrate) {
    if (!hapticUtil.hasVibrator()) {
      vibrate = false;
    }
    beatModeVibrate = vibrate;
    audioUtil.setMuted(vibrate);
    hapticUtil.setEnabled(vibrate || alwaysVibrate);
    sharedPrefs.edit().putBoolean(PREF.BEAT_MODE_VIBRATE, vibrate).apply();
  }

  public boolean isBeatModeVibrate() {
    return beatModeVibrate;
  }

  public void setAlwaysVibrate(boolean always) {
    alwaysVibrate = always;
    hapticUtil.setEnabled(always || beatModeVibrate);
    sharedPrefs.edit().putBoolean(PREF.ALWAYS_VIBRATE, always).apply();
  }

  public boolean isAlwaysVibrate() {
    return alwaysVibrate;
  }

  public boolean areHapticEffectsPossible() {
    return !isPlaying() || (!beatModeVibrate && !alwaysVibrate);
  }

  public void setLatency(long offset) {
    latency = offset;
    sharedPrefs.edit().putLong(PREF.LATENCY, offset).apply();
  }

  public long getLatency() {
    return latency;
  }

  public void setIgnoreFocus(boolean ignore) {
    audioUtil.setIgnoreFocus(ignore);
    sharedPrefs.edit().putBoolean(PREF.IGNORE_FOCUS, ignore).apply();
  }

  public boolean getIgnoreAudioFocus() {
    return audioUtil.getIgnoreFocus();
  }

  public void setGain(int gain) {
    audioUtil.setGain(gain);
    sharedPrefs.edit().putInt(PREF.GAIN, gain).apply();
  }

  public int getGain() {
    return audioUtil.getGain();
  }

  public boolean neverStartedWithGainBefore() {
    return neverStartedWithGain;
  }

  public void setFlashScreen(boolean flash) {
    flashScreen = flash;
    sharedPrefs.edit().putBoolean(PREF.FLASH_SCREEN, flash).apply();
  }

  public boolean getFlashScreen() {
    return flashScreen;
  }

  public void setKeepAwake(boolean keepAwake) {
    this.keepAwake = keepAwake;
    sharedPrefs.edit().putBoolean(PREF.KEEP_AWAKE, keepAwake).apply();
  }

  public boolean getKeepAwake() {
    return keepAwake;
  }

  public void setCountIn(int bars) {
    config.setCountIn(bars);
    sharedPrefs.edit().putInt(PREF.COUNT_IN, bars).apply();
  }

  public int getCountIn() {
    return config.getCountIn();
  }

  public boolean isCountInActive() {
    return config.getCountIn() > 0;
  }

  public boolean isCountingIn() {
    return isCountingIn;
  }

  public long getCountInInterval() {
    return getInterval() * getBeatsCount() * config.getCountIn();
  }

  public void setIncrementalAmount(int bpm) {
    config.setIncrementalAmount(bpm);
    sharedPrefs.edit().putInt(PREF.INCREMENTAL_AMOUNT, bpm).apply();
    updateIncrementalHandler();
  }

  public int getIncrementalAmount() {
    return config.getIncrementalAmount();
  }

  public boolean isIncrementalActive() {
    return config.getIncrementalAmount() > 0;
  }

  public void setIncrementalIncrease(boolean increase) {
    config.setIncrementalIncrease(increase);
    sharedPrefs.edit().putBoolean(PREF.INCREMENTAL_INCREASE, increase).apply();
  }

  public boolean isIncrementalIncrease() {
    return config.isIncrementalIncrease();
  }

  public void setIncrementalInterval(int interval) {
    config.setIncrementalInterval(interval);
    sharedPrefs.edit().putInt(PREF.INCREMENTAL_INTERVAL, interval).apply();
    updateIncrementalHandler();
  }

  public int getIncrementalInterval() {
    return config.getIncrementalInterval();
  }

  public void setIncrementalUnit(String unit) {
    if (unit.equals(config.getIncrementalUnit())) {
      return;
    }
    config.setIncrementalUnit(unit);
    sharedPrefs.edit().putString(PREF.INCREMENTAL_UNIT, unit).apply();
    updateIncrementalHandler();
  }

  public String getIncrementalUnit() {
    return config.getIncrementalUnit();
  }

  public void setIncrementalLimit(int limit) {
    config.setIncrementalLimit(limit);
    sharedPrefs.edit().putInt(PREF.INCREMENTAL_LIMIT, limit).apply();
  }

  public int getIncrementalLimit() {
    return config.getIncrementalLimit();
  }

  private void updateIncrementalHandler() {
    if (!fromService || !isPlaying()) {
      return;
    }
    incrementalHandler.removeCallbacksAndMessages(null);
    String unit = config.getIncrementalUnit();
    int amount = config.getIncrementalAmount();
    int limit = config.getIncrementalLimit();
    boolean increase = config.isIncrementalIncrease();
    if (!unit.equals(UNIT.BARS) && isIncrementalActive()) {
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
    if (!fromService || !isPlaying()) {
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

  public int getTimerDuration() {
    return config.getTimerDuration();
  }

  public boolean isTimerActive() {
    return config.getTimerDuration() > 0;
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
        factor = getInterval() * getBeatsCount();
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

  public String getTimerUnit() {
    return config.getTimerUnit();
  }

  public void setResetTimerOnStop(boolean reset) {
    resetTimerOnStop = reset;
    sharedPrefs.edit().putBoolean(PREF.RESET_TIMER_ON_STOP, reset).apply();
  }

  public boolean getResetTimerOnStop() {
    return resetTimerOnStop;
  }

  public float getTimerProgress() {
    if (isTimerActive()) {
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

  public boolean equalsTimerProgress(float fraction) {
    try {
      BigDecimal bdProgress = BigDecimal.valueOf(getTimerProgress()).setScale(
          2, RoundingMode.HALF_UP
      );
      BigDecimal bdFraction = new BigDecimal(fraction).setScale(2, RoundingMode.HALF_UP);
      return bdProgress.equals(bdFraction);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public void updateTimerHandler(float fraction, boolean startAtFirstBeat) {
    timerProgress = fraction;
    updateTimerHandler(startAtFirstBeat, false);
  }

  public void updateTimerHandler(boolean startAtFirstBeat, boolean performOneTime) {
    if (!fromService || !isPlaying()) {
      return;
    }
    timerHandler.removeCallbacksAndMessages(null);
    if (!isTimerActive()) {
      return;
    }

    if (equalsTimerProgress(1)) {
      timerProgress = 0;
    } else if (startAtFirstBeat) {
      // set timer progress on start of this bar
      long progressInterval = (long) (getTimerProgress() * getTimerInterval());
      long barInterval = getInterval() * getBeatsCount();
      int progressBarCount = (int) (progressInterval / barInterval);
      long progressIntervalFullBars = progressBarCount * barInterval;
      timerProgress = (float) progressIntervalFullBars / getTimerInterval();
    }

    if (!config.getTimerUnit().equals(UNIT.BARS)) {
      timerHandler.postDelayed(() -> {
        if (hasNextPart()) {
          setCurrentPartIndex(currentPartIndex + 1, true);
        } else if (currentSongWithParts != null && currentSongWithParts.getSong().isLooped()) {
          // Restart song
          setCurrentPartIndex(0, true);
        } else {
          stop();
          if (currentSongWithParts != null) {
            setCurrentPartIndex(0, false);
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

    synchronized (listeners) {
      for (MetronomeListener listener : listeners) {
        if (performOneTime) {
          listener.onMetronomeTimerProgressOneTime();
        } else {
          listener.onMetronomeTimerStarted();
        }
      }
    }
  }

  public void resetTimerNow() {
    if (isTimerActive()) {
      restartIfPlaying(true);
    }
  }

  public String getCurrentTimerString() {
    if (!isTimerActive()) {
      return "";
    }
    long elapsedTime = (long) (getTimerProgress() * getTimerInterval());
    int timerDuration = config.getTimerDuration();
    switch (config.getTimerUnit()) {
      case UNIT.SECONDS:
      case UNIT.MINUTES:
        int seconds = (int) (elapsedTime / 1000);
        // Decide whether to force hours for consistency with total time
        int totalHours = timerDuration / 3600;
        if (config.getTimerUnit().equals(UNIT.MINUTES)) {
          totalHours = timerDuration / 60;
        }
        return getTimeStringFromSeconds(seconds, totalHours > 0);
      default:
        long barInterval = getInterval() * getBeatsCount();
        int progressBarCount = Math.min((int) (elapsedTime / barInterval), timerDuration - 1);

        long elapsedTimeFullBars = progressBarCount * barInterval;
        long remaining = elapsedTime - elapsedTimeFullBars;
        int beatCount = Math.min((int) (remaining / getInterval()), getBeatsCount() - 1);

        String format = getBeatsCount() < 10 ? "%d.%01d" : "%d.%02d";
        return String.format(Locale.ENGLISH, format, progressBarCount + 1, beatCount + 1);
    }
  }

  public String getTotalTimeString() {
    if (!isTimerActive()) {
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

  public int getMutePlay() {
    return config.getMutePlay();
  }

  public boolean isMuteActive() {
    return config.getMutePlay() > 0;
  }

  public void setMuteMute(int mute) {
    config.setMuteMute(mute);
    sharedPrefs.edit().putInt(PREF.MUTE_MUTE, mute).apply();
    updateMuteHandler();
  }

  public int getMuteMute() {
    return config.getMuteMute();
  }

  public void setMuteUnit(String unit) {
    if (unit.equals(config.getMuteUnit())) {
      return;
    }
    config.setMuteUnit(unit);
    sharedPrefs.edit().putString(PREF.MUTE_UNIT, unit).apply();
    updateMuteHandler();
  }

  public String getMuteUnit() {
    return config.getMuteUnit();
  }

  public void setMuteRandom(boolean random) {
    config.setMuteRandom(random);
    sharedPrefs.edit().putBoolean(PREF.MUTE_RANDOM, random).apply();
    updateMuteHandler();
  }

  public boolean isMuteRandom() {
    return config.isMuteRandom();
  }

  private void updateMuteHandler() {
    if (!fromService || !isPlaying()) {
      return;
    }
    muteHandler.removeCallbacksAndMessages(null);
    isMuted = false;
    if (!config.getMuteUnit().equals(UNIT.BARS) && isMuteActive()) {
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

  private @Nullable Tick performTick() {
    int beat = getCurrentBeat();
    int subdivision = getCurrentSubdivision();
    String tickType = getCurrentTickType();

    long beatIndex = tickIndex / getSubdivisionsCount();
    long barIndex = beatIndex / getBeatsCount();
    long barIndexWithoutCountIn = barIndex - getCountIn();
    boolean isCountIn = barIndex < getCountIn();

    boolean isBeat = subdivision == 1;
    boolean isFirstBeat = ((tickIndex / getSubdivisionsCount()) % getBeatsCount()) == 0;

    if (isTimerActive() && config.getTimerUnit().equals(UNIT.BARS) && !isCountIn) {
      boolean increaseTimerProgress = barIndexWithoutCountIn != 0 || !isFirstBeat;
      if (increaseTimerProgress) {
        // Play the first beat without increasing
        float stepSize = 1f / (getTimerDuration() * getBeatsCount() * getSubdivisionsCount());
        timerProgress += stepSize;
        int steps = Math.round(timerProgress / stepSize);
        timerProgress = steps * stepSize; // to avoid rounding errors
      }
      if (timerProgress >= 1) {
        timerProgress = 1;
        if (hasNextPart()) {
          setCurrentPartIndex(currentPartIndex + 1, true);
        } else if (currentSongWithParts != null && currentSongWithParts.getSong().isLooped()) {
          // Restart song
          setCurrentPartIndex(0, true);
        } else {
          stop();
          if (currentSongWithParts != null) {
            setCurrentPartIndex(0, false);
          }
        }
        return null;
      }
    }

    if (isBeat && isFirstBeat) {
      if (isIncrementalActive() && config.getIncrementalUnit().equals(UNIT.BARS) && !isCountIn) {
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
      if (isMuteActive() && config.getMuteUnit().equals(UNIT.BARS) && !isCountIn) {
        if (muteCountDown > 0) {
          muteCountDown--;
        } else {
          isMuted = !isMuted;
          // Minus 1 because it's already the next bar
          muteCountDown = Math.max(calculateMuteCount(isMuted) - 1, 0);
        }
      }
    }

    Tick tick = new Tick(tickIndex, beat, subdivision, tickType, isMuted);

    latencyHandler.postDelayed(() -> {
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomePreTick(tick);
        }
      }
    }, Math.max(0, latency - Constants.BEAT_ANIM_OFFSET));
    latencyHandler.postDelayed(() -> {
      if ((beatModeVibrate || alwaysVibrate) && !isMuted) {
        switch (tick.type) {
          case TICK_TYPE.STRONG:
            hapticUtil.heavyClick();
            break;
          case TICK_TYPE.SUB:
            hapticUtil.tick();
            break;
          case TICK_TYPE.MUTED:
            break;
          default:
            hapticUtil.click();
        }
      }
      synchronized (listeners) {
        for (MetronomeListener listener : listeners) {
          listener.onMetronomeTick(tick);
        }
      }
    }, latency);

    return tick;
  }

  private int getCurrentBeat() {
    return (int) ((tickIndex / getSubdivisionsCount()) % config.getBeats().length) + 1;
  }

  private int getCurrentSubdivision() {
    return (int) (tickIndex % getSubdivisionsCount()) + 1;
  }

  private String getCurrentTickType() {
    int subdivisionsCount = getSubdivisionsCount();
    if ((tickIndex % subdivisionsCount) == 0) {
      String[] beats = config.getBeats();
      return beats[(int) ((tickIndex / subdivisionsCount) % beats.length)];
    } else {
      String[] subdivisions = config.getSubdivisions();
      return subdivisions[(int) (tickIndex % subdivisionsCount)];
    }
  }

  public boolean isFromService() {
    return fromService;
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
    void onMetronomeTimerProgressOneTime();
    void onMetronomeConfigChanged();
    void onMetronomeSongOrPartChanged(@Nullable SongWithParts song, int partIndex);
    void onMetronomeConnectionMissing();
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
    public void onMetronomeTimerProgressOneTime() {}
    public void onMetronomeConfigChanged() {}
    public void onMetronomeSongOrPartChanged(@Nullable SongWithParts song, int partIndex) {}
    public void onMetronomeConnectionMissing() {}
    public void onMetronomePermissionMissing() {}
  }

  public static class Tick {
    public final long index;
    public final int beat, subdivision;
    @NonNull
    public final String type;
    public final boolean isMuted;

    public Tick(long index, int beat, int subdivision, @NonNull String type, boolean isMuted) {
      this.index = index;
      this.beat = beat;
      this.subdivision = subdivision;
      this.type = type;
      this.isMuted = isMuted;
    }

    @NonNull
    @Override
    public String toString() {
      return "Tick{index = " + index +
          ", beat=" + beat +
          ", sub=" + subdivision +
          ", type=" + type +
          ", muted=" + isMuted + '}';
    }
  }
}