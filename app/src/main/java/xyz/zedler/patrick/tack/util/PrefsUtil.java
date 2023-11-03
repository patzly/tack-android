package xyz.zedler.patrick.tack.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.util.Objects;

public class PrefsUtil {

  private final static String TAG = PrefsUtil.class.getSimpleName();

  private final SharedPreferences sharedPrefs;

  public PrefsUtil(Context context) {
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public PrefsUtil checkForMigrations() {

    return this;
  }

  public SharedPreferences getSharedPrefs() {
    return sharedPrefs;
  }

  private void migrateString(String keyOld, String keyNew, String def) {
    if (sharedPrefs.contains(keyOld) && !sharedPrefs.contains(keyNew)) {
      SharedPreferences.Editor editor = sharedPrefs.edit();
      try {
        String current = sharedPrefs.getString(keyOld, def);
        if (!Objects.equals(current, def)) {
          editor.remove(keyOld);
          editor.putString(keyNew, current);
        }
      } catch (ClassCastException ignored) {
        editor.remove(keyOld);
      }
      editor.apply();
    }
  }

  private void migrateBoolean(String keyOld, String keyNew, boolean def) {
    if (sharedPrefs.contains(keyOld) && !sharedPrefs.contains(keyNew)) {
      SharedPreferences.Editor editor = sharedPrefs.edit();
      try {
        boolean current = sharedPrefs.getBoolean(keyOld, def);
        if (!Objects.equals(current, def)) {
          editor.remove(keyOld);
          editor.putBoolean(keyNew, current);
        }
      } catch (ClassCastException ignored) {
        editor.remove(keyOld);
      }
      editor.apply();
    }
  }

  private void migrateInteger(String keyOld, String keyNew, int def) {
    if (sharedPrefs.contains(keyOld) && !sharedPrefs.contains(keyNew)) {
      SharedPreferences.Editor editor = sharedPrefs.edit();
      try {
        int current = sharedPrefs.getInt(keyOld, def);
        if (!Objects.equals(current, def)) {
          editor.remove(keyOld);
          editor.putInt(keyNew, current);
        }
      } catch (ClassCastException ignored) {
        editor.remove(keyOld);
      }
      editor.apply();
    }
  }

  private void migrateFloat(String keyOld, String keyNew, float def) {
    if (sharedPrefs.contains(keyOld) && !sharedPrefs.contains(keyNew)) {
      SharedPreferences.Editor editor = sharedPrefs.edit();
      try {
        float current = sharedPrefs.getFloat(keyOld, def);
        if (!Objects.equals(current, def)) {
          editor.remove(keyOld);
          editor.putFloat(keyNew, current);
        }
      } catch (ClassCastException ignored) {
        editor.remove(keyOld);
      }
      editor.apply();
    }
  }

  private void removePreference(String key) {
    if (sharedPrefs.contains(key)) {
      sharedPrefs.edit().remove(key).apply();
    }
  }

  public static void restartToApply(Activity activity, Class<Activity> launchClass) {
    Intent intent = new Intent(activity, launchClass);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    activity.startActivity(intent);
    activity.finish();
    Runtime.getRuntime().exit(0);
  }
}
