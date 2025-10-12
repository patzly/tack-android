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

import android.annotation.SuppressLint;
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
import com.google.android.material.shape.MaterialShapes;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.tack.Constants;
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
import xyz.zedler.patrick.tack.drawable.ShapeDrawable;
import xyz.zedler.patrick.tack.fragment.SongsFragmentDirections.ActionSongsToSong;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListener;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListenerAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter.OnSongClickListener;
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.NotificationUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.SortUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
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
  private DialogUtil dialogUtilWidgetPrompt, dialogUtilDelete, dialogUtilPermission, dialogUtilGain;
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
    dialogUtilWidgetPrompt.dismiss();
    dialogUtilDelete.dismiss();
    unlockDialogUtil.dismiss();
    backupDialogUtil.dismiss();
    dialogUtilPermission.dismiss();
    dialogUtilGain.dismiss();
    if (metronomeListener != null && getMetronomeEngine() != null) {
      getMetronomeEngine().removeListener(metronomeListener);
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
    int bottomInset = ResUtil.getDimension(activity, R.dimen.fab_margin_bottom);
    bottomInset += UiUtil.dpToPx(activity, isPortrait || isTablet ? 80 : 56); // fab height
    systemBarBehavior.setAdditionalBottomInset(bottomInset);
    systemBarBehavior.setUp();
    int bottomMargin = ResUtil.getDimension(activity, R.dimen.fab_margin_bottom);
    SystemBarBehavior.applyBottomInset(binding.fabSongs, bottomMargin);
    SystemBarBehavior.applyBottomInset(binding.songsEmpty.linearSongsEmptyContainer, bottomMargin);

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
        if (getViewUtil().isClickDisabled(id) || getMetronomeEngine() == null) {
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
          getMetronomeEngine().setSongsOrder(sortOrder);
          if (!songsWithParts.isEmpty()) {
            // only update widget if sort order is important
            WidgetUtil.sendSongsWidgetUpdate(activity);
          }
        } else if (id == R.id.action_backup) {
          backupDialogUtil.show();
        } else if (id == R.id.action_settings) {
          activity.navigate(SongsFragmentDirections.actionSongsToSettings());
        } else if (id == R.id.action_feedback) {
          activity.showFeedback();
        } else if (id == R.id.action_help) {
          activity.showHelp();
        }
        return true;
      };
      OnMenuInflatedListener menuInflatedListener = menu -> {
        if (getMetronomeEngine() == null) {
          return;
        }
        sortOrder = getMetronomeEngine().getSongsOrder();
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
      public void onMetronomePermissionMissing() {
        activity.runOnUiThread(() -> activity.requestNotificationPermission(true));
      }
    };

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
        if (getMetronomeEngine() == null) {
          return;
        }
        if (getMetronomeEngine().getGain() > 0 &&
            getMetronomeEngine().neverStartedWithGainBefore()
        ) {
          dialogUtilGain.show();
        } else {
          boolean permissionDenied = getSharedPrefs().getBoolean(
              PREF.PERMISSION_DENIED, false
          );
          getMetronomeEngine().setCurrentSong(song.getSong().getId(), 0);
          if (NotificationUtil.hasPermission(activity) || permissionDenied) {
            getMetronomeEngine().start();
          } else {
            dialogUtilPermission.show();
          }
        }
        performHapticClick();
      }

      @Override
      public void onPlayStopClick() {
        if (getMetronomeEngine() == null) {
          return;
        }
        if (getMetronomeEngine().isPlaying()) {
          performHapticClick();
          getMetronomeEngine().stop();
        } else {
          if (getMetronomeEngine().getGain() > 0 &&
              getMetronomeEngine().neverStartedWithGainBefore()
          ) {
            dialogUtilGain.show();
          } else {
            boolean permissionDenied = getSharedPrefs().getBoolean(
                PREF.PERMISSION_DENIED, false
            );
            if (NotificationUtil.hasPermission(activity) || permissionDenied) {
              getMetronomeEngine().start();
            } else {
              dialogUtilPermission.show();
            }
          }
          performHapticClick();
        }
      }

      @Override
      public void onMoreClick() {
        performHapticClick();
      }

      @Override
      public void onApplyClick(@NonNull SongWithParts song) {
        if (getMetronomeEngine() == null) {
          return;
        }
        performHapticClick();
        getMetronomeEngine().setCurrentSong(song.getSong().getId(), 0);
      }

      @Override
      public void onDeleteClick(@NonNull SongWithParts song) {
        performHapticClick();
        songToDelete = song.getSong();
        partsToDelete = new ArrayList<>(song.getParts());
        dialogUtilDelete.show();
      }
    });
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

    updateMetronomeControls(true);

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
        } else if (getMetronomeEngine() == null) {
          return;
        }
        if (songToDelete.getId().equals(getMetronomeEngine().getCurrentSongId())) {
          // if current song is deleted, change to default
          getMetronomeEngine().setCurrentSong(Constants.SONG_ID_DEFAULT, 0);
        }
        activity.getSongViewModel().deleteSong(songToDelete, () -> {
          if (getMetronomeEngine() == null) {
            return;
          }
          activity.getSongViewModel().deleteParts(partsToDelete);
          // update shortcut names
          getMetronomeEngine().updateShortcuts();
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

    dialogUtilPermission = new DialogUtil(activity, "notification_permission");
    dialogUtilPermission.createDialog(builder -> {
      builder.setTitle(R.string.msg_notification_permission);
      builder.setMessage(R.string.msg_notification_permission_description);
      builder.setPositiveButton(R.string.action_next, (dialog, which) -> {
        if (getMetronomeEngine() != null) {
          performHapticClick();
          getMetronomeEngine().start();
        }
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilPermission.showIfWasShown(savedInstanceState);

    dialogUtilGain = new DialogUtil(activity, "gain");
    dialogUtilGain.createDialogError(builder -> {
      builder.setTitle(R.string.msg_gain);
      builder.setMessage(R.string.msg_gain_description);
      builder.setPositiveButton(R.string.action_play, (dialog, which) -> {
        if (getMetronomeEngine() != null) {
          performHapticClick();
          getMetronomeEngine().start();
        }
      });
      builder.setNegativeButton(
          R.string.action_deactivate,
          (dialog, which) -> {
            if (getMetronomeEngine() != null) {
              performHapticClick();
              getMetronomeEngine().setGain(0);
              getMetronomeEngine().start();
            }
          });
    });
    dialogUtilGain.showIfWasShown(savedInstanceState);

    binding.fabSongs.setOnClickListener(v -> {
      performHapticClick();
      if (activity.isUnlocked() || songsWithParts.size() < 3) {
        activity.navigate(SongsFragmentDirections.actionSongsToSong());
      } else {
        unlockDialogUtil.show();
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
    if (dialogUtilPermission != null) {
      dialogUtilPermission.saveState(outState);
    }
    if (dialogUtilGain != null) {
      dialogUtilGain.saveState(outState);
    }
    // dialogIntro not needed here

    outState.putParcelable(KEY_SONG_TO_DELETE, songToDelete);
    outState.putParcelableArrayList(KEY_PARTS_TO_DELETE, new ArrayList<>(partsToDelete));
  }

  @Override
  public void updateMetronomeControls(boolean init) {
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    if (binding == null || metronomeEngine == null) {
      return;
    }
    metronomeEngine.addListener(metronomeListener);

    adapter.setCurrentSongId(metronomeEngine.getCurrentSongId());
    adapter.setPlaying(metronomeEngine.isPlaying());
  }

  @SuppressLint("RestrictedApi")
  private void setSongsWithParts(@Nullable List<SongWithParts> songsWithParts) {
    if (songsWithParts != null) {
      this.songsWithParts = songsWithParts;

      // placeholder illustration
      binding.songsEmpty.getRoot().setVisibility(
          songsWithParts.isEmpty() ? View.VISIBLE : View.GONE
      );
      if (songsWithParts.isEmpty()) {
        binding.songsEmpty.imageSongsEmpty.setImageDrawable(
            new ShapeDrawable(
                activity,
                MaterialShapes.COOKIE_7,
                R.drawable.illustration_songs_empty
            )
        );
      }
    }
    SortUtil.sortSongsWithParts(this.songsWithParts, sortOrder);
    adapter.setSongsWithParts(new ArrayList<>(this.songsWithParts));
    adapter.setSortOrder(sortOrder);
  }
}