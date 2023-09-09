package xyz.zedler.patrick.tack.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.SETTINGS;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.NotificationUtil;
import xyz.zedler.patrick.tack.util.OldAudioUtil;

public class OldMetronomeService extends Service implements Runnable {

  private static final String TAG = OldMetronomeService.class.getSimpleName();

  public static final String ACTION_START = "action_start";
  public static final String ACTION_PAUSE = "action_pause";
  public static final String EXTRA_BPM = "extra_bpm";
  public static final String CHANNEL_ID = "metronome";

  private final IBinder binder = new LocalBinder();

  private SharedPreferences sharedPrefs;
  private HapticUtil hapticUtil;
  private OldAudioUtil audioUtil;
  private int bpm;
  private long interval;

  private Handler handler;
  private String sound = null;
  private int emphasis, emphasisIndex;
  private boolean isPlaying;
  private boolean vibrateAlways;
  private boolean isBeatModeVibrate;

  private TickListener listener;

  @Override
  public void onCreate() {
    super.onCreate();

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    hapticUtil = new HapticUtil(this);
    audioUtil = new OldAudioUtil(this);

    isBeatModeVibrate = sharedPrefs.getBoolean(
        Constants.PREF.BEAT_MODE_VIBRATE, Constants.DEF.BEAT_MODE_VIBRATE
    );
    if (!isBeatModeVibrate) {
      sound = sharedPrefs.getString(SETTINGS.SOUND, DEF.SOUND);
    } else {
      sound = null;
    }

    interval = sharedPrefs.getLong(Constants.PREF.INTERVAL, Constants.DEF.INTERVAL);
    emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, Constants.DEF.EMPHASIS);
    vibrateAlways = sharedPrefs.getBoolean(
        SETTINGS.VIBRATE_ALWAYS, Constants.DEF.ALWAYS_VIBRATE
    );
    bpm = toBpm(interval);

    HandlerThread thread = new HandlerThread("MetronomeHandlerThread");
    thread.start();
    handler = new Handler(thread.getLooper());
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

    int immutableFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        ? PendingIntent.FLAG_IMMUTABLE
        : PendingIntent.FLAG_UPDATE_CURRENT;

    Intent intentApp = new Intent(this, MainActivity.class);
    intentApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent pendingIntentApp = PendingIntent.getActivity(
        this, 0, intentApp, immutableFlag
    );

    Intent intentStop = new Intent(this, OldMetronomeService.class);
    intentStop.setAction(ACTION_PAUSE);
    PendingIntent pendingIntentStop = PendingIntent.getService(
        this, 0, intentStop, immutableFlag
    );

    NotificationUtil notificationUtil = new NotificationUtil(this);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationUtil.createNotificationChannel();
    }
    startForeground(
        1,
        notificationUtil.getNotification()
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
      sound = sharedPrefs.getString(SETTINGS.SOUND, DEF.SOUND);
    } else {
      sound = null;
    }
    emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, Constants.DEF.EMPHASIS);
    vibrateAlways = sharedPrefs.getBoolean(
        SETTINGS.VIBRATE_ALWAYS, Constants.DEF.ALWAYS_VIBRATE
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

  public boolean areHapticEffectsPossible() {
    return !isPlaying || (!isBeatModeVibrate && !vibrateAlways);
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

      if (sound != null) {
        audioUtil.play(sound, isEmphasis);
        if (vibrateAlways) {
          hapticUtil.vibrate(isEmphasis);
        }
      } else {
        hapticUtil.vibrate(isEmphasis);
      }

      if (listener != null) {
        listener.onTick(interval, isEmphasis, emphasisIndex);
      }
    }
  }

  public class LocalBinder extends Binder {

    public OldMetronomeService getService() {
      return OldMetronomeService.this;
    }
  }

  public interface TickListener {

    void onStartTicks();

    void onTick(long interval, boolean isEmphasis, int index);

    void onBpmChanged(int bpm);

    void onStopTicks();
  }
}
