package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.input.WearableButtons;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;

public class ButtonUtil {

    private final Context context;
    private final SharedPreferences sharedPrefs;
    private final OnPressListener listener;
    private final boolean hasMinTwoButtons;
    private boolean isFirstButtonPress;
    private boolean isDown;
    private int nextRun = 400;
    private Handler handler;
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            listener.onPress();
            handler.postDelayed(this, nextRun);
            if (nextRun > 60) nextRun = (int) (nextRun * 0.9);
        }
    };

    public ButtonUtil(Context context, @NonNull OnPressListener listener) {
        this.context = context;
        this.listener = listener;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        isFirstButtonPress = sharedPrefs.getBoolean(
                Constants.PREF.FIRST_PRESS, Constants.DEF.FIRST_PRESS
        );
        hasMinTwoButtons = WearableButtons.getButtonCount(context) >= 2;
        isDown = false;
    }

    public void onPressDown() {
        if (!hasMinTwoButtons || isDown) return;
        isDown = true;
        if (isFirstButtonPress) {
            isFirstButtonPress = false;
            Toast.makeText(context, R.string.msg_long_press, Toast.LENGTH_LONG).show();
            sharedPrefs.edit().putBoolean(Constants.PREF.FIRST_PRESS, isFirstButtonPress).apply();
        }
        if (handler != null) handler.removeCallbacks(runnable);
        listener.onPress();
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(runnable, 800);
    }

    public void onPressUp() {
        if (!hasMinTwoButtons || !isDown) return;
        isDown = false;
        if (handler != null) handler.removeCallbacks(runnable);
        handler = null;
        nextRun = 400;
    }

    public interface OnPressListener {
        void onPress();
    }
}
