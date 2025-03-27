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
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.FragmentSongsBinding;
import xyz.zedler.patrick.tack.fragment.SongsFragmentDirections.ActionSongsToSong;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.UnlockUtil;

public class SongsFragment extends BaseFragment {

  private static final String TAG = SongsFragment.class.getSimpleName();

  private FragmentSongsBinding binding;
  private MainActivity activity;
  private DialogUtil dialogUtilUnlock;
  private List<SongWithParts> songsWithParts = new ArrayList<>();
  private int songsOrder;
  private SongAdapter adapter;
  private ActivityResultLauncher<String> launcherBackup;
  private ActivityResultLauncher<String[]> launcherRestore;
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
    dialogUtilUnlock.dismiss();
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

    /*new ScrollBehavior().setUpScroll(
        binding.appBarSongs, null, true
    );*/

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
        if (id == R.id.action_sort_name) {
          songsOrder = SONGS_ORDER.NAME_ASC;
        } else if (id == R.id.action_sort_last_played) {
          songsOrder = SONGS_ORDER.LAST_PLAYED_ASC;
        } else {
          songsOrder = SONGS_ORDER.MOST_PLAYED_ASC;
        }
        item.setChecked(true);
        setSongsWithParts(null);
        getSharedPrefs().edit().putInt(PREF.SONGS_ORDER, songsOrder).apply();
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

    songsOrder = getSharedPrefs().getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER);
    int itemId = R.id.action_sort_name;
    if (songsOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      itemId = R.id.action_sort_last_played;
    } else if (songsOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
      itemId = R.id.action_sort_most_played;
    }
    MenuItem itemSort = binding.toolbarSongs.getMenu().findItem(itemId);
    if (itemSort != null) {
      itemSort.setChecked(true);
    }

    adapter = new SongAdapter(song -> {
      performHapticClick();
      ActionSongsToSong action
          = SongsFragmentDirections.actionSongsToSong();
      action.setSongId(song.getSong().getId());
      activity.navigate(action);
    });
    binding.recyclerSongs.setAdapter(adapter);
    // Layout manager
    LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
    binding.recyclerSongs.setLayoutManager(layoutManager);
    binding.recyclerSongs.setItemAnimator(new DefaultItemAnimator());

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

    dialogUtilUnlock = new DialogUtil(activity, "unlock_songs");
    dialogUtilUnlock.createAction(
        R.string.msg_unlock,
        R.string.msg_unlock_description,
        R.string.action_open_play_store,
        () -> UnlockUtil.openPlayStore(activity)
    );
    dialogUtilUnlock.showIfWasShown(savedInstanceState);

    binding.fabSongs.setOnClickListener(v -> {
      performHapticClick();
      if (UnlockUtil.isUnlocked(activity) || songsWithParts.size() < 5) {
        activity.navigate(SongsFragmentDirections.actionSongsToSong());
      } else {
        dialogUtilUnlock.show();
      }
    });
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (dialogUtilUnlock != null) {
      dialogUtilUnlock.saveState(outState);
    }
  }

  private void setSongsWithParts(@Nullable List<SongWithParts> songsWithParts) {
    if (songsWithParts != null) {
      this.songsWithParts = songsWithParts;
      // placeholder illustration
      binding.linearSongsEmpty.setVisibility(songsWithParts.isEmpty() ? View.VISIBLE : View.GONE);
    }
    if (songsOrder == SONGS_ORDER.NAME_ASC) {
      if (VERSION.SDK_INT >= VERSION_CODES.N) {
        Collections.sort(
            this.songsWithParts,
            Comparator.comparing(
                o -> o.getSong().getName(),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            )
        );
      } else {
        Collections.sort(this.songsWithParts, (o1, o2) -> {
          String name1 = (o1.getSong() != null) ? o1.getSong().getName() : null;
          String name2 = (o2.getSong() != null) ? o2.getSong().getName() : null;
          // Nulls last handling
          if (name1 == null && name2 == null) return 0;
          if (name1 == null) return 1;
          if (name2 == null) return -1;
          return name1.compareToIgnoreCase(name2);
        });
      }
    } else if (songsOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      Collections.sort(
          this.songsWithParts,
          (s1, s2) -> Long.compare(
              s2.getSong().getLastPlayed(), s1.getSong().getLastPlayed()
          )
      );
    } else if (songsOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
      Collections.sort(
          this.songsWithParts,
          (s1, s2) -> Integer.compare(
              s2.getSong().getPlayCount(), s1.getSong().getPlayCount()
          )
      );
    }
    adapter.submitList(new ArrayList<>(this.songsWithParts));
    adapter.setSortOrder(songsOrder);
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
        // count existing song names
        for (SongWithParts existingSong : this.songsWithParts) {
          String existingName = existingSong.getSong().getName();
          if (existingName == null) {
            continue;
          }
          Integer currentCount = nameCountMap.get(existingName);
          Integer newCount = currentCount == null ? 1 : currentCount + 1;
          nameCountMap.put(existingName, newCount);
        }
        for (SongWithParts songWithParts : songsWithParts) {
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
        activity.getSongViewModel().insertSongsWithParts(
            songsWithParts, () -> showSnackbar(R.string.msg_restore_success)
        );
      } else {
        showSnackbar(R.string.msg_restore_error);
      }
    } catch (Exception e) {
      showSnackbar(R.string.msg_restore_error);
      Log.e(TAG, "importJsonFromFile: ", e);
    }
  }

  private void showSnackbar(int resId) {
    Snackbar snackbar = activity.getSnackbar(resId, Snackbar.LENGTH_SHORT);
    snackbar.setAnchorView(binding.fabSongs);
    activity.showSnackbar(snackbar);
  }
}