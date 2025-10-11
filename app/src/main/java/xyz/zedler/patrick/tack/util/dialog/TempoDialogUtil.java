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

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.Editable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener;
import java.util.LinkedList;
import java.util.Queue;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogTempoBinding;
import xyz.zedler.patrick.tack.fragment.MainFragment;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class TempoDialogUtil implements OnButtonCheckedListener, OnCheckedChangeListener {

  private static final String TAG = TempoDialogUtil.class.getSimpleName();

  private static final int MAX_TAPS = 20;
  private static final double TEMPO_FACTOR = 0.5;
  private static final int INTERVAL_FACTOR = 3;

  private final MainActivity activity;
  private final MainFragment fragment;
  private final PartialDialogTempoBinding binding;
  private final DialogUtil dialogUtil;
  private final TempoDialogListener listener;
  private final Queue<Long> intervals = new LinkedList<>();
  private final OnTouchListener onTouchListener;
  private long previous;
  private int tempoOld;
  private boolean inputMethodKeyboard, instantApply;

  @SuppressLint("ClickableViewAccessibility")
  public TempoDialogUtil(
      MainActivity activity, MainFragment fragment, TempoDialogListener listener
  ) {
    this.activity = activity;
    this.fragment = fragment;
    this.listener = listener;

    binding = PartialDialogTempoBinding.inflate(activity.getLayoutInflater());

    binding.editTextTempo.setOnEditorActionListener((v, actionId, event) -> {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        if (isInputValid()) {
          activity.performHapticClick();
          setTempoFromInputAndDismiss();
        } else {
          activity.performHapticReject(binding.getRoot());
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

    binding.linearTempoInstant.setOnClickListener(
        v -> binding.switchTempoInstant.toggle()
    );

    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      Typeface variableTypeface = ResourcesCompat.getFont(activity, R.font.nunito_variable_wght);
      binding.textTempoTapTempo.setTypeface(variableTypeface);
      binding.textTempoTapTempo.setFontVariationSettings("'wght' " + 750);
    }
    binding.textSwitcherTempoTapTempoTerm.setFactory(() -> {
      TextView textView = new TextView(activity);
      textView.setGravity(Gravity.CENTER_HORIZONTAL);
      textView.setTextSize(
          TypedValue.COMPLEX_UNIT_PX,
          activity.getResources().getDimension(R.dimen.tempo_tap_label_text_size)
      );
      Typeface typeface = ResourcesCompat.getFont(activity, R.font.nunito_bold);
      textView.setTypeface(typeface);
      textView.setTextColor(ResUtil.getColor(activity, R.attr.colorOnTertiaryContainer));
      return textView;
    });
    onTouchListener = (v, event) -> {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        binding.tempoTapTempo.setTouched(true);
        boolean enoughData = tap();
        if (enoughData) {
          if (binding.frameTempoTapTempo.getVisibility() == View.GONE) {
            binding.frameTempoTapTempo.setAlpha(0);
            binding.frameTempoTapTempo.setVisibility(View.VISIBLE);
            binding.frameTempoTapTempo.animate()
                .alpha(1)
                .setDuration(150)
                .start();
            binding.textTempoPlaceholder.animate()
                .alpha(0)
                .setDuration(150)
                .withEndAction(() -> binding.textTempoPlaceholder.setVisibility(View.GONE))
                .start();
          }
          int tempoNew = getTapTempo();
          setTapTempoDisplay(tempoOld, tempoNew);
          tempoOld = tempoNew;
          if (instantApply && getMetronomeEngine() != null) {
            getMetronomeEngine().setTempo(tempoNew);
            getMetronomeEngine().maybeUpdateDefaultSong();
          }
        }
        activity.performHapticHeavyClick();
        return true;
      } else if (event.getAction() == MotionEvent.ACTION_UP
          || event.getAction() == MotionEvent.ACTION_CANCEL) {
        binding.tempoTapTempo.setTouched(false);
        v.performClick();
        return true;
      }
      return false;
    };

    dialogUtil = new DialogUtil(activity, "tempo");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.action_change_tempo);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(R.string.action_apply, null);
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> activity.performHapticClick()
      );
    });
    dialogUtil.setOnShowListener(dialog -> {
      overrideDialogActions();
      if (inputMethodKeyboard) {
        showKeyboard();
      }
    });

    setDividerVisibility(!UiUtil.isOrientationPortrait(activity));
  }

  @Override
  public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
    if (!isChecked || getMetronomeEngine() == null) {
      return;
    }
    activity.performHapticClick();
    int groupId = group.getId();
    if (groupId == R.id.toggle_tempo_method) {
      boolean inputMethodKeyboard = checkedId == R.id.button_tempo_keyboard;
      getMetronomeEngine().setTempoInputKeyboard(inputMethodKeyboard);
      if (!inputMethodKeyboard) {
        View currentFocus = binding.linearTempoContainer.getFocusedChild();
        if (currentFocus != null) {
          UiUtil.hideKeyboard(currentFocus);
        }
        intervals.clear();
        previous = 0;
      }
      update();
      if (inputMethodKeyboard) {
        showKeyboard();
      }
      overrideDialogActions();
    }
  }

  @Override
  public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
    MetronomeEngine metronomeEngine = getMetronomeEngine();
    int id = buttonView.getId();
    if (id == R.id.switch_tempo_instant && metronomeEngine != null) {
      activity.performHapticClick();
      instantApply = isChecked;
      metronomeEngine.setTempoTapInstant(isChecked);
      if (isChecked) {
        long tapAverage = getTapAverage();
        if (tapAverage > 0) {
          int tempo = getTapTempo(tapAverage);
          if (listener != null) {
            listener.onTempoChanged(tempo);
          }
          metronomeEngine.setTempo(tempo);
          metronomeEngine.maybeUpdateDefaultSong();
        }
      }
      overrideDialogActions();
    }
  }

  public void show() {
    update();
    dialogUtil.show();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    update();
    dialogUtil.showIfWasShown(state);
  }

  private void overrideDialogActions() {
    Button buttonPositive = null;
    Button buttonNegative = null;
    if (dialogUtil.getDialog() != null) {
      buttonPositive = dialogUtil.getDialog().getButton(DialogInterface.BUTTON_POSITIVE);
      buttonNegative = dialogUtil.getDialog().getButton(DialogInterface.BUTTON_NEGATIVE);
    }
    boolean showApplyButton = inputMethodKeyboard || !instantApply;
    if (buttonPositive != null) {
      buttonPositive.setText(
          activity.getString(showApplyButton ? R.string.action_apply : R.string.action_close)
      );
      buttonPositive.setOnClickListener(v -> {
        if (inputMethodKeyboard) {
          if (isInputValid()) {
            activity.performHapticClick();
            setTempoFromInputAndDismiss();
          } else {
            activity.performHapticReject(binding.getRoot());
          }
        } else {
          activity.performHapticClick();
          setTempoFromInputAndDismiss();
        }
      });
    }
    if (buttonNegative != null) {
      buttonNegative.setVisibility(showApplyButton ? View.VISIBLE : View.GONE);
    }
  }

  public void dismiss() {
    dialogUtil.dismiss();
    intervals.clear();
    previous = 0;
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
    measureScrollView();

    MetronomeEngine metronomeEngine = getMetronomeEngine();
    if (metronomeEngine == null) {
      return;
    }
    inputMethodKeyboard = metronomeEngine.getTempoInputKeyboard();
    instantApply = metronomeEngine.getTempoTapInstant();

    binding.toggleTempoMethod.removeOnButtonCheckedListener(this);
    if (inputMethodKeyboard) {
      binding.toggleTempoMethod.check(R.id.button_tempo_keyboard);
    } else {
      binding.toggleTempoMethod.check(R.id.button_tempo_tap);
    }
    binding.toggleTempoMethod.addOnButtonCheckedListener(this);

    if (inputMethodKeyboard) {
      setError(false);
      binding.editTextTempo.setText(null);
      binding.editTextTempo.requestFocus();
    } else {
      binding.switchTempoInstant.setOnCheckedChangeListener(null);
      binding.switchTempoInstant.setChecked(instantApply);
      binding.switchTempoInstant.jumpDrawablesToCurrentState();
      binding.switchTempoInstant.setOnCheckedChangeListener(this);

      tempoOld = metronomeEngine.getConfig().getTempo();
      setTapTempoDisplay(tempoOld, tempoOld);
      binding.textSwitcherTempoTapTempoTerm.setCurrentText(fragment.getTempoTerm(tempoOld));
      binding.tempoTapTempo.setReduceAnimations(fragment.isReduceAnimations());

      binding.frameTempoTapTempo.setVisibility(View.GONE);
      binding.frameTempoTapTempo.setAlpha(0);
      binding.textTempoPlaceholder.setVisibility(View.VISIBLE);
      binding.textTempoPlaceholder.setAlpha(1);
    }
    binding.frameTempoInputContainer.setVisibility(inputMethodKeyboard ? View.VISIBLE : View.GONE);
    binding.frameTempoTapContainer.setVisibility(inputMethodKeyboard ? View.GONE : View.VISIBLE);
    binding.linearTempoInstant.setVisibility(inputMethodKeyboard ? View.GONE : View.VISIBLE);
  }

  private void showKeyboard() {
    if (binding != null) {
      binding.editTextTempo.requestFocus();
      UiUtil.showKeyboard(binding.editTextTempo);
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
    MetronomeEngine metronomeEngine = getMetronomeEngine();
    if (binding == null || metronomeEngine == null) {
      return;
    }
    if (inputMethodKeyboard) {
      if (!isInputValid()) {
        return;
      }
      Editable tempoEditable = binding.editTextTempo.getText();
      if (tempoEditable == null) {
        return;
      }
      int tempo = Integer.parseInt(tempoEditable.toString());
      if (listener != null) {
        listener.onTempoChanged(tempo);
      }
      metronomeEngine.setTempo(tempo);
      metronomeEngine.maybeUpdateDefaultSong();

      binding.editTextTempo.clearFocus();
    } else {
      long tapAverage = getTapAverage();
      if (tapAverage > 0) {
        int tempo = getTapTempo(tapAverage);
        if (listener != null) {
          listener.onTempoChanged(tempo);
        }
        metronomeEngine.setTempo(tempo);
        metronomeEngine.maybeUpdateDefaultSong();
      }
    }
    dismiss();
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

  public boolean tap() {
    boolean enoughData = false;
    long current = System.currentTimeMillis();
    if (previous > 0) {
      enoughData = true;
      long interval = current - previous;
      if (!intervals.isEmpty() && shouldTapReset(interval)) {
        intervals.clear();
        enoughData = false;
      } else if (intervals.size() >= MAX_TAPS) {
        intervals.poll();
      }
      intervals.offer(interval);
    }
    previous = current;
    return enoughData;
  }

  private void setTapTempoDisplay(int tempoOld, int tempoNew) {
    if (binding == null || fragment == null || !fragment.isAdded()) {
      return;
    }
    if (instantApply && listener != null) {
      listener.onTempoChanged(tempoNew);
    }
    binding.textTempoTapTempo.setText(String.valueOf(tempoNew));
    String termNew = fragment.getTempoTerm(tempoNew);
    if (!termNew.equals(fragment.getTempoTerm(tempoOld))) {
      boolean isFaster = tempoNew > tempoOld;
      binding.textSwitcherTempoTapTempoTerm.setInAnimation(
          activity, isFaster ? R.anim.tempo_term_open_enter : R.anim.tempo_term_close_enter
      );
      binding.textSwitcherTempoTapTempoTerm.setOutAnimation(
          activity, isFaster ? R.anim.tempo_term_open_exit : R.anim.tempo_term_close_exit
      );
      binding.textSwitcherTempoTapTempoTerm.setText(termNew);
    }
  }

  public int getTapTempo() {
    return getTapTempo(getTapAverage());
  }

  private int getTapTempo(long interval) {
    if (interval > 0) {
      return (int) (60000 / interval);
    } else {
      return 0;
    }
  }

  private long getTapAverage() {
    long sum = 0;
    for (long interval : intervals) {
      sum += interval;
    }
    if (!intervals.isEmpty()) {
      return sum / intervals.size();
    } else {
      return 0;
    }
  }

  private boolean shouldTapReset(long interval) {
    return getTapTempo(interval) >= getTapTempo() * (1 + TEMPO_FACTOR)
        || getTapTempo(interval) <= getTapTempo() * (1 - TEMPO_FACTOR)
        || interval > getTapAverage() * INTERVAL_FACTOR;
  }

  private void measureScrollView() {
    binding.scrollTempo.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @SuppressLint("ClickableViewAccessibility")
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.scrollTempo.canScrollVertically(-1)
                || binding.scrollTempo.canScrollVertically(1);
            if (isScrollable) {
              // Only tempo tap view should handle touch events to avoid conflicts with scrolling
              binding.tempoTapTempo.setOnTouchListener(onTouchListener);
              binding.frameTempoTapContainer.setOnTouchListener(null);
            } else {
              binding.tempoTapTempo.setOnTouchListener(null);
              binding.frameTempoTapContainer.setOnTouchListener(onTouchListener);
            }
            setDividerVisibility(isScrollable);
            binding.scrollTempo.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  private void setDividerVisibility(boolean visible) {
    binding.dividerTempoTop.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.dividerTempoBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.linearTempoContainer.setPadding(
        binding.linearTempoContainer.getPaddingLeft(),
        visible ? UiUtil.dpToPx(activity, 16) : 0,
        binding.linearTempoContainer.getPaddingRight(),
        visible ? UiUtil.dpToPx(activity, 16) : 0
    );
  }

  @Nullable
  private MetronomeEngine getMetronomeEngine() {
    return activity.getMetronomeEngine();
  }

  public interface TempoDialogListener {
    void onTempoChanged(int tempo);
  }
}
