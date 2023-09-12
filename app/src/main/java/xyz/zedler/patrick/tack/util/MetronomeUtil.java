package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.media.AudioTrack;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.SOUND;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;

public class MetronomeUtil implements Runnable {

  private static final String TAG = MetronomeUtil.class.getSimpleName();
  private static final boolean DEBUG = false;

  private final float[] silence = AudioUtil.getSilence();
  private final Context context;
  private final TickListener listener;
  private final Handler handler;
  private AudioTrack track;
  private LoudnessEnhancer loudnessEnhancer;
  private int tempo, gain;
  private long tickCount;
  private String[] beats, subdivisions;
  private float[] tickStrong, tickNormal, tickSub;
  private boolean playing, beatModeVibrate;

  public MetronomeUtil(@NonNull Context context, @NonNull TickListener listener) {
    this.context = context;
    this.listener = listener;

    HandlerThread thread = new HandlerThread("metronome");
    thread.start();
    handler = new Handler(thread.getLooper());

    setTempo(DEF.TEMPO);
    setSound(DEF.SOUND);
    setBeats(DEF.BEATS.split(" "));
    setSubdivisions(DEF.SUBDIVISIONS.split(" "));
    setGain(DEF.GAIN);
  }

  public void setBeats(String[] beats) {
    this.beats = beats;
  }

  public void setSubdivisions(String[] subdivisions) {
    this.subdivisions = subdivisions;
  }

  public void setTempo(int tempo) {
    this.tempo = tempo;
  }

  public int getTempo() {
    return tempo;
  }

  public long getInterval() {
    return 1000 * 60 / tempo;
  }

  public void setSound(String sound) {
    int resIdNormal, resIdStrong, resIdSub;
    switch (sound) {
      case SOUND.SINE:
        resIdNormal = R.raw.sine_normal;
        resIdStrong = R.raw.sine_strong;
        resIdSub = R.raw.sine_sub;
        break;
      case SOUND.CLICK:
      case SOUND.DING:
      case SOUND.BEEP:
      default:
        resIdNormal = R.raw.wood_normal;
        resIdStrong = R.raw.wood_strong;
        resIdSub = R.raw.wood_normal;
        break;
    }
    tickNormal = AudioUtil.loadAudio(context, resIdNormal);
    tickStrong = AudioUtil.loadAudio(context, resIdStrong);
    tickSub = AudioUtil.loadAudio(context, resIdSub);
  }

  public void setBeatModeVibrate(boolean vibrate) {
    beatModeVibrate = vibrate;
  }

  public boolean isBeatModeVibrate() {
    return beatModeVibrate;
  }

  public void setGain(int db) {
    gain = db;
    if (loudnessEnhancer != null) {
      loudnessEnhancer.setTargetGain(db * 100);
      loudnessEnhancer.setEnabled(db > 0);
    }
  }

  public void start() {
    if (isPlaying()) {
      return;
    }
    playing = true;
    track = AudioUtil.getNewAudioTrack();
    loudnessEnhancer = new LoudnessEnhancer(track.getAudioSessionId());
    setGain(gain);
    track.play();

    tickCount = 0;
    handler.post(this);
    Log.i(TAG, "start: started metronome handler");
  }

  public void stop() {
    if (!isPlaying()) {
      return;
    }
    playing = false;
    track.flush();
    track.release();

    handler.removeCallbacks(this);
    Log.i(TAG, "stop: stopped metronome handler");
  }

  @Override
  public void run() {
    if (playing) {
      handler.postDelayed(this, getInterval() / subdivisions.length);

      Tick tick = new Tick(getCurrentBeat(), getCurrentTickType());
      if (listener != null) {
        listener.onTick(tick);
      }
      writeTickPeriod(tick);
      tickCount++;
    }
  }

  public boolean isPlaying() {
    return playing;
  }

  private void writeTickPeriod(Tick tick) {
    float[] tickSound = getTickSound(tick.type);
    int periodSize = 60 * AudioUtil.SAMPLE_RATE_IN_HZ / tempo / subdivisions.length;
    int sizeWritten = writeNextAudioData(tickSound, periodSize, 0);
    if (DEBUG) {
      Log.v(TAG, "writeTickPeriod: wrote tick sound for " + tick);
    }
    writeSilenceUntilPeriodFinished(sizeWritten, periodSize);
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
      AudioUtil.writeAudio(track, data, size);
    }
    return size;
  }

  private int getCurrentBeat() {
    return (int) ((tickCount / subdivisions.length) % beats.length) + 1;
  }

  private String getCurrentTickType() {
    if (tickCount % subdivisions.length == 0) {
      return beats[(int) (tickCount % beats.length)];
    } else {
      return subdivisions[(int) (tickCount % subdivisions.length)];
    }
  }

  private float[] getTickSound(String tickType) {
    if (beatModeVibrate) {
      return silence;
    }
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

  public interface TickListener {
    void onTick(Tick tick);
  }

  public static class Tick {
    public final int beat;
    @NonNull public final String type;

    public Tick(int beat, @NonNull String type) {
      this.beat = beat;
      this.type = type;
    }

    @NonNull
    @Override
    public String toString() {
      return "Tick{beat=" + beat + ", type=" + type + '}';
    }
  }
}
