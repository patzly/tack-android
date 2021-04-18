package xyz.zedler.patrick.tack.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.AudioUtil;
import xyz.zedler.patrick.tack.util.VibratorUtil;

public class MetronomeService extends Service implements Runnable {

  public static final String ACTION_START = "action_start";
  public static final String ACTION_PAUSE = "action_pause";
  public static final String EXTRA_BPM = "extra_bpm";
  public static final String CHANNEL_ID = "metronome";

  private final IBinder binder = new LocalBinder();

  private SharedPreferences sharedPrefs;
  private VibratorUtil vibratorUtil;
  private AudioUtil audioUtil;
  private int bpm;
  private long interval;

  private Handler handler;
  private int soundId = -1, emphasis, emphasisIndex;
  private boolean isPlaying;
  private boolean vibrateAlways;
  private boolean isBeatModeVibrate;

  private TickListener listener;

  @Override
  public void onCreate() {
    super.onCreate();

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    vibratorUtil = new VibratorUtil(this);
    audioUtil = new AudioUtil(this);

    isBeatModeVibrate = sharedPrefs.getBoolean(
        Constants.PREF.BEAT_MODE_VIBRATE, Constants.DEF.BEAT_MODE_VIBRATE
    );
    if (!isBeatModeVibrate) {
      soundId = audioUtil.getCurrentSoundId();
    } else {
      soundId = -1;
    }

    interval = sharedPrefs.getLong(Constants.PREF.INTERVAL, Constants.DEF.INTERVAL);
    emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, Constants.DEF.EMPHASIS);
    vibrateAlways = sharedPrefs.getBoolean(
        Constants.SETTING.VIBRATE_ALWAYS, Constants.DEF.VIBRATE_ALWAYS
    );
    bpm = toBpm(interval);

    handler = new Handler(Looper.getMainLooper());
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null && intent.getAction() != null) {
      switch (intent.getAction()) {
        case ACTION_START:
          setBpm(intent.getIntExtra(EXTRA_BPM, bpm));
          pause();
          play();
          break;
        case ACTION_PAUSE:
          pause();
          break;
      }
    }
    return START_STICKY;
  }

  private static int toBpm(long interval) {
    return (int) (60000 / interval);
  }

  private static long toInterval(int bpm) {
    return (long) 60000 / bpm;
  }

  public void play() {
    handler.post(this);
    isPlaying = true;
    emphasisIndex = 0;

    Intent intent = new Intent(this, MetronomeService.class);
    intent.setAction(ACTION_PAUSE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManager manager = (NotificationManager) getSystemService(
          Context.NOTIFICATION_SERVICE
      );
      manager.createNotificationChannel(
          new NotificationChannel(
              CHANNEL_ID,
              getString(R.string.notification_channel),
              NotificationManager.IMPORTANCE_LOW
          )
      );
    }
    startForeground(
        1,
        new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_desc))
            .setColor(ContextCompat.getColor(this, R.color.retro_green_fg))
            .setSmallIcon(R.drawable.ic_round_tack_notification)
            .setContentIntent(
                PendingIntent.getService(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_ONE_SHOT
                )
            ).setPriority(NotificationCompat.PRIORITY_LOW)
            .setChannelId(CHANNEL_ID)
            .build()
    );

    if (listener != null) {
      listener.onStartTicks();
    }
  }

  public void pause() {
    handler.removeCallbacks(this);
    stopForeground(true);
    isPlaying = false;

    if (listener != null) {
      listener.onStopTicks();
    }
  }

  public void setBpm(int bpm) {
    this.bpm = bpm;
    interval = toInterval(bpm);
    sharedPrefs.edit().putLong(Constants.PREF.INTERVAL, interval).apply();
    if (listener != null) {
      listener.onBpmChanged(bpm);
    }
  }

  public void updateTick() {
    isBeatModeVibrate = sharedPrefs.getBoolean(
        Constants.PREF.BEAT_MODE_VIBRATE, Constants.DEF.BEAT_MODE_VIBRATE
    );
    if (!isBeatModeVibrate) {
      soundId = audioUtil.getCurrentSoundId();
      if (!isPlaying) {
        audioUtil.play(soundId);
      }
    } else {
      soundId = -1;
    }
    emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, Constants.DEF.EMPHASIS);
    vibrateAlways = sharedPrefs.getBoolean(
        Constants.SETTING.VIBRATE_ALWAYS, Constants.DEF.VIBRATE_ALWAYS
    );
  }

  public boolean isPlaying() {
    return isPlaying;
  }

  public boolean vibrateAlways() {
    return vibrateAlways;
  }

  public boolean isBeatModeVibrate() {
    return isBeatModeVibrate;
  }

  public int getBpm() {
    return bpm;
  }

  public void setTickListener(TickListener listener) {
    this.listener = listener;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    listener = null;
    return super.onUnbind(intent);
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacks(this);
    super.onDestroy();
  }

  @Override
  public void run() {
    if (isPlaying) {
      handler.postDelayed(this, interval);

      boolean isEmphasis = emphasis != 0 && emphasisIndex == 0;
      if (emphasis != 0) {
        if (emphasisIndex < emphasis - 1) {
          emphasisIndex++;
        } else {
          emphasisIndex = 0;
        }
      }

      if (soundId != -1) {
        audioUtil.play(soundId, isEmphasis);
        if (vibrateAlways) {
          vibratorUtil.vibrate(isEmphasis);
        }
      } else {
        vibratorUtil.vibrate(isEmphasis);
      }

      if (listener != null) {
        listener.onTick(interval, isEmphasis, emphasisIndex);
      }
    }
  }

  public class LocalBinder extends Binder {

    public MetronomeService getService() {
      return MetronomeService.this;
    }
  }

  public interface TickListener {

    void onStartTicks();

    void onTick(long interval, boolean isEmphasis, int index);

    void onBpmChanged(int bpm);

    void onStopTicks();
  }
}
