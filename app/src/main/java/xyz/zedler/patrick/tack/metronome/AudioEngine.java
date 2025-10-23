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
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import java.io.IOException;
import java.io.InputStream;
import xyz.zedler.patrick.tack.Constants.SOUND;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.Tick;
import xyz.zedler.patrick.tack.util.AudioUtil;

public class AudioEngine implements OnAudioFocusChangeListener {

  private static final String TAG = AudioEngine.class.getSimpleName();
  private static final boolean DEBUG = false;

  private static final int SAMPLE_RATE_IN_HZ = 48000;

  private final Context context;
  private final AudioManager audioManager;
  private final AudioListener listener;
  private final float[] silence;
  private HandlerThread audioThread;
  private Handler audioHandler;
  private AudioTrack audioTrack;
  private LoudnessEnhancer loudnessEnhancer;
  private int gain, volumeReductionDb;
  private boolean playing, muted, ignoreFocus;
  private float[] tickStrong, tickNormal, tickSub;
  private float[] scaledBuffer;

  public AudioEngine(@NonNull Context context, @NonNull AudioListener listener) {
    this.context = context;
    this.listener = listener;
    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    resetHandlersIfRequired();

    AudioFormat audioFormat = new AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
        .setSampleRate(SAMPLE_RATE_IN_HZ)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build();
    int bufferSize = AudioTrack.getMinBufferSize(
        audioFormat.getSampleRate(), audioFormat.getChannelMask(), audioFormat.getEncoding()
    );
    audioTrack = new AudioTrack(
        AudioUtil.getAttributes(),
        audioFormat,
        bufferSize,
        AudioTrack.MODE_STREAM,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    );

    silence = new float[bufferSize];
    scaledBuffer = new float[bufferSize];

    try {
      audioTrack.setVolume(AudioUtil.dbToLinearVolume(volumeReductionDb));
      loudnessEnhancer = new LoudnessEnhancer(audioTrack.getAudioSessionId());
      loudnessEnhancer.setTargetGain(Math.max(0, gain * 100));
      loudnessEnhancer.setEnabled(gain > 0);
    } catch (Exception e) {
      Log.e(TAG, "play: failed to initialize LoudnessEnhancer: ", e);
    }

    if (!isInitialized()) {
      return;
    }
    audioTrack.play();
    audioHandler.post(() -> {
      // pre-fill AudioTrack buffer to avoid initial glitches
      writeSilenceUntilPeriodFinished(0, bufferSize);
      audioTrack.pause();
      audioTrack.flush();
    });
  }

  public void destroy() {
    removeHandlerCallbacks();
    audioThread.quitSafely();

    try {
      if (isInitialized()) {
        audioTrack.stop();
      }
      audioTrack.flush();
      audioTrack.release();
      audioTrack = null;
    } catch (Exception e) {
      Log.e(TAG, "destroy: failed to release AudioTrack: ", e);
    }

    if (loudnessEnhancer != null) {
      try {
        loudnessEnhancer.release();
      } catch (RuntimeException e) {
        Log.e(TAG, "stop: failed to release LoudnessEnhancer resources: ", e);
      }
      loudnessEnhancer = null;
    }
  }

  private void resetHandlersIfRequired() {
    if (audioThread == null || !audioThread.isAlive()) {
      audioThread = new HandlerThread("audio");
      audioThread.setPriority(Thread.MAX_PRIORITY);
      audioThread.start();
      removeHandlerCallbacks();
      audioHandler = new Handler(audioThread.getLooper());
    }
  }

  private void removeHandlerCallbacks() {
    if (audioHandler != null) {
      audioHandler.removeCallbacksAndMessages(null);
    }
  }

  public void play() {
    resetHandlersIfRequired();

    try {
      if (isInitialized()) {
        audioTrack.play();
      }
      playing = true;
    } catch (Exception e) {
      Log.e(TAG, "play: failed to start AudioTrack: ", e);
    }

    if (!ignoreFocus && VERSION.SDK_INT >= VERSION_CODES.O) {
      AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(AudioUtil.getAttributes())
          .setWillPauseWhenDucked(true)
          .setOnAudioFocusChangeListener(this)
          .build();
      audioManager.requestAudioFocus(request);
    } else if (!ignoreFocus) {
      audioManager.requestAudioFocus(
          this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
      );
    }
  }

  public void stop() {
    removeHandlerCallbacks();

    try {
      if (isInitialized()) {
        audioTrack.pause();
      }
      playing = false;
      audioTrack.flush();
    } catch (Exception e) {
      Log.e(TAG, "play: failed to start AudioTrack: ", e);
    }

    if (!ignoreFocus) {
      audioManager.abandonAudioFocus(this);
    }
    listener.onAudioStop();
  }

  public void restart() {
    if (!playing) {
      return;
    }
    playing = false;
    removeHandlerCallbacks();

    if (isInitialized()) {
      audioTrack.pause();
      audioTrack.flush();
    }
    resetHandlersIfRequired();

    try {
      audioTrack.play();
      playing = true;
    } catch (Exception e) {
      Log.e(TAG, "play: failed to start AudioTrack: ", e);
    }
  }

  @Override
  public void onAudioFocusChange(int focusChange) {
    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
      if (isInitialized()) {
        audioTrack.setVolume(AudioUtil.dbToLinearVolume(volumeReductionDb));
      }
    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
      stop();
    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
    ) {
      if (isInitialized()) {
        audioTrack.setVolume(Math.min(0.25f, AudioUtil.dbToLinearVolume(volumeReductionDb)));
      }
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
    tickNormal = loadAudio(resIdNormal, pitchNormal);
    tickStrong = loadAudio(resIdStrong, pitchStrong);
    tickSub = loadAudio(resIdSub, pitchSub);
  }

  public void setGain(int gain) {
    this.gain = gain;
    if (loudnessEnhancer != null) {
      try {
        loudnessEnhancer.setTargetGain(Math.max(0, gain * 100));
        loudnessEnhancer.setEnabled(gain > 0);
      } catch (RuntimeException e) {
        Log.e(TAG, "setGain: failed to set target gain: ", e);
      }
    }
    volumeReductionDb = Math.max(0, -gain * 100);
    if (isInitialized()) {
      try {
        audioTrack.setVolume(AudioUtil.dbToLinearVolume(volumeReductionDb));
      } catch (IllegalStateException e) {
        Log.e(TAG, "setGain: failed to set volume on AudioTrack: ", e);
      }
    }
  }

  public int getGain() {
    return gain;
  }

  public void setMuted(boolean muted) {
    this.muted = muted;
  }

  public void setIgnoreFocus(boolean ignore) {
    ignoreFocus = ignore;
  }

  public boolean getIgnoreFocus() {
    return ignoreFocus;
  }

  public void writeTickPeriod(Tick tick, int subdivisionRate) {
    long scheduledNano = System.nanoTime();
    removeHandlerCallbacks();
    audioHandler.post(() -> {
      int periodSize = (60 * SAMPLE_RATE_IN_HZ) / subdivisionRate;
      int periodSizeTrimmed = periodSize;
      long nowNano = System.nanoTime();
      long delayMs = (nowNano - scheduledNano) / 1_000_000L;
      if (delayMs > 1) {
        int trimSize = (int) (Math.max(delayMs, 10) * (SAMPLE_RATE_IN_HZ / 1000));
        periodSizeTrimmed = Math.max(0, periodSize - trimSize);
      }
      float[] tickSound = muted || tick.isMuted ? silence : getTickSound(tick.type);
      int sizeWritten = writeNextAudioData(tickSound, periodSizeTrimmed, 0);
      if (DEBUG) {
        Log.v(TAG, "writeTickPeriod: wrote tick sound for tick " + tick);
      }
      writeSilenceUntilPeriodFinished(sizeWritten, periodSizeTrimmed);
    });
  }

  private void writeSilenceUntilPeriodFinished(int previousSizeWritten, int periodSize) {
    int sizeWritten = previousSizeWritten;
    while (sizeWritten < periodSize) {
      sizeWritten += writeNextAudioData(silence, periodSize, sizeWritten);
      if (DEBUG) {
        Log.v(TAG, "writeSilenceUntilPeriodFinished: wrote silence");
      }
    }
  }

  private int writeNextAudioData(float[] data, int periodSize, int sizeWritten) {
    int size = Math.min(data.length, periodSize - sizeWritten);
    if (playing) {
      writeAudio(data, size);
    }
    return size;
  }

  private float[] getTickSound(String tickType) {
    switch (tickType) {
      case TICK_TYPE.STRONG:
        return tickStrong;
      case TICK_TYPE.SUB:
        return tickSub;
      case TICK_TYPE.MUTED:
        return silence;
      default:
        return tickNormal;
    }
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

  private void writeAudio(float[] data, int size) {
    if (!isInitialized()) {
      return;
    }
    try {
      boolean reduceVolume = gain < 0;
      float[] out = reduceVolume ? scaledBuffer : data;
      if (reduceVolume) {
        if (scaledBuffer.length < size) {
          scaledBuffer = new float[size];
        }
        float fraction = 1 - ((float) Math.abs(gain * 4) / 100);
        for (int i = 0; i < size; i++) {
          scaledBuffer[i] = data[i] * fraction;
        }
      }
      int result = audioTrack.write(out, 0, size, AudioTrack.WRITE_BLOCKING);
      if (result < 0) {
        stop();
        throw new IllegalStateException("Error code: " + result);
      }
    } catch (Exception e) {
      Log.e(TAG, "writeAudio: failed to play audion data", e);
    }
  }

  private boolean isInitialized() {
    return audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED;
  }

  private enum Pitch {
    NORMAL, HIGH, LOW
  }

  public interface AudioListener {
    void onAudioStop();
  }
}