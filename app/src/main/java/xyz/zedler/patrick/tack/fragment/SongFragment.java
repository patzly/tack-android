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

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.FragmentSongBinding;
import xyz.zedler.patrick.tack.fragment.SongsFragmentDirections.ActionSongsToSong;
import xyz.zedler.patrick.tack.recyclerview.adapter.PartAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.PartAdapter.OnPartMenuItemClickListener;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongAdapter;
import xyz.zedler.patrick.tack.recyclerview.decoration.PartItemDecoration;
import xyz.zedler.patrick.tack.recyclerview.decoration.SongChipItemDecoration;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.viewmodel.SongViewModel;
import xyz.zedler.patrick.tack.viewmodel.SongViewModel.OnSongWithPartsFetchedListener;

public class SongFragment extends BaseFragment implements OnClickListener, OnCheckedChangeListener {

  private static final String TAG = SongFragment.class.getSimpleName();

  private FragmentSongBinding binding;
  private MainActivity activity;
  private DialogUtil dialogUtilUnsaved;
  private OnBackPressedCallback onBackPressedCallback;
  private PartAdapter adapter;
  private Song songSource;
  private Song songResult = new Song();
  private List<Song> songsExisting = new LinkedList<>();
  private List<Part> partsSource = new ArrayList<>();
  private List<Part> partsResult = new LinkedList<>();
  private boolean isNewSong;
  private boolean hasUnsavedChanges = true;

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
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarSong);
    systemBarBehavior.setContainer(binding.constraintSongContainer);
    systemBarBehavior.setRecycler(binding.recyclerSongParts);
    systemBarBehavior.setAdditionalBottomInset(UiUtil.dpToPx(activity, 96));
    systemBarBehavior.setUp();
    SystemBarBehavior.applyBottomInset(binding.fabSong);

    /*new ScrollBehavior().setUpScroll(
        binding.appBarSongs, null, true
    );*/

    binding.toolbarSong.setNavigationOnClickListener(v -> {
      if (getViewUtil().isClickEnabled(v.getId())) {
        performHapticClick();
        if (hasUnsavedChanges) {
          dialogUtilUnsaved.show();
        } else {
          navigateUp();
        }
      }
    });
    binding.toolbarSong.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (getViewUtil().isClickDisabled(id)) {
        return false;
      }
      performHapticClick();
      if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_help) {
        activity.showTextBottomSheet(R.raw.help, R.string.title_help);
      }
      return true;
    });

    adapter = new PartAdapter((part, item) -> {
      performHapticClick();
      int itemId = item.getItemId();
      if (itemId == R.id.action_update) {
        Part partResult = new Part(part);
        partResult.setConfig(getMetronomeUtil().getConfig());
        partsResult.set(part.getPartIndex(), partResult);
        adapter.submitList(new ArrayList<>(partsResult));
        updateResult();
      } else if (itemId == R.id.action_move_up) {
        Part partCurrent = new Part(part);
        int index = partCurrent.getPartIndex();
        if (index > 0) {
          Part partAbove = new Part(partsResult.get(index - 1));
          partCurrent.setPartIndex(index - 1);
          partAbove.setPartIndex(index);
          partsResult.set(index - 1, partCurrent);
          partsResult.set(index, partAbove);
          sortParts();
          adapter.submitList(new ArrayList<>(partsResult));
          adapter.notifyMenusChanged();
          updateResult();
        }
      } else if (itemId == R.id.action_move_down) {
        Part partCurrent = new Part(part);
        int index = partCurrent.getPartIndex();
        if (index < partsResult.size() - 1) {
          Part partBelow = new Part(partsResult.get(index + 1));
          partCurrent.setPartIndex(index + 1);
          partBelow.setPartIndex(index);
          partsResult.set(index + 1, partCurrent);
          partsResult.set(index, partBelow);
          sortParts();
          adapter.submitList(new ArrayList<>(partsResult));
          adapter.notifyMenusChanged();
          updateResult();
        }
      } else if (itemId == R.id.action_delete) {
        if (partsResult.size() <= 1) {
          return;
        }
        partsResult.remove(part.getPartIndex());
        for (int i = 0; i < partsResult.size(); i++) {
          partsResult.get(i).setPartIndex(i);
        }
        adapter.submitList(new ArrayList<>(partsResult));
        adapter.notifyMenusChanged();
        updateResult();
      }
    });
    binding.recyclerSongParts.setAdapter(adapter);
    // Layout manager
    LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
    binding.recyclerSongParts.setLayoutManager(layoutManager);
    binding.recyclerSongParts.setItemAnimator(new DefaultItemAnimator());

    PartItemDecoration decoration = new PartItemDecoration(
        UiUtil.dpToPx(activity, 16), UiUtil.dpToPx(activity, 8)
    );
    binding.recyclerSongParts.addItemDecoration(decoration);

    String songId = SongFragmentArgs.fromBundle(getArguments()).getSongId();
    if (songId != null) {
      isNewSong = false;
      activity.getSongViewModel().fetchSongWithParts(songId, songWithParts -> {
        if (songWithParts != null) {
          songSource = songWithParts.getSong();
          // Copy song to result
          songResult = new Song(songSource);
          // Copy name to form
          binding.textInputSongName.setHintAnimationEnabled(false);
          binding.editTextSongName.setText(songWithParts.getSong().getName());
          binding.editTextSongName.post(() -> {
            UiUtil.hideKeyboard(activity);
            binding.editTextSongName.clearFocus();
          });
          binding.textInputSongName.setHintAnimationEnabled(true);
          // Copy looped to form
          binding.checkboxSongLooped.setOnCheckedChangeListener(null);
          binding.checkboxSongLooped.setChecked(songWithParts.getSong().isLooped());
          binding.checkboxSongLooped.jumpDrawablesToCurrentState();
          binding.checkboxSongLooped.setOnCheckedChangeListener(this);
          // Copy parts to form
          partsSource = songWithParts.getParts();
          partsResult = new LinkedList<>();
          for (Part part : partsSource) {
            partsResult.add(new Part(part));
          }
          sortParts();
          try {
            adapter.submitList(new ArrayList<>(partsResult));
            adapter.notifyMenusChanged();
          } catch (IllegalStateException e) {
            // "Cannot call this method while RecyclerView is computing a layout or scrolling"
            Log.e(TAG, "onViewCreated: ", e);
          }
        } else {
          Log.e(TAG, "onViewCreated: song with id=" + songId + " not found");
        }
        updateResult();
      });
    } else {
      isNewSong = true;
      songSource = new Song();
      songResult = new Song(songSource);

      binding.editTextSongName.setText(songResult.getName());
      binding.editTextSongName.post(() -> {
        binding.editTextSongName.requestFocus();
        UiUtil.showKeyboard(activity, binding.editTextSongName);
      });
      binding.checkboxSongLooped.setChecked(songResult.isLooped());
      binding.checkboxSongLooped.jumpDrawablesToCurrentState();

      partsResult = new LinkedList<>();
      addPart();
      // Copy result to source after default part to prevent diff on result check
      partsSource = new ArrayList<>();
      for (Part part : partsResult) {
        partsSource.add(new Part(part));
      }
      updateResult();
      clearError(); // first time the name is empty but user is not guilty
    }

    activity.getSongViewModel().getAllSongsLive().observe(
        getViewLifecycleOwner(), songs -> songsExisting = songs
    );

    binding.editTextSongName.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_DONE) {
            UiUtil.hideKeyboard(activity);
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
        updateResult();
      }
    });

    // Disable save first
    binding.buttonSongSave.setEnabled(false);

    ViewUtil.setOnClickListeners(
        this,
        binding.fabSong,
        binding.buttonSongSave,
        binding.linearSongLooped
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.checkboxSongLooped
    );

    dialogUtilUnsaved = new DialogUtil(activity, "discard_changes");
    dialogUtilUnsaved.createCaution(
        R.string.msg_discard_changes,
        R.string.msg_discard_changes_description,
        R.string.action_discard,
        () -> activity.navigateUp()
    );
    dialogUtilUnsaved.showIfWasShown(savedInstanceState);

    onBackPressedCallback = new OnBackPressedCallback(false) {
      @Override
      public void handleOnBackPressed() {
        dialogUtilUnsaved.show();
      }
    };
    activity.getOnBackPressedDispatcher().addCallback(activity, onBackPressedCallback);
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.button_song_save) {
      performHapticClick();
      if (hasUnsavedChanges) {
        if (isNewSong) {
          activity.getSongViewModel().insertSong(songResult);
          activity.getSongViewModel().insertParts(partsResult);
        } else {
          activity.getSongViewModel().updateSong(songResult, () -> {
            // To update looped in metronome
            getMetronomeUtil().reloadCurrentSong();
            // To update shortcut names
            getMetronomeUtil().updateShortcuts();
          });
          for (Part part : partsResult) {
            boolean isNew = true;
            for (Part partSource : partsSource) {
              if (part.getId().equals(partSource.getId())) {
                isNew = false;
                break;
              }
            }
            if (isNew) {
              activity.getSongViewModel().insertPart(part);
            } else {
              activity.getSongViewModel().updatePart(part);
            }
          }
          for (Part part : partsSource) {
            boolean isDeleted = true;
            for (Part partResult : partsResult) {
              if (part.getId().equals(partResult.getId())) {
                isDeleted = false;
                break;
              }
            }
            if (isDeleted) {
              activity.getSongViewModel().deletePart(part);
            }
          }
        }
        navigateUp();
      }
    } else if (id == R.id.linear_song_looped) {
      binding.checkboxSongLooped.toggle();
    } else if (id == R.id.fab_song) {
      performHapticClick();
      addPart();
      updateResult();
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.checkbox_song_looped) {
      performHapticClick();
      UiUtil.hideKeyboard(activity);
      binding.editTextSongName.clearFocus();
      updateResult();
    }
  }

  private void addPart() {
    Part part = new Part(
        null, songResult.getId(), partsResult.size(), getMetronomeUtil().getConfig()
    );
    partsResult.add(part);
    sortParts();
    adapter.submitList(new ArrayList<>(partsResult));
    adapter.notifyMenusChanged();
  }

  private void sortParts() {
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
      Collections.sort(partsSource, Comparator.comparingInt(Part::getPartIndex));
      Collections.sort(partsResult, Comparator.comparingInt(Part::getPartIndex));
    } else {
      Collections.sort(
          partsSource, (p1, p2) -> Integer.compare(p1.getPartIndex(), p2.getPartIndex())
      );
      Collections.sort(
          partsResult, (p1, p2) -> Integer.compare(p1.getPartIndex(), p2.getPartIndex())
      );
    }
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

    songResult.setLooped(binding.checkboxSongLooped.isChecked());
    binding.textSongParts.setText(getString(R.string.label_song_parts, partsResult.size()));

    if (isValid) {
      clearError();
    }

    boolean hasUnsavedChanges = !songResult.equals(songSource) || !partsResult.equals(partsSource);
    //Log.i(TAG, "updateResult: hello " + hasUnsavedChanges + "\n" + partsResult + "\n" + partsSource);
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
}