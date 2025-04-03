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

package xyz.zedler.patrick.tack.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import com.google.android.material.snackbar.Snackbar;
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
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.FragmentSongsBinding;
import xyz.zedler.patrick.tack.fragment.SongsFragmentDirections.ActionSongsToSong;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter.OnSongClickListener;
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListenerAdapter;
import xyz.zedler.patrick.tack.util.SortUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.UnlockDialogUtil;
import xyz.zedler.patrick.tack.util.UnlockUtil;
import xyz.zedler.patrick.tack.util.WidgetUtil;

public class SongsFragment extends BaseFragment {

  private static final String TAG = SongsFragment.class.getSimpleName();

  private FragmentSongsBinding binding;
  private MainActivity activity;
  private DialogUtil dialogUtilIntro;
  private UnlockDialogUtil unlockDialogUtil;
  private List<SongWithParts> songsWithParts = new ArrayList<>();
  private int sortOrder;
  private SongAdapter adapter;
  private ActivityResultLauncher<String> launcherBackup;
  private ActivityResultLauncher<String[]> launcherRestore;
  private MetronomeListener metronomeListener;
  private final Gson gson = new Gson();

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentSongsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    dialogUtilIntro.dismiss();
    unlockDialogUtil.dismiss();
    if (metronomeListener != null) {
      getMetronomeUtil().removeListener(metronomeListener);
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarSongs);
    systemBarBehavior.setContainer(binding.constraintSongs);
    systemBarBehavior.setRecycler(binding.recyclerSongs);
    systemBarBehavior.setAdditionalBottomInset(UiUtil.dpToPx(activity, 96));
    systemBarBehavior.setUp();
    SystemBarBehavior.applyBottomInset(binding.fabSongs);

    new ScrollBehavior().setUpScroll(
        binding.appBarSongs, binding.recyclerSongs, ScrollBehavior.LIFT_ON_SCROLL
    );

    binding.toolbarSongs.setNavigationOnClickListener(getNavigationOnClickListener());
    binding.toolbarSongs.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (getViewUtil().isClickDisabled(id)) {
        return false;
      }
      performHapticClick();
      if (id == R.id.action_sort_name
          || id == R.id.action_sort_last_played
          || id == R.id.action_sort_most_played) {
        if (item.isChecked()) {
          return false;
        }
        if (id == R.id.action_sort_name) {
          sortOrder = SONGS_ORDER.NAME_ASC;
        } else if (id == R.id.action_sort_last_played) {
          sortOrder = SONGS_ORDER.LAST_PLAYED_ASC;
        } else {
          sortOrder = SONGS_ORDER.MOST_PLAYED_ASC;
        }
        item.setChecked(true);
        setSongsWithParts(null);
        getSharedPrefs().edit().putInt(PREF.SONGS_ORDER, sortOrder).apply();
        getMetronomeUtil().updateSongsOrder(sortOrder);
        if (!songsWithParts.isEmpty()) {
          // only update widget if sort order is important
          WidgetUtil.sendWidgetUpdate(activity);
        }
      } else if (id == R.id.action_backup) {
        launcherBackup.launch("song_library.json");
      } else if (id == R.id.action_restore) {
        launcherRestore.launch(new String[]{"application/json"});
      } else if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_help) {
        activity.showTextBottomSheet(R.raw.help, R.string.title_help);
      }
      return true;
    });

    sortOrder = getSharedPrefs().getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER);
    int itemId = R.id.action_sort_name;
    if (sortOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      itemId = R.id.action_sort_last_played;
    } else if (sortOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
      itemId = R.id.action_sort_most_played;
    }
    MenuItem itemSort = binding.toolbarSongs.getMenu().findItem(itemId);
    if (itemSort != null) {
      itemSort.setChecked(true);
    }

    metronomeListener = new MetronomeListenerAdapter() {
      @Override
      public void onMetronomeStart() {
        activity.runOnUiThread(() -> adapter.setPlaying(true));
      }

      @Override
      public void onMetronomeStop() {
        activity.runOnUiThread(() -> adapter.setPlaying(false));
      }

      @Override
      public void onMetronomeSongOrPartChanged(@Nullable SongWithParts song, int partIndex) {
        activity.runOnUiThread(
            () -> adapter.setCurrentSongId(song != null ? song.getSong().getId() : null)
        );
      }

      @Override
      public void onMetronomeConnectionMissing() {
        activity.runOnUiThread(() -> showSnackbar(R.string.msg_connection_lost));
      }
    };
    getMetronomeUtil().addListener(metronomeListener);

    adapter = new SongAdapter(new OnSongClickListener() {
      @Override
      public void onSongClick(@NonNull SongWithParts song) {
        performHapticClick();
        ActionSongsToSong action
            = SongsFragmentDirections.actionSongsToSong();
        action.setSongId(song.getSong().getId());
        activity.navigate(action);
      }

      @Override
      public void onPlayClick(@NonNull SongWithParts song) {
        if (getMetronomeUtil().areHapticEffectsPossible(true)) {
          performHapticClick();
        }
        getMetronomeUtil().setCurrentSong(
            song.getSong().getId(), 0, true, true
        );
      }

      @Override
      public void onPlayStopClick() {
        if (getMetronomeUtil().areHapticEffectsPossible(true)) {
          performHapticClick();
        }
        if (getMetronomeUtil().isPlaying()) {
          getMetronomeUtil().stop();
        } else {
          getMetronomeUtil().start();
        }
      }

      @Override
      public void onCloseClick() {
        performHapticClick();
        getMetronomeUtil().setCurrentSong(Constants.SONG_ID_DEFAULT, 0, true);
      }
    });
    adapter.setCurrentSongId(getMetronomeUtil().getCurrentSongId());
    adapter.setPlaying(getMetronomeUtil().isPlaying());
    binding.recyclerSongs.setAdapter(adapter);
    // Layout manager
    LinearLayoutManager layoutManager = new WrapperLinearLayoutManager(activity);
    binding.recyclerSongs.setLayoutManager(layoutManager);
    ItemAnimator itemAnimator = new DefaultItemAnimator();
    // Suppress fading on play tap caused by song play count
    itemAnimator.setChangeDuration(0);
    binding.recyclerSongs.setItemAnimator(itemAnimator);

    activity.getSongViewModel().getAllSongsWithPartsLive().observe(
        getViewLifecycleOwner(), songs -> {
          List<SongWithParts> songsWithParts = new ArrayList<>(songs);
          for (SongWithParts songWithParts : songsWithParts) {
            // Remove default song from list
            if (songWithParts.getSong().getId().equals(Constants.SONG_ID_DEFAULT)) {
              songsWithParts.remove(songWithParts);
              break;
            }
          }
          setSongsWithParts(songsWithParts);
        }
    );

    launcherBackup = registerForActivityResult(
        new ActivityResultContracts.CreateDocument("application/json"),
        this::exportJsonToFile
    );
    launcherRestore = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(),
        this::importJsonFromFile
    );

    dialogUtilIntro = new DialogUtil(activity, "songs_intro");
    dialogUtilIntro.createDialog(builder -> {
      builder.setTitle(R.string.msg_songs_intro);
      builder.setMessage(R.string.msg_songs_intro_description);
      builder.setPositiveButton(
          R.string.action_close,
          (dialog, which) -> {
            performHapticClick();
            getSharedPrefs().edit().putBoolean(PREF.SONGS_INTRO_SHOWN, true).apply();
          });
      builder.setOnCancelListener(dialog -> {
        performHapticClick();
        getSharedPrefs().edit().putBoolean(PREF.SONGS_INTRO_SHOWN, true).apply();
      });
    });
    if (!getSharedPrefs().getBoolean(PREF.SONGS_INTRO_SHOWN, false)) {
      dialogUtilIntro.show();
    }

    unlockDialogUtil = new UnlockDialogUtil(activity);
    unlockDialogUtil.showIfWasShown(savedInstanceState);

    binding.fabSongs.setOnClickListener(v -> {
      performHapticClick();
      if (activity.isUnlocked() || songsWithParts.size() < 3) {
        activity.navigate(SongsFragmentDirections.actionSongsToSong());
      } else {
        unlockDialogUtil.show(
            UnlockUtil.isKeyInstalled(activity)
                && !UnlockUtil.isInstallerValid(activity)
        );
      }
    });
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (unlockDialogUtil != null) {
      unlockDialogUtil.saveState(outState);
    }
    // dialogIntro not needed here
  }

  private void setSongsWithParts(@Nullable List<SongWithParts> songsWithParts) {
    if (songsWithParts != null) {
      this.songsWithParts = songsWithParts;
      // placeholder illustration
      binding.linearSongsEmpty.getRoot().setVisibility(
          songsWithParts.isEmpty() ? View.VISIBLE : View.GONE
      );
      // toolbar backup menu item
      MenuItem itemBackup = binding.toolbarSongs.getMenu().findItem(R.id.action_backup);
      if (itemBackup != null) {
        itemBackup.setEnabled(!songsWithParts.isEmpty());
      }
    }
    SortUtil.sortSongsWithParts(this.songsWithParts, sortOrder);
    adapter.submitList(new ArrayList<>(this.songsWithParts));
    adapter.setSortOrder(sortOrder);
  }

  private void exportJsonToFile(Uri uri) {
    if (uri == null) {
      showSnackbar(R.string.msg_backup_directory_missing);
      return;
    }
    try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
      if (outputStream != null) {
        String json = gson.toJson(songsWithParts);
        outputStream.write(json.getBytes());
        outputStream.flush();
        showSnackbar(R.string.msg_backup_success);
      }
    } catch (Exception e) {
      showSnackbar(R.string.msg_backup_error);
      Log.e(TAG, "exportJsonToFile: ", e);
    }
  }

  private void importJsonFromFile(Uri uri) {
    if (uri == null) {
      showSnackbar(R.string.msg_restore_file_missing);
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
        for (SongWithParts existingSong : this.songsWithParts) {
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
              newName = getString(R.string.msg_restore_duplicate_name, originalName, counter);
              counter++;
            } while (nameCountMap.containsKey(newName));
          }
          songWithParts.getSong().setName(newName);
          nameCountMap.put(newName, 1);
        }
        activity.getSongViewModel().insertSongsWithParts(songsWithParts, () -> {
          showSnackbar(R.string.msg_restore_success);
          // update shortcuts
          activity.getMetronomeUtil().updateShortcuts();
          // update widget
          WidgetUtil.sendWidgetUpdate(activity);
        });
      } else {
        showSnackbar(R.string.msg_restore_error);
      }
    } catch (Exception e) {
      showSnackbar(R.string.msg_restore_error);
      Log.e(TAG, "importJsonFromFile: ", e);
    }
  }

  private void showSnackbar(int resId) {
    if (binding == null) {
      return;
    }
    Snackbar snackbar = activity.getSnackbar(resId, Snackbar.LENGTH_SHORT);
    snackbar.setAnchorView(binding.fabSongs);
    activity.showSnackbar(snackbar);
  }
}