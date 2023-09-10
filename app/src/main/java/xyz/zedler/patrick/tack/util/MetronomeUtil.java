package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.media.AudioTrack;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.concurrent.CancellationException;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.SOUND;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;

public class MetronomeUtil {

  private static final String TAG = MetronomeUtil.class.getSimpleName();

  private final float[] silence = AudioUtil.getSilence();
  private final Context context;
  private final TickListener listener;
  private Thread thread;
  private AudioTrack track;
  private LoudnessEnhancer loudnessEnhancer;
  private int tempo, volumeBoost;
  private String[] beats, subdivisions;
  private float[] tickStrong, tickNormal, tickSub;
  private boolean playing, beatModeVibrate;

  public MetronomeUtil(@NonNull Context context, @NonNull TickListener listener) {
    this.context = context;
    this.listener = listener;

    setTempo(DEF.TEMPO);
    setSound(DEF.SOUND);
    setBeats(DEF.BEATS.split(" "));
    setSubdivisions(DEF.SUBDIVISIONS.split(" "));
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

  public void setVolumeBoost(int db) {
    volumeBoost = db;
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
    thread = new Thread(this::loop);
    thread.start();
    Log.i(TAG, "start: started metronome thread");
  }

  public void stop() {
    if (!isPlaying()) {
      return;
    }
    playing = false;
    if (thread != null) {
      thread.interrupt();
      thread = null;
    }
    Log.i(TAG, "stop: interrupted metronome thread");
  }

  private void loop() {
    track = AudioUtil.getNewAudioTrack();
    loudnessEnhancer = new LoudnessEnhancer(track.getAudioSessionId());
    setVolumeBoost(volumeBoost);
    track.play();
    try {
      int tickCount = 0;
      while (playing) {
        writeTickPeriod(tickCount);
        tickCount++;
      }
    } catch (CancellationException e) {
      Log.d(TAG, "loop: received cancellation");
      track.pause();
    } finally {
      track.release();
    }
  }

  public boolean isPlaying() {
    return thread != null && thread.isAlive() && playing;
  }

  private void writeTickPeriod(long tickCount) {
    Tick tick = getCurrentTick(tickCount);
    float[] tickSound = getTickSound(tick.type);
    int periodSize = calculatePeriodSize();
    int sizeWritten = writeNextAudioData(tickSound, periodSize, 0);
    Log.v(TAG, "writeTickPeriod: wrote tick sound for " + tick);
    listener.onTick(tick);
    writeSilenceUntilPeriodFinished(sizeWritten);
  }

  private void writeSilenceUntilPeriodFinished(int previousSizeWritten) {
    int sizeWritten = previousSizeWritten;
    while (true) {
      int periodSize = calculatePeriodSize();
      if (sizeWritten >= periodSize) {
        break;
      }
      sizeWritten += writeNextAudioData(silence, periodSize, sizeWritten);
      Log.v(TAG, "writeSilenceUntilPeriodFinished: wrote silence");
    }
  }

  private int calculatePeriodSize() {
    return 60 * AudioUtil.SAMPLE_RATE_IN_HZ / tempo / subdivisions.length;
  }

  private int writeNextAudioData(float[] data, int periodSize, int sizeWritten) {
    int size = calculateAudioSizeToWriteNext(data, periodSize, sizeWritten);
    AudioUtil.writeAudio(track, data, size);
    return size;
  }

  private int calculateAudioSizeToWriteNext(float[] data, int periodSize, int sizeWritten) {
    int sizeLeft = periodSize - sizeWritten;
    return Math.min(data.length, sizeLeft);
  }

  private Tick getCurrentTick(long tickCount) {
    return new Tick(getCurrentBeat(tickCount), getCurrentTickType(tickCount));
  }

  private int getCurrentBeat(long tickCount) {
    return (int) (((tickCount / subdivisions.length) % beats.length) + 1);
  }

  private String getCurrentTickType(long tickCount) {
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

  public static class Tick implements Parcelable {
    public final int beat;
    @NonNull public final String type;

    public Tick(int beat, @NonNull String type) {
      this.beat = beat;
      this.type = type;
    }

    protected Tick(Parcel in) {
      beat = in.readInt();
      String rawType = in.readString();
      type = rawType != null ? rawType : TICK_TYPE.NORMAL;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
      dest.writeInt(beat);
      dest.writeString(type);
    }

    public static final Creator<Tick> CREATOR = new Creator<>() {
      @Override
      public Tick createFromParcel(Parcel in) {
        return new Tick(in);
      }

      @Override
      public Tick[] newArray(int size) {
        return new Tick[size];
      }
    };

    @NonNull
    @Override
    public String toString() {
      return "Tick{beat=" + beat + ", type=" + type + '}';
    }
  }
}
