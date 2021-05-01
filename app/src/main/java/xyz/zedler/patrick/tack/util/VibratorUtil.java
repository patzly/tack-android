package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.SETTING;

public class VibratorUtil {

  private final SharedPreferences sharedPrefs;
  private final Vibrator vibrator;
  private boolean enabled;

  public static final long TICK = 13;
  public static final long CLICK = 20;
  public static final long HEAVY = 50;

  public VibratorUtil(Context context) {
    vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    enabled = hasVibrator();
  }

  public void vibrate(long duration) {
    if (!enabled) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
    } else {
      vibrator.vibrate(duration);
    }
  }

  public void vibrate(boolean emphasize) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(
          VibrationEffect.createOneShot(
              emphasize ? HEAVY : CLICK, VibrationEffect.DEFAULT_AMPLITUDE
          )
      );
    } else {
      vibrator.vibrate(emphasize ? HEAVY : CLICK);
    }
  }

  private void vibrate(int effectId) {
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrator.vibrate(VibrationEffect.createPredefined(effectId));
    }
  }

  public void tick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrate(VibrationEffect.EFFECT_TICK);
    } else {
      vibrate(TICK);
    }
  }

  public void click() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrate(VibrationEffect.EFFECT_CLICK);
    } else {
      vibrate(CLICK);
    }
  }

  public void doubleClick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK);
    } else {
      vibrate(CLICK);
    }
  }

  public void heavyClick() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrate(VibrationEffect.EFFECT_HEAVY_CLICK);
    } else {
      vibrate(HEAVY);
    }
  }

  public void onResume() {
    enabled = sharedPrefs.getBoolean(SETTING.HAPTIC_FEEDBACK, DEF.HAPTIC_FEEDBACK) && hasVibrator();
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled && hasVibrator();
  }

  public boolean hasVibrator() {
    return vibrator.hasVibrator();
  }
}
