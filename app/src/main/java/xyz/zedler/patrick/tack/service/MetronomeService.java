package xyz.zedler.patrick.tack.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioFocusRequest;
import android.media.AudioFocusRequest.Builder;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.util.AudioUtil;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.MetronomeUtil.TickListener;
import xyz.zedler.patrick.tack.util.NotificationUtil;
import xyz.zedler.patrick.tack.util.ShortcutUtil;

public class MetronomeService extends Service implements TickListener {

  private static final String TAG = MetronomeService.class.getSimpleName();

  private final static int NOTIFICATION_ID = 1;

  private AudioManager audioManager;
  private SharedPreferences sharedPrefs;
  private MetronomeUtil metronomeUtil;
  private NotificationUtil notificationUtil;
  private HapticUtil hapticUtil;
  private ShortcutUtil shortcutUtil;
  private StopReceiver stopReceiver;
  private MetronomeListener listener;
  private HandlerThread thread;
  private Handler latencyHandler, countInHandler, incrementalHandler, timerHandler;
  private boolean alwaysVibrate, incrementalIncrease;
  private long latency, timerStartTime;
  private int incrementalAmount, incrementalInterval, timerDuration;
  private String incrementalUnit, timerUnit;
  private float timerProgress;
  private boolean tempPlaying, tempBeatModeVibrate, tempAlwaysVibrate;
  private int tempTempo, tempGain, tempCountIn, tempIncrementalAmount;
  private String[] tempBeats, tempSubs;

  @Override
  public void onCreate() {
    super.onCreate();

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    notificationUtil = new NotificationUtil(this);
    notificationUtil.createNotificationChannel();

    shortcutUtil = new ShortcutUtil(this);

    metronomeUtil = new MetronomeUtil(this, this);
    metronomeUtil.setTempo(sharedPrefs.getInt(PREF.TEMPO, DEF.TEMPO));
    metronomeUtil.setSound(sharedPrefs.getString(PREF.SOUND, DEF.SOUND));
    metronomeUtil.setBeats(
        sharedPrefs.getString(PREF.BEATS, DEF.BEATS).split(",")
    );
    metronomeUtil.setSubdivisions(
        sharedPrefs.getString(PREF.SUBDIVISIONS, DEF.SUBDIVISIONS).split(",")
    );
    metronomeUtil.setSubdivisionsUsed(sharedPrefs.getBoolean(PREF.USE_SUBS, DEF.USE_SUBS));
    metronomeUtil.setGain(sharedPrefs.getInt(PREF.GAIN, DEF.GAIN));
    metronomeUtil.setCountIn(sharedPrefs.getInt(PREF.COUNT_IN, DEF.COUNT_IN));
    setLatency(sharedPrefs.getLong(PREF.LATENCY, DEF.LATENCY));
    setIncrementalAmount(sharedPrefs.getInt(PREF.INCREMENTAL_AMOUNT, DEF.INCREMENTAL_AMOUNT));
    setIncrementalIncrease(
        sharedPrefs.getBoolean(PREF.INCREMENTAL_INCREASE, DEF.INCREMENTAL_INCREASE)
    );
    setIncrementalInterval(sharedPrefs.getInt(PREF.INCREMENTAL_INTERVAL, DEF.INCREMENTAL_INTERVAL));
    setIncrementalUnit(sharedPrefs.getString(PREF.INCREMENTAL_UNIT, DEF.INCREMENTAL_UNIT));
    setTimerDuration(sharedPrefs.getInt(PREF.TIMER_DURATION, DEF.TIMER_DURATION));
    setTimerUnit(sharedPrefs.getString(PREF.TIMER_UNIT, DEF.TIMER_UNIT));

    thread = new HandlerThread("metronome_service");
    thread.start();
    latencyHandler = new Handler(thread.getLooper());
    countInHandler = new Handler(thread.getLooper());
    incrementalHandler = new Handler(thread.getLooper());
    timerHandler = new Handler(thread.getLooper());

    hapticUtil = new HapticUtil(this);
    setBeatModeVibrate(sharedPrefs.getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE));
    setAlwaysVibrate(sharedPrefs.getBoolean(PREF.ALWAYS_VIBRATE, DEF.ALWAYS_VIBRATE));

    stopReceiver = new StopReceiver();
    ContextCompat.registerReceiver(
        this, stopReceiver, new IntentFilter(ACTION.STOP),
        ContextCompat.RECEIVER_EXPORTED
    );

    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    Log.d(TAG, "onCreate: service created");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    metronomeUtil.destroy();
    latencyHandler.removeCallbacksAndMessages(null);
    countInHandler.removeCallbacksAndMessages(null);
    incrementalHandler.removeCallbacksAndMessages(null);
    timerHandler.removeCallbacksAndMessages(null);
    thread.quit();
    unregisterReceiver(stopReceiver);
    Log.i(TAG, "onDestroy: service destroyed");
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return new LocalBinder();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && intent.getAction() != null) {
      switch (intent.getAction()) {
        case ACTION.START:
          setTempo(intent.getIntExtra(EXTRA.TEMPO, getTempo()));
          start();
          break;
        case ACTION.STOP:
          stop();
          break;
      }
    }
    return START_STICKY;
  }

  @Override
  public void onTick(Tick tick) {
    latencyHandler.postDelayed(() -> {
      if (listener != null) {
        listener.onMetronomePreTick(tick);
      }
    }, Math.max(0, latency - Constants.BEAT_ANIM_OFFSET));
    latencyHandler.postDelayed(() -> {
      if (metronomeUtil.isBeatModeVibrate() || alwaysVibrate) {
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
      if (listener != null) {
        listener.onMetronomeTick(tick);
      }
    }, latency);

    boolean isFirstBeat = ((tick.index / getSubsCount()) % getBeatsCount()) == 0;
    if (isFirstBeat && tick.subdivision == 1) { // next bar
      long beatIndex = tick.index / getSubsCount();
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

  public void start() {
    start(true);
  }

  public void start(boolean reportTempoUsage) {
    if (reportTempoUsage) {
      shortcutUtil.reportUsage(getTempo()); // notify system for shortcut usage prediction
    }
    if (isPlaying()) {
      return;
    }
    metronomeUtil.start();
    if (listener != null) {
      listener.onMetronomeStart();
    }
    countInHandler.postDelayed(() -> {
      updateIncrementalHandler();
      updateTimerHandler();
      // TODO: start muted and elapsed time
    }, getCountInInterval()); // 0 if count-in is disabled
    if (notificationUtil.hasPermission()) {
      startForeground(NOTIFICATION_ID, notificationUtil.getNotification());
    }
    Log.i(TAG, "start: foreground service started");

    // TODO
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      AudioFocusRequest request = new Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(AudioUtil.getAudioAttributes())
          .setWillPauseWhenDucked(true)
          .setOnAudioFocusChangeListener(focusChange -> {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {

            }
          }).build();
      audioManager.requestAudioFocus(new Builder(AudioManager.AUDIOFOCUS_GAIN).build());
    } else {
        /*audioManager.requestAudioFocus(new OnAudioFocusChangeListener() {
          @Override
          public void onAudioFocusChange(int focusChange) {

          }
        }, AudioUtil.getAudioAttributes())*/
    }
  }

  public void stop() {
    if (!isPlaying()) {
      return;
    } else if (listener != null) {
      listener.onMetronomeStop();
    }
    timerProgress = getTimerProgress(); // must be called before metronome stopping

    metronomeUtil.stop();
    latencyHandler.removeCallbacksAndMessages(null);
    countInHandler.removeCallbacksAndMessages(null);
    incrementalHandler.removeCallbacksAndMessages(null);
    timerHandler.removeCallbacksAndMessages(null);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE);
    } else {
      stopForeground(true);
    }
    Log.i(TAG, "stop: foreground service stopped");
  }

  public void saveState() {
    tempPlaying = isPlaying();
    tempTempo = getTempo();
    tempBeats = getBeats();
    tempSubs = getSubdivisions();
    tempBeatModeVibrate = isBeatModeVibrate();
    tempAlwaysVibrate = isAlwaysVibrate();
    tempGain = getGain();
    tempCountIn = getCountIn();
    tempIncrementalAmount = getIncrementalAmount();
  }

  public void restoreState() {
    setTempo(tempTempo);
    setBeats(tempBeats);
    setSubdivisions(tempSubs);
    setBeatModeVibrate(tempBeatModeVibrate);
    setAlwaysVibrate(tempAlwaysVibrate);
    setGain(tempGain);
    setCountIn(tempCountIn);
    setIncrementalAmount(tempIncrementalAmount);
    if (tempPlaying) {
      start(false);
    } else {
      stop();
    }
  }

  public void setMetronomeListener(MetronomeListener listener) {
    this.listener = listener;
  }

  public boolean isPlaying() {
    return metronomeUtil != null && metronomeUtil.isPlaying();
  }

  public void setSubdivisionsUsed(boolean useSubdivisions) {
    metronomeUtil.setSubdivisionsUsed(useSubdivisions);
    sharedPrefs.edit().putBoolean(PREF.USE_SUBS, useSubdivisions).apply();
  }

  public boolean getSubdivisionsUsed() {
    return metronomeUtil.getSubdivisionsUsed();
  }

  public void setBeatModeVibrate(boolean vibrate) {
    if (!hapticUtil.hasVibrator()) {
      vibrate = false;
    }
    metronomeUtil.setBeatModeVibrate(vibrate);
    hapticUtil.setEnabled(vibrate || alwaysVibrate);
    sharedPrefs.edit().putBoolean(PREF.BEAT_MODE_VIBRATE, vibrate).apply();
  }

  public boolean isBeatModeVibrate() {
    return metronomeUtil.isBeatModeVibrate();
  }

  public void setAlwaysVibrate(boolean always) {
    alwaysVibrate = always;
    hapticUtil.setEnabled(always || metronomeUtil.isBeatModeVibrate());
    sharedPrefs.edit().putBoolean(PREF.ALWAYS_VIBRATE, always).apply();
  }

  public boolean isAlwaysVibrate() {
    return alwaysVibrate;
  }

  public boolean areHapticEffectsPossible() {
    return !metronomeUtil.isPlaying() || (!isBeatModeVibrate() && !alwaysVibrate);
  }

  public void setTempo(int tempo) {
    metronomeUtil.setTempo(tempo);
    sharedPrefs.edit().putInt(PREF.TEMPO, tempo).apply();
  }

  public int getTempo() {
    return metronomeUtil.getTempo();
  }

  private void changeTempo(int change) {
    int tempoOld = getTempo();
    int tempoNew = tempoOld + change;
    setTempo(tempoNew);
    if (listener != null) {
      listener.onTempoChanged(tempoOld, tempoNew);
    }
  }

  public long getInterval() {
    return metronomeUtil.getInterval();
  }

  public void setBeats(String[] beats) {
    metronomeUtil.setBeats(beats);
    saveBeats();
  }

  private void saveBeats() {
    sharedPrefs.edit().putString(PREF.BEATS, String.join(",", getBeats())).apply();
  }

  public String[] getBeats() {
    return metronomeUtil.getBeats();
  }

  public int getBeatsCount() {
    return getBeats().length;
  }

  public void setBeat(int beat, String tickType) {
    String[] beats = getBeats();
    beats[beat] = tickType;
    setBeats(beats);
  }

  public boolean addBeat() {
    boolean success = metronomeUtil.addBeat();
    if (success) {
      saveBeats();
    }
    return success;
  }

  public boolean removeBeat() {
    boolean success = metronomeUtil.removeBeat();
    if (success) {
      saveBeats();
    }
    return success;
  }

  public void setSubdivisions(String[] subdivisions) {
    metronomeUtil.setSubdivisions(subdivisions);
    saveSubdivisions();
  }

  public void saveSubdivisions() {
    sharedPrefs.edit()
        .putString(PREF.SUBDIVISIONS, String.join(",", getSubdivisions()))
        .apply();
  }

  public String[] getSubdivisions() {
    return metronomeUtil.getSubdivisions();
  }

  public int getSubsCount() {
    return getSubdivisions().length;
  }

  public void setSubdivision(int subdivision, String tickType) {
    String[] subdivisions = getSubdivisions();
    subdivisions[subdivision] = tickType;
    setSubdivisions(subdivisions);
  }

  public boolean addSubdivision() {
    boolean success = metronomeUtil.addSubdivision();
    if (success) {
      saveSubdivisions();
    }
    return success;
  }

  public boolean removeSubdivision() {
    boolean success = metronomeUtil.removeSubdivision();
    if (success) {
      saveSubdivisions();
    }
    return success;
  }

  public void setSwing3() {
    setSubdivisions(
        String.join(
            ",", TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL).split(","
        )
    );
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
    setSubdivisions(
        String.join(
            ",",
            TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.NORMAL, TICK_TYPE.MUTED
        ).split(",")
    );
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
    setSubdivisions(
        String.join(
            ",",
            TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.MUTED,
            TICK_TYPE.NORMAL, TICK_TYPE.MUTED, TICK_TYPE.MUTED
        ).split(",")
    );
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

  public void setSound(String sound) {
    metronomeUtil.setSound(sound);
    sharedPrefs.edit().putString(PREF.SOUND, sound).apply();
  }

  public void setLatency(long offset) {
    latency = offset;
    sharedPrefs.edit().putLong(PREF.LATENCY, offset).apply();
  }

  public void setGain(int gain) {
    metronomeUtil.setGain(gain);
    sharedPrefs.edit().putInt(PREF.GAIN, gain).apply();
  }

  public int getGain() {
    return metronomeUtil.getGain();
  }

  public void setCountIn(int bars) {
    metronomeUtil.setCountIn(bars);
    sharedPrefs.edit().putInt(PREF.COUNT_IN, bars).apply();
  }

  public int getCountIn() {
    return metronomeUtil.getCountIn();
  }

  public boolean isCountInActive() {
    return getCountIn() > 0;
  }

  public long getCountInInterval() {
    return getInterval() * getBeatsCount() * getCountIn();
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
    timerDuration = duration;
    sharedPrefs.edit().putInt(PREF.TIMER_DURATION, duration).apply();
    updateTimerHandler(0);
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

  public float getTimerProgress() {
    if (!isTimerActive()) {
      return 0;
    } else if (isPlaying()) {
      long previousDuration = (long) (timerProgress * getTimerInterval());
      long elapsedTime = System.currentTimeMillis() - timerStartTime + previousDuration;
      float fraction = elapsedTime / (float) getTimerInterval();
      return Math.min(1, Math.max(0, fraction));
    } else {
      return timerProgress;
    }
  }

  public boolean equalsTimerProgress(float fraction) {
    BigDecimal bdProgress = BigDecimal.valueOf(getTimerProgress()).setScale(
        2, RoundingMode.HALF_UP
    );
    BigDecimal bdFraction = new BigDecimal(fraction).setScale(2, RoundingMode.HALF_UP);
    return bdProgress.equals(bdFraction);
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
      timerStartTime = System.currentTimeMillis(); // important before calling other methods!
      if (equalsTimerProgress(1)) {
        timerProgress = 0;
      } else {
        long progressInterval = (long) (getTimerProgress() * getTimerInterval());
        long barInterval = getInterval() * getBeatsCount();
        int progressBarCount = (int) (progressInterval / barInterval);
        long progressIntervalFullBars = progressBarCount * barInterval;
        timerProgress = (float) progressIntervalFullBars / getTimerInterval();
      }
      listener.onTimerStarted();
      timerHandler.postDelayed(
          () -> new Handler(Looper.getMainLooper()).post(this::stop), getTimerIntervalRemaining()
      );
    }
  }

  public interface MetronomeListener {
    void onMetronomeStart();
    void onMetronomeStop();
    void onMetronomePreTick(Tick tick);
    void onMetronomeTick(Tick tick);
    void onTempoChanged(int tempoOld, int tempoNew);
    void onTimerStarted();
  }

  public class StopReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d(TAG, "onReceive: received stop command");
      stop();
    }
  }

  public class LocalBinder extends Binder {

    public MetronomeService getService() {
      return MetronomeService.this;
    }
  }
}
