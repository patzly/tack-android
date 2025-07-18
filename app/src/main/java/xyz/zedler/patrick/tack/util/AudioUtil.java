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
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import xyz.zedler.patrick.tack.Constants.SOUND;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;

public class AudioUtil implements OnAudioFocusChangeListener {

  private static final String TAG = AudioUtil.class.getSimpleName();
  private static final boolean DEBUG = false;

  public static final int SAMPLE_RATE_IN_HZ = 48000;
  private static final int SILENCE_CHUNK_SIZE = 8000;
  private static final int DATA_CHUNK_SIZE = 8;
  private static final byte[] DATA_MARKER = "data".getBytes(StandardCharsets.US_ASCII);

  private final Context context;
  private final AudioManager audioManager;
  private final AudioListener listener;
  private HandlerThread audioThread;
  private Handler audioHandler;
  private AudioTrack audioTrack;
  private LoudnessEnhancer loudnessEnhancer;
  private float[] tickStrong, tickNormal, tickSub;
  private int gain;
  private boolean playing, muted, ignoreFocus;
  private final float[] silence = new float[SILENCE_CHUNK_SIZE];

  public AudioUtil(@NonNull Context context, @NonNull AudioListener listener) {
    this.context = context;
    this.listener = listener;
    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    resetHandlersIfRequired();
  }

  public void destroy() {
    removeHandlerCallbacks();
    audioThread.quitSafely();
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

    playing = true;
    audioTrack = getTrack();
    try {
      loudnessEnhancer = new LoudnessEnhancer(audioTrack.getAudioSessionId());
      loudnessEnhancer.setTargetGain(Math.max(0, gain * 100));
      loudnessEnhancer.setEnabled(gain > 0);
    } catch (RuntimeException e) {
      Log.e(TAG, "play: failed to initialize LoudnessEnhancer: ", e);
    }
    try {
      audioTrack.play();
    } catch (IllegalStateException e) {
      Log.e(TAG, "play: failed to start AudioTrack: ", e);
    }

    if (ignoreFocus) {
      return;
    }
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(getAttributes())
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

  public void stop() {
    playing = false;
    removeHandlerCallbacks();

    if (audioTrack != null) {
      if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
        audioTrack.stop();
      }
      audioTrack.flush();
      audioTrack.release();
    }
    if (loudnessEnhancer != null) {
      try {
        loudnessEnhancer.release();
      } catch (RuntimeException e) {
        Log.e(TAG, "stop: failed to release LoudnessEnhancer resources: ", e);
      }
      loudnessEnhancer = null;
    }
    if (!ignoreFocus) {
      audioManager.abandonAudioFocus(this);
    }
    listener.onAudioStop();
  }

  @Override
  public void onAudioFocusChange(int focusChange) {
    if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
      if (audioTrack != null) {
        audioTrack.setVolume(1);
      }
    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
      stop();
    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
    ) {
      if (audioTrack != null) {
        audioTrack.setVolume(0.25f);
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

  public void writeTickPeriod(Tick tick, int tempo, int subdivisionCount) {
    final int periodSize = 60 * SAMPLE_RATE_IN_HZ / tempo / subdivisionCount;
    final long expectedTime = SystemClock.elapsedRealtime();
    audioHandler.post(() -> {
      int periodSizeTrimmed = periodSize;
      if (tick.subdivision == 1) {
        long currentTime = SystemClock.elapsedRealtime();
        long delay = currentTime - expectedTime;
        if (delay > 1) {
          int trimSize = (int) (Math.max(delay, 10) * (SAMPLE_RATE_IN_HZ / 1000));
          periodSizeTrimmed = Math.max(0, periodSize - trimSize);
        }
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

  private static AudioTrack getTrack() {
    AudioFormat audioFormat = new AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
        .setSampleRate(SAMPLE_RATE_IN_HZ)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build();
    return new AudioTrack(
        getAttributes(),
        audioFormat,
        AudioTrack.getMinBufferSize(
            audioFormat.getSampleRate(), audioFormat.getChannelMask(), audioFormat.getEncoding()
        ),
        AudioTrack.MODE_STREAM,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    );
  }

  private static AudioAttributes getAttributes() {
    return new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build();
  }

  private float[] loadAudio(@RawRes int resId, Pitch pitch) {
    try (InputStream stream = context.getResources().openRawResource(resId)) {
      return adjustPitch(readDataFromWavFloat(stream), pitch);
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
    try {
      boolean reduceVolume = gain < 0;
      float[] scaled = new float[size];
      if (reduceVolume) {
        float fraction = 1 - ((float) Math.abs(gain * 4) / 100);
        for (int i = 0; i < size; i++) {
          scaled[i] = data[i] * fraction;
        }
      }
      int result = audioTrack.write(
          reduceVolume ? scaled : data, 0, size, AudioTrack.WRITE_BLOCKING
      );
      if (result < 0) {
        stop();
        throw new IllegalStateException("Error code: " + result);
      }
    } catch (Exception e) {
      Log.e(TAG, "writeAudio: failed to play audion data", e);
    }
  }

  private static float[] readDataFromWavFloat(InputStream input) throws IOException {
    byte[] content;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      content = input.readAllBytes();
    } else {
      content = readInputStreamToBytes(input);
    }
    int indexOfDataMarker = getIndexOfDataMarker(content);
    if (indexOfDataMarker < 0) {
      throw new RuntimeException("Could not find data marker in the content");
    }
    int startOfSound = indexOfDataMarker + DATA_CHUNK_SIZE;
    if (startOfSound > content.length) {
      throw new RuntimeException("Too short data chunk");
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(
        content, startOfSound, content.length - startOfSound
    );
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
    float[] data = new float[floatBuffer.remaining()];
    floatBuffer.get(data);
    return data;
  }

  private static byte[] readInputStreamToBytes(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int read;
    byte[] data = new byte[4096];
    while ((read = input.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, read);
    }
    return buffer.toByteArray();
  }

  private static int getIndexOfDataMarker(byte[] array) {
    if (DATA_MARKER.length == 0) {
      return 0;
    }
    outer:
    for (int i = 0; i < array.length - DATA_MARKER.length + 1; i++) {
      for (int j = 0; j < DATA_MARKER.length; j++) {
        if (array[i + j] != DATA_MARKER[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  private enum Pitch {
    NORMAL, HIGH, LOW
  }

  public interface AudioListener {
    void onAudioStop();
  }
}