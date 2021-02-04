package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;

import androidx.preference.PreferenceManager;

import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;

public class AudioUtil {

    private final Context context;
    private final SoundPool soundPool;
    private final SharedPreferences sharedPrefs;

    public AudioUtil(Context context) {
        this.context = context;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                ).build();
    }

    public void destroy() {
        soundPool.release();
    }

    public void play(int soundId, boolean isEmphasis) {
        soundPool.play(
                soundId, 1, 1, 0, 0, isEmphasis ? 1.5f : 1
        );
    }

    public int getCurrentSoundId() {
        return soundPool.load(context, getResId(), 1);
    }

    private int getResId() {
        switch (sharedPrefs.getString(Constants.SETTING.SOUND, Constants.DEF.SOUND)) {
            case Constants.SOUND.CLICK:
                return R.raw.click;
            case Constants.SOUND.DING:
                return R.raw.ding;
            case Constants.SOUND.BEEP:
                return R.raw.beep;
            default:
                return R.raw.wood;
        }
    }
}
