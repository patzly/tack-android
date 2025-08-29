/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import xyz.zedler.patrick.tack.Constants.BEAT_MODE;
import xyz.zedler.patrick.tack.Constants.FLASH_SCREEN;
import xyz.zedler.patrick.tack.Constants.KEEP_AWAKE;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.SongDatabase;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.model.MetronomeConfig;

public class PrefsUtil {

  private final static String TAG = PrefsUtil.class.getSimpleName();

  private final Context context;
  private final SharedPreferences sharedPrefs;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  public PrefsUtil(Context context) {
    this.context = context;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public PrefsUtil checkForMigrations() {
    migrateBookmarks();
    migrateBeatModeVibrateAndAlwaysVibrate();
    migrateFlashScreen();
    migrateKeepAwake();
    return this;
  }

  public SharedPreferences getSharedPrefs() {
    return sharedPrefs;
  }

  private void migrateBookmarks() {
    String BOOKMARKS = "bookmarks";
    if (sharedPrefs.contains(BOOKMARKS)) {
      // from String to Set<String>
      try {
        Set<String> bookmarks = sharedPrefs.getStringSet(BOOKMARKS, Set.of());
      } catch (Exception ignored) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        try {
          String prefBookmarks = getSharedPrefs().getString(BOOKMARKS, "");
          List<String> bookmarksArray = Arrays.asList(prefBookmarks.split(","));
          List<Integer> bookmarks = new ArrayList<>(bookmarksArray.size());
          for (String bookmark : bookmarksArray) {
            if (!bookmark.isEmpty()) {
              bookmarks.add(Integer.parseInt(bookmark));
            }
          }
          editor.remove(BOOKMARKS);
          Set<String> bookmarksSet = new HashSet<>();
          for (Integer tempo : bookmarks) {
            bookmarksSet.add(String.valueOf(tempo));
          }
          editor.putStringSet(BOOKMARKS, bookmarksSet);
        } catch (Exception ignore) {
          editor.remove(BOOKMARKS);
        }
        editor.apply();
      }

      // from bookmarks to songs
      Set<String> bookmarks = sharedPrefs.getStringSet(BOOKMARKS, Set.of());
      if (!bookmarks.isEmpty()) {
        sharedPrefs.edit().remove(BOOKMARKS).apply();
        SongDatabase db = SongDatabase.getInstance(context.getApplicationContext());
        for (String bookmark : bookmarks) {
          try {
            int tempo = Integer.parseInt(bookmark);
            String songName = context.getString(R.string.label_bpm_value, tempo);
            Song song = new Song(songName);
            MetronomeConfig config = new MetronomeConfig();
            config.setTempo(tempo);
            Part part = new Part(null, song.getId(), 0, config);
            executorService.execute(() -> {
              db.songDao().insertSong(song);
              db.songDao().insertPart(part);
            });
            Log.i(TAG, "migrateBookmarks: added " + song + " for " + bookmark);
          } catch (NumberFormatException e) {
            Log.e(TAG, "migrateBookmarks: bookmark to tempo: ", e);
          }
        }
        // Remove deprecated shortcuts
        ShortcutUtil shortcutUtil = new ShortcutUtil(context);
        shortcutUtil.removeAllShortcuts();
      }
    }
  }

  private void migrateBeatModeVibrateAndAlwaysVibrate() {
    String beatModeVibrateKeyOld = "beat_mode_vibrate";
    boolean beatModeVibrateDefault = false;
    String alwaysVibrateKeyOld = "always_vibrate";
    boolean alwaysVibrateDefault = true;
    if (sharedPrefs.contains(beatModeVibrateKeyOld)) {
      SharedPreferences.Editor editor = sharedPrefs.edit();
      try {
        boolean currentBeatModeVibrate = sharedPrefs.getBoolean(
            beatModeVibrateKeyOld, beatModeVibrateDefault
        );
        boolean currentAlwaysVibrate = sharedPrefs.getBoolean(
            alwaysVibrateKeyOld, alwaysVibrateDefault
        );
        if (currentBeatModeVibrate) {
          editor.putString(PREF.BEAT_MODE, BEAT_MODE.VIBRATION);
        } else if (currentAlwaysVibrate) {
          editor.putString(PREF.BEAT_MODE, BEAT_MODE.ALL);
        } else {
          editor.putString(PREF.BEAT_MODE, BEAT_MODE.SOUND);
        }
      } catch (ClassCastException ignored) {
      } finally {
        editor.remove(beatModeVibrateKeyOld);
        editor.remove(alwaysVibrateKeyOld);
      }
      editor.apply();
    }
  }

  private void migrateFlashScreen() {
    String flashScreenKeyOld = "flash_screen"; // now flash_screen_strength
    boolean flashScreenDefault = false;
    if (sharedPrefs.contains(flashScreenKeyOld)) {
      SharedPreferences.Editor editor = sharedPrefs.edit();
      try {
        boolean current = sharedPrefs.getBoolean(flashScreenKeyOld, flashScreenDefault);
        editor.putString(PREF.FLASH_SCREEN, current ? FLASH_SCREEN.STRONG : FLASH_SCREEN.OFF);
      } catch (ClassCastException ignored) {
      } finally {
        editor.remove(flashScreenKeyOld);
      }
      editor.apply();
    }
  }

  private void migrateKeepAwake() {
    String keepAwakeKeyOld = "keep_awake"; // now keep_screen_awake
    boolean keepAwakeDefault = true;
    if (sharedPrefs.contains(keepAwakeKeyOld)) {
      SharedPreferences.Editor editor = sharedPrefs.edit();
      try {
        boolean current = sharedPrefs.getBoolean(keepAwakeKeyOld, keepAwakeDefault);
        editor.putString(PREF.KEEP_AWAKE, current ? KEEP_AWAKE.WHILE_PLAYING : KEEP_AWAKE.NEVER);
      } catch (ClassCastException ignored) {
      } finally {
        editor.remove(keepAwakeKeyOld);
      }
      editor.apply();
    }
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
}
