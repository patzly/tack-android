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
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SOUND;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
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
  private long latencyOffset, offset;
  private long time;

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

    latencyHandler = new Handler(Looper.getMainLooper());
    latencyOffset = sharedPrefs.getLong(PREF.LATENCY_OFFSET, DEF.OFFSET);

    hapticUtil = new HapticUtil(this);
    setBeatModeVibrate(sharedPrefs.getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE));
    setAlwaysVibrate(sharedPrefs.getBoolean(PREF.ALWAYS_VIBRATE, DEF.ALWAYS_VIBRATE));

    // TODO: remove
    setBeatModeVibrate(false);
    setAlwaysVibrate(false);
    setSound(SOUND.SINE);

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
    long diff = System.currentTimeMillis() - time;
    Log.i(TAG, "onTick: interval = " + diff);
    time = System.currentTimeMillis();
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
    }, offset);
    offset = latencyOffset;
  }

  public void start() {
    if (isPlaying()) {
      return;
    } else if (listener != null) {
      listener.onMetronomeStart();
    }
    offset = 0;
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

  public void setSound(String sound) {
    metronomeUtil.setSound(sound);
    sharedPrefs.edit().putString(PREF.SOUND, sound).apply();
  }

  public void setLatencyOffset(long offset) {
    latencyOffset = offset;
    sharedPrefs.edit().putLong(PREF.LATENCY_OFFSET, offset).apply();
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
