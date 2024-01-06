package xyz.zedler.patrick.tack.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.util.Objects;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.util.MetronomeUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.NotificationUtil;

public class MetronomeService extends Service implements MetronomeListener {

  private static final String TAG = MetronomeService.class.getSimpleName();

  private final static int NOTIFICATION_ID = 1;

  private MetronomeUtil metronomeUtil;
  private NotificationUtil notificationUtil;
  private StopReceiver stopReceiver;

  @Override
  public void onCreate() {
    super.onCreate();

    notificationUtil = new NotificationUtil(this);
    metronomeUtil = new MetronomeUtil(this, true);
    metronomeUtil.addListener(this);

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

    metronomeUtil.destroy();
    unregisterReceiver(stopReceiver);
    Log.d(TAG, "onDestroy: service destroyed");
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
          metronomeUtil.setTempo(intent.getIntExtra(EXTRA.TEMPO, metronomeUtil.getTempo()));
          metronomeUtil.start();
          break;
        case ACTION.STOP:
          metronomeUtil.stop();
          break;
      }
    }
    return START_STICKY;
  }

  @Override
  public void onMetronomeStart() {
    if (notificationUtil.hasPermission()) {
      notificationUtil.createNotificationChannel();
      startForeground(NOTIFICATION_ID, notificationUtil.getNotification());
    }
  }

  @Override
  public void onMetronomeStop() {
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE);
    } else {
      stopForeground(true);
    }
  }

  @Override
  public void onMetronomePreTick(Tick tick) {}

  @Override
  public void onMetronomeTick(Tick tick) {}

  @Override
  public void onMetronomeTempoChanged(int tempoOld, int tempoNew) {}

  @Override
  public void onMetronomeTimerStarted() {}

  @Override
  public void onMetronomeConnectionMissing() {}

  public MetronomeUtil getMetronomeUtil() {
    return metronomeUtil;
  }

  public class StopReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent != null && Objects.equals(intent.getAction(), ACTION.STOP)) {
        Log.d(TAG, "onReceive: received stop command");
        metronomeUtil.stop();
      }
    }
  }

  public class LocalBinder extends Binder {

    public MetronomeService getService() {
      return MetronomeService.this;
    }
  }
}
