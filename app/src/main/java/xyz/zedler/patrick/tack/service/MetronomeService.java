package xyz.zedler.patrick.tack.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import xyz.zedler.patrick.tack.R;

public class MetronomeService extends Service implements Runnable {

    public static final String ACTION_START = "action_start";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String EXTRA_BPM = "extra_bpm";
    public static final String CHANNEL_ID = "metronome";

    private final IBinder binder = new LocalBinder();

    private SharedPreferences sharedPrefs;
    private int bpm;
    private long interval;

    private SoundPool soundPool;
    private Handler handler;
    private int soundId = -1, emphasis, emphasisIndex;
    private boolean isPlaying, vibrateAlways;

    private Vibrator vibrator;

    private TickListener listener;

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .build();

        if (!sharedPrefs.getBoolean("beat_mode_vibrate", true)) {
            soundId = soundPool.load(this, getSoundId(), 1);
        } else soundId = -1;

        interval = sharedPrefs.getLong("interval", 500);
        emphasis = sharedPrefs.getInt("emphasis", 0);
        vibrateAlways = sharedPrefs.getBoolean("vibrate_always", false);
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
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(
                    new NotificationChannel(
                            CHANNEL_ID,
                            getString(R.string.notification_channel),
                            NotificationManager.IMPORTANCE_LOW
                    )
            );
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this,
                CHANNEL_ID
        );
        startForeground(
                1,
                builder.setContentTitle(getString(R.string.notification_title))
                        .setContentText(getString(R.string.notification_desc))
                        .setColor(ContextCompat.getColor(this, R.color.secondary))
                        .setSmallIcon(R.drawable.ic_round_tack_notification)
                        .setContentIntent(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
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
        sharedPrefs.edit().putLong("interval", interval).apply();
        if (listener != null) {
            listener.onBpmChanged(bpm);
        }
    }

    public void updateTick() {
        if (!sharedPrefs.getBoolean("beat_mode_vibrate", true)) {
            soundId = soundPool.load(this, getSoundId(), 1);
            if (!isPlaying) {
                soundPool.play(soundId, 1, 1, 0, 0, 1);
            }
        } else soundId = -1;
        emphasis = sharedPrefs.getInt("emphasis", 0);
        vibrateAlways = sharedPrefs.getBoolean("vibrate_always", false);
    }

    private int getSoundId() {
        String sound = sharedPrefs.getString("sound", "wood");
        assert sound != null;
        switch (sound) {
            case "click":
                return R.raw.click;
            case "ding":
                return R.raw.ding;
            case "beep":
                return R.raw.beep;
            default:
                return R.raw.wood;
        }
    }

    public boolean isPlaying() {
        return isPlaying;
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
                } else emphasisIndex = 0;
            }

            if (soundId != -1) {
                soundPool.play(soundId, 1, 1, 0, 0, isEmphasis ? 1.5f : 1);
                if (vibrateAlways) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(isEmphasis ? 50 : 20, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(isEmphasis ? 50 : 20);
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(isEmphasis ? 50 : 20, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(isEmphasis ? 50 : 20);
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
