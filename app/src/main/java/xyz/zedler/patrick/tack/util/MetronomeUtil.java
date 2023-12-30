package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.media.AudioTrack;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.Arrays;
import xyz.zedler.patrick.tack.Constants;
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
  private final HandlerThread thread;
  private final Handler handler;
  private AudioTrack track;
  private LoudnessEnhancer loudnessEnhancer;
  private int tempo, gain;
  private long tickIndex;
  private String[] beats, subdivisions;
  private float[] tickStrong, tickNormal, tickSub;
  private boolean playing, useSubdivisions, beatModeVibrate;
  private int countIn;

  public MetronomeUtil(@NonNull Context context, @NonNull TickListener listener) {
    this.context = context;
    this.listener = listener;

    thread = new HandlerThread("metronome");
    thread.start();
    handler = new Handler(thread.getLooper());

    setTempo(DEF.TEMPO);
    setSound(DEF.SOUND);
    setBeats(DEF.BEATS.split(","));
    setSubdivisions(DEF.SUBDIVISIONS.split(","));
    setGain(DEF.GAIN);
    setCountIn(DEF.COUNT_IN);
  }

  public void destroy() {
    handler.removeCallbacks(this);
    thread.quit();
  }

  public void setBeats(String[] beats) {
    this.beats = beats;
  }

  public String[] getBeats() {
    return beats;
  }

  public boolean addBeat() {
    if (beats.length >= Constants.BEATS_MAX) {
      return false;
    }
    beats = Arrays.copyOf(beats, beats.length + 1);
    beats[beats.length - 1] = TICK_TYPE.NORMAL;
    return true;
  }

  public boolean removeBeat() {
    if (beats.length <= 1) {
      return false;
    }
    beats = Arrays.copyOf(beats, beats.length - 1);
    return true;
  }

  public void setSubdivisions(String[] subdivisions) {
    this.subdivisions = subdivisions;
  }

  public String[] getSubdivisions() {
    return subdivisions;
  }

  private int getSubdivisionsCount() {
    return useSubdivisions ? subdivisions.length : 1;
  }

  public boolean addSubdivision() {
    if (subdivisions.length >= Constants.SUBS_MAX) {
      return false;
    }
    subdivisions = Arrays.copyOf(subdivisions, subdivisions.length + 1);
    subdivisions[subdivisions.length - 1] = TICK_TYPE.SUB;
    return true;
  }

  public boolean removeSubdivision() {
    if (subdivisions.length <= 1) {
      return false;
    }
    subdivisions = Arrays.copyOf(subdivisions, subdivisions.length - 1);
    return true;
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

  public void setSubdivisionsUsed(boolean useSubdivisions) {
    this.useSubdivisions = useSubdivisions;
  }

  public boolean getSubdivisionsUsed() {
    return useSubdivisions;
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

  public int getGain() {
    return gain;
  }

  public void setCountIn(int bars) {
    countIn = bars;
  }

  public int getCountIn() {
    return countIn;
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

    tickIndex = 0;
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
      handler.postDelayed(this, getInterval() / getSubdivisionsCount());

      Tick tick = new Tick(
          tickIndex, getCurrentBeat(), getCurrentSubdivision(), getCurrentTickType()
      );
      if (listener != null) {
        listener.onTick(tick);
      }
      writeTickPeriod(tick);
      tickIndex++;
    }
  }

  public boolean isPlaying() {
    return playing;
  }

  private void writeTickPeriod(Tick tick) {
    float[] tickSound = getTickSound(tick.type);
    int periodSize = 60 * AudioUtil.SAMPLE_RATE_IN_HZ / tempo / getSubdivisionsCount();
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
    return (int) ((tickIndex / getSubdivisionsCount()) % beats.length) + 1;
  }

  private int getCurrentSubdivision() {
    return (int) (tickIndex % getSubdivisionsCount()) + 1;
  }

  private String getCurrentTickType() {
    int subdivisionsCount = getSubdivisionsCount();
    if ((tickIndex % subdivisionsCount) == 0) {
      return beats[(int) ((tickIndex / subdivisionsCount) % beats.length)];
    } else {
      return subdivisions[(int) (tickIndex % subdivisionsCount)];
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
