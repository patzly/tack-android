package xyz.zedler.patrick.tack.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
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
import xyz.zedler.patrick.tack.util.MetronomeUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.TickListener;
import xyz.zedler.patrick.tack.util.NotificationUtil;

public class MetronomeService extends Service {

  private static final String TAG = MetronomeService.class.getSimpleName();

  private final static int NOTIFICATION_ID = 1;

  private SharedPreferences sharedPrefs;
  private MetronomeUtil metronomeUtil;
  private NotificationUtil notificationUtil;
  private StopReceiver stopReceiver;
  private boolean beatModeVibrate, alwaysVibrate;

  @SuppressLint("UnspecifiedRegisterReceiverFlag")
  @Override
  public void onCreate() {
    super.onCreate();

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    notificationUtil = new NotificationUtil(this);
    notificationUtil.createNotificationChannel();

    metronomeUtil = new MetronomeUtil(this);
    metronomeUtil.setTempo(sharedPrefs.getInt(PREF.TEMPO, DEF.TEMPO));
    metronomeUtil.setSound(SOUND.SINE);
    metronomeUtil.setSubdivisions(new String[]{TICK_TYPE.MUTED, TICK_TYPE.MUTED, TICK_TYPE.SUB});

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

  public void start() {
    if (isPlaying()) {
      return;
    }
    metronomeUtil.start();
    startForeground(NOTIFICATION_ID, notificationUtil.getNotification());
    Log.i(TAG, "start: foreground service started");
  }

  public void stop() {
    if (!isPlaying()) {
      return;
    }
    metronomeUtil.stop();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE);
    } else {
      stopForeground(true);
    }
    Log.i(TAG, "stop: foreground service stopped");
  }

  public void setTickListener(TickListener listener) {
    metronomeUtil.setTickListener(listener);
  }

  public boolean isPlaying() {
    return metronomeUtil != null && metronomeUtil.isPlaying();
  }

  public boolean isBeatModeVibrate() {
    return beatModeVibrate;
  }

  public boolean isAlwaysVibrate() {
    return alwaysVibrate;
  }

  public void setTempo(int tempo) {
    metronomeUtil.setTempo(tempo);
  }

  public int getTempo() {
    return metronomeUtil.getTempo();
  }

  public long getInterval() {
    return 1000 * 60 / getTempo();
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
