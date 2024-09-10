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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.TickType;
import xyz.zedler.patrick.tack.presentation.state.MainState;

public class MetronomeUtil {

  private static final String TAG = MetronomeUtil.class.getSimpleName();

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
  private boolean playing, beatModeVibrate;
  private boolean alwaysVibrate, flashScreen;
  private boolean neverStartedWithGain = true;

  public MetronomeUtil(@NonNull Context context, boolean fromService) {
    this.fromService = fromService;

    audioUtil = new AudioUtil(context, this::stop);
    hapticUtil = new HapticUtil(context);
    bookmarkUtil = new BookmarkUtil(context);
    notificationUtil = new NotificationUtil(context);

    resetHandlersIfRequired();
  }

  public void updateFromState(MainState state) {
    tempo = state.getTempo();
    beats = new ArrayList<>(state.getBeats());
    subdivisions = new ArrayList<>(state.getSubdivisions());
    latency = state.getLatency();
    flashScreen = state.getFlashScreen();

    setSound(state.getSound());
    setIgnoreFocus(state.getIgnoreFocus());
    setGain(state.getGain());
    setBeatModeVibrate(state.getBeatModeVibrate());
    setAlwaysVibrate(state.getAlwaysVibrate());
    setStrongVibration(state.getStrongVibration());
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

  public void setPlaying(boolean playing) {
    if (playing) {
      if (notificationUtil.hasPermission()) {
        start();
      } else {
        for (MetronomeListener listener : listeners) {
          listener.onPermissionMissing();
        }
      }
    } else {
      stop();
    }
  }

  public boolean isPlaying() {
    return playing;
  }

  public void setBeats(List<String> beats) {
    this.beats = beats;
  }

  public List<String> getBeats() {
    return beats;
  }

  public int getBeatsCount() {
    return beats.size();
  }

  public void changeBeat(int beat, String tickType) {
    List<String> beats = getBeats();
    beats.set(beat, tickType);
    setBeats(beats);
  }

  public void addBeat() {
    if (beats.size() < Constants.BEATS_MAX) {
      List<String> beats = getBeats();
      beats.add(TickType.NORMAL);
      setBeats(beats);
    }
  }

  public void removeBeat() {
    if (beats.size() > 1) {
      List<String> beats = getBeats();
      beats.remove(beats.size() - 1);
      setBeats(beats);
    }
  }

  public void setSubdivisions(List<String> subdivisions) {
    this.subdivisions = subdivisions;
  }

  public List<String> getSubdivisions() {
    return subdivisions;
  }

  public int getSubdivisionsCount() {
    return subdivisions.size();
  }

  public void changeSubdivision(int subdivision, String tickType) {
    List<String> subdivisions = getSubdivisions();
    subdivisions.set(subdivision, tickType);
    setSubdivisions(subdivisions);
  }

  public void addSubdivision() {
    if (subdivisions.size() < Constants.SUBS_MAX) {
      List<String> subdivisions = getSubdivisions();
      subdivisions.add(TickType.SUB);
      setSubdivisions(subdivisions);
    }
  }

  public void removeSubdivision() {
    if (subdivisions.size() > 1) {
      List<String> subdivisions = getSubdivisions();
      subdivisions.remove(subdivisions.size() - 1);
      setSubdivisions(subdivisions);
    }
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
    }
  }

  public int getTempo() {
    return tempo;
  }

  public void toggleBookmark() {
    int tempoNew = bookmarkUtil.toggleBookmark(tempo);
    setTempo(tempoNew);
  }

  public long getInterval() {
    return 1000 * 60 / tempo;
  }

  public void setSound(String sound) {
    audioUtil.setSound(sound);
  }

  public void setBeatModeVibrate(boolean vibrate) {
    if (!hapticUtil.hasVibrator()) {
      vibrate = false;
    }
    beatModeVibrate = vibrate;
    audioUtil.setMuted(vibrate);
    hapticUtil.setEnabled(vibrate || alwaysVibrate);
  }

  public boolean getBeatModeVibrate() {
    return beatModeVibrate;
  }

  public void setAlwaysVibrate(boolean always) {
    alwaysVibrate = always;
    hapticUtil.setEnabled(always || beatModeVibrate);
  }

  public boolean getAlwaysVibrate() {
    return alwaysVibrate;
  }

  public void setStrongVibration(boolean strong) {
    hapticUtil.setStrong(strong);
  }

  public boolean getStrongVibration() {
    return hapticUtil.getStrong();
  }

  public boolean areHapticEffectsPossible() {
    return !isPlaying() || (!beatModeVibrate && !alwaysVibrate);
  }

  public void setLatency(long latency) {
    this.latency = latency;
  }

  public long getLatency() {
    return latency;
  }

  public void setIgnoreFocus(boolean ignore) {
    audioUtil.setIgnoreFocus(ignore);
  }

  public boolean getIgnoreFocus() {
    return audioUtil.getIgnoreFocus();
  }

  public void setGain(int gain) {
    audioUtil.setGain(gain);
    if (gain > 0) {
      neverStartedWithGain = true;
    }
  }

  public int getGain() {
    return audioUtil.getGain();
  }

  public boolean neverStartedWithGainBefore() {
    return neverStartedWithGain;
  }

  public void setFlashScreen(boolean flash) {
    flashScreen = flash;
  }

  public boolean getFlashScreen() {
    return flashScreen;
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

  private static List<String> arrayAsList(String[] array) {
    return new ArrayList<>(Arrays.asList(array));
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