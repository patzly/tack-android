package xyz.zedler.patrick.tack.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.MetronomeUtil.TickListener;
import xyz.zedler.patrick.tack.util.NotificationUtil;

public class MetronomeService extends Service implements TickListener {

  private static final String TAG = MetronomeService.class.getSimpleName();

  private final static int NOTIFICATION_ID = 1;

  private SharedPreferences sharedPrefs;
  private MetronomeUtil metronomeUtil;
  private NotificationUtil notificationUtil;
  private HapticUtil hapticUtil;
  private StopReceiver stopReceiver;
  private MetronomeListener listener;
  private Handler latencyHandler;
  private boolean alwaysVibrate;
  private long latency;

  @Override
  public void onCreate() {
    super.onCreate();

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    notificationUtil = new NotificationUtil(this);
    notificationUtil.createNotificationChannel();

    metronomeUtil = new MetronomeUtil(this, this);
    metronomeUtil.setTempo(sharedPrefs.getInt(PREF.TEMPO, DEF.TEMPO));
    metronomeUtil.setSound(sharedPrefs.getString(PREF.SOUND, DEF.SOUND));
    metronomeUtil.setBeats(
        sharedPrefs.getString(PREF.BEATS, DEF.BEATS).split(" ")
    );
    metronomeUtil.setSubdivisions(
        sharedPrefs.getString(PREF.SUBDIVISIONS, DEF.SUBDIVISIONS).split(" ")
    );
    metronomeUtil.setGain(sharedPrefs.getInt(PREF.GAIN, DEF.GAIN));

    HandlerThread thread = new HandlerThread("metronome_feedback");
    thread.start();
    latencyHandler = new Handler(thread.getLooper());
    latency = sharedPrefs.getLong(PREF.LATENCY, DEF.LATENCY);

    hapticUtil = new HapticUtil(this);
    setBeatModeVibrate(sharedPrefs.getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE));
    setAlwaysVibrate(sharedPrefs.getBoolean(PREF.ALWAYS_VIBRATE, DEF.ALWAYS_VIBRATE));

    stopReceiver = new StopReceiver();
    ContextCompat.registerReceiver(
        this, stopReceiver, new IntentFilter(ACTION.STOP),
        ContextCompat.RECEIVER_EXPORTED
    );
    Log.d(TAG, "onCreate: service created");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    unregisterReceiver(stopReceiver);
    Log.i(TAG, "onDestroy: server destroyed");
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
  }

  public void start() {
    if (isPlaying()) {
      return;
    } else if (listener != null) {
      listener.onMetronomeStart();
    }
    metronomeUtil.start();
    startForeground(NOTIFICATION_ID, notificationUtil.getNotification());
    Log.i(TAG, "start: foreground service started");
  }

  public void stop() {
    if (!isPlaying()) {
      return;
    } else if (listener != null) {
      listener.onMetronomeStop();
    }
    metronomeUtil.stop();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE);
    } else {
      stopForeground(true);
    }
    Log.i(TAG, "stop: foreground service stopped");
  }

  public void setMetronomeListener(MetronomeListener listener) {
    this.listener = listener;
  }

  public boolean isPlaying() {
    return metronomeUtil != null && metronomeUtil.isPlaying();
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

  public long getInterval() {
    return metronomeUtil.getInterval();
  }

  public void setBeats(String[] beats) {
    metronomeUtil.setBeats(beats);
    sharedPrefs.edit().putString(PREF.BEATS, String.join(" ", beats)).apply();
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
      sharedPrefs.edit().putString(PREF.BEATS, String.join(" ", getBeats())).apply();
    }
    return success;
  }

  public boolean removeBeat() {
    boolean success = metronomeUtil.removeBeat();
    if (success) {
      sharedPrefs.edit().putString(PREF.BEATS, String.join(" ", getBeats())).apply();
    }
    return success;
  }

  public void setSubdivisions(String[] subdivisions) {
    metronomeUtil.setSubdivisions(subdivisions);
    sharedPrefs.edit().putString(PREF.SUBDIVISIONS, String.join(" ", subdivisions)).apply();
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
      sharedPrefs.edit()
          .putString(PREF.SUBDIVISIONS, String.join(" ", getSubdivisions()))
          .apply();
    }
    return success;
  }

  public boolean removeSubdivision() {
    boolean success = metronomeUtil.removeSubdivision();
    if (success) {
      sharedPrefs.edit()
          .putString(PREF.SUBDIVISIONS, String.join(" ", getSubdivisions()))
          .apply();
    }
    return success;
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

  public String getTempoTerm() {
    String[] terms = getResources().getStringArray(R.array.label_tempo_terms);
    int tempo = getTempo();
    if (tempo < 60) {
      return terms[0];
    } else if (tempo < 66) {
      return terms[1];
    } else if (tempo < 76) {
      return terms[2];
    } else if (tempo < 108) {
      return terms[3];
    } else if (tempo < 120) {
      return terms[4];
    } else if (tempo < 168) {
      return terms[5];
    } else if (tempo < 200) {
      return terms[6];
    } else {
      return terms[7];
    }
  }

  public interface MetronomeListener {
    void onMetronomeStart();
    void onMetronomeStop();
    void onMetronomeTick(Tick tick);
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
