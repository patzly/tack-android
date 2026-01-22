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

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.transition.AutoTransition;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import java.util.Arrays;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.databinding.FragmentMainBinding;
import xyz.zedler.patrick.tack.databinding.PartialDialogOptionsBinding;
import xyz.zedler.patrick.tack.databinding.PartialOptionsBinding;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.model.MetronomeConfig;
import xyz.zedler.patrick.tack.view.BeatView;

public class OptionsUtil implements OnClickListener, OnButtonCheckedListener,
    OnChangeListener, OnSliderTouchListener {

  private static final String TAG = OptionsUtil.class.getSimpleName();

  private static final String PART = "part_dialog";
  private static final String IS_NEW = "new_part_dialog";

  private final MainActivity activity;
  private final boolean useDialog, editPart;
  private OnOptionsListener listener;
  private OnPartEditListener onPartEditListener;
  private boolean isCountInActive, isIncrementalActive, isTimerActive, isMuteActive, usePolyrhythm;
  private boolean isNew, isInitialized;
  private DialogUtil dialogUtil;
  private PartialOptionsBinding binding;
  private PartialDialogOptionsBinding bindingDialog;
  private Part part;
  private MetronomeConfig config;
  private final int ticksMaxPerRange;

  public OptionsUtil(
      MainActivity activity, FragmentMainBinding fragmentBinding, OnOptionsListener listener
  ) {
    this.activity = activity;
    this.listener = listener;

    editPart = false;
    useDialog = !UiUtil.isLandTablet(activity);
    if (useDialog) {
      bindingDialog = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
      dialogUtil = new DialogUtil(activity, "options");
    }
    binding = useDialog ? bindingDialog.partialOptions : fragmentBinding.partialOptions;

    if (binding != null) {
      binding.sliderOptionsCountIn.addOnSliderTouchListener(this);
      binding.sliderOptionsIncrementalAmount.addOnSliderTouchListener(this);
      binding.sliderOptionsIncrementalInterval.addOnSliderTouchListener(this);
      binding.sliderOptionsIncrementalLimit.addOnSliderTouchListener(this);
      binding.sliderOptionsTimerDuration.addOnSliderTouchListener(this);
      binding.sliderOptionsMutePlay.addOnSliderTouchListener(this);
      binding.sliderOptionsMuteMute.addOnSliderTouchListener(this);
    }

    ticksMaxPerRange = UiUtil.isTablet(activity) ? 50 : 20;

    if (useDialog) {
      dialogUtil.createDialog(builder -> {
        builder.setTitle(R.string.title_options);
        builder.setView(bindingDialog.getRoot());
        builder.setPositiveButton(
            R.string.action_close,
            (dialog, which) -> activity.performHapticClick()
        );
      });
    }
  }

  public OptionsUtil(MainActivity activity, OnPartEditListener onPartEditListener) {
    this.activity = activity;
    this.onPartEditListener = onPartEditListener;

    editPart = true;
    useDialog = true;
    dialogUtil = new DialogUtil(activity, "edit_part");
    bindingDialog = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
    binding = bindingDialog.partialOptions;

    ticksMaxPerRange = UiUtil.isTablet(activity) ? 50 : 20;
  }

  public void maybeInit() {
    if (editPart || getConfig() == null || isInitialized) {
      return;
    }
    MetronomeConfig config = getConfig();
    isCountInActive = config.isCountInActive();
    isIncrementalActive = config.isIncrementalActive();
    isTimerActive = config.isTimerActive();
    isMuteActive = config.isMuteActive();

    isInitialized = true;
  }

  public void show() {
    update();
    if (useDialog) {
      dialogUtil.show();
    }
  }

  public void showIfWasShown(@Nullable Bundle state) {
    if (editPart) {
      part = state != null ? state.getParcelable(PART) : null;
      isNew = state != null && state.getBoolean(IS_NEW, false);
      if (part != null) {
        setPart(part, isNew);
        update();
        dialogUtil.showIfWasShown(state);
      }
    } else {
      update();
      if (useDialog) {
        dialogUtil.showIfWasShown(state);
      }
    }
  }

  public void dismiss() {
    if (useDialog) {
      dialogUtil.dismiss();
    }
  }

  public void saveState(@NonNull Bundle outState) {
    if (useDialog && dialogUtil != null) {
      dialogUtil.saveState(outState);
      if (editPart && part != null) {
        part.setConfig(config);
        outState.putParcelable(PART, part);
        outState.putBoolean(IS_NEW, isNew);
      }
    }
  }

  public void setPart(@NonNull Part part, boolean isNew) {
    this.part = part;
    this.isNew = isNew;

    config = part.toConfig();

    bindingDialog = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
    binding = bindingDialog.partialOptions;

    dialogUtil.createDialog(builder -> {
      String title = activity.getString(
          R.string.label_part_edit, part.getPartIndex() + 1
      );
      if (isNew) {
        title = activity.getString(R.string.action_add_part);
      }
      builder.setTitle(title);
      builder.setView(bindingDialog.getRoot());
      builder.setPositiveButton(
          isNew ? R.string.action_add : R.string.action_apply,
          (dialog, which) -> {
            activity.performHapticClick();
            if (onPartEditListener != null) {
              Part partResult = new Part(part);
              partResult.setConfig(config);
              if (isNew) {
                onPartEditListener.onPartAdded(partResult);
              } else {
                onPartEditListener.onPartUpdated(partResult);
              }
            }
          });
      builder.setNegativeButton(
          R.string.action_cancel,
          (dialog, which) -> activity.performHapticClick()
      );
    });

    update();
  }

  public void update() {
    if (binding == null) {
      return;
    }
    binding.linearOptionsEditPartContainer.setVisibility(editPart ? View.VISIBLE : View.GONE);
    binding.linearOptionsUseCurrentConfig.setOnClickListener(this);
    updateTempo();
    updateBeats(false);
    updateSubdivisions(false);
    updateCountIn();
    updateIncremental();
    updateTimer();
    updateMute();
    updateSwing();
    updatePolyrhythm();
  }

  private void updateTempo() {
    if (!editPart || getConfig() == null) {
      return;
    }
    int tempo = getConfig().getTempo();
    binding.textOptionsTempo.setText(activity.getString(R.string.label_bpm_value, tempo));

    // Calculate current range
    int tempoFactor = (tempo - 1) / ticksMaxPerRange;
    int tempoFromNew = 1 + tempoFactor * ticksMaxPerRange;
    int tempoToNew = tempoFromNew + ticksMaxPerRange - 1;

    binding.buttonOptionsTempoDecrease.setEnabled(tempoFromNew > Constants.TEMPO_MIN);
    binding.buttonOptionsTempoDecrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsTempoDecrease, activity.getString(R.string.action_decrease)
    );

    binding.buttonOptionsTempoIncrease.setEnabled(tempoToNew < Constants.TEMPO_MAX);
    binding.buttonOptionsTempoIncrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsTempoIncrease, activity.getString(R.string.action_increase)
    );

    binding.sliderOptionsTempo.removeOnChangeListener(this);
    ViewUtil.configureSliderSafely(
        binding.sliderOptionsTempo, tempoFromNew, tempoToNew, 1, tempo
    );
    binding.sliderOptionsTempo.addOnChangeListener(this);
    binding.sliderOptionsTempo.setLabelFormatter(
        value -> activity.getString(R.string.label_bpm_value, (int) value)
    );
  }

  private void updateBeats(boolean firstSubChanged) {
    if (!editPart || getConfig() == null) {
      return;
    }
    String[] beats = getConfig().getBeats();
    boolean isFirstSubMuted = getConfig().isFirstSubdivisionMuted();

    if (firstSubChanged) {
      for (int i = 0; i < binding.linearOptionsBeats.getChildCount(); i++) {
        BeatView beatView = (BeatView) binding.linearOptionsBeats.getChildAt(i);
        if (usePolyrhythm) {
          boolean muted = isFirstSubMuted && i == 0;
          beatView.setTickType(muted ? TICK_TYPE.MUTED : beats[i] , true);
        } else {
          beatView.setTickType(isFirstSubMuted ? TICK_TYPE.MUTED : beats[i], true);
        }
      }
      // Only update tick types, no need to rebuild views
      return;
    }

    String[] beatsMaybeMuted = beats.clone();
    if (isFirstSubMuted) {
      Arrays.fill(beatsMaybeMuted, TICK_TYPE.MUTED);
    }
    String[] currentBeats = new String[binding.linearOptionsBeats.getChildCount()];
    for (int i = 0; i < binding.linearOptionsBeats.getChildCount(); i++) {
      currentBeats[i] = String.valueOf(binding.linearOptionsBeats.getChildAt(i));
    }

    if (Arrays.equals(beatsMaybeMuted, currentBeats)) {
      return;
    } else if (beatsMaybeMuted.length == currentBeats.length) {
      for (int i = 0; i < beatsMaybeMuted.length; i++) {
        BeatView beatView = (BeatView) binding.linearOptionsBeats.getChildAt(i);
        beatView.setTickType(beatsMaybeMuted[i], false);
      }
    } else {
      binding.linearOptionsBeats.removeAllViews();
      for (int i = 0; i < beats.length; i++) {
        BeatView beatView = getNewBeatView(false);
        beatView.setTickType(beatsMaybeMuted[i], false);
        beatView.setIndex(i);
        binding.linearOptionsBeats.addView(beatView);
      }
    }

    binding.linearOptionsBeats.post(
        () -> ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizOptionsBeats)
    );

    updateBeatControls();
  }

  private void updateBeatControls() {
    if (!editPart || getConfig() == null) {
      return;
    }
    int beatsCount = getConfig().getBeatsCount();
    binding.textOptionsBeats.setText(
        activity.getResources().getQuantityString(
            R.plurals.options_beats_description, beatsCount, beatsCount
        )
    );
    binding.buttonOptionsBeatsAdd.setOnClickListener(this);
    binding.buttonOptionsBeatsAdd.setEnabled(beatsCount < Constants.BEATS_MAX);
    binding.buttonOptionsBeatsRemove.setOnClickListener(this);
    binding.buttonOptionsBeatsRemove.setEnabled(beatsCount > 1);
  }

  private void updateSubdivisions(boolean firstSubChanged) {
    if (!editPart || getConfig() == null) {
      return;
    }
    String[] subdivisions = getConfig().getSubdivisions();
    boolean isFirstSubMuted = getConfig().isFirstSubdivisionMuted();

    if (firstSubChanged) {
      BeatView beatView = (BeatView) binding.linearOptionsSubs.getChildAt(0);
      beatView.setTickType(
          isFirstSubMuted ? TICK_TYPE.BEAT_SUB_MUTED : TICK_TYPE.BEAT_SUB, true
      );
      // Only update first tick type, no need to rebuild views
      return;
    }

    String[] currentSubs = new String[binding.linearOptionsSubs.getChildCount()];
    for (int i = 0; i < binding.linearOptionsSubs.getChildCount(); i++) {
      currentSubs[i] = String.valueOf(binding.linearOptionsSubs.getChildAt(i));
    }
    if (Arrays.equals(subdivisions, currentSubs)) {
      return;
    } else if (subdivisions.length == currentSubs.length) {
      for (int i = 0; i < subdivisions.length; i++) {
        BeatView beatView = (BeatView) binding.linearOptionsSubs.getChildAt(i);
        beatView.setTickType(subdivisions[i], false);
      }
    } else {
      binding.linearOptionsSubs.removeAllViews();
      for (int i = 0; i < subdivisions.length; i++) {
        BeatView beatView = getNewBeatView(true);
        String tickType = subdivisions[i];
        if (i == 0 && tickType.equals(TICK_TYPE.MUTED)) {
          // Migration from old muted first subdivision
          tickType = TICK_TYPE.BEAT_SUB;
        }
        beatView.setTickType(tickType, false);
        beatView.setIndex(i);
        binding.linearOptionsSubs.addView(beatView);
      }
    }

    binding.linearOptionsSubs.post(
        () -> ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizOptionsSubs)
    );

    updateSubControls();
  }

  private void updateSubControls() {
    if (!editPart || getConfig() == null) {
      return;
    }
    int subdivisionsCount = getConfig().getSubdivisionsCount();
    boolean isSubdivisionActive = getConfig().isSubdivisionActive();
    if (isSubdivisionActive) {
      binding.textOptionsSubs.setText(
          activity.getResources().getQuantityString(
              R.plurals.options_subdivisions_description, subdivisionsCount, subdivisionsCount
          )
      );
    } else if (getConfig().isFirstSubdivisionMuted()) {
      binding.textOptionsSubs.setText(R.string.options_subdivisions_beats_muted);
    } else {
      binding.textOptionsSubs.setText(R.string.options_inactive);
    }
    binding.buttonOptionsSubsAdd.setOnClickListener(this);
    binding.buttonOptionsSubsAdd.setEnabled(subdivisionsCount < Constants.SUBS_MAX);
    binding.buttonOptionsSubsRemove.setOnClickListener(this);
    binding.buttonOptionsSubsRemove.setEnabled(subdivisionsCount > 1);
  }

  @NonNull
  private BeatView getNewBeatView(boolean isSubdivision) {
    BeatView beatView = new BeatView(activity);
    beatView.setIsSubdivision(isSubdivision);
    beatView.setOnClickListener(beat -> {
      MetronomeConfig config = getConfig();
      if (config == null) {
        return;
      }
      activity.performHapticClick();

      if (isSubdivision) {
        config.setSubdivision(beatView.getIndex(), beatView.nextTickType());
        if (beatView.getIndex() == 0) {
          // Update all beats if first subdivision was changed (muted or not)
          updateBeats(true);
        }
      } else {
        if (config.isFirstSubdivisionMuted()) {
          config.setSubdivision(0, TICK_TYPE.BEAT_SUB);
          updateBeats(true);
          updateSubdivisions(true);
        } else {
          config.setBeat(beatView.getIndex(), beatView.nextTickType());
        }
      }
      // Maybe change description
      updateSubControls();
    });
    return beatView;
  }

  private void updateCountIn() {
    if (getConfig() == null) {
      return;
    }
    boolean isCountInActive = getConfig().isCountInActive();
    if (this.isCountInActive != isCountInActive) {
      this.isCountInActive = isCountInActive;
      if (listener != null) {
        listener.onModifiersCountChanged();
      }
    }
    int countIn = getConfig().getCountIn();
    binding.sliderOptionsCountIn.removeOnChangeListener(this);
    ViewUtil.configureSliderSafely(
        binding.sliderOptionsCountIn, 0, Constants.COUNT_IN_MAX, 1, countIn
    );
    binding.sliderOptionsCountIn.addOnChangeListener(this);
    binding.sliderOptionsCountIn.setLabelFormatter(
        value -> activity.getResources().getQuantityString(
            R.plurals.options_unit_bars, (int) value, (int) value
        )
    );
    if (getConfig().isCountInActive()) {
      binding.textOptionsCountIn.setText(
          activity.getResources().getQuantityString(
              R.plurals.options_count_in_description, countIn, countIn
          )
      );
    } else {
      binding.textOptionsCountIn.setText(R.string.options_inactive);
    }

    binding.linearOptionsCountInContainer.setBackgroundResource(
        editPart
            ? R.drawable.ripple_list_item_bg_segmented_middle
            : R.drawable.ripple_list_item_bg_segmented_first
    );
  }

  private void updateIncremental() {
    if (getConfig() == null) {
      return;
    }
    int incrementalAmount = getConfig().getIncrementalAmount();
    boolean incrementalIncrease = getConfig().isIncrementalIncrease();
    boolean isIncrementalActive = getConfig().isIncrementalActive();
    if (this.isIncrementalActive != isIncrementalActive) {
      this.isIncrementalActive = isIncrementalActive;
      if (listener != null) {
        listener.onModifiersCountChanged();
      }
    }
    if (isIncrementalActive) {
      binding.textOptionsIncrementalAmount.setText(activity.getString(
          incrementalIncrease
              ? R.string.options_incremental_amount_increase
              : R.string.options_incremental_amount_decrease,
          incrementalAmount
      ));
    } else {
      binding.textOptionsIncrementalAmount.setText(R.string.options_inactive);
    }

    // Calculate current range
    int factorAmount = incrementalAmount / ticksMaxPerRange;
    int valueFromNewAmount = factorAmount * ticksMaxPerRange;
    int valueToNewAmount = Math.min(
        valueFromNewAmount + ticksMaxPerRange - 1, Constants.INCREMENTAL_AMOUNT_MAX
    );

    binding.buttonOptionsIncrementalAmountDecrease.setEnabled(valueFromNewAmount > 0);
    binding.buttonOptionsIncrementalAmountDecrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsIncrementalAmountDecrease,
        activity.getString(R.string.action_decrease)
    );

    binding.buttonOptionsIncrementalAmountIncrease.setEnabled(
        valueToNewAmount < Constants.INCREMENTAL_AMOUNT_MAX
    );
    binding.buttonOptionsIncrementalAmountIncrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsIncrementalAmountIncrease,
        activity.getString(R.string.action_increase)
    );

    binding.sliderOptionsIncrementalAmount.removeOnChangeListener(this);
    ViewUtil.configureSliderSafely(
        binding.sliderOptionsIncrementalAmount,
        valueFromNewAmount, valueToNewAmount, 1, incrementalAmount
    );
    binding.sliderOptionsIncrementalAmount.addOnChangeListener(this);
    binding.sliderOptionsIncrementalAmount.setLabelFormatter(
        value -> activity.getString(R.string.label_bpm_value, (int) value)
    );

    int visibilityOld = binding.linearOptionsIncrementalContainer.getVisibility();
    int visibilityNew = (isIncrementalActive || !useDialog) ? View.VISIBLE : View.GONE;
    if (visibilityOld != visibilityNew) {
      Transition transition = new AutoTransition();
      transition.setDuration(Constants.ANIM_DURATION_SHORT);
      TransitionManager.beginDelayedTransition(binding.linearOptionsContainer, transition);
      binding.linearOptionsIncrementalContainer.setVisibility(visibilityNew);
    }

    binding.toggleOptionsIncrementalDirection.removeOnButtonCheckedListener(this);
    binding.toggleOptionsIncrementalDirection.check(
        incrementalIncrease
            ? R.id.button_options_incremental_increase
            : R.id.button_options_incremental_decrease
    );
    binding.toggleOptionsIncrementalDirection.addOnButtonCheckedListener(this);
    binding.toggleOptionsIncrementalDirection.setEnabled(isIncrementalActive);

    int incrementalInterval = getConfig().getIncrementalInterval();
    String incrementalUnit = getConfig().getIncrementalUnit();
    int intervalResId, checkedId;
    switch (incrementalUnit) {
      case UNIT.SECONDS:
        intervalResId = R.plurals.options_incremental_interval_seconds;
        checkedId = R.id.button_options_incremental_unit_seconds;
        break;
      case UNIT.MINUTES:
        intervalResId = R.plurals.options_incremental_interval_minutes;
        checkedId = R.id.button_options_incremental_unit_minutes;
        break;
      default:
        intervalResId = R.plurals.options_incremental_interval_bars;
        checkedId = R.id.button_options_incremental_unit_bars;
        break;
    }
    binding.textOptionsIncrementalInterval.setText(
        activity.getResources().getQuantityString(
            intervalResId, incrementalInterval, incrementalInterval
        )
    );
    binding.textOptionsIncrementalInterval.setAlpha(isIncrementalActive ? 1 : 0.5f);

    // Calculate current range
    int intervalFactor = (incrementalInterval - 1) / ticksMaxPerRange;
    int intervalFromNew = 1 + intervalFactor * ticksMaxPerRange;
    int intervalToNew = Math.min(
        intervalFromNew + ticksMaxPerRange - 1, Constants.INCREMENTAL_INTERVAL_MAX
    );

    binding.buttonOptionsIncrementalIntervalDecrease.setEnabled(
        isIncrementalActive && intervalFromNew > 1
    );
    binding.buttonOptionsIncrementalIntervalDecrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsIncrementalIntervalDecrease,
        activity.getString(R.string.action_decrease)
    );

    binding.buttonOptionsIncrementalIntervalIncrease.setEnabled(
        isIncrementalActive && intervalToNew < Constants.INCREMENTAL_INTERVAL_MAX
    );
    binding.buttonOptionsIncrementalIntervalIncrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsIncrementalIntervalIncrease,
        activity.getString(R.string.action_increase)
    );

    binding.sliderOptionsIncrementalInterval.removeOnChangeListener(this);
    ViewUtil.configureSliderSafely(
        binding.sliderOptionsIncrementalInterval,
        intervalFromNew, intervalToNew, 1, incrementalInterval
    );
    binding.sliderOptionsIncrementalInterval.addOnChangeListener(this);
    binding.sliderOptionsIncrementalInterval.setLabelFormatter(value -> {
      int resId;
      switch (incrementalUnit) {
        case UNIT.SECONDS:
          resId = R.plurals.options_unit_seconds;
          break;
        case UNIT.MINUTES:
          resId = R.plurals.options_unit_minutes;
          break;
        default:
          resId = R.plurals.options_unit_bars;
          break;
      }
      int interval = (int) value;
      return activity.getResources().getQuantityString(resId, interval, interval);
    });
    binding.sliderOptionsIncrementalInterval.setEnabled(isIncrementalActive);

    binding.toggleOptionsIncrementalUnit.removeOnButtonCheckedListener(this);
    binding.toggleOptionsIncrementalUnit.check(checkedId);
    binding.toggleOptionsIncrementalUnit.addOnButtonCheckedListener(this);
    binding.toggleOptionsIncrementalUnit.setEnabled(isIncrementalActive);

    int incrementalLimit = getConfig().getIncrementalLimit();
    /* When slider should be automatically adjusted to tempo
    int tempo = getMetronomeEngine().getTempo();
    if ((incrementalIncrease && incrementalLimit < tempo)
        || (!incrementalIncrease && incrementalLimit > tempo)
    ) {
      //getMetronomeEngine().setIncrementalLimit(tempo);
      //incrementalLimit = tempo;
    } */
    if (incrementalLimit > 0) {
      binding.textOptionsIncrementalLimit.setText(
          activity.getResources().getString(
              incrementalIncrease
                  ? R.string.options_incremental_max
                  : R.string.options_incremental_min,
              incrementalLimit
          )
      );
    } else {
      binding.textOptionsIncrementalLimit.setText(
          incrementalIncrease
              ? R.string.options_incremental_no_max
              : R.string.options_incremental_no_min
      );
    }
    binding.textOptionsIncrementalLimit.setAlpha(isIncrementalActive ? 1 : 0.5f);

    // Calculate current range
    int factor = incrementalLimit / ticksMaxPerRange;
    int valueFromNew = factor * ticksMaxPerRange;
    int valueToNew = Math.min(
        valueFromNew + ticksMaxPerRange - 1, Constants.TEMPO_MAX - 1
    );

    binding.buttonOptionsIncrementalLimitDecrease.setEnabled(
        isIncrementalActive && valueFromNew > 0
    );
    binding.buttonOptionsIncrementalLimitDecrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsIncrementalLimitDecrease,
        activity.getString(R.string.action_decrease)
    );

    binding.buttonOptionsIncrementalLimitIncrease.setEnabled(
        isIncrementalActive && valueToNew < Constants.TEMPO_MAX - 1
    );
    binding.buttonOptionsIncrementalLimitIncrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsIncrementalLimitIncrease,
        activity.getString(R.string.action_increase)
    );

    binding.sliderOptionsIncrementalLimit.removeOnChangeListener(this);
    ViewUtil.configureSliderSafely(
        binding.sliderOptionsIncrementalLimit,
        valueFromNew, valueToNew, 1, incrementalLimit
    );
    binding.sliderOptionsIncrementalLimit.addOnChangeListener(this);
    binding.sliderOptionsIncrementalLimit.setLabelFormatter(
        value -> activity.getString(R.string.label_bpm_value, (int) value)
    );
    binding.sliderOptionsIncrementalLimit.setEnabled(isIncrementalActive);
  }

  private void updateTimer() {
    if (getConfig() == null) {
      return;
    }
    int timerDuration = getConfig().getTimerDuration();
    boolean isTimerActive = getConfig().isTimerActive();
    if (this.isTimerActive != isTimerActive) {
      this.isTimerActive = isTimerActive;
      if (listener != null) {
        listener.onModifiersCountChanged();
      }
    }
    String timerUnit = getConfig().getTimerUnit();
    int durationResId, checkedId;
    switch (timerUnit) {
      case UNIT.SECONDS:
        durationResId = R.plurals.options_timer_description_seconds;
        checkedId = R.id.button_options_timer_unit_seconds;
        break;
      case UNIT.MINUTES:
        durationResId = R.plurals.options_timer_description_minutes;
        checkedId = R.id.button_options_timer_unit_minutes;
        break;
      default:
        durationResId = R.plurals.options_timer_description_bars;
        checkedId = R.id.button_options_timer_unit_bars;
        break;
    }
    if (isTimerActive) {
      binding.textOptionsTimerDuration.setText(
          activity.getResources().getQuantityString(durationResId, timerDuration, timerDuration)
      );
    } else {
      binding.textOptionsTimerDuration.setText(R.string.options_inactive);
    }

    // Calculate current range
    int factor = timerDuration / ticksMaxPerRange;
    int valueFromNew = factor * ticksMaxPerRange;
    int valueToNew = Math.min(valueFromNew + ticksMaxPerRange - 1, Constants.TIMER_MAX);

    binding.buttonOptionsTimerDecrease.setEnabled(valueFromNew > 0);
    binding.buttonOptionsTimerDecrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsTimerDecrease,
        activity.getString(R.string.action_decrease)
    );

    binding.buttonOptionsTimerIncrease.setEnabled(valueToNew < Constants.TIMER_MAX);
    binding.buttonOptionsTimerIncrease.setOnClickListener(this);
    ViewCompat.setTooltipText(
        binding.buttonOptionsTimerIncrease,
        activity.getString(R.string.action_increase)
    );

    binding.sliderOptionsTimerDuration.removeOnChangeListener(this);
    ViewUtil.configureSliderSafely(
        binding.sliderOptionsTimerDuration,
        valueFromNew, valueToNew, 1, timerDuration
    );
    binding.sliderOptionsTimerDuration.addOnChangeListener(this);
    binding.sliderOptionsTimerDuration.setLabelFormatter(value -> {
      int resId;
      switch (timerUnit) {
        case UNIT.SECONDS:
          resId = R.plurals.options_unit_seconds;
          break;
        case UNIT.MINUTES:
          resId = R.plurals.options_unit_minutes;
          break;
        default:
          resId = R.plurals.options_unit_bars;
          break;
      }
      int interval = (int) value;
      return activity.getResources().getQuantityString(resId, interval, interval);
    });

    int visibilityOld = binding.linearOptionsTimerContainer.getVisibility();
    int visibilityNew = (isTimerActive || !useDialog) ? View.VISIBLE : View.GONE;
    if (visibilityOld != visibilityNew) {
      Transition transition = new AutoTransition();
      transition.setDuration(Constants.ANIM_DURATION_SHORT);
      TransitionManager.beginDelayedTransition(binding.linearOptionsContainer, transition);
      binding.linearOptionsTimerContainer.setVisibility(visibilityNew);
    }

    binding.toggleOptionsTimerUnit.removeOnButtonCheckedListener(this);
    binding.toggleOptionsTimerUnit.check(checkedId);
    binding.toggleOptionsTimerUnit.addOnButtonCheckedListener(this);
    binding.toggleOptionsTimerUnit.setEnabled(isTimerActive);
  }

  private void updateMute() {
    if (getConfig() == null) {
      return;
    }
    int mutePlay = getConfig().getMutePlay();
    int muteMute = getConfig().getMuteMute();
    String muteUnit = getConfig().getMuteUnit();
    boolean isUnitBeats = muteUnit.equals(UNIT.BEATS);
    boolean muteRandom = getConfig().isMuteRandom();
    boolean isMuteActive = getConfig().isMuteActive();
    if (this.isMuteActive != isMuteActive) {
      this.isMuteActive = isMuteActive;
      if (listener != null) {
        listener.onModifiersCountChanged();
      }
    }

    // Mute Play

    boolean showPlay = !isUnitBeats;
    int visibilityPlayOld = binding.linearOptionsMutePlay.getVisibility();
    int visibilityPlayNew = showPlay ? View.VISIBLE : View.GONE;
    boolean visibilityPlayChanged = visibilityPlayOld != visibilityPlayNew;

    int resIdPlay, resIdLabelPlay;
    if (muteUnit.equals(UNIT.SECONDS)) {
      resIdPlay = R.plurals.options_mute_play_seconds;
      resIdLabelPlay = R.plurals.options_unit_seconds;
    } else {
      resIdPlay = R.plurals.options_mute_play_bars;
      resIdLabelPlay = R.plurals.options_unit_bars;
    }
    if (showPlay) {
      if (isMuteActive) {
        binding.textOptionsMutePlay.setText(
            activity.getResources().getQuantityString(resIdPlay, mutePlay, mutePlay)
        );
      } else {
        binding.textOptionsMutePlay.setText(R.string.options_inactive);
      }
    }

    binding.sliderOptionsMutePlay.removeOnChangeListener(this);
    ViewUtil.configureSliderSafely(
        binding.sliderOptionsMutePlay, 0, Constants.MUTE_PLAY_MAX, 1, mutePlay
    );
    binding.sliderOptionsMutePlay.addOnChangeListener(this);
    binding.sliderOptionsMutePlay.setLabelFormatter(value -> {
      int play = (int) value;
      return activity.getResources().getQuantityString(resIdLabelPlay, play, play);
    });

    // Mute Mute

    boolean showMute = isUnitBeats || isMuteActive || !useDialog;
    int visibilityMuteOld = binding.linearOptionsMuteMute.getVisibility();
    int visibilityMuteNew = showMute ? View.VISIBLE : View.GONE;
    boolean visibilityMuteChanged = visibilityMuteOld != visibilityMuteNew;

    int resIdMute, resIdLabelMute;
    if (muteUnit.equals(UNIT.SECONDS)) {
      resIdMute = R.plurals.options_mute_mute_seconds;
      resIdLabelMute = R.plurals.options_unit_seconds;
    } else if (muteUnit.equals(UNIT.BARS)) {
      resIdMute = R.plurals.options_mute_mute_bars;
      resIdLabelMute = R.plurals.options_unit_bars;
    } else {
      resIdMute = 0;
      resIdLabelMute = R.plurals.options_unit_beats;
    }
    if (isUnitBeats && isMuteActive) {
      binding.textOptionsMuteMute.setText(
          activity.getString(R.string.options_mute_mute_beats, muteMute)
      );
    } else if (isUnitBeats) {
      binding.textOptionsMuteMute.setText(R.string.options_inactive);
    } else {
      binding.textOptionsMuteMute.setText(
          activity.getResources().getQuantityString(resIdMute, muteMute, muteMute)
      );
    }
    binding.textOptionsMuteMute.setAlpha(isUnitBeats || isMuteActive ? 1 : 0.5f);

    binding.sliderOptionsMuteMute.removeOnChangeListener(this);
    ViewUtil.configureSliderSafely(
        binding.sliderOptionsMuteMute,
        isUnitBeats ? Constants.MUTE_MUTE_MIN_BEATS : Constants.MUTE_MUTE_MIN,
        isUnitBeats ? Constants.MUTE_MUTE_MAX_BEATS : Constants.MUTE_MUTE_MAX,
        isUnitBeats ? Constants.MUTE_MUTE_STEP_SIZE_BEATS : Constants.MUTE_MUTE_STEP_SIZE,
        muteMute
    );
    binding.sliderOptionsMuteMute.addOnChangeListener(this);
    binding.sliderOptionsMuteMute.setLabelFormatter(value -> {
      int mute = (int) value;
      if (isUnitBeats) {
        return activity.getString(R.string.options_mute_mute_beats, mute);
      } else {
        return activity.getResources().getQuantityString(resIdLabelMute, mute, mute);
      }
    });
    binding.sliderOptionsMuteMute.setEnabled(isUnitBeats || isMuteActive);

    // Mute Unit

    boolean showUnit = isMuteActive || !useDialog;
    int visibilityUnitOld = binding.scrollHorizOptionsMuteUnit.getVisibility();
    int visibilityUnitNew = showUnit ? View.VISIBLE : View.GONE;
    boolean visibleUnitChanged = visibilityUnitOld != visibilityUnitNew;

    int checkedId;
    if (muteUnit.equals(UNIT.SECONDS)) {
      checkedId = R.id.button_options_mute_unit_seconds;
    } else if (muteUnit.equals(UNIT.BARS)) {
      checkedId = R.id.button_options_mute_unit_bars;
    } else {
      checkedId = R.id.button_options_mute_unit_beats;
    }
    binding.toggleOptionsMuteUnit.removeOnButtonCheckedListener(this);
    binding.toggleOptionsMuteUnit.check(checkedId);
    binding.toggleOptionsMuteUnit.addOnButtonCheckedListener(this);
    binding.toggleOptionsMuteUnit.setEnabled(isMuteActive);

    // Mute Random

    boolean showRandom = !isUnitBeats && (isMuteActive || !useDialog);
    int visibilityRandomOld = binding.linearOptionsMuteRandom.getVisibility();
    int visibilityRandomNew = showRandom ? View.VISIBLE : View.GONE;
    boolean visibilityRandomChanged = visibilityRandomOld != visibilityRandomNew;

    binding.linearOptionsMuteRandom.setOnClickListener(this);
    binding.linearOptionsMuteRandom.setEnabled(isMuteActive);
    binding.linearOptionsMuteRandom.setBackgroundResource(
        useDialog
            ? R.drawable.ripple_list_item_surface_bright
            : R.drawable.ripple_list_item_bg
    );
    binding.textOptionsMuteRandom.setAlpha(isMuteActive ? 1 : 0.5f);
    binding.switchOptionsMuteRandom.setOnCheckedChangeListener(null);
    binding.switchOptionsMuteRandom.setChecked(muteRandom);
    binding.switchOptionsMuteRandom.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          activity.performHapticClick();
          if (editPart && getConfig() != null) {
            getConfig().setMuteRandom(isChecked);
          } else if (activity.getMetronomeEngine() != null) {
            activity.getMetronomeEngine().setMuteRandom(isChecked);
            activity.getMetronomeEngine().maybeUpdateDefaultSong();
          }
          updateMute();
        });
    binding.switchOptionsMuteRandom.setEnabled(isMuteActive);

    if (visibilityPlayChanged || visibilityMuteChanged
        || visibleUnitChanged || visibilityRandomChanged
    ) {
      Transition transition = new AutoTransition();
      transition.setDuration(Constants.ANIM_DURATION_SHORT);
      TransitionManager.beginDelayedTransition(binding.linearOptionsContainer, transition);
    }

    if (visibilityPlayChanged) {
      binding.linearOptionsMutePlay.setVisibility(visibilityPlayNew);
    }
    if (visibilityMuteChanged) {
      binding.linearOptionsMuteMute.setVisibility(visibilityMuteNew);
    }
    if (visibleUnitChanged) {
      binding.scrollHorizOptionsMuteUnit.setVisibility(visibilityUnitNew);
    }
    if (visibilityRandomChanged) {
      binding.linearOptionsMuteRandom.setVisibility(visibilityRandomNew);
    }
  }

  public void updateSwing() {
    if (getConfig() == null) {
      return;
    }
    boolean isSwingActive = getConfig().isSwingActive();

    binding.textOptionsSwing.setText(
        isSwingActive ? R.string.options_swing_description : R.string.options_inactive
    );

    binding.toggleOptionsSwing.removeOnButtonCheckedListener(this);
    if (getConfig().isSwing3()) {
      binding.toggleOptionsSwing.check(R.id.button_options_swing_3);
    } else if (getConfig().isSwing5()) {
      binding.toggleOptionsSwing.check(R.id.button_options_swing_5);
    } else if (getConfig().isSwing7()) {
      binding.toggleOptionsSwing.check(R.id.button_options_swing_7);
    } else {
      binding.toggleOptionsSwing.clearChecked();
    }
    binding.toggleOptionsSwing.addOnButtonCheckedListener(this);
  }

  private void updatePolyrhythm() {
    if (getConfig() == null) {
      return;
    }
    boolean usePolyrhythm = getConfig().usePolyrhythm();
    if (this.usePolyrhythm != usePolyrhythm) {
      this.usePolyrhythm = usePolyrhythm;
      if (listener != null) {
        listener.onModifiersCountChanged();
      }
    }

    binding.linearOptionsPolyrhythm.setOnClickListener(this);
    binding.switchOptionsPolyrhythm.setOnCheckedChangeListener(null);
    binding.switchOptionsPolyrhythm.setChecked(usePolyrhythm);
    binding.switchOptionsPolyrhythm.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          activity.performHapticClick();
          if (editPart && getConfig() != null) {
            getConfig().setUsePolyrhythm(isChecked);
          } else if (activity.getMetronomeEngine() != null) {
            activity.getMetronomeEngine().setUsePolyrhythm(isChecked);
            activity.getMetronomeEngine().maybeUpdateDefaultSong();
          }
          updatePolyrhythm();
          if (!editPart && listener != null) {
            listener.onBeatsChanged();
          }
          updateBeats(true);
        });
  }

  @Override
  public void onClick(View v) {
    MetronomeConfig config = getConfig();
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    if (config == null || metronomeEngine == null) {
      return;
    }
    int id = v.getId();
    if (id == R.id.linear_options_use_current_config) {
      activity.performHapticClick();
      this.config = new MetronomeConfig(metronomeEngine.getConfig());
      update();
    } else if (id == R.id.button_options_tempo_decrease) {
      activity.performHapticClick();
      int valueFrom = (int) binding.sliderOptionsTempo.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTempo.getValueTo();
      int range = valueTo - valueFrom;
      int decreasedTempo = config.getTempo() - range - 1;
      if (editPart) {
        config.setTempo(decreasedTempo);
      } else {
        metronomeEngine.setTempo(decreasedTempo);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateTempo();
      ViewUtil.startIcon(binding.buttonOptionsTempoDecrease.getIcon());
    } else if (id == R.id.button_options_tempo_increase) {
      activity.performHapticClick();
      int valueFrom = (int) binding.sliderOptionsTempo.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTempo.getValueTo();
      int range = valueTo - valueFrom;
      int increasedTempo = config.getTempo() + range + 1;
      if (editPart) {
        config.setTempo(increasedTempo);
      } else {
        metronomeEngine.setTempo(increasedTempo);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateTempo();
      ViewUtil.startIcon(binding.buttonOptionsTempoIncrease.getIcon());
    } else if (id == R.id.button_options_beats_add) {
      activity.performHapticClick();
      ViewUtil.startIcon(binding.buttonOptionsBeatsAdd.getIcon());
      boolean success = config.addBeat();
      if (success) {
        Transition transition = new AutoTransition();
        transition.setDuration(Constants.ANIM_DURATION_SHORT);
        TransitionManager.beginDelayedTransition(binding.linearOptionsBeats, transition);

        // already add new BeatView's width to centering calculation
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizOptionsBeats, UiUtil.dpToPx(activity, 48)
        );

        BeatView beatView = getNewBeatView(false);
        boolean isFirstSubMuted = config.isFirstSubdivisionMuted();
        beatView.setTickType(isFirstSubMuted ? TICK_TYPE.MUTED : TICK_TYPE.NORMAL, false);
        beatView.setIndex(binding.linearOptionsBeats.getChildCount());
        binding.linearOptionsBeats.addView(beatView);
        updateBeatControls();
      }
    } else if (id == R.id.button_options_beats_remove) {
      activity.performHapticClick();
      ViewUtil.startIcon(binding.buttonOptionsBeatsRemove.getIcon());
      boolean success = config.removeBeat();
      if (success) {
        Transition transition = new ChangeBounds();
        transition.setDuration(Constants.ANIM_DURATION_SHORT);
        TransitionManager.beginDelayedTransition(binding.linearOptionsBeats, transition);

        // already remove old BeatView's width from centering calculation
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizOptionsBeats, -UiUtil.dpToPx(activity, 48)
        );

        binding.linearOptionsBeats.removeViewAt(
            binding.linearOptionsBeats.getChildCount() - 1
        );
        updateBeatControls();
      }
    } else if (id == R.id.button_options_subs_add) {
      activity.performHapticClick();
      ViewUtil.startIcon(binding.buttonOptionsSubsAdd.getIcon());
      boolean success = config.addSubdivision();
      if (success) {
        Transition transition = new AutoTransition();
        transition.setDuration(Constants.ANIM_DURATION_SHORT);
        TransitionManager.beginDelayedTransition(binding.linearOptionsSubs, transition);

        // already add new BeatView's width to centering calculation
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizOptionsSubs, UiUtil.dpToPx(activity, 48)
        );

        BeatView beatView = getNewBeatView(true);
        beatView.setIndex(binding.linearOptionsSubs.getChildCount());
        binding.linearOptionsSubs.addView(beatView);
        updateSubControls();
      }
    } else if (id == R.id.button_options_subs_remove) {
      activity.performHapticClick();
      ViewUtil.startIcon(binding.buttonOptionsSubsRemove.getIcon());
      boolean success = config.removeSubdivision();
      if (success) {
        Transition transition = new ChangeBounds();
        transition.setDuration(Constants.ANIM_DURATION_SHORT);
        TransitionManager.beginDelayedTransition(binding.linearOptionsSubs, transition);

        // already remove old BeatView's width from centering calculation
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizOptionsSubs, -UiUtil.dpToPx(activity, 48)
        );

        binding.linearOptionsSubs.removeViewAt(binding.linearOptionsSubs.getChildCount() - 1);
        updateSubControls();
      }
    } else if (id == R.id.button_options_incremental_amount_decrease) {
      activity.performHapticClick();
      int decreasedAmount = config.getIncrementalAmount() - ticksMaxPerRange;
      if (editPart) {
        config.setIncrementalAmount(decreasedAmount);
      } else {
        metronomeEngine.setIncrementalAmount(decreasedAmount);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalAmountDecrease.getIcon());
    } else if (id == R.id.button_options_incremental_amount_increase) {
      activity.performHapticClick();
      int increasedAmount = config.getIncrementalAmount() + ticksMaxPerRange;
      if (editPart) {
        config.setIncrementalAmount(increasedAmount);
      } else {
        metronomeEngine.setIncrementalAmount(increasedAmount);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalAmountIncrease.getIcon());
    } else if (id == R.id.button_options_incremental_interval_decrease) {
      activity.performHapticClick();
      int decreasedInterval = config.getIncrementalInterval() - ticksMaxPerRange;
      if (editPart) {
        config.setIncrementalInterval(decreasedInterval);
      } else {
        metronomeEngine.setIncrementalInterval(decreasedInterval);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalIntervalDecrease.getIcon());
    } else if (id == R.id.button_options_incremental_interval_increase) {
      activity.performHapticClick();
      int increasedInterval = config.getIncrementalInterval() + ticksMaxPerRange;
      if (editPart) {
        config.setIncrementalInterval(increasedInterval);
      } else {
        metronomeEngine.setIncrementalInterval(increasedInterval);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalIntervalIncrease.getIcon());
    } else if (id == R.id.button_options_incremental_limit_decrease) {
      activity.performHapticClick();
      int decreasedLimit = config.getIncrementalLimit() - ticksMaxPerRange;
      if (editPart) {
        config.setIncrementalLimit(decreasedLimit);
      } else {
        metronomeEngine.setIncrementalLimit(decreasedLimit);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalLimitDecrease.getIcon());
    } else if (id == R.id.button_options_incremental_limit_increase) {
      activity.performHapticClick();
      int increasedLimit = config.getIncrementalLimit() + ticksMaxPerRange;
      if (editPart) {
        config.setIncrementalLimit(increasedLimit);
      } else {
        metronomeEngine.setIncrementalLimit(increasedLimit);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalLimitIncrease.getIcon());
    } else if (id == R.id.button_options_timer_decrease) {
      activity.performHapticClick();
      int decreasedDuration = config.getTimerDuration() - ticksMaxPerRange;
      if (editPart) {
        config.setTimerDuration(decreasedDuration);
      } else {
        metronomeEngine.setTimerDuration(decreasedDuration);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateTimer();
      if (!editPart && listener != null) {
        listener.onTimerChanged();
      }
      ViewUtil.startIcon(binding.buttonOptionsTimerDecrease.getIcon());
    } else if (id == R.id.button_options_timer_increase) {
      activity.performHapticClick();
      int increasedDuration = config.getTimerDuration() + ticksMaxPerRange;
      if (editPart) {
        config.setTimerDuration(increasedDuration);
      } else {
        metronomeEngine.setTimerDuration(increasedDuration);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateTimer();
      if (!editPart && listener != null) {
        listener.onTimerChanged();
      }
      ViewUtil.startIcon(binding.buttonOptionsTimerIncrease.getIcon());
    } else if (id == R.id.linear_options_mute_random) {
      binding.switchOptionsMuteRandom.toggle();
    } else if (id == R.id.linear_options_polyrhythm) {
      binding.switchOptionsPolyrhythm.toggle();
    }
  }

  @Override
  public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
    MetronomeConfig config = getConfig();
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    if (!isChecked || config == null || metronomeEngine == null) {
      return;
    }
    activity.performHapticClick();
    int groupId = group.getId();
    if (groupId == R.id.toggle_options_incremental_direction) {
      boolean incrementalIncrease = checkedId == R.id.button_options_incremental_increase;
      if (editPart) {
        config.setIncrementalIncrease(incrementalIncrease);
      } else {
        metronomeEngine.setIncrementalIncrease(incrementalIncrease);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateIncremental();
    } else if (groupId == R.id.toggle_options_incremental_unit) {
      String unit = UNIT.BARS;
      if (checkedId == R.id.button_options_incremental_unit_seconds) {
        unit = UNIT.SECONDS;
      } else if (checkedId == R.id.button_options_incremental_unit_minutes) {
        unit = UNIT.MINUTES;
      }
      if (editPart) {
        config.setIncrementalUnit(unit);
      } else {
        metronomeEngine.setIncrementalUnit(unit);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateIncremental();
    } else if (groupId == R.id.toggle_options_timer_unit) {
      String unit = UNIT.BARS;
      if (checkedId == R.id.button_options_timer_unit_seconds) {
        unit = UNIT.SECONDS;
      } else if (checkedId == R.id.button_options_timer_unit_minutes) {
        unit = UNIT.MINUTES;
      }
      if (editPart) {
        config.setTimerUnit(unit);
      } else {
        metronomeEngine.setTimerUnit(unit);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateTimer();
      if (!editPart && listener != null) {
        listener.onTimerChanged();
      }
    } else if (groupId == R.id.toggle_options_mute_unit) {
      String unit = UNIT.BEATS;
      if (checkedId == R.id.button_options_mute_unit_bars) {
        unit = UNIT.BARS;
      } else if (checkedId == R.id.button_options_mute_unit_seconds) {
        unit = UNIT.SECONDS;
      }
      if (editPart) {
        config.setMuteUnit(unit);
      } else {
        metronomeEngine.setMuteUnit(unit);
        metronomeEngine.maybeUpdateDefaultSong();
      }
      updateMute();
    } else if (groupId == R.id.toggle_options_swing) {
      if (checkedId == R.id.button_options_swing_3) {
        if (editPart) {
          config.setSwing3();
        } else {
          metronomeEngine.setSwing3();
          // config has not changed timer duration or timer unit so we can reuse it
          if (config.isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
            metronomeEngine.restartIfPlaying(false);
          }
          metronomeEngine.maybeUpdateDefaultSong();
        }
      } else if (checkedId == R.id.button_options_swing_5) {
        if (editPart) {
          config.setSwing5();
        } else {
          metronomeEngine.setSwing5();
          if (config.isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
            metronomeEngine.restartIfPlaying(false);
          }
          metronomeEngine.maybeUpdateDefaultSong();
        }
      } else if (checkedId == R.id.button_options_swing_7) {
        if (editPart) {
          config.setSwing7();
        } else {
          metronomeEngine.setSwing7();
          if (config.isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
            metronomeEngine.restartIfPlaying(false);
          }
          metronomeEngine.maybeUpdateDefaultSong();
        }
      }
      updateSwing();
      updateSubdivisions(false);
      if (!editPart && listener != null) {
        listener.onSubsChanged();
      }
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    MetronomeConfig config = getConfig();
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    if (!fromUser || config == null || metronomeEngine == null) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_options_tempo) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        config.setTempo((int) value);
      } else {
        metronomeEngine.setTempo((int) value);
      }
      updateTempo();
    } else if (id == R.id.slider_options_count_in) {
      activity.performHapticSegmentTick(slider, false);
      if (editPart) {
        config.setCountIn((int) value);
      } else {
        metronomeEngine.setCountIn((int) value);
      }
      updateCountIn();
    } else if (id == R.id.slider_options_incremental_amount) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        config.setIncrementalAmount((int) value);
      } else {
        metronomeEngine.setIncrementalAmount((int) value);
      }
      updateIncremental();
    } else if (id == R.id.slider_options_incremental_interval) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        config.setIncrementalInterval((int) value);
      } else {
        metronomeEngine.setIncrementalInterval((int) value);
      }
      updateIncremental();
    } else if (id == R.id.slider_options_incremental_limit) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        config.setIncrementalLimit((int) value);
      } else {
        metronomeEngine.setIncrementalLimit((int) value);
      }
      updateIncremental();
    } else if (id == R.id.slider_options_timer_duration) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        config.setTimerDuration((int) value);
      } else {
        metronomeEngine.setTimerDuration((int) value);
      }
      updateTimer();
    } else if (id == R.id.slider_options_mute_play) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        config.setMutePlay((int) value);
      } else {
        metronomeEngine.setMutePlay((int) value);
      }
      updateMute();
    } else if (id == R.id.slider_options_mute_mute) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        config.setMuteMute((int) value);
      } else {
        metronomeEngine.setMuteMute((int) value);
      }
      updateMute();
    }
  }

  @Override
  public void onStartTrackingTouch(@NonNull Slider slider) {
    // listener only registered in non-editPart mode
    boolean stopMetronome = slider.getId() != R.id.slider_options_mute_mute;
    if (getMetronomeEngine() != null && stopMetronome) {
      getMetronomeEngine().savePlayingState();
      getMetronomeEngine().stop();
    }
  }

  @Override
  public void onStopTrackingTouch(@NonNull Slider slider) {
    // listener only registered in non-editPart mode
    MetronomeEngine metronomeEngine = getMetronomeEngine();
    if (metronomeEngine == null) {
      return;
    }
    boolean stopMetronome = slider.getId() != R.id.slider_options_mute_mute;
    if (stopMetronome) {
      metronomeEngine.restorePlayingState();
    }
    metronomeEngine.maybeUpdateDefaultSong();

    if (slider.getId() == R.id.slider_options_timer_duration && listener != null) {
      listener.onTimerChanged();
    }
  }

  @Nullable
  private MetronomeConfig getConfig() {
    if (editPart) {
      return config;
    } else if (getMetronomeEngine() != null) {
      return getMetronomeEngine().getConfig();
    } else {
      return null;
    }
  }

  @Nullable
  private MetronomeEngine getMetronomeEngine() {
    return activity.getMetronomeEngine();
  }

  public interface OnPartEditListener {
    void onPartAdded(@NonNull Part part);
    void onPartUpdated(@NonNull Part part);
  }

  public interface OnOptionsListener {
    void onModifiersCountChanged();
    void onTimerChanged();
    void onBeatsChanged();
    void onSubsChanged();
  }
}
