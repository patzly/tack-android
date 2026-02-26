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

package xyz.zedler.patrick.tack.metronome;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.SOUND;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.Tick;
import xyz.zedler.patrick.tack.util.AudioUtil;

public class AudioEngine implements OnAudioFocusChangeListener {

  private static final String TAG = AudioEngine.class.getSimpleName();

  static {
    System.loadLibrary("oboe-audio-engine");
  }
  private static final int NATIVE_TICK_TYPE_STRONG = 1;
  private static final int NATIVE_TICK_TYPE_NORMAL = 2;
  private static final int NATIVE_TICK_TYPE_SUB = 3;
  private static final long STREAM_DELAY_SECONDS = 60;

  private final Context context;
  private final AudioManager audioManager;
  private final AudioListener listener;
  private final ScheduledExecutorService executor;
  private final AudioFocusRequest audioFocusRequest;
  private long engineHandle;
  private int gain;
  private volatile boolean playing, streamRunning;
  private boolean muted, ignoreFocus;
  private ScheduledFuture<?> delayedStopTask;

  private native long nativeCreate();
  private native void nativeDestroy(long handle);
  private native boolean nativeInit(long handle);
  private native boolean nativeStart(long handle);
  private native boolean nativeStop(long handle);
  private native void nativeSetTickData(long handle, int tickType, float[] data);
  private native void nativePlayTick(long handle, int tickType);
  private native void nativeSetMasterVolume(long handle, float volume);
  private native void nativeSetDuckingVolume(long handle, float volume);
  private native void nativeSetMuted(long handle, boolean muted);

  public AudioEngine(@NonNull Context context, @NonNull AudioListener listener) {
    this.context = context;
    this.listener = listener;

    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    executor = Executors.newSingleThreadScheduledExecutor();

    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(AudioUtil.getAttributes())
          .setWillPauseWhenDucked(true)
          .setOnAudioFocusChangeListener(this)
          .build();
    } else {
      audioFocusRequest = null;
    }

    engineHandle = nativeCreate();
    if (engineHandle == 0) {
      Log.e(TAG, "Failed to create Oboe engine");
      return;
    }
    boolean initSuccess = nativeInit(engineHandle);
    if (!initSuccess) {
      Log.e(TAG, "Failed to init Oboe audio stream");
      return;
    }

    setSound(DEF.SOUND);
    setGain(DEF.GAIN);
  }

  public void destroy() {
    executor.shutdownNow();
    stop();
    if (isInitialized()) {
      nativeDestroy(engineHandle);
      engineHandle = 0;
    }
  }

  public void warmUp() {
    if (!isInitialized()) {
      return;
    }
    cancelDelayedStop();

    if (!streamRunning) {
      boolean success = nativeStart(engineHandle);
      if (success) {
        streamRunning = true;
      } else {
        Log.e(TAG, "Failed to warm up Oboe audio stream");
      }
    }

    scheduleStreamShutdown();
  }

  public void play() {
    if (!isInitialized()) {
      return;
    }

    cancelDelayedStop();

    if (!streamRunning) {
      boolean success = nativeStart(engineHandle);
      if (success) {
        streamRunning = true;
      } else {
        Log.e(TAG, "Failed to start Oboe audio stream");
        return;
      }
    }

    if (!playing) {
      playing = true;
      requestAudioFocus();
    }
  }

  public void stop() {
    if (!isInitialized()) {
      return;
    }
    cancelDelayedStop();

    boolean success = nativeStop(engineHandle);
    if (success) {
      streamRunning = false;
      playing = false;

      if (!ignoreFocus && VERSION.SDK_INT >= VERSION_CODES.O) {
        audioManager.abandonAudioFocusRequest(audioFocusRequest);
      } else if (!ignoreFocus) {
        audioManager.abandonAudioFocus(this);
      }
    } else {
      Log.e(TAG, "Failed to stop Oboe engine");
    }
  }

  public void scheduleDelayedStop() {
    if (!playing) {
      return;
    }
    playing = false;

    if (!ignoreFocus && VERSION.SDK_INT >= VERSION_CODES.O) {
      audioManager.abandonAudioFocusRequest(audioFocusRequest);
    } else if (!ignoreFocus) {
      audioManager.abandonAudioFocus(this);
    }

    listener.onAudioStop();
    scheduleStreamShutdown();
  }

  @Override
  public void onAudioFocusChange(int focusChange) {
    if (!isInitialized()) {
      return;
    }
    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
      nativeSetDuckingVolume(engineHandle, 1.0f);
    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
      stop();
    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
      nativeSetDuckingVolume(engineHandle, 0.25f);
    }
  }

  public void setSound(String sound) {
    int resIdNormal, resIdStrong, resIdSub;
    Pitch pitchNormal = Pitch.NORMAL;
    Pitch pitchStrong = Pitch.HIGH;
    Pitch pitchSub = Pitch.LOW;
    switch (sound) {
      case SOUND.WOOD:
        resIdNormal = R.raw.wood;
        resIdStrong = R.raw.wood;
        resIdSub = R.raw.wood;
        break;
      case SOUND.MECHANICAL:
        resIdNormal = R.raw.mechanical_tick;
        resIdStrong = R.raw.mechanical_ding;
        resIdSub = R.raw.mechanical_knock;
        pitchStrong = Pitch.NORMAL;
        pitchSub = Pitch.NORMAL;
        break;
      case SOUND.BEATBOXING_1:
        resIdNormal = R.raw.beatbox_snare1;
        resIdStrong = R.raw.beatbox_kick1;
        resIdSub = R.raw.beatbox_hihat1;
        pitchStrong = Pitch.NORMAL;
        pitchSub = Pitch.NORMAL;
        break;
      case SOUND.BEATBOXING_2:
        resIdNormal = R.raw.beatbox_snare2;
        resIdStrong = R.raw.beatbox_kick2;
        resIdSub = R.raw.beatbox_hihat2;
        pitchStrong = Pitch.NORMAL;
        pitchSub = Pitch.NORMAL;
        break;
      case SOUND.HANDS:
        resIdNormal = R.raw.hands_hit;
        resIdStrong = R.raw.hands_clap;
        resIdSub = R.raw.hands_snap;
        pitchStrong = Pitch.NORMAL;
        pitchSub = Pitch.NORMAL;
        break;
      case SOUND.FOLDING:
        resIdNormal = R.raw.folding_knock;
        resIdStrong = R.raw.folding_fold;
        resIdSub = R.raw.folding_tap;
        pitchStrong = Pitch.NORMAL;
        pitchSub = Pitch.NORMAL;
        break;
      default:
        resIdNormal = R.raw.sine;
        resIdStrong = R.raw.sine;
        resIdSub = R.raw.sine;
        break;
    }

    if (isInitialized()) {
      nativeSetTickData(engineHandle, NATIVE_TICK_TYPE_NORMAL, loadAudio(resIdNormal, pitchNormal));
      nativeSetTickData(engineHandle, NATIVE_TICK_TYPE_STRONG, loadAudio(resIdStrong, pitchStrong));
      nativeSetTickData(engineHandle, NATIVE_TICK_TYPE_SUB, loadAudio(resIdSub, pitchSub));
    }
  }

  public void setGain(int gain) {
    this.gain = gain;
    if (isInitialized()) {
      float dbToLinear = (float) Math.pow(10.0, gain / 20.0);
      nativeSetMasterVolume(engineHandle, dbToLinear);
    }
  }

  public int getGain() {
    return gain;
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
    if (isInitialized()) {
      nativeSetMuted(engineHandle, muted);
    }
  }

  public void setIgnoreFocus(boolean ignore) {
    ignoreFocus = ignore;
  }

  public boolean getIgnoreFocus() {
    return ignoreFocus;
  }

  public void playTick(Tick tick) {
    if (!playing || !isInitialized() || muted || tick.isMuted) {
      return;
    }

    int nativeTickType;
    switch (tick.type) {
      case TICK_TYPE.STRONG:
        nativeTickType = NATIVE_TICK_TYPE_STRONG;
        break;
      case TICK_TYPE.SUB:
        nativeTickType = NATIVE_TICK_TYPE_SUB;
        break;
      case TICK_TYPE.MUTED:
      case TICK_TYPE.BEAT_SUB_MUTED:
        // silence instead
        return;
      default:
        nativeTickType = NATIVE_TICK_TYPE_NORMAL;
        break;
    }
    nativePlayTick(engineHandle, nativeTickType);
  }

  private float[] loadAudio(@RawRes int resId, Pitch pitch) {
    try (InputStream stream = context.getResources().openRawResource(resId)) {
      return adjustPitch(AudioUtil.readDataFromWavFloat(stream), pitch);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private float[] adjustPitch(float[] originalData, Pitch pitch) {
    if (pitch == Pitch.HIGH) {
      float[] newData = new float[originalData.length / 2];
      for (int i = 0; i < newData.length; i++) {
        newData[i] = originalData[i * 2];
      }
      return newData;
    } else if (pitch == Pitch.LOW) {
      float[] newData = new float[originalData.length * 2];
      for (int i = 0, j = 0; i < originalData.length; i++, j += 2) {
        newData[j] = originalData[i];
        newData[j + 1] = originalData[i];
      }
      return newData;
    } else {
      return originalData;
    }
  }

  private void scheduleStreamShutdown() {
    if (!streamRunning) {
      return;
    }

    try {
      delayedStopTask = executor.schedule(() -> {
        if (!playing && streamRunning) {
          stop();
        }
      }, STREAM_DELAY_SECONDS, TimeUnit.SECONDS);
    } catch (Exception e) {
      Log.e(TAG, "scheduleStreamShutdown: failed to schedule stream stop", e);
    }
  }

  private void cancelDelayedStop() {
    if (delayedStopTask != null && !delayedStopTask.isDone()) {
      delayedStopTask.cancel(false);
    }
  }

  private void requestAudioFocus() {
    if (ignoreFocus) {
      return;
    }
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      audioManager.requestAudioFocus(audioFocusRequest);
    } else {
      audioManager.requestAudioFocus(
          this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
      );
    }
  }

  private boolean isInitialized() {
    return engineHandle != 0;
  }

  private enum Pitch {
    NORMAL, HIGH, LOW
  }

  public interface AudioListener {
    void onAudioStop();
  }
}