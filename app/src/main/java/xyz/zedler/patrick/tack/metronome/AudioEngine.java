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

  private final Context context;
  private final AudioManager audioManager;
  private final AudioListener listener;
  private long engineHandle;
  private int gain;
  private volatile boolean playing;
  private boolean muted, ignoreFocus;

  private native long nativeCreate();
  private native void nativeDestroy(long handle);
  private native boolean nativeStart(long handle);
  private native void nativeStop(long handle);
  private native void nativeSetTickData(long handle, int tickType, float[] data);
  private native void nativePlayTick(long handle, int tickType);
  private native void nativeSetMasterVolume(long handle, float volume);
  private native void nativeSetDuckingVolume(long handle, float volume);
  private native void nativeSetMuted(long handle, boolean muted);

  public AudioEngine(@NonNull Context context, @NonNull AudioListener listener) {
    this.context = context;
    this.listener = listener;

    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    engineHandle = nativeCreate();
    if (engineHandle == 0) {
      Log.e(TAG, "Failed to create Oboe engine");
      return;
    }

    setSound(DEF.SOUND);
    setGain(DEF.GAIN);
  }

  public void destroy() {
    stop();
    if (isInitialized()) {
      nativeDestroy(engineHandle);
      engineHandle = 0;
    }
  }

  private float dbToLinear(float db) {
    return (float) Math.pow(10.0, db / 20.0);
  }

  public void play() {
    if (playing || !isInitialized()) {
      return;
    }
    boolean success = nativeStart(engineHandle);
    if (success) {
      playing = true;
      requestAudioFocus();
    } else {
      Log.e(TAG, "Failed to start Oboe engine");
      playing = false;
      if (!ignoreFocus) {
        audioManager.abandonAudioFocus(this);
      }
    }
  }

  public void stop() {
    if (!playing || !isInitialized()) {
      return;
    }
    playing = false;

    if (!ignoreFocus) {
      audioManager.abandonAudioFocus(this);
    }

    if (isInitialized()) {
      nativeStop(engineHandle);
    }
    listener.onAudioStop();
  }

  private void requestAudioFocus() {
    if (ignoreFocus) {
      return;
    }
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(AudioUtil.getAttributes())
          .setWillPauseWhenDucked(true)
          .setOnAudioFocusChangeListener(this)
          .build();
      audioManager.requestAudioFocus(request);
    } else {
      audioManager.requestAudioFocus(
          this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
      );
    }
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
      nativeSetMasterVolume(engineHandle, dbToLinear((float) (gain * 100) / 100.0f));
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

  public void writeTickPeriod(Tick tick) {
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