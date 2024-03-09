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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;

public class MetronomeUtil {

  private static final String TAG = MetronomeUtil.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final AudioUtil audioUtil;
  private final HapticUtil hapticUtil;
  private final Set<MetronomeListener> listeners = new HashSet<>();
  public final boolean fromService;
  private HandlerThread audioThread, callbackThread;
  private Handler tickHandler, latencyHandler;
  private List<String> beats, subdivisions;
  private int tempo;
  private long tickIndex, latency;
  private boolean playing, tempPlaying, useSubdivisions, beatModeVibrate;
  private boolean alwaysVibrate, flashScreen, keepAwake;
  private boolean neverStartedWithGain = true;

  public MetronomeUtil(@NonNull Context context, boolean fromService) {
    this.fromService = fromService;

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    audioUtil = new AudioUtil(context, this::stop);
    hapticUtil = new HapticUtil(context);

    resetHandlersIfRequired();
    setToPreferences();
  }

  public void setToPreferences() {
    tempo = sharedPrefs.getInt(PREF.TEMPO, DEF.TEMPO);
    beats = arrayAsList(sharedPrefs.getString(PREF.BEATS, DEF.BEATS).split(","));
    subdivisions = arrayAsList(
        sharedPrefs.getString(PREF.SUBDIVISIONS, DEF.SUBDIVISIONS).split(",")
    );
    useSubdivisions = sharedPrefs.getBoolean(PREF.USE_SUBS, DEF.USE_SUBS);
    latency = sharedPrefs.getLong(PREF.LATENCY, DEF.LATENCY);
    alwaysVibrate = sharedPrefs.getBoolean(PREF.ALWAYS_VIBRATE, DEF.ALWAYS_VIBRATE);
    keepAwake = sharedPrefs.getBoolean(PREF.KEEP_AWAKE, DEF.KEEP_AWAKE);

    setSound(sharedPrefs.getString(PREF.SOUND, DEF.SOUND));
    setIgnoreFocus(sharedPrefs.getBoolean(PREF.IGNORE_FOCUS, DEF.IGNORE_FOCUS));
    setGain(sharedPrefs.getInt(PREF.GAIN, DEF.GAIN));
    setBeatModeVibrate(sharedPrefs.getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE));
  }

  private static List<String> arrayAsList(String[] array) {
    return new ArrayList<>(Arrays.asList(array));
  }

  private void resetHandlersIfRequired() {
    if (!fromService) {
      return;
    }
    if (audioThread == null || !audioThread.isAlive()) {
      audioThread = new HandlerThread("metronome_audio");
      audioThread.start();
      removeHandlerCallbacks();
      tickHandler = new Handler(audioThread.getLooper());
    }
    if (callbackThread == null || !callbackThread.isAlive()) {
      callbackThread = new HandlerThread("metronome_callback");
      callbackThread.start();
      removeHandlerCallbacks();
      latencyHandler = new Handler(callbackThread.getLooper());
    }
  }

  private void removeHandlerCallbacks() {
    if (tickHandler != null) {
      tickHandler.removeCallbacksAndMessages(null);
    }
    if (latencyHandler != null) {
      latencyHandler.removeCallbacksAndMessages(null);
    }
  }

  public void savePlayingState() {
    tempPlaying = isPlaying();
  }

  public void restorePlayingState() {
    if (tempPlaying) {
      start();
    } else {
      stop();
    }
  }

  public void setUpLatencyCalibration() {
    tempo = 80;
    beats = arrayAsList(DEF.BEATS.split(","));
    subdivisions = arrayAsList(DEF.SUBDIVISIONS.split(","));
    alwaysVibrate = true;
    setGain(0);
    setBeatModeVibrate(false);
    start();
  }

  public void destroy() {
    listeners.clear();
    if (fromService) {
      removeHandlerCallbacks();
      audioThread.quitSafely();
      callbackThread.quit();
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
    if (isPlaying()) {
      return;
    }
    if (!fromService) {
      return;
    } else {
      resetHandlersIfRequired();
    }

    playing = true;
    audioUtil.play();
    tickIndex = 0;
    tickHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (isPlaying()) {
          tickHandler.postDelayed(this, getInterval() / getSubdivisionsCount());
          Tick tick = new Tick(
              tickIndex, getCurrentBeat(), getCurrentSubdivision(), getCurrentTickType()
          );
          performTick(tick);
          audioUtil.tick(tick, tempo, getSubdivisionsCount());
          tickIndex++;
        }
      }
    }, 100); // Fix distorted first sound caused by AudioTrack setup

    if (getGain() > 0) {
      neverStartedWithGain = false;
    }

    for (MetronomeListener listener : listeners) {
      listener.onMetronomeStart();
    }
    Log.i(TAG, "start: started metronome handler");
  }

  public void stop() {
    if (!isPlaying()) {
      return;
    }
    playing = false;
    audioUtil.stop();

    if (fromService) {
      removeHandlerCallbacks();
    }

    for (MetronomeListener listener : listeners) {
      listener.onMetronomeStop();
    }
    Log.i(TAG, "stop: stopped metronome handler");
  }

  public void setPlaying(boolean playing) {
    if (playing) {
      start();
    } else {
      stop();
    }
  }

  public boolean isPlaying() {
    return playing;
  }

  public void setBeats(List<String> beats) {
    this.beats = beats;
    sharedPrefs.edit().putString(PREF.BEATS, String.join(",", beats)).apply();
  }

  public List<String> getBeats() {
    return beats;
  }

  public int getBeatsCount() {
    return beats.size();
  }

  public void setBeat(int beat, String tickType) {
    List<String> beats = getBeats();
    beats.set(beat, tickType);
    setBeats(beats);
  }

  public boolean addBeat() {
    if (beats.size() >= Constants.BEATS_MAX) {
      return false;
    }
    List<String> beats = getBeats();
    beats.add(TICK_TYPE.NORMAL);
    setBeats(beats);
    return true;
  }

  public boolean removeBeat() {
    if (beats.size() <= 1) {
      return false;
    }
    List<String> beats = getBeats();
    beats.remove(beats.size() - 1);
    setBeats(beats);
    return true;
  }

  public void setSubdivisions(List<String> subdivisions) {
    this.subdivisions = subdivisions;
    sharedPrefs.edit()
        .putString(PREF.SUBDIVISIONS, String.join(",", getSubdivisions()))
        .apply();
  }

  public List<String> getSubdivisions() {
    return useSubdivisions ? subdivisions : arrayAsList(DEF.SUBDIVISIONS.split(","));
  }

  public int getSubdivisionsCount() {
    return useSubdivisions ? subdivisions.size() : 1;
  }

  public void setSubdivision(int subdivision, String tickType) {
    List<String> subdivisions = getSubdivisions();
    subdivisions.set(subdivision, tickType);
    setSubdivisions(subdivisions);
  }

  public boolean addSubdivision() {
    if (subdivisions.size() >= Constants.SUBS_MAX) {
      return false;
    }
    List<String> subdivisions = getSubdivisions();
    subdivisions.add(TICK_TYPE.SUB);
    setSubdivisions(subdivisions);
    return true;
  }

  public boolean removeSubdivision() {
    if (subdivisions.size() <= 1) {
      return false;
    }
    List<String> subdivisions = getSubdivisions();
    subdivisions.remove(subdivisions.size() - 1);
    setSubdivisions(subdivisions);
    return true;
  }

  public void setSubdivisionsUsed(boolean used) {
    useSubdivisions = used;
    sharedPrefs.edit().putBoolean(PREF.USE_SUBS, used).apply();
  }

  public boolean getSubdivisionsUsed() {
    return useSubdivisions;
  }

  public void setSwing3() {
    setSubdivisions(List.of(TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL));
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
    setSubdivisions(List.of(
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL, TICK_TYPE.MUTED
    ));
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
    setSubdivisions(List.of(
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED,
        TICK_TYPE.NORMAL, TICK_TYPE.MUTED, TICK_TYPE.MUTED
    ));
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
    if (this.tempo != tempo) {
      this.tempo = tempo;
      sharedPrefs.edit().putInt(PREF.TEMPO, tempo).apply();
    }
  }

  public int getTempo() {
    return tempo;
  }

  public long getInterval() {
    return 1000 * 60 / tempo;
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

  public boolean getIgnoreFocus() {
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

  public void setKeepAwake(boolean keep) {
    keepAwake = keep;
    sharedPrefs.edit().putBoolean(PREF.KEEP_AWAKE, keep).apply();
  }

  public boolean getKeepAwake() {
    return keepAwake;
  }

  private void performTick(Tick tick) {
    latencyHandler.postDelayed(() -> {
      if (beatModeVibrate || alwaysVibrate) {
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
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeTick(tick);
      }
    }, latency);
  }

  private int getCurrentBeat() {
    return (int) ((tickIndex / getSubdivisionsCount()) % beats.size()) + 1;
  }

  private int getCurrentSubdivision() {
    return (int) (tickIndex % getSubdivisionsCount()) + 1;
  }

  private String getCurrentTickType() {
    int subdivisionsCount = getSubdivisionsCount();
    if ((tickIndex % subdivisionsCount) == 0) {
      return beats.get((int) ((tickIndex / subdivisionsCount) % beats.size()));
    } else {
      return subdivisions.get((int) (tickIndex % subdivisionsCount));
    }
  }

  public interface MetronomeListener {
    void onMetronomeStart();
    void onMetronomeStop();
    void onMetronomeTick(@NonNull Tick tick);
  }

  public static class Tick {
    public final long index;
    public final int beat, subdivision;
    @NonNull
    public final String type;

    public Tick(long index, int beat, int subdivision, @NonNull String type) {
      this.index = index;
      this.beat = beat;
      this.subdivision = subdivision;
      this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
      return "Tick{index = " + index +
          ", beat=" + beat +
          ", sub=" + subdivision +
          ", type=" + type + '}';
    }
  }
}