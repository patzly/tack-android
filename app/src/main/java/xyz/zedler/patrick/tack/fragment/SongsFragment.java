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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior.OnScrollChangedListener;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.FragmentSongsBinding;
import xyz.zedler.patrick.tack.fragment.SongsFragmentDirections.ActionSongsToSong;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter.OnSongClickListener;
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListenerAdapter;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.SortUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.UnlockUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.util.ViewUtil.OnMenuInflatedListener;
import xyz.zedler.patrick.tack.util.WidgetUtil;
import xyz.zedler.patrick.tack.util.dialog.BackupDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.UnlockDialogUtil;

public class SongsFragment extends BaseFragment {

  private static final String TAG = SongsFragment.class.getSimpleName();

  private static final String KEY_SONG_TO_DELETE = "song_to_delete";
  private static final String KEY_PARTS_TO_DELETE = "parts_to_delete";

  private FragmentSongsBinding binding;
  private MainActivity activity;
  private DialogUtil dialogUtilIntro, dialogUtilWidgetPrompt, dialogUtilDelete;
  private UnlockDialogUtil unlockDialogUtil;
  private BackupDialogUtil backupDialogUtil;
  private List<SongWithParts> songsWithParts = new ArrayList<>();
  private int sortOrder;
  private Song songToDelete;
  private List<Part> partsToDelete = new ArrayList<>();
  private SongAdapter adapter;
  private MetronomeListener metronomeListener;

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
    dialogUtilWidgetPrompt.dismiss();
    dialogUtilDelete.dismiss();
    unlockDialogUtil.dismiss();
    backupDialogUtil.dismiss();
    if (metronomeListener != null) {
      getMetronomeUtil().removeListener(metronomeListener);
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    boolean isPortrait = UiUtil.isOrientationPortrait(activity);
    boolean isTablet = UiUtil.isTablet(activity);

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarSongs);
    systemBarBehavior.setContainer(binding.constraintSongs);
    systemBarBehavior.setRecycler(binding.recyclerSongs);
    // portrait and tablet landscape: 32 + 80 = 112
    // landscape: 16 + 56 = 72
    // tablet portrait: 56 + 80 = 136
    int bottomInset = ResUtil.getDimension(activity, R.dimen.controls_bottom_margin_bottom);
    bottomInset += UiUtil.dpToPx(activity, isPortrait || isTablet ? 80 : 56); // fab height
    systemBarBehavior.setAdditionalBottomInset(bottomInset);
    systemBarBehavior.setUp();
    SystemBarBehavior.applyBottomInset(binding.fabSongs);

    ScrollBehavior scrollBehavior = new ScrollBehavior();
    if (!isTablet) {
      scrollBehavior.setOnScrollChangedListener(new OnScrollChangedListener() {
        @Override
        public void onScrollUp() {
          binding.fabSongs.extend();
        }

        @Override
        public void onScrollDown() {
          binding.fabSongs.shrink();
        }

        @Override
        public void onTopScroll() {
          binding.fabSongs.extend();
        }
      });
    }
    scrollBehavior.setUpScroll(
        binding.appBarSongs, binding.recyclerSongs, ScrollBehavior.LIFT_ON_SCROLL
    );

    binding.buttonSongsBack.setOnClickListener(getNavigationOnClickListener());
    binding.buttonSongsMenu.setOnClickListener(v -> {
      performHapticClick();

      PopupMenu.OnMenuItemClickListener itemClickListener = item -> {
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
            WidgetUtil.sendSongsWidgetUpdate(activity);
          }
        } else if (id == R.id.action_backup) {
          backupDialogUtil.show();
        } else if (id == R.id.action_settings) {
          activity.navigate(SongsFragmentDirections.actionSongsToSettings());
        } else if (id == R.id.action_feedback) {
          activity.showFeedbackBottomSheet();
        } else if (id == R.id.action_help) {
          activity.showTextBottomSheet(R.raw.help, R.string.title_help);
        }
        return true;
      };
      OnMenuInflatedListener menuInflatedListener = menu -> {
        sortOrder = getSharedPrefs().getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER);
        int itemId = R.id.action_sort_name;
        if (sortOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
          itemId = R.id.action_sort_last_played;
        } else if (sortOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
          itemId = R.id.action_sort_most_played;
        }
        MenuItem itemSort = menu.findItem(itemId);
        if (itemSort != null) {
          itemSort.setChecked(true);
        }
        MenuItem itemBackup = menu.findItem(R.id.action_backup);
        if (itemBackup != null) {
          itemBackup.setEnabled(!songsWithParts.isEmpty());
        }
      };
      ViewUtil.showMenu(v, R.menu.menu_songs, itemClickListener, menuInflatedListener);
    });
    ViewUtil.setTooltipText(binding.buttonSongsBack, R.string.action_back);
    ViewUtil.setTooltipText(binding.buttonSongsMenu, R.string.action_more);

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
      public void onMoreClick() {
        performHapticClick();
      }

      @Override
      public void onApplyClick(@NonNull SongWithParts song) {
        performHapticClick();
        getMetronomeUtil().setCurrentSong(
            song.getSong().getId(), 0, true, false
        );
      }

      @Override
      public void onDeleteClick(@NonNull SongWithParts song) {
        performHapticClick();
        songToDelete = song.getSong();
        partsToDelete = new ArrayList<>(song.getParts());
        dialogUtilDelete.show();
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

    dialogUtilWidgetPrompt = new DialogUtil(activity, "widget_prompt");
    dialogUtilWidgetPrompt.createDialog(builder -> {
      builder.setTitle(R.string.msg_widget_prompt);
      builder.setMessage(R.string.msg_widget_prompt_description);
      builder.setPositiveButton(R.string.action_apply, (dialog, which) -> {
        performHapticClick();
        WidgetUtil.requestSongsWidgetPin(activity);
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilWidgetPrompt.showIfWasShown(savedInstanceState);
    // show widget prompt if needed
    if (savedInstanceState == null) {
      int visitCount = getSharedPrefs().getInt(PREF.SONGS_VISIT_COUNT, 0);
      if (visitCount >= 5) {
        getSharedPrefs().edit().putInt(PREF.SONGS_VISIT_COUNT, -1).apply();
        // show song library widget prompt
        dialogUtilWidgetPrompt.show();
      }
    }

    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SONG_TO_DELETE)) {
      songToDelete = savedInstanceState.getParcelable(KEY_SONG_TO_DELETE);
    }
    dialogUtilDelete = new DialogUtil(activity, "delete");
    dialogUtilDelete.createDialogError(builder -> {
      builder.setTitle(R.string.msg_delete_song);
      builder.setMessage(R.string.msg_delete_song_description);
      builder.setPositiveButton(R.string.action_delete, (dialog, which) -> {
        performHapticClick();
        if (songToDelete == null) {
          Log.e(TAG, "No song to delete set");
          return;
        } else if (partsToDelete.isEmpty()) {
          Log.e(TAG, "No parts to delete set");
          return;
        }
        if (songToDelete.getId().equals(getMetronomeUtil().getCurrentSongId())) {
          // if current song is deleted, change to default
          getMetronomeUtil().setCurrentSong(Constants.SONG_ID_DEFAULT, 0, true);
        }
        activity.getSongViewModel().deleteSong(songToDelete, () -> {
          activity.getSongViewModel().deleteParts(partsToDelete);
          // update shortcut names
          activity.getMetronomeUtil().updateShortcuts();
          // update widget
          WidgetUtil.sendSongsWidgetUpdate(activity);
        });
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilDelete.showIfWasShown(savedInstanceState);

    unlockDialogUtil = new UnlockDialogUtil(activity);
    unlockDialogUtil.showIfWasShown(savedInstanceState);

    backupDialogUtil = new BackupDialogUtil(activity, this);
    backupDialogUtil.showIfWasShown(savedInstanceState);

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
    if (dialogUtilWidgetPrompt != null) {
      dialogUtilWidgetPrompt.saveState(outState);
    }
    if (dialogUtilDelete != null) {
      dialogUtilDelete.saveState(outState);
    }
    if (backupDialogUtil != null) {
      backupDialogUtil.saveState(outState);
    }
    // dialogIntro not needed here

    outState.putParcelable(KEY_SONG_TO_DELETE, songToDelete);
    outState.putParcelableArrayList(KEY_PARTS_TO_DELETE, new ArrayList<>(partsToDelete));
  }

  private void setSongsWithParts(@Nullable List<SongWithParts> songsWithParts) {
    if (songsWithParts != null) {
      this.songsWithParts = songsWithParts;
      // placeholder illustration
      binding.linearSongsEmpty.getRoot().setVisibility(
          songsWithParts.isEmpty() ? View.VISIBLE : View.GONE
      );
    }
    SortUtil.sortSongsWithParts(this.songsWithParts, sortOrder);
    adapter.setSongsWithParts(new ArrayList<>(this.songsWithParts));
    adapter.setSortOrder(sortOrder);
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