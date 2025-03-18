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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import java.util.LinkedList;
import java.util.List;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.FragmentSongBinding;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class SongFragment extends BaseFragment implements OnClickListener {

  private static final String TAG = SongFragment.class.getSimpleName();

  private FragmentSongBinding binding;
  private MainActivity activity;
  private DialogUtil dialogUtilUnsaved;
  private OnBackPressedCallback onBackPressedCallback;
  private Song songSource;
  private Song songResult = new Song();
  private List<Song> songsExisting = new LinkedList<>();
  private List<Part> partsResult = new LinkedList<>();
  private boolean hasUnsavedChanges = false;

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
    activity.getSongViewModel().clearSongWithParts();
    activity.getOnBackPressedDispatcher().addCallback(activity, onBackPressedCallback);
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarSong);
    systemBarBehavior.setContainer(binding.constraintSongContainer);
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

    String songId = SongFragmentArgs.fromBundle(getArguments()).getSongId();
    if (songId != null) {
      Observer<SongWithParts> observer = songWithParts -> {
        if (songWithParts != null) {
          songSource = songWithParts.getSong();
          // Copy song to result
          songResult = songSource.copy();
          // Copy name to form
          binding.textInputSongName.setHintAnimationEnabled(false);
          binding.editTextSongName.setText(songWithParts.getSong().getName());
          binding.editTextSongName.post(() -> {
            UiUtil.hideKeyboard(activity);
            binding.editTextSongName.clearFocus();
          });
          binding.textInputSongName.setHintAnimationEnabled(true);
          // Copy looped to form
          binding.checkboxSongLooped.setChecked(songWithParts.getSong().isLooped());
          binding.checkboxSongLooped.jumpDrawablesToCurrentState();
          // Copy parts to form
          binding.textSongParts.setText(
              getString(R.string.label_song_parts, songWithParts.getParts().size())
          );
        }
      };
      activity.getSongViewModel().getSongWithParts().observe(getViewLifecycleOwner(), observer);
      activity.getSongViewModel().fetchSongWithParts(songId);
    } else {
      binding.editTextSongName.setText(null);
      binding.editTextSongName.post(() -> {
        binding.editTextSongName.requestFocus();
        UiUtil.showKeyboard(activity, binding.editTextSongName);
      });
      binding.checkboxSongLooped.setChecked(false);
      binding.checkboxSongLooped.jumpDrawablesToCurrentState();
      binding.textSongParts.setText(getString(R.string.label_song_parts, 0));
    }

    activity.getSongViewModel().getAllSongsLive().observe(
        getViewLifecycleOwner(),
        songs -> songsExisting = songs
    );

    binding.editTextSongName.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_DONE) {
            updateResult();
          }
          return false;
        });
    binding.editTextSongName.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void afterTextChanged(Editable s) {
        updateResult();
      }
    });

    binding.linearSongLooped.setOnClickListener(this);
    binding.checkboxSongLooped.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          performHapticClick();
          updateResult();
        });

    binding.buttonSongSave.setOnClickListener(this);
    // Disable save first
    binding.buttonSongSave.setEnabled(false);

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
        if (songSource == null) {
          activity.getSongViewModel().insertSong(songResult);
        } else {
          activity.getSongViewModel().updateSong(
              songResult, () -> getMetronomeUtil().reloadCurrentSong()
          );
        }
        navigateUp();
      }
    } else if (id == R.id.linear_song_looped) {
      binding.checkboxSongLooped.toggle();
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
    }

    songResult.setLooped(binding.checkboxSongLooped.isChecked());

    if (isValid) {
      clearError();
    }

    boolean hasUnsavedChanges = !songResult.equals(songSource);
    setHasUnsavedChanges(hasUnsavedChanges, isValid);
  }

  private void setHasUnsavedChanges(boolean hasUnsavedChanges, boolean isValid) {
    this.hasUnsavedChanges = hasUnsavedChanges;
    onBackPressedCallback.setEnabled(hasUnsavedChanges);
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