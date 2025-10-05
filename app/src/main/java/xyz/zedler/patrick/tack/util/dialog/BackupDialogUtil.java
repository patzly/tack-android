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

package xyz.zedler.patrick.tack.util.dialog;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.PartialDialogBackupBinding;
import xyz.zedler.patrick.tack.fragment.BaseFragment;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.util.WidgetUtil;

public class BackupDialogUtil implements OnClickListener {

  private static final String TAG = BackupDialogUtil.class.getSimpleName();

  private final MainActivity activity;
  private final PartialDialogBackupBinding binding;
  private final DialogUtil dialogUtil;
  private final Gson gson = new Gson();
  private final ViewUtil viewUtil = new ViewUtil();
  private final ActivityResultLauncher<String> launcherBackup;
  private final ActivityResultLauncher<String[]> launcherRestore;

  public BackupDialogUtil(MainActivity activity, BaseFragment fragment) {
    this.activity = activity;

    binding = PartialDialogBackupBinding.inflate(activity.getLayoutInflater());

    dialogUtil = new DialogUtil(activity, "backup");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.settings_backup);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> activity.performHapticClick()
      );
    });

    launcherBackup = fragment.registerForActivityResult(
        new ActivityResultContracts.CreateDocument("application/json"),
        this::exportJsonToFile
    );
    launcherRestore = fragment.registerForActivityResult(
        new ActivityResultContracts.OpenDocument(),
        this::importJsonFromFile
    );

    ViewUtil.setOnClickListeners(
        this,
        binding.linearBackupBackup,
        binding.linearBackupRestore
    );

    setDividerVisibility(!UiUtil.isOrientationPortrait(activity));
  }

  public void show() {
    update();
    dialogUtil.show();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    update();
    dialogUtil.showIfWasShown(state);
  }

  public void dismiss() {
    dialogUtil.dismiss();
  }

  public void saveState(@NonNull Bundle outState) {
    if (dialogUtil != null) {
      dialogUtil.saveState(outState);
    }
  }

  public void update() {
    if (binding == null) {
      return;
    }
    binding.scrollBackup.scrollTo(0, 0);
    measureScrollView();
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (viewUtil.isClickDisabled(id)) {
      return;
    } else {
      activity.performHapticClick();
    }

    if (id == R.id.linear_backup_backup) {
      launcherBackup.launch("song_library.json");
    } else if (id == R.id.linear_backup_restore) {
      launcherRestore.launch(new String[]{"application/json"});
    }
  }

  private void exportJsonToFile(Uri uri) {
    if (uri == null) {
      showToast(R.string.msg_backup_directory_missing);
      return;
    }
    activity.getSongViewModel().fetchAllSongsWithParts(songsWithParts -> {
      List<SongWithParts> filteredSongs = new ArrayList<>(songsWithParts);
      for (int i = filteredSongs.size() - 1; i >= 0; i--) {
        if (filteredSongs.get(i).getSong().getId().equals(Constants.SONG_ID_DEFAULT)) {
          filteredSongs.remove(i);
        }
      }
      try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
        if (outputStream != null) {
          String json = gson.toJson(filteredSongs);
          outputStream.write(json.getBytes());
          outputStream.flush();
          showToast(R.string.msg_backup_success);
        }
      } catch (Exception e) {
        showToast(R.string.msg_backup_error);
        Log.e(TAG, "exportJsonToFile: ", e);
      }
    });
  }

  private void importJsonFromFile(Uri uri) {
    if (uri == null) {
      showToast(R.string.msg_restore_file_missing);
      return;
    }
    try (InputStream inputStream = activity.getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
    ) {
      StringBuilder jsonString = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        jsonString.append(line);
      }
      Type listType = new TypeToken<List<SongWithParts>>(){}.getType();
      List<SongWithParts> songsWithParts = gson.fromJson(jsonString.toString(), listType);
      if (songsWithParts != null) {
        // look for duplicates of existing song names
        Map<String, Integer> nameCountMap = new HashMap<>();
        Map<String, String> idNameMap = new HashMap<>();
        // count existing song names
        activity.getSongViewModel().fetchAllSongsWithParts(existingSongs -> {
          for (SongWithParts existingSong : existingSongs) {
            // add existing song id to map
            idNameMap.put(existingSong.getSong().getId(), existingSong.getSong().getName());
            String existingName = existingSong.getSong().getName();
            if (existingName == null || existingName.isEmpty()) {
              continue;
            }
            Integer currentCount = nameCountMap.get(existingName);
            Integer newCount = currentCount == null ? 1 : currentCount + 1;
            nameCountMap.put(existingName, newCount);
          }
          for (SongWithParts songWithParts : songsWithParts) {
            String songId = songWithParts.getSong().getId();
            if (idNameMap.containsKey(songId)) {
              // if song id already exists, use existing song name
              String existingName = idNameMap.get(songId);
              songWithParts.getSong().setName(existingName);
              continue;
            }
            String originalName = songWithParts.getSong().getName();
            String newName = originalName;
            Integer count = nameCountMap.get(originalName);
            int counter = count == null ? 0 : count;
            // increment counter if name already exists
            if (counter > 0) {
              do {
                newName = activity.getString(
                    R.string.msg_restore_duplicate_name, originalName, counter
                );
                counter++;
              } while (nameCountMap.containsKey(newName));
            }
            songWithParts.getSong().setName(newName);
            nameCountMap.put(newName, 1);
          }
          activity.getSongViewModel().insertSongsWithParts(songsWithParts, () -> {
            showToast(R.string.msg_restore_success);
            // update shortcuts
            if (activity.getMetronomeEngine() != null) {
              activity.getMetronomeEngine().updateShortcuts();
            }
            // update widget
            WidgetUtil.sendSongsWidgetUpdate(activity);
          });
        });
      } else {
        showToast(R.string.msg_restore_error);
      }
    } catch (Exception e) {
      showToast(R.string.msg_restore_error);
      Log.e(TAG, "importJsonFromFile: ", e);
    }
  }

  private void showToast(int resId) {
    activity.runOnUiThread(() -> Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show());
  }

  private void measureScrollView() {
    binding.scrollBackup.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.scrollBackup.canScrollVertically(-1)
                || binding.scrollBackup.canScrollVertically(1);
            setDividerVisibility(isScrollable);
            binding.scrollBackup.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  private void setDividerVisibility(boolean visible) {
    binding.dividerBackupTop.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.dividerBackupBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.linearBackupContainer.setPadding(
        binding.linearBackupContainer.getPaddingLeft(),
        visible ? UiUtil.dpToPx(activity, 16) : 0,
        binding.linearBackupContainer.getPaddingRight(),
        visible ? UiUtil.dpToPx(activity, 16) : 0
    );
  }
}
