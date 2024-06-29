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
import android.os.Looper;
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
import xyz.zedler.patrick.tack.Constants.Def;
import xyz.zedler.patrick.tack.Constants.Pref;
import xyz.zedler.patrick.tack.Constants.TickType;

public class MetronomeUtil {

  private static final String TAG = MetronomeUtil.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final AudioUtil audioUtil;
  private final HapticUtil hapticUtil;
  private final BookmarkUtil bookmarkUtil;
  private final NotificationUtil notificationUtil;
  private final Set<MetronomeListener> listeners = new HashSet<>();
  public final boolean fromService;
  private HandlerThread audioThread, callbackThread;
  private Handler tickHandler, latencyHandler, flashHandler;
  private List<String> beats, subdivisions;
  private int tempo;
  private long tickIndex, latency;
  private boolean playing, useSubdivisions, beatModeVibrate;
  private boolean alwaysVibrate, flashScreen, keepAwake, reduceAnim;
  private boolean neverStartedWithGain = true;

  public MetronomeUtil(@NonNull Context context, boolean fromService) {
    this.fromService = fromService;

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    audioUtil = new AudioUtil(context, this::stop);
    hapticUtil = new HapticUtil(context);
    bookmarkUtil = new BookmarkUtil(context);
    notificationUtil = new NotificationUtil(context);

    resetHandlersIfRequired();
    setToPreferences();
  }

  public void setToPreferences() {
    tempo = sharedPrefs.getInt(Pref.TEMPO, Def.TEMPO);
    beats = arrayAsList(sharedPrefs.getString(Pref.BEATS, Def.BEATS).split(","));
    subdivisions = arrayAsList(
        sharedPrefs.getString(Pref.SUBDIVISIONS, Def.SUBDIVISIONS).split(",")
    );
    useSubdivisions = sharedPrefs.getBoolean(Pref.USE_SUBS, Def.USE_SUBS);
    latency = sharedPrefs.getLong(Pref.LATENCY, Def.LATENCY);
    alwaysVibrate = sharedPrefs.getBoolean(Pref.ALWAYS_VIBRATE, Def.ALWAYS_VIBRATE);
    flashScreen = sharedPrefs.getBoolean(Pref.FLASH_SCREEN, Def.FLASH_SCREEN);
    keepAwake = sharedPrefs.getBoolean(Pref.KEEP_AWAKE, Def.KEEP_AWAKE);
    reduceAnim = sharedPrefs.getBoolean(Pref.REDUCE_ANIM, Def.REDUCE_ANIM);

    setSound(sharedPrefs.getString(Pref.SOUND, Def.SOUND));
    setIgnoreFocus(sharedPrefs.getBoolean(Pref.IGNORE_FOCUS, Def.IGNORE_FOCUS));
    setGain(sharedPrefs.getInt(Pref.GAIN, Def.GAIN));
    setBeatModeVibrate(sharedPrefs.getBoolean(Pref.BEAT_MODE_VIBRATE, Def.BEAT_MODE_VIBRATE));
    setStrongVibration(sharedPrefs.getBoolean(Pref.STRONG_VIBRATION, Def.STRONG_VIBRATION));
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
    flashHandler = new Handler(Looper.getMainLooper());
  }

  private void removeHandlerCallbacks() {
    if (tickHandler != null) {
      tickHandler.removeCallbacksAndMessages(null);
    }
    if (latencyHandler != null) {
      latencyHandler.removeCallbacksAndMessages(null);
    }
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
    bookmarkUtil.reportUsage(tempo);
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
    tickHandler.post(new Runnable() {
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
    });

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

  public boolean setPlaying(boolean playing) {
    if (playing) {
      boolean hasPermission = notificationUtil.hasPermission();
      if (hasPermission) {
        start();
      } else {
        for (MetronomeListener listener : listeners) {
          listener.onPermissionMissing();
        }
      }
      return hasPermission;
    } else {
      stop();
      return true;
    }
  }

  public boolean isPlaying() {
    return playing;
  }

  public void setBeats(List<String> beats) {
    this.beats = beats;
    sharedPrefs.edit().putString(Pref.BEATS, String.join(",", beats)).apply();
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
    beats.add(TickType.NORMAL);
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
        .putString(Pref.SUBDIVISIONS, String.join(",", getSubdivisions()))
        .apply();
  }

  public List<String> getSubdivisions() {
    return useSubdivisions ? subdivisions : arrayAsList(Def.SUBDIVISIONS.split(","));
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
    subdivisions.add(TickType.SUB);
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

  public void setSwing3() {
    List<String> subdivisions = new ArrayList<>(
        List.of(TickType.MUTED, TickType.MUTED, TickType.NORMAL)
    );
    setSubdivisions(subdivisions);
  }

  public void setSwing5() {
    List<String> subdivisions = new ArrayList<>(List.of(
        TickType.MUTED, TickType.MUTED, TickType.MUTED, TickType.NORMAL, TickType.MUTED
    ));
    setSubdivisions(subdivisions);
  }

  public void setSwing7() {
    List<String> subdivisions = new ArrayList<>(List.of(
        TickType.MUTED, TickType.MUTED, TickType.MUTED, TickType.MUTED,
        TickType.NORMAL, TickType.MUTED, TickType.MUTED
    ));
    setSubdivisions(subdivisions);
  }

  public void setTempo(int tempo) {
    if (this.tempo != tempo) {
      this.tempo = tempo;
      sharedPrefs.edit().putInt(Pref.TEMPO, tempo).apply();
    }
  }

  public int getTempo() {
    return tempo;
  }

  public int toggleBookmark() {
    int tempoNew = bookmarkUtil.toggleBookmark(tempo);
    setTempo(tempoNew);
    return tempoNew;
  }

  public long getInterval() {
    return 1000 * 60 / tempo;
  }

  public void setSound(String sound) {
    audioUtil.setSound(sound);
    sharedPrefs.edit().putString(Pref.SOUND, sound).apply();
  }

  public String getSound() {
    return sharedPrefs.getString(Pref.SOUND, Def.SOUND);
  }

  public void setBeatModeVibrate(boolean vibrate) {
    if (!hapticUtil.hasVibrator()) {
      vibrate = false;
    }
    beatModeVibrate = vibrate;
    audioUtil.setMuted(vibrate);
    hapticUtil.setEnabled(vibrate || alwaysVibrate);
    sharedPrefs.edit().putBoolean(Pref.BEAT_MODE_VIBRATE, vibrate).apply();
  }

  public boolean isBeatModeVibrate() {
    return beatModeVibrate;
  }

  public void setAlwaysVibrate(boolean always) {
    alwaysVibrate = always;
    hapticUtil.setEnabled(always || beatModeVibrate);
    sharedPrefs.edit().putBoolean(Pref.ALWAYS_VIBRATE, always).apply();
  }

  public boolean isAlwaysVibrate() {
    return alwaysVibrate;
  }

  public void setStrongVibration(boolean strong) {
    hapticUtil.setStrong(strong);
    sharedPrefs.edit().putBoolean(Pref.STRONG_VIBRATION, strong).apply();
  }

  public boolean isStrongVibration() {
    return hapticUtil.getStrong();
  }

  public boolean areHapticEffectsPossible() {
    return !isPlaying() || (!beatModeVibrate && !alwaysVibrate);
  }

  public void setLatency(long offset) {
    latency = offset;
    sharedPrefs.edit().putLong(Pref.LATENCY, offset).apply();
  }

  public long getLatency() {
    return latency;
  }

  public void setIgnoreFocus(boolean ignore) {
    audioUtil.setIgnoreFocus(ignore);
    sharedPrefs.edit().putBoolean(Pref.IGNORE_FOCUS, ignore).apply();
  }

  public boolean getIgnoreFocus() {
    return audioUtil.getIgnoreFocus();
  }

  public void setGain(int gain) {
    audioUtil.setGain(gain);
    sharedPrefs.edit().putInt(Pref.GAIN, gain).apply();
  }

  public int getGain() {
    return audioUtil.getGain();
  }

  public boolean neverStartedWithGainBefore() {
    return neverStartedWithGain;
  }

  public void setFlashScreen(boolean flash) {
    flashScreen = flash;
    sharedPrefs.edit().putBoolean(Pref.FLASH_SCREEN, flash).apply();
  }

  public boolean getFlashScreen() {
    return flashScreen;
  }

  public void setKeepAwake(boolean keepAwake) {
    this.keepAwake = keepAwake;
    sharedPrefs.edit().putBoolean(Pref.KEEP_AWAKE, keepAwake).apply();
  }

  public boolean getKeepAwake() {
    return keepAwake;
  }

  public void setReduceAnim(boolean reduceAnim) {
    this.reduceAnim = reduceAnim;
    sharedPrefs.edit().putBoolean(Pref.REDUCE_ANIM, reduceAnim).apply();
  }

  public boolean getReduceAnim() {
    return reduceAnim;
  }

  private void performTick(Tick tick) {
    latencyHandler.postDelayed(() -> {
      for (MetronomeListener listener : listeners) {
        listener.onMetronomePreTick(tick);
      }
    }, Math.max(0, latency - Constants.BEAT_ANIM_OFFSET));
    latencyHandler.postDelayed(() -> {
      if (beatModeVibrate || alwaysVibrate) {
        switch (tick.type) {
          case TickType.STRONG:
            hapticUtil.heavyClick();
            break;
          case TickType.SUB:
            hapticUtil.tick();
            break;
          case TickType.MUTED:
            break;
          default:
            hapticUtil.click();
        }
      }
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeTick(tick);
      }
    }, latency);
    if (flashScreen) {
      flashHandler.postDelayed(() -> {
        for (MetronomeListener listener : listeners) {
          listener.onFlashScreenEnd();
        }
      }, latency + Constants.FLASH_SCREEN_DURATION);
    }
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
    void onMetronomePreTick(@NonNull Tick tick);
    void onMetronomeTick(@NonNull Tick tick);
    void onFlashScreenEnd();
    void onPermissionMissing();
  }

  public static class MetronomeListenerAdapter implements MetronomeListener {
    @Override
    public void onMetronomeStart() {}
    @Override
    public void onMetronomeStop() {}
    @Override
    public void onMetronomePreTick(@NonNull Tick tick) {}
    @Override
    public void onMetronomeTick(@NonNull Tick tick) {}
    @Override
    public void onFlashScreenEnd() {}
    @Override
    public void onPermissionMissing() {}
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