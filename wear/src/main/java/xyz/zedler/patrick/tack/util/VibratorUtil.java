package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibratorUtil {

    private static final long TICK = 20;
    private static final long TACK = 50;

    public static void vibrate(Context context, boolean emphasize) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    emphasize ? TACK : TICK,
                    VibrationEffect.DEFAULT_AMPLITUDE)
            );
        } else {
            vibrator.vibrate(emphasize ? TACK : TICK);
        }
    }
}
