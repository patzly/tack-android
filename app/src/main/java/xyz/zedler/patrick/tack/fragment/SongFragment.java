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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat;
import androidx.core.view.WindowInsetsAnimationCompat.Callback;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.math.MathUtils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior.OnScrollChangedListener;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.databinding.FragmentSongBinding;
import xyz.zedler.patrick.tack.model.MetronomeConfig;
import xyz.zedler.patrick.tack.recyclerview.adapter.PartAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.PartAdapter.OnPartItemClickListener;
import xyz.zedler.patrick.tack.recyclerview.decoration.PartItemDecoration;
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.OptionsUtil;
import xyz.zedler.patrick.tack.util.OptionsUtil.OnPartEditListener;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.SortUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.util.ViewUtil.OnMenuInflatedListener;
import xyz.zedler.patrick.tack.util.WidgetUtil;
import xyz.zedler.patrick.tack.util.dialog.RenameDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.UnlockDialogUtil;

public class SongFragment extends BaseFragment implements OnClickListener, OnCheckedChangeListener {

  private static final String TAG = SongFragment.class.getSimpleName();

  private static final String KEY_SONG_RESULT = "song_result";
  private static final String KEY_PARTS_RESULT = "parts_result";

  private FragmentSongBinding binding;
  private MainActivity activity;
  private DialogUtil dialogUtilDiscard, dialogUtilDelete;
  private UnlockDialogUtil unlockDialogUtil;
  private RenameDialogUtil renameDialogUtil;
  private OptionsUtil optionsUtil;
  private OnBackPressedCallback onBackPressedCallback;
  private PartAdapter adapter;
  private Song songSource;
  private Song songResult = new Song();
  private List<Song> songsExisting = new LinkedList<>();
  private List<Part> partsSource = new ArrayList<>();
  private List<Part> partsResult = new LinkedList<>();
  private boolean isNewSong;
  private boolean hasUnsavedChanges = true;
  private float fabBaseY;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentSongBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (onBackPressedCallback != null) {
      onBackPressedCallback.remove();
    }
    dialogUtilDiscard.dismiss();
    dialogUtilDelete.dismiss();
    renameDialogUtil.dismiss();
    unlockDialogUtil.dismiss();
    optionsUtil.dismiss();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    boolean isPortrait = UiUtil.isOrientationPortrait(activity);
    boolean isTablet = UiUtil.isTablet(activity);

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarSong);
    systemBarBehavior.setContainer(binding.constraintSongContainer);
    systemBarBehavior.setRecycler(binding.recyclerSongParts);
    int bottomInset = ResUtil.getDimension(activity, R.dimen.fab_margin_bottom);
    bottomInset += UiUtil.dpToPx(activity, isPortrait || isTablet ? 80 : 56); // fab height
    systemBarBehavior.setAdditionalBottomInset(bottomInset);
    systemBarBehavior.setMultiColumnLayout(!isPortrait);
    systemBarBehavior.setUp();
    SystemBarBehavior.applyBottomInset(
        binding.fabSong, ResUtil.getDimension(activity, R.dimen.fab_margin_bottom)
    );

    ScrollBehavior scrollBehavior = new ScrollBehavior();
    if (!isTablet) {
      scrollBehavior.setOnScrollChangedListener(new OnScrollChangedListener() {
        @Override
        public void onScrollUp() {
          binding.fabSong.extend();
        }

        @Override
        public void onScrollDown() {
          binding.fabSong.shrink();
        }

        @Override
        public void onTopScroll() {
          binding.fabSong.extend();
        }
      });
    }
    int liftMode = isPortrait ? ScrollBehavior.ALWAYS_LIFTED : ScrollBehavior.LIFT_ON_SCROLL;
    scrollBehavior.setUpScroll(binding.appBarSong, binding.recyclerSongParts, liftMode);

    binding.fabSong.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            fabBaseY = binding.fabSong.getY();
            // Kill ViewTreeObserver
            if (binding.fabSong.getViewTreeObserver().isAlive()) {
              binding.fabSong.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
          }
        });

    setupImeAnimation(systemBarBehavior);

    binding.toolbarSong.setTitleCentered(isTablet || !isPortrait);

    binding.buttonSongClose.setOnClickListener(v -> {
      if (getViewUtil().isClickEnabled(v.getId())) {
        performHapticClick();
        if (hasUnsavedChanges) {
          dialogUtilDiscard.show();
        } else {
          navigateUp();
        }
      }
    });
    binding.buttonSongSave.setOnClickListener(v -> {
      if (getViewUtil().isClickDisabled(v.getId())) {
        return;
      }
      performHapticClick();
      if (hasUnsavedChanges) {
        if (isNewSong) {
          activity.getSongViewModel().insertSong(songResult);
          activity.getSongViewModel().insertParts(partsResult);
          // update widget, no shortcuts update needed because play count is zero
          WidgetUtil.sendSongsWidgetUpdate(activity);
        } else {
          activity.getSongViewModel().updateSongAndParts(
              songResult, partsResult, partsSource, () -> {
                if (getMetronomeEngine() == null) {
                  return;
                }
                // update looped in metronome
                getMetronomeEngine().reloadCurrentSong();
                // update shortcut names
                getMetronomeEngine().updateShortcuts();
                // update widget
                WidgetUtil.sendSongsWidgetUpdate(activity);
              });
        }
        navigateUp();
      }
    });
    binding.buttonSongMenu.setOnClickListener(v -> {
      performHapticClick();
      PopupMenu.OnMenuItemClickListener itemClickListener = item -> {
        int id = item.getItemId();
        if (getViewUtil().isClickDisabled(id)) {
          return false;
        }
        performHapticClick();
        if (id == R.id.action_delete) {
          dialogUtilDelete.show();
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
        MenuItem itemDelete = menu.findItem(R.id.action_delete);
        if (itemDelete != null) {
          itemDelete.setVisible(!isNewSong);
        }
      };
      ViewUtil.showMenu(v, R.menu.menu_song, itemClickListener, menuInflatedListener);
    });
    ViewUtil.setTooltipText(binding.buttonSongClose, R.string.action_close);
    ViewUtil.setTooltipText(binding.buttonSongSave, R.string.action_save);
    ViewUtil.setTooltipText(binding.buttonSongMenu, R.string.action_more);

    binding.buttonSongSave.setEnabled(false);

    adapter = new PartAdapter(new OnPartItemClickListener() {
      @Override
      public void onEditClick(@NonNull Part part) {
        performHapticClick();
        optionsUtil.setPart(new Part(part), false);
        optionsUtil.show();

        binding.editTextSongName.postDelayed(() -> {
          // Remove focus from edit text but with delay to prevent animation jank
          UiUtil.hideKeyboard(binding.editTextSongName);
          binding.editTextSongName.clearFocus();
        }, 200);
      }

      @Override
      public void onMoveUpClick(@NonNull Part part) {
        performHapticClick();
        Part partCurrent = new Part(part);
        int index = partCurrent.getPartIndex();
        if (index > 0) {
          Part partAbove = new Part(partsResult.get(index - 1));
          partCurrent.setPartIndex(index - 1);
          partAbove.setPartIndex(index);
          partsResult.set(index - 1, partCurrent);
          partsResult.set(index, partAbove);
          sortParts();
          adapter.setParts(new ArrayList<>(partsResult));
          updateResult();
        }
        // Remove focus from edit text
        UiUtil.hideKeyboard(binding.editTextSongName);
        binding.editTextSongName.clearFocus();
      }

      @Override
      public void onMoveDownClick(@NonNull Part part) {
        performHapticClick();
        Part partCurrent = new Part(part);
        int index = partCurrent.getPartIndex();
        if (index < partsResult.size() - 1) {
          Part partBelow = new Part(partsResult.get(index + 1));
          partCurrent.setPartIndex(index + 1);
          partBelow.setPartIndex(index);
          partsResult.set(index + 1, partCurrent);
          partsResult.set(index, partBelow);
          sortParts();
          adapter.setParts(new ArrayList<>(partsResult));
          updateResult();
        }
        // Remove focus from edit text
        UiUtil.hideKeyboard(binding.editTextSongName);
        binding.editTextSongName.clearFocus();
      }

      @Override
      public void onMoreClick(@NonNull Part part) {
        performHapticClick();
        // Remove focus from edit text
        UiUtil.hideKeyboard(binding.editTextSongName);
        binding.editTextSongName.clearFocus();
      }

      @Override
      public void onRenameClick(@NonNull Part part) {
        performHapticClick();
        renameDialogUtil.setPart(part);
        renameDialogUtil.show();
      }

      @Override
      public void onDuplicateClick(@NonNull Part part) {
        performHapticClick();
        if (activity.isUnlocked() || partsResult.size() < 2) {
          Part partNew = new Part(part);
          partNew.setRandomId();
          partNew.setPartIndex(part.getPartIndex() + 1);
          // move all parts below down by one
          for (int i = partNew.getPartIndex(); i < partsResult.size(); i++) {
            Part partBelow = new Part(partsResult.get(i));
            partBelow.setPartIndex(partBelow.getPartIndex() + 1);
            partsResult.set(i, partBelow);
          }
          partsResult.add(partNew);
          sortParts();
          adapter.setParts(new ArrayList<>(partsResult));
          updateResult();
        } else {
          unlockDialogUtil.show();
        }
      }

      @Override
      public void onDeleteClick(@NonNull Part part) {
        performHapticClick();
        if (partsResult.size() <= 1) {
          return;
        }
        int index = part.getPartIndex();
        partsResult.remove(index);
        for (int i = 0; i < partsResult.size(); i++) {
          partsResult.get(i).setPartIndex(i);
        }
        adapter.setParts(new ArrayList<>(partsResult));
        updateResult();
      }
    });
    binding.recyclerSongParts.setAdapter(adapter);
    // Layout manager
    LinearLayoutManager layoutManager = new WrapperLinearLayoutManager(activity);
    binding.recyclerSongParts.setLayoutManager(layoutManager);
    binding.recyclerSongParts.setItemAnimator(new DefaultItemAnimator());

    PartItemDecoration decoration = new PartItemDecoration(UiUtil.dpToPx(activity, 0));
    binding.recyclerSongParts.addItemDecoration(decoration);

    String songId = SongFragmentArgs.fromBundle(getArguments()).getSongId();
    if (songId != null) {
      isNewSong = false;
      activity.getSongViewModel().fetchSongWithParts(songId, songWithParts -> {
        Runnable runnable = () -> {
          if (songWithParts != null) {
            songSource = songWithParts.getSong();
            // Copy song to result
            songResult = new Song(songSource);
            if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SONG_RESULT)) {
              Song songRestored = savedInstanceState.getParcelable(KEY_SONG_RESULT);
              if (songRestored != null) {
                songResult = songRestored;
              }
            }
            // Copy name to form
            binding.textInputSongName.setHintAnimationEnabled(false);
            binding.editTextSongName.clearFocus(); // to prevent TextWatcher from being triggered
            binding.editTextSongName.setText(songResult.getName());
            binding.editTextSongName.post(() -> {
              UiUtil.hideKeyboard(binding.editTextSongName);
              binding.editTextSongName.clearFocus();
            });
            binding.textInputSongName.setHintAnimationEnabled(true);
            // Copy looped to form
            binding.switchSongLooped.setChecked(songResult.isLooped());
            binding.switchSongLooped.jumpDrawablesToCurrentState();
            binding.switchSongLooped.setOnCheckedChangeListener(this);
            // Copy parts to form
            partsSource = songWithParts.getParts();
            partsResult = new LinkedList<>();
            for (Part part : partsSource) {
              partsResult.add(new Part(part));
            }
            if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PARTS_RESULT)) {
              List<Part> restored = savedInstanceState.getParcelableArrayList(KEY_PARTS_RESULT);
              if (restored != null) {
                partsResult = new ArrayList<>(restored);
              }
            }
            sortParts();
            binding.recyclerSongParts.stopScroll();
            adapter.setParts(new ArrayList<>(partsResult));
          } else {
            Log.e(TAG, "onViewCreated: song with id=" + songId + " not found");
          }
          updateResult();
        };
        activity.runOnUiThread(runnable);
      });
    } else {
      isNewSong = true;
      songSource = new Song();
      songResult = new Song(songSource);
      if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SONG_RESULT)) {
        Song songRestored = savedInstanceState.getParcelable(KEY_SONG_RESULT);
        if (songRestored != null) {
          songResult = new Song(songRestored);
        }
      }

      binding.editTextSongName.clearFocus(); // to prevent TextWatcher from being triggered
      binding.editTextSongName.setText(songResult.getName());
      binding.editTextSongName.post(() -> {
        boolean isLandTablet = UiUtil.isLandTablet(activity);
        if (savedInstanceState == null && (isPortrait || isLandTablet)) {
          binding.editTextSongName.requestFocus();
          UiUtil.showKeyboard(binding.editTextSongName);
        } else {
          // Only show keyboard if not restored from orientation change
          UiUtil.hideKeyboard(binding.editTextSongName);
        }
      });
      binding.switchSongLooped.setOnCheckedChangeListener(null);
      binding.switchSongLooped.setChecked(songResult.isLooped());
      binding.switchSongLooped.jumpDrawablesToCurrentState();
      binding.switchSongLooped.setOnCheckedChangeListener(this);

      partsResult = new LinkedList<>();

      Part firstPart = new Part(
          null,
          songResult.getId(),
          partsResult.size(),
          getMetronomeEngine() != null
              ? getMetronomeEngine().getConfig()
              : new MetronomeConfig()
      );
      partsResult.add(firstPart);
      sortParts();
      adapter.setParts(new ArrayList<>(partsResult));

      // Copy result to source after default part to prevent diff on result check
      partsSource = new ArrayList<>();
      for (Part part : partsResult) {
        partsSource.add(new Part(part));
      }
      if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PARTS_RESULT)) {
        List<Part> partsRestored = savedInstanceState.getParcelableArrayList(KEY_PARTS_RESULT);
        if (partsRestored != null) {
          partsResult = new ArrayList<>(partsRestored);
          sortParts();
          binding.recyclerSongParts.stopScroll();
          adapter.setParts(partsResult);
        }
      }
      updateResult();
      if (songResult.getName() == null || songResult.getName().isEmpty()) {
        clearError(); // first time the name is empty but user is not guilty
      }
    }

    MenuItem itemDelete = binding.toolbarSong.getMenu().findItem(R.id.action_delete);
    if (itemDelete != null) {
      // Only show delete if song not new
      itemDelete.setVisible(!isNewSong);
    }

    activity.getSongViewModel().getAllSongsLive().observe(
        getViewLifecycleOwner(), songs -> songsExisting = songs
    );

    binding.editTextSongName.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_DONE) {
            UiUtil.hideKeyboard(binding.editTextSongName);
            binding.editTextSongName.clearFocus();
            updateResult();
          }
          return false;
        });
    binding.editTextSongName.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        if (binding.editTextSongName.hasFocus()) {
          updateResult();
        }
      }
    });

    ViewUtil.setOnClickListeners(
        this,
        binding.fabSong,
        binding.linearSongLooped
    );

    dialogUtilDiscard = new DialogUtil(activity, "discard_changes");
    dialogUtilDiscard.createDialogError(builder -> {
      builder.setTitle(R.string.msg_discard_changes);
      builder.setMessage(R.string.msg_discard_changes_description);
      builder.setPositiveButton(R.string.action_discard, (dialog, which) -> {
        performHapticClick();
        activity.navigateUp();
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilDiscard.showIfWasShown(savedInstanceState);

    dialogUtilDelete = new DialogUtil(activity, "delete");
    dialogUtilDelete.createDialogError(builder -> {
      builder.setTitle(R.string.msg_delete_song);
      builder.setMessage(R.string.msg_delete_song_description);
      builder.setPositiveButton(R.string.action_delete, (dialog, which) -> {
        performHapticClick();
        if (songSource == null) {
          Log.e(TAG, "onViewCreated: songSource cannot be null");
          return;
        } else if (getMetronomeEngine() == null) {
          return;
        }
        if (songSource.getId().equals(getMetronomeEngine().getCurrentSongId())) {
          // if current song is deleted, change to default
          getMetronomeEngine().setCurrentSong(Constants.SONG_ID_DEFAULT, 0);
        }
        activity.getSongViewModel().deleteSong(songSource, () -> {
          if (getMetronomeEngine() == null) {
            return;
          }
          activity.getSongViewModel().deleteParts(partsSource);
          // update shortcut names
          getMetronomeEngine().updateShortcuts();
          // update widget
          WidgetUtil.sendSongsWidgetUpdate(activity);
        });
        activity.navigateUp();
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilDelete.showIfWasShown(savedInstanceState);

    unlockDialogUtil = new UnlockDialogUtil(activity);
    unlockDialogUtil.showIfWasShown(savedInstanceState);

    renameDialogUtil = new RenameDialogUtil(activity, this);
    renameDialogUtil.showIfWasShown(savedInstanceState);

    optionsUtil = new OptionsUtil(activity, new OnPartEditListener() {
      @Override
      public void onPartAdded(@NonNull Part part) {
        partsResult.add(part);
        sortParts();
        adapter.setParts(new ArrayList<>(partsResult));
        updateResult();
      }

      @Override
      public void onPartUpdated(@NonNull Part part) {
        partsResult.set(part.getPartIndex(), part);
        adapter.setParts(new ArrayList<>(partsResult));
        updateResult();
      }
    });
    optionsUtil.showIfWasShown(savedInstanceState);

    onBackPressedCallback = new OnBackPressedCallback(false) {
      @Override
      public void handleOnBackPressed() {
        dialogUtilDiscard.show();
      }
    };
    activity.getOnBackPressedDispatcher().addCallback(activity, onBackPressedCallback);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (dialogUtilDiscard != null) {
      dialogUtilDiscard.saveState(outState);
    }
    if (dialogUtilDelete != null) {
      dialogUtilDelete.saveState(outState);
    }
    if (unlockDialogUtil != null) {
      unlockDialogUtil.saveState(outState);
    }
    if (renameDialogUtil != null) {
      renameDialogUtil.saveState(outState);
    }
    if (optionsUtil != null) {
      optionsUtil.saveState(outState);
    }
    outState.putParcelable(KEY_SONG_RESULT, songResult);
    outState.putParcelableArrayList(KEY_PARTS_RESULT, new ArrayList<>(partsResult));
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_song_looped) {
      binding.switchSongLooped.toggle();
    } else if (id == R.id.fab_song) {
      performHapticClick();
      // Remove focus from edit text
      UiUtil.hideKeyboard(binding.editTextSongName);
      binding.editTextSongName.clearFocus();
      if (activity.isUnlocked() || partsResult.size() < 2) {
        Part part = new Part(
            null,
            songResult.getId(),
            partsResult.size(),
            new MetronomeConfig()
        );
        optionsUtil.setPart(part, true);
        optionsUtil.show();
      } else {
        unlockDialogUtil.show();
      }
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.switch_song_looped) {
      performHapticClick();
      // Remove focus from edit text
      UiUtil.hideKeyboard(binding.editTextSongName);
      binding.editTextSongName.clearFocus();
      updateResult();
    }
  }

  public void renamePart(String partId, String name) {
    for (Part part : partsResult) {
      if (part.getId().equals(partId)) {
        Part partResult = new Part(part);
        partResult.setName(name);
        partsResult.set(part.getPartIndex(), partResult);
        adapter.setParts(new ArrayList<>(partsResult));
        updateResult();
        break;
      }
    }
  }

  private void sortParts() {
    SortUtil.sortPartsByIndex(partsSource);
    SortUtil.sortPartsByIndex(partsResult);
  }

  private void updateResult() {
    boolean isValid = true;
    Editable songName = binding.editTextSongName.getText();
    if (songName != null && !songName.toString().trim().isEmpty()) {
      boolean isUnique = true;
      for (Song song : songsExisting) {
        boolean isSameSong = songSource != null && songSource.getId().equals(song.getId());
        if (!isSameSong && songName.toString().trim().equals(song.getName())) {
          isUnique = false;
          break;
        }
      }
      if (isUnique) {
        songResult.setName(songName.toString().trim());
      } else {
        isValid = false;
        setErrorSongName(true);
      }
    } else {
      isValid = false;
      setErrorSongName(false);
      if (songName != null) {
        if (songName.toString().trim().isEmpty()) {
          // for proper diff calculation
          songResult.setName(null);
        }
      }
    }

    songResult.setLooped(binding.switchSongLooped.isChecked());

    if (isValid) {
      clearError();
    }

    boolean hasUnsavedChanges = !songResult.equals(songSource) || !partsResult.equals(partsSource);
    setHasUnsavedChanges(hasUnsavedChanges, isValid);
  }

  private void setHasUnsavedChanges(boolean hasUnsavedChanges, boolean isValid) {
    this.hasUnsavedChanges = hasUnsavedChanges;
    if (onBackPressedCallback != null) {
      onBackPressedCallback.setEnabled(hasUnsavedChanges);
    }
    binding.buttonSongSave.setEnabled(hasUnsavedChanges && isValid);
  }

  private void setErrorSongName(boolean notUnique) {
    binding.textInputSongName.setError(
        getString(notUnique ? R.string.label_song_name_used : R.string.msg_invalid_input)
    );
  }

  private void clearError() {
    binding.textInputSongName.setError(null);
    binding.textInputSongName.setErrorEnabled(false);
  }

  private void setupImeAnimation(SystemBarBehavior systemBarBehavior) {
    Callback callback = new Callback(Callback.DISPATCH_MODE_STOP) {
      int imeInsetStart, imeInsetEnd;
      float yStart, yEnd;

      @Override
      public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
        imeInsetStart = systemBarBehavior.getImeInset();
        yStart = binding.fabSong.getY();
      }

      @NonNull
      @Override
      public BoundsCompat onStart(
          @NonNull WindowInsetsAnimationCompat animation,
          @NonNull BoundsCompat bounds
      ) {
        imeInsetEnd = systemBarBehavior.getImeInset();
        systemBarBehavior.setImeInset(imeInsetStart);
        systemBarBehavior.refresh(false);

        yEnd = binding.fabSong.getY();
        binding.fabSong.setY(yStart);
        return bounds;
      }

      @NonNull
      @Override
      public WindowInsetsCompat onProgress(
          @NonNull WindowInsetsCompat insets,
          @NonNull List<WindowInsetsAnimationCompat> animations
      ) {
        if (animations.isEmpty() || animations.get(0) == null) {
          return insets;
        }
        WindowInsetsAnimationCompat animation = animations.get(0);
        systemBarBehavior.setImeInset(
            (int) MathUtils.lerp(imeInsetStart, imeInsetEnd, animation.getInterpolatedFraction())
        );
        systemBarBehavior.refresh(false);
        binding.fabSong.setY(MathUtils.lerp(yStart, yEnd, animation.getInterpolatedFraction()));
        return insets;
      }
    };
    ViewCompat.setOnApplyWindowInsetsListener(
        binding.constraintSongContainer, (v, insets) -> {
          int bottomInsetIme = insets.getInsets(Type.ime()).bottom;
          systemBarBehavior.setImeInset(bottomInsetIme);
          systemBarBehavior.refresh(false);
          if (insets.isVisible(Type.ime())) {
            int bottomInsetNav = insets.getInsets(Type.systemBars()).bottom;
            binding.fabSong.setTranslationY(-bottomInsetIme + bottomInsetNav);
          } else {
            binding.fabSong.setY(fabBaseY);
          }
          return insets;
        });
    ViewCompat.setWindowInsetsAnimationCallback(binding.constraintSongContainer, callback);
  }
}