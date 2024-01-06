package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioTrack;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SOUND;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;

public class MetronomeUtil {

  private static final String TAG = MetronomeUtil.class.getSimpleName();
  private static final boolean DEBUG = false;

  private final Context context;
  private final SharedPreferences sharedPrefs;
  private final HapticUtil hapticUtil;
  private final ShortcutUtil shortcutUtil;
  private final HandlerThread audioThread, callbackThread;
  private final Handler tickHandler, latencyHandler;
  private final Handler countInHandler, incrementalHandler, timerHandler;
  private final Runnable runnableTick;
  private final Set<MetronomeListener> listeners = new HashSet<>();
  private final float[] silence = AudioUtil.getSilence();
  private final boolean fromService;
  private AudioTrack track;
  private LoudnessEnhancer loudnessEnhancer;
  private String incrementalUnit, timerUnit;
  private String[] beats, subdivisions;
  private int tempo, gain, countIn, incrementalAmount, incrementalInterval, timerDuration;
  private long tickIndex, latency, startTime, timerStartTime;
  private float timerProgress;
  private float[] tickStrong, tickNormal, tickSub;
  private boolean playing, tempPlaying, useSubdivisions, beatModeVibrate;
  private boolean alwaysVibrate, incrementalIncrease, resetTimer, flashScreen, keepAwake;

  public MetronomeUtil(@NonNull Context context, boolean fromService) {
    this.context = context;
    this.fromService = fromService;

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    hapticUtil = new HapticUtil(context);
    shortcutUtil = new ShortcutUtil(context);

    audioThread = new HandlerThread("metronome_audio");
    audioThread.start();
    callbackThread = new HandlerThread("metronome_callback");
    callbackThread.start();

    tickHandler = new Handler(audioThread.getLooper());
    latencyHandler = new Handler(callbackThread.getLooper());
    countInHandler = new Handler(callbackThread.getLooper());
    incrementalHandler = new Handler(callbackThread.getLooper());
    timerHandler = new Handler(callbackThread.getLooper());

    runnableTick = new Runnable() {
      @Override
      public void run() {
        if (playing) {
          tickHandler.postDelayed(this, getInterval() / getSubdivisionsCount());
          Tick tick = new Tick(
              tickIndex, getCurrentBeat(), getCurrentSubdivision(), getCurrentTickType()
          );
          performTick(tick);
          writeTickPeriod(tick);
          tickIndex++;
        }
      }
    };

    setToPreferences();
  }

  public void setToPreferences() {
    tempo = sharedPrefs.getInt(PREF.TEMPO, DEF.TEMPO);
    beats = sharedPrefs.getString(PREF.BEATS, DEF.BEATS).split(",");
    subdivisions = sharedPrefs.getString(PREF.SUBDIVISIONS, DEF.SUBDIVISIONS).split(",");
    useSubdivisions = sharedPrefs.getBoolean(PREF.USE_SUBS, DEF.USE_SUBS);
    countIn = sharedPrefs.getInt(PREF.COUNT_IN, DEF.COUNT_IN);
    latency = sharedPrefs.getLong(PREF.LATENCY, DEF.LATENCY);
    incrementalAmount = sharedPrefs.getInt(PREF.INCREMENTAL_AMOUNT, DEF.INCREMENTAL_AMOUNT);
    incrementalIncrease = sharedPrefs.getBoolean(
        PREF.INCREMENTAL_INCREASE, DEF.INCREMENTAL_INCREASE
    );
    incrementalInterval = sharedPrefs.getInt(PREF.INCREMENTAL_INTERVAL, DEF.INCREMENTAL_INTERVAL);
    incrementalUnit = sharedPrefs.getString(PREF.INCREMENTAL_UNIT, DEF.INCREMENTAL_UNIT);
    timerDuration = sharedPrefs.getInt(PREF.TIMER_DURATION, DEF.TIMER_DURATION);
    timerUnit = sharedPrefs.getString(PREF.TIMER_UNIT, DEF.TIMER_UNIT);
    beatModeVibrate = sharedPrefs.getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE);
    alwaysVibrate = sharedPrefs.getBoolean(PREF.ALWAYS_VIBRATE, DEF.ALWAYS_VIBRATE);
    resetTimer = sharedPrefs.getBoolean(PREF.RESET_TIMER, DEF.RESET_TIMER);

    setSound(sharedPrefs.getString(PREF.SOUND, DEF.SOUND));
    setGain(sharedPrefs.getInt(PREF.GAIN, DEF.GAIN));
  }

  public void savePlayingState() {
    tempPlaying = playing;
  }

  public void restorePlayingState() {
    if (tempPlaying) {
      start(false, false);
    } else {
      stop();
    }
  }

  public void setUpLatencyCalibration() {
    tempo = 80;
    beats = DEF.BEATS.split(",");
    subdivisions = DEF.SUBDIVISIONS.split(",");
    beatModeVibrate = false;
    alwaysVibrate = true;
    countIn = 0;
    incrementalAmount = 0;
    timerDuration = 0;
    setGain(0);
    start(false, false);
  }

  public void destroy() {
    listeners.clear();
    tickHandler.removeCallbacksAndMessages(null);
    latencyHandler.removeCallbacksAndMessages(null);
    countInHandler.removeCallbacksAndMessages(null);
    incrementalHandler.removeCallbacksAndMessages(null);
    timerHandler.removeCallbacksAndMessages(null);
    audioThread.quit();
    callbackThread.quit();
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
    start(true, true);
  }

  public void start(boolean resetTimerIfNecessary, boolean reportTempoUsage) {
    if (reportTempoUsage) {
      // notify system for shortcut usage prediction
      shortcutUtil.reportUsage(getTempo());
    }
    if (isPlaying()) {
      return;
    }
    if (!fromService) {
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeConnectionMissing();
      }
      return;
    }
    playing = true;
    track = AudioUtil.getNewAudioTrack();
    loudnessEnhancer = new LoudnessEnhancer(track.getAudioSessionId());
    setGain(gain);
    track.play();
    tickIndex = 0;
    tickHandler.post(runnableTick);

    // Timer
    startTime = System.currentTimeMillis();
    countInHandler.postDelayed(() -> {
      updateIncrementalHandler();
      timerStartTime = System.currentTimeMillis();
      updateTimerHandler(resetTimer && resetTimerIfNecessary ? 0 : timerProgress);
      // TODO: start muted and elapsed time
    }, getCountInInterval()); // 0 if count-in is disabled

    for (MetronomeListener listener : listeners) {
      listener.onMetronomeStart();
    }
    Log.i(TAG, "start: started metronome handler");
  }

  public void stop() {
    if (!isPlaying()) {
      return;
    }
    timerProgress = getTimerProgress(); // must be called before playing is set to false

    playing = false;
    track.flush();
    track.release();

    tickHandler.removeCallbacksAndMessages(null);
    latencyHandler.removeCallbacksAndMessages(null);
    countInHandler.removeCallbacksAndMessages(null);
    incrementalHandler.removeCallbacksAndMessages(null);
    timerHandler.removeCallbacksAndMessages(null);

    for (MetronomeListener listener : listeners) {
      listener.onMetronomeStop();
    }
    Log.i(TAG, "stop: stopped metronome handler");
  }

  public boolean isPlaying() {
    return playing;
  }

  public void setBeats(String[] beats) {
    this.beats = beats;
    sharedPrefs.edit().putString(PREF.BEATS, String.join(",", beats)).apply();
    if (isTimerActive() && timerUnit.equals(UNIT.BARS)) {
      updateTimerHandler(0);
    }
  }

  public String[] getBeats() {
    return beats;
  }

  public int getBeatsCount() {
    return beats.length;
  }

  public void setBeat(int beat, String tickType) {
    String[] beats = getBeats();
    beats[beat] = tickType;
    setBeats(beats);
  }

  public boolean addBeat() {
    if (beats.length >= Constants.BEATS_MAX) {
      return false;
    }
    String[] beats = Arrays.copyOf(this.beats, this.beats.length + 1);
    beats[beats.length - 1] = TICK_TYPE.NORMAL;
    setBeats(beats);
    return true;
  }

  public boolean removeBeat() {
    if (beats.length <= 1) {
      return false;
    }
    setBeats(Arrays.copyOf(beats, beats.length - 1));
    return true;
  }

  public void setSubdivisions(String[] subdivisions) {
    this.subdivisions = subdivisions;
    sharedPrefs.edit()
        .putString(PREF.SUBDIVISIONS, String.join(",", getSubdivisions()))
        .apply();
  }

  public String[] getSubdivisions() {
    return useSubdivisions ? subdivisions : DEF.SUBDIVISIONS.split(",");
  }

  public int getSubdivisionsCount() {
    return useSubdivisions ? subdivisions.length : 1;
  }

  public void setSubdivision(int subdivision, String tickType) {
    String[] subdivisions = getSubdivisions();
    subdivisions[subdivision] = tickType;
    setSubdivisions(subdivisions);
  }

  public boolean addSubdivision() {
    if (subdivisions.length >= Constants.SUBS_MAX) {
      return false;
    }
    String[] subdivisions = Arrays.copyOf(
        this.subdivisions, this.subdivisions.length + 1
    );
    subdivisions[subdivisions.length - 1] = TICK_TYPE.SUB;
    setSubdivisions(subdivisions);
    return true;
  }

  public boolean removeSubdivision() {
    if (subdivisions.length <= 1) {
      return false;
    }
    setSubdivisions(Arrays.copyOf(subdivisions, subdivisions.length - 1));
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
    setSubdivisions(String.join(
        ",", TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL).split(","));
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
    setSubdivisions(String.join(
        ",",
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL, TICK_TYPE.MUTED
    ).split(","));
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
    setSubdivisions(String.join(
        ",",
        TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED,
        TICK_TYPE.NORMAL, TICK_TYPE.MUTED, TICK_TYPE.MUTED
    ).split(","));
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
      if (isTimerActive() && timerUnit.equals(UNIT.BARS)) {
        updateTimerHandler(0);
      }
    }
  }

  public int getTempo() {
    return tempo;
  }

  private void changeTempo(int change) {
    int tempoOld = getTempo();
    int tempoNew = tempoOld + change;
    setTempo(tempoNew);
    for (MetronomeListener listener : listeners) {
      listener.onMetronomeTempoChanged(tempoOld, tempoNew);
    }
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
    return !playing || (!beatModeVibrate && !alwaysVibrate);
  }

  public void setLatency(long offset) {
    latency = offset;
    sharedPrefs.edit().putLong(PREF.LATENCY, offset).apply();
  }

  public long getLatency() {
    return latency;
  }

  public void setGain(int db) {
    gain = db;
    if (loudnessEnhancer != null) {
      loudnessEnhancer.setTargetGain(db * 100);
      loudnessEnhancer.setEnabled(db > 0);
    }
    sharedPrefs.edit().putInt(PREF.GAIN, gain).apply();
  }

  public int getGain() {
    return gain;
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

  public void setCountIn(int bars) {
    countIn = bars;
    sharedPrefs.edit().putInt(PREF.COUNT_IN, bars).apply();
  }

  public int getCountIn() {
    return countIn;
  }

  public boolean isCountInActive() {
    return countIn > 0;
  }

  public long getCountInInterval() {
    return getInterval() * getBeatsCount() * countIn;
  }

  public void setIncrementalAmount(int bpm) {
    incrementalAmount = bpm;
    sharedPrefs.edit().putInt(PREF.INCREMENTAL_AMOUNT, bpm).apply();
    updateIncrementalHandler();
  }

  public int getIncrementalAmount() {
    return incrementalAmount;
  }

  public boolean isIncrementalActive() {
    return incrementalAmount > 0;
  }

  public void setIncrementalIncrease(boolean increase) {
    incrementalIncrease = increase;
    sharedPrefs.edit().putBoolean(PREF.INCREMENTAL_INCREASE, increase).apply();
  }

  public boolean getIncrementalIncrease() {
    return incrementalIncrease;
  }

  public void setIncrementalInterval(int interval) {
    incrementalInterval = interval;
    sharedPrefs.edit().putInt(PREF.INCREMENTAL_INTERVAL, interval).apply();
    updateIncrementalHandler();
  }

  public int getIncrementalInterval() {
    return incrementalInterval;
  }

  public void setIncrementalUnit(String unit) {
    if (unit.equals(incrementalUnit)) {
      return;
    }
    incrementalUnit = unit;
    sharedPrefs.edit().putString(PREF.INCREMENTAL_UNIT, unit).apply();
    updateIncrementalHandler();
  }

  public String getIncrementalUnit() {
    return incrementalUnit;
  }

  private void updateIncrementalHandler() {
    if (!isPlaying()) {
      return;
    }
    incrementalHandler.removeCallbacksAndMessages(null);
    if (!incrementalUnit.equals(UNIT.BARS) && isIncrementalActive()) {
      long factor = incrementalUnit.equals(UNIT.SECONDS) ? 1000L : 60000L;
      long interval = factor * incrementalInterval;
      incrementalHandler.postDelayed(new Runnable() {
        @Override
        public void run() {
          incrementalHandler.postDelayed(this, interval);
          changeTempo(incrementalAmount * (incrementalIncrease ? 1 : -1));
        }
      }, interval);
    }
  }

  public void setTimerDuration(int duration) {
    setTimerDuration(duration, true);
  }

  private void setTimerDuration(int duration, boolean resetTimer) {
    timerDuration = duration;
    sharedPrefs.edit().putInt(PREF.TIMER_DURATION, duration).apply();
    updateTimerHandler(resetTimer ? 0 : timerProgress);
  }

  public int getTimerDuration() {
    return timerDuration;
  }

  public boolean isTimerActive() {
    return timerDuration > 0;
  }

  public long getTimerInterval() {
    long factor;
    switch (timerUnit) {
      case UNIT.SECONDS:
        factor = 1000L;
        break;
      case UNIT.MINUTES:
        factor = 60000L;
        break;
      default:
        factor = getInterval() * getBeatsCount();
        break;
    }
    return factor * timerDuration;
  }

  public long getTimerIntervalRemaining() {
    return (long) (getTimerInterval() * (1 - getTimerProgress()));
  }

  public void setTimerUnit(String unit) {
    if (unit.equals(timerUnit)) {
      return;
    }
    timerUnit = unit;
    sharedPrefs.edit().putString(PREF.TIMER_UNIT, unit).apply();
    updateTimerHandler(0);
  }

  public String getTimerUnit() {
    return timerUnit;
  }

  public void setResetTimer(boolean reset) {
    resetTimer = reset;
    sharedPrefs.edit().putBoolean(PREF.RESET_TIMER, reset).apply();
  }

  public boolean getResetTimer() {
    return resetTimer;
  }

  public float getTimerProgress() {
    if (!isTimerActive()) {
      return 0;
    } else if (isPlaying()) {
      // TODO: fix wrong progress when interval-critical stuff changes
      long previousDuration = (long) (timerProgress * getTimerInterval());
      long elapsedTime = System.currentTimeMillis() - timerStartTime + previousDuration;
      float fraction = elapsedTime / (float) getTimerInterval();
      return Math.min(1, Math.max(0, fraction));
    } else {
      return timerProgress;
    }
  }

  public boolean equalsTimerProgress(float fraction) {
    try {
      BigDecimal bdProgress = BigDecimal.valueOf(getTimerProgress()).setScale(
          2, RoundingMode.HALF_UP
      );
      BigDecimal bdFraction = new BigDecimal(fraction).setScale(2, RoundingMode.HALF_UP);
      return bdProgress.equals(bdFraction);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public void updateTimerHandler(float fraction) {
    timerProgress = fraction;
    updateTimerHandler();
  }

  public void updateTimerHandler() {
    if (!isPlaying()) {
      return;
    }
    timerHandler.removeCallbacksAndMessages(null);
    if (isTimerActive()) {
      if (equalsTimerProgress(1)) {
        timerProgress = 0;
      } else {
        long progressInterval = (long) (getTimerProgress() * getTimerInterval());
        long barInterval = getInterval() * getBeatsCount();
        int progressBarCount = (int) (progressInterval / barInterval);
        long progressIntervalFullBars = progressBarCount * barInterval;
        timerProgress = (float) progressIntervalFullBars / getTimerInterval();
      }
      timerStartTime = System.currentTimeMillis();
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeTimerStarted();
      }
      timerHandler.postDelayed(
          () -> new Handler(Looper.getMainLooper()).post(this::stop), getTimerIntervalRemaining()
      );
    }
  }

  public long getElapsedTime() {
    return System.currentTimeMillis() - startTime;
  }

  public String getElapsedTimeString() {
    if (!isTimerActive()) {
      return "";
    }
    long progressInterval = (long) (getTimerProgress() * getTimerInterval());
    switch (timerUnit) {
      case UNIT.SECONDS:
        int seconds = (int) (progressInterval / 1000);
        return "00:" + String.format(Locale.ENGLISH, "%02d", seconds);
      case UNIT.MINUTES:
        return timerDuration + ":00";
      default:
        long barInterval = getInterval() * getBeatsCount();
        int progressBarCount = (int) (progressInterval / barInterval);

        long progressIntervalFullBars = progressBarCount * barInterval;
        long remaining = progressInterval - progressIntervalFullBars;
        int beatCount = (int) (remaining / getInterval());

        String format = getBeatsCount() < 10 ? "%d.%01d" : "%d.%02d";
        return String.format(Locale.ENGLISH, format, progressBarCount + 1, beatCount + 1);
    }
  }

  public String getTotalTimeString() {
    if (!isTimerActive()) {
      return "";
    }
    switch (timerUnit) {
      case UNIT.SECONDS:
        return "00:" + String.format(Locale.ENGLISH, "%02d", timerDuration);
      case UNIT.MINUTES:
        return timerDuration + ":00";
      default:
        return String.valueOf(timerDuration);
    }
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
      Log.i(TAG, "performTick: hellooo " + tick);
      for (MetronomeListener listener : listeners) {
        listener.onMetronomeTick(tick);
      }
    }, latency);
    Log.i(TAG, "performTick: hello " + tick);

    boolean isFirstBeat = ((tick.index / getSubdivisionsCount()) % getBeatsCount()) == 0;
    if (isFirstBeat && tick.subdivision == 1) { // next bar
      long beatIndex = tick.index / getSubdivisionsCount();
      long barIndex = beatIndex / getBeatsCount();
      boolean isCountIn = barIndex < getCountIn();
      if (isIncrementalActive() && incrementalUnit.equals(UNIT.BARS) && !isCountIn) {
        barIndex = barIndex - getCountIn();
        if (barIndex >= incrementalInterval && barIndex % incrementalInterval == 0) {
          changeTempo(incrementalAmount * (incrementalIncrease ? 1 : -1));
        }
      }
    }
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

  public interface MetronomeListener {
    void onMetronomeStart();
    void onMetronomeStop();
    void onMetronomePreTick(Tick tick);
    void onMetronomeTick(Tick tick);
    void onMetronomeTempoChanged(int tempoOld, int tempoNew);
    void onMetronomeTimerStarted();
    void onMetronomeConnectionMissing();
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
