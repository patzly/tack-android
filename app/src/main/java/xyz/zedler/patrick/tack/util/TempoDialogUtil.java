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

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogTempoBinding;
import xyz.zedler.patrick.tack.fragment.MainFragment;

public class TempoDialogUtil {

  private static final String TAG = TempoDialogUtil.class.getSimpleName();

  private final MainActivity activity;
  private final MainFragment fragment;
  private final PartialDialogTempoBinding binding;
  private final DialogUtil dialogUtil;

  public TempoDialogUtil(MainActivity activity, MainFragment fragment) {
    this.activity = activity;
    this.fragment = fragment;

    binding = PartialDialogTempoBinding.inflate(activity.getLayoutInflater());
    binding.editTextTempo.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        if (isInputValid()) {
          activity.performHapticClick();
          setTempoFromInputAndDismiss();
        } else {
          activity.performHapticDoubleClick();
          return true;
        }
      }
      return false;
    });
    binding.textInputTempo.setHelperText(
        activity.getString(
            R.string.label_tempo_input_help, Constants.TEMPO_MIN, Constants.TEMPO_MAX
        )
    );

    dialogUtil = new DialogUtil(activity, "tempo");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.action_change_tempo);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(R.string.action_apply, null);
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> activity.performHapticClick()
      );
    });
  }

  public void show() {
    update();
    dialogUtil.show();
    overridePositiveAction();
    showKeyboard();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    update();
    boolean showing = dialogUtil.showIfWasShown(state);
    if (showing) {
      overridePositiveAction();
      showKeyboard();
    }
  }

  private void overridePositiveAction() {
    Button button = null;
    if (dialogUtil.getDialog() != null) {
      button = dialogUtil.getDialog().getButton(DialogInterface.BUTTON_POSITIVE);
    }
    if (button != null) {
      button.setOnClickListener(v -> {
        if (isInputValid()) {
          activity.performHapticClick();
          setTempoFromInputAndDismiss();
        } else {
          activity.performHapticDoubleClick();
        }
      });
    }
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
    setError(false);
    binding.editTextTempo.setText(null);
    binding.editTextTempo.requestFocus();
  }

  private void showKeyboard() {
    if (binding != null) {
      binding.editTextTempo.requestFocus();
      UiUtil.showKeyboard(activity, binding.editTextTempo);
    }
  }

  private boolean isInputValid() {
    if (binding == null) {
      return false;
    }
    Editable tempoEditable = binding.editTextTempo.getText();
    if (tempoEditable == null) {
      return false;
    }
    String tempoString = tempoEditable.toString();
    if (tempoString.isEmpty()) {
      setError(true);
      return false;
    }
    try {
      int tempo = Integer.parseInt(tempoString);
      boolean valid = tempo >= Constants.TEMPO_MIN && tempo <= Constants.TEMPO_MAX;
      setError(!valid);
      return valid;
    } catch (NumberFormatException e) {
      setError(true);
      return false;
    }
  }

  private void setTempoFromInputAndDismiss() {
    if (binding == null || !isInputValid()) {
      return;
    }
    Editable tempoEditable = binding.editTextTempo.getText();
    if (tempoEditable == null) {
      return;
    }
    int tempo = Integer.parseInt(tempoEditable.toString());
    fragment.updateTempoDisplay(getMetronomeUtil().getTempo(), tempo);
    getMetronomeUtil().setTempo(tempo);

    binding.editTextTempo.clearFocus();
    dialogUtil.dismiss();
  }

  private void setError(boolean error) {
    if (binding == null) {
      return;
    }
    if (error) {
      binding.textInputTempo.setError(activity.getString(R.string.msg_invalid_input));
    } else {
      binding.textInputTempo.setErrorEnabled(false);
    }
  }

  private MetronomeUtil getMetronomeUtil() {
    return activity.getMetronomeUtil();
  }
}
