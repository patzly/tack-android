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

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
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

  private final MainActivity activity;
  private final boolean useDialog, editPart;
  private Runnable onModifiersCountChanged, onTimerChanged, onSubsChanged;
  private OnPartUpdatedListener onPartUpdatedListener;
  private boolean isCountInActive, isIncrementalActive, isTimerActive, isMuteActive;
  private boolean isInitialized;
  private DialogUtil dialogUtil;
  private PartialOptionsBinding binding;
  private PartialDialogOptionsBinding bindingDialog;
  private Part part;
  private MetronomeConfig config;

  public OptionsUtil(
      MainActivity activity, FragmentMainBinding fragmentBinding,
      Runnable onModifiersCountChanged, Runnable onTimerChanged, Runnable onSubsChanged
  ) {
    this.activity = activity;
    this.onModifiersCountChanged = onModifiersCountChanged;
    this.onTimerChanged = onTimerChanged;
    this.onSubsChanged = onSubsChanged;

    editPart = false;
    useDialog = !UiUtil.isLandTablet(activity);
    if (useDialog) {
      bindingDialog = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
      dialogUtil = new DialogUtil(activity, "options");
    }
    binding = useDialog ? bindingDialog.partialOptions : fragmentBinding.partialOptions;

    if (binding != null) {
      binding.sliderOptionsIncrementalAmount.addOnSliderTouchListener(this);
      binding.sliderOptionsIncrementalInterval.addOnSliderTouchListener(this);
      binding.sliderOptionsTimerDuration.addOnSliderTouchListener(this);
      binding.sliderOptionsMutePlay.addOnSliderTouchListener(this);
      binding.sliderOptionsMuteMute.addOnSliderTouchListener(this);
    }

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

  public OptionsUtil(MainActivity activity, OnPartUpdatedListener onPartUpdatedListener) {
    this.activity = activity;
    this.onPartUpdatedListener = onPartUpdatedListener;

    editPart = true;
    useDialog = true;
    dialogUtil = new DialogUtil(activity, "edit_part");
    bindingDialog = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
    binding = bindingDialog.partialOptions;
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
      if (part != null) {
        setPart(part);
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
      if (editPart) {
        outState.putParcelable(PART, part);
      }
    }
  }

  public void setPart(@NonNull Part part) {
    this.part = part;
    config = part.toConfig();

    bindingDialog = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
    binding = bindingDialog.partialOptions;

    String title = activity.getString(
        R.string.label_part_edit, part.getPartIndex() + 1
    );
    dialogUtil.createDialog(builder -> {
      builder.setTitle(title);
      builder.setView(bindingDialog.getRoot());
      builder.setPositiveButton(R.string.action_apply, (dialog, which) -> {
        activity.performHapticClick();
        if (onPartUpdatedListener != null) {
          Part partResult = new Part(part);
          partResult.setConfig(config);
          onPartUpdatedListener.onPartUpdated(partResult);
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
    if (useDialog) {
      bindingDialog.scrollOptions.scrollTo(0, 0);
    }
    if (binding == null) {
      return;
    }

    binding.linearOptionsEditPartContainer.setVisibility(editPart ? View.VISIBLE : View.GONE);
    binding.linearOptionsUseCurrentConfig.setOnClickListener(this);
    updateTempo();
    updateBeats();
    updateSubdivisions();
    updateCountIn();
    updateIncremental();
    updateTimer();
    updateMute();
    updateSwing();
  }

  private void updateTempo() {
    if (!editPart || getConfig() == null) {
      return;
    }
    int tempo = getConfig().getTempo();
    binding.textOptionsTempo.setText(activity.getString(R.string.label_bpm_value, tempo));

    int tempoFrom = (int) binding.sliderOptionsTempo.getValueFrom();
    int tempoTo = (int) binding.sliderOptionsTempo.getValueTo();
    int tempoRange = tempoTo - tempoFrom;

    // Calculate current range
    int tempoFactor = (tempo - 1) / (tempoRange + 1);
    int tempoFromNew = 1 + tempoFactor * (tempoRange + 1);
    int tempoToNew = tempoFromNew + tempoRange;

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
    binding.sliderOptionsTempo.setValueFrom(tempoFromNew);
    binding.sliderOptionsTempo.setValueTo(tempoToNew);
    int tempoSafe = Math.max(tempoFromNew, Math.min(tempo, tempoToNew));
    binding.sliderOptionsTempo.setValue(tempoSafe);
    binding.sliderOptionsTempo.addOnChangeListener(this);
    binding.sliderOptionsTempo.setLabelFormatter(
        value -> activity.getString(R.string.label_bpm_value, value)
    );
  }

  private void updateBeats() {
    if (!editPart || getConfig() == null) {
      return;
    }
    String[] beats = getConfig().getBeats();
    String[] currentBeats = new String[binding.linearOptionsBeats.getChildCount()];
    for (int i = 0; i < binding.linearOptionsBeats.getChildCount(); i++) {
      currentBeats[i] = String.valueOf(binding.linearOptionsBeats.getChildAt(i));
    }
    if (Arrays.equals(beats, currentBeats)) {
      return;
    }
    binding.linearOptionsBeats.removeAllViews();
    for (int i = 0; i < beats.length; i++) {
      String tickType = beats[i];
      BeatView beatView = new BeatView(activity);
      beatView.setTickType(tickType);
      beatView.setIndex(i);
      beatView.setOnClickListener(beat -> {
        activity.performHapticClick();
        getConfig().setBeat(beatView.getIndex(), beatView.nextTickType());
      });
      binding.linearOptionsBeats.addView(beatView);
    }
    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizOptionsBeats);
    binding.linearOptionsBeats.post(() -> {
      if (binding == null) {
        return;
      }
      LayoutTransition transition = new LayoutTransition();
      transition.setDuration(Constants.ANIM_DURATION_LONG);
      binding.linearOptionsBeats.setLayoutTransition(transition);
    });
    updateBeatControls();
  }

  private void updateBeatControls() {
    if (!editPart || getConfig() == null) {
      return;
    }
    int beatsCount = getConfig().getBeatsCount();
    binding.textOptionsBeats.setText(
        activity.getResources().getQuantityString(
            R.plurals.options_beats_subs_description, beatsCount, beatsCount
        )
    );
    binding.buttonOptionsBeatsAdd.setOnClickListener(this);
    binding.buttonOptionsBeatsAdd.setEnabled(beatsCount < Constants.BEATS_MAX);
    binding.buttonOptionsBeatsRemove.setOnClickListener(this);
    binding.buttonOptionsBeatsRemove.setEnabled(beatsCount > 1);
  }

  private void updateSubdivisions() {
    if (!editPart || getConfig() == null) {
      return;
    }
    String[] subdivisions = getConfig().getSubdivisions();
    String[] currentSubs = new String[binding.linearOptionsSubs.getChildCount()];
    for (int i = 0; i < binding.linearOptionsSubs.getChildCount(); i++) {
      currentSubs[i] = String.valueOf(binding.linearOptionsSubs.getChildAt(i));
    }
    if (Arrays.equals(subdivisions, currentSubs)) {
      return;
    }
    binding.linearOptionsSubs.removeAllViews();
    for (int i = 0; i < subdivisions.length; i++) {
      String tickType = subdivisions[i];
      BeatView beatView = new BeatView(activity);
      beatView.setIsSubdivision(true);
      beatView.setTickType(i == 0 ? TICK_TYPE.MUTED : tickType);
      beatView.setIndex(i);
      if (i > 0) {
        beatView.setOnClickListener(beat -> {
          if (getConfig() != null) {
            activity.performHapticClick();
            getConfig().setSubdivision(beatView.getIndex(), beatView.nextTickType());
          }
        });
      }
      binding.linearOptionsSubs.addView(beatView);
    }
    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizOptionsSubs, true);
    binding.linearOptionsSubs.post(() -> {
      if (binding == null) {
        return;
      }
      LayoutTransition transition = new LayoutTransition();
      transition.setDuration(Constants.ANIM_DURATION_LONG);
      binding.linearOptionsSubs.setLayoutTransition(transition);
    });
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
              R.plurals.options_beats_subs_description, subdivisionsCount, subdivisionsCount
          )
      );
    } else {
      binding.textOptionsSubs.setText(R.string.options_inactive);
    }
    binding.buttonOptionsSubsAdd.setOnClickListener(this);
    binding.buttonOptionsSubsAdd.setEnabled(subdivisionsCount < Constants.SUBS_MAX);
    binding.buttonOptionsSubsRemove.setOnClickListener(this);
    binding.buttonOptionsSubsRemove.setEnabled(subdivisionsCount > 1);
  }

  private void updateCountIn() {
    if (getConfig() == null) {
      return;
    }
    boolean isCountInActive = getConfig().isCountInActive();
    if (this.isCountInActive != isCountInActive) {
      this.isCountInActive = isCountInActive;
      if (onModifiersCountChanged != null) {
        onModifiersCountChanged.run();
      }
    }
    int countIn = getConfig().getCountIn();
    binding.sliderOptionsCountIn.removeOnChangeListener(this);
    binding.sliderOptionsCountIn.setValue(countIn);
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
      if (onModifiersCountChanged != null) {
        onModifiersCountChanged.run();
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

    binding.sliderOptionsIncrementalAmount.removeOnChangeListener(this);
    binding.sliderOptionsIncrementalAmount.setValue(incrementalAmount);
    binding.sliderOptionsIncrementalAmount.addOnChangeListener(this);
    binding.sliderOptionsIncrementalAmount.setLabelFormatter(
        value -> activity.getString(R.string.label_bpm_value, (int) value)
    );

    boolean visibleControls = isIncrementalActive || !useDialog;
    binding.linearMainIncrementalContainer.setVisibility(
        visibleControls ? View.VISIBLE : View.GONE
    );

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

    int intervalFrom = (int) binding.sliderOptionsIncrementalInterval.getValueFrom();
    int intervalTo = (int) binding.sliderOptionsIncrementalInterval.getValueTo();
    int intervalRange = intervalTo - intervalFrom;

    // Calculate current range
    int intervalFactor = (incrementalInterval - 1) / (intervalRange + 1);
    int intervalFromNew = 1 + intervalFactor * (intervalRange + 1);
    int intervalToNew = intervalFromNew + intervalRange;

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
    binding.sliderOptionsIncrementalInterval.setValueFrom(intervalFromNew);
    binding.sliderOptionsIncrementalInterval.setValueTo(intervalToNew);
    int incrementalIntervalSafe = Math.max(
        intervalFromNew, Math.min(incrementalInterval, intervalToNew)
    );
    binding.sliderOptionsIncrementalInterval.setValue(incrementalIntervalSafe);
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

    int valueFrom = (int) binding.sliderOptionsIncrementalLimit.getValueFrom();
    int valueTo = (int) binding.sliderOptionsIncrementalLimit.getValueTo();
    int range = valueTo - valueFrom;

    // Calculate current range
    int factor = incrementalLimit / (range + 1);
    int valueFromNew = factor * (range + 1);
    int valueToNew = valueFromNew + range;

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
    binding.sliderOptionsIncrementalLimit.setValueFrom(valueFromNew);
    binding.sliderOptionsIncrementalLimit.setValueTo(valueToNew);
    int incrementalLimitSafe = Math.max(valueFromNew, Math.min(incrementalLimit, valueToNew));
    binding.sliderOptionsIncrementalLimit.setValue(incrementalLimitSafe);
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
      if (onModifiersCountChanged != null) {
        onModifiersCountChanged.run();
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

    int valueFrom = (int) binding.sliderOptionsTimerDuration.getValueFrom();
    int valueTo = (int) binding.sliderOptionsTimerDuration.getValueTo();
    int range = valueTo - valueFrom;

    // Calculate current range
    int factor = timerDuration / (range + 1);
    int valueFromNew = factor * (range + 1);
    int valueToNew = valueFromNew + range;

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
    binding.sliderOptionsTimerDuration.setValueFrom(valueFromNew);
    binding.sliderOptionsTimerDuration.setValueTo(valueToNew);
    int timerDurationSafe = Math.max(valueFromNew, Math.min(timerDuration, valueToNew));
    binding.sliderOptionsTimerDuration.setValue(timerDurationSafe);
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

    boolean visibleControls = isTimerActive || !useDialog;
    binding.linearOptionsTimerContainer.setVisibility(visibleControls ? View.VISIBLE : View.GONE);

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
    boolean muteRandom = getConfig().isMuteRandom();
    boolean isMuteActive = getConfig().isMuteActive();
    if (this.isMuteActive != isMuteActive) {
      this.isMuteActive = isMuteActive;
      if (onModifiersCountChanged != null) {
        onModifiersCountChanged.run();
      }
    }
    int resIdPlay, resIdMute, resIdLabel, checkedId;
    if (muteUnit.equals(UNIT.SECONDS)) {
      resIdPlay = R.plurals.options_mute_play_seconds;
      resIdMute = R.plurals.options_mute_mute_seconds;
      resIdLabel = R.plurals.options_unit_seconds;
      checkedId = R.id.button_options_mute_unit_seconds;
    } else {
      resIdPlay = R.plurals.options_mute_play_bars;
      resIdMute = R.plurals.options_mute_mute_bars;
      resIdLabel = R.plurals.options_unit_bars;
      checkedId = R.id.button_options_mute_unit_bars;
    }
    if (isMuteActive) {
      binding.textOptionsMutePlay.setText(
          activity.getResources().getQuantityString(resIdPlay, mutePlay, mutePlay)
      );
    } else {
      binding.textOptionsMutePlay.setText(R.string.options_inactive);
    }

    binding.sliderOptionsMutePlay.removeOnChangeListener(this);
    binding.sliderOptionsMutePlay.setValue(mutePlay);
    binding.sliderOptionsMutePlay.addOnChangeListener(this);
    binding.sliderOptionsMutePlay.setLabelFormatter(value -> {
      int play = (int) value;
      return activity.getResources().getQuantityString(resIdLabel, play, play);
    });

    binding.textOptionsMuteMute.setText(
        activity.getResources().getQuantityString(resIdMute, muteMute, muteMute)
    );
    binding.textOptionsMuteMute.setAlpha(isMuteActive ? 1 : 0.5f);

    binding.sliderOptionsMuteMute.removeOnChangeListener(this);
    binding.sliderOptionsMuteMute.setValue(muteMute);
    binding.sliderOptionsMuteMute.addOnChangeListener(this);
    binding.sliderOptionsMuteMute.setLabelFormatter(value -> {
      int mute = (int) value;
      return activity.getResources().getQuantityString(resIdLabel, mute, mute);
    });
    binding.sliderOptionsMuteMute.setEnabled(isMuteActive);

    binding.toggleOptionsMuteUnit.removeOnButtonCheckedListener(this);
    binding.toggleOptionsMuteUnit.check(checkedId);
    binding.toggleOptionsMuteUnit.addOnButtonCheckedListener(this);
    binding.toggleOptionsMuteUnit.setEnabled(isMuteActive);

    binding.linearOptionsMuteRandom.setOnClickListener(this);
    binding.linearOptionsMuteRandom.setEnabled(isMuteActive);
    binding.linearOptionsMuteRandom.setBackgroundResource(
        useDialog
            ? R.drawable.ripple_list_item_surface_bright
            : R.drawable.ripple_list_item_bg
    );
    binding.textOptionsMuteRandom.setAlpha(isMuteActive ? 1 : 0.5f);
    binding.checkboxOptionsMuteRandom.setOnCheckedChangeListener(null);
    binding.checkboxOptionsMuteRandom.setChecked(muteRandom);
    binding.checkboxOptionsMuteRandom.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (editPart && getConfig() != null) {
            getConfig().setMuteRandom(isChecked);
          } else if (getMetronomeEngine() != null) {
            getMetronomeEngine().setMuteRandom(isChecked);
          }
          updateMute();
        });
    binding.checkboxOptionsMuteRandom.setEnabled(isMuteActive);

    boolean visibleControls = isMuteActive || !useDialog;
    binding.linearOptionsMuteContainer.setVisibility(visibleControls ? View.VISIBLE : View.GONE);
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

  @Override
  public void onClick(View v) {
    MetronomeConfig config = getConfig();
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    if (config == null || metronomeEngine == null) {
      return;
    }
    activity.performHapticClick();
    int id = v.getId();
    if (id == R.id.linear_options_use_current_config) {
      this.config = new MetronomeConfig(metronomeEngine.getConfig());
      update();
    } else if (id == R.id.button_options_tempo_decrease) {
      int valueFrom = (int) binding.sliderOptionsTempo.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTempo.getValueTo();
      int range = valueTo - valueFrom;
      int decreasedTempo = config.getTempo() - range - 1;
      if (editPart) {
        config.setTempo(decreasedTempo);
      } else {
        metronomeEngine.setTempo(decreasedTempo);
      }
      updateTempo();
      ViewUtil.startIcon(binding.buttonOptionsTempoDecrease.getIcon());
    } else if (id == R.id.button_options_tempo_increase) {
      int valueFrom = (int) binding.sliderOptionsTempo.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTempo.getValueTo();
      int range = valueTo - valueFrom;
      int increasedTempo = config.getTempo() + range + 1;
      if (editPart) {
        config.setTempo(increasedTempo);
      } else {
        metronomeEngine.setTempo(increasedTempo);
      }
      updateTempo();
      ViewUtil.startIcon(binding.buttonOptionsTempoIncrease.getIcon());
    } else if (id == R.id.button_options_beats_add) {
      ViewUtil.startIcon(binding.buttonOptionsBeatsAdd.getIcon());
      activity.performHapticClick();
      boolean success = config.addBeat();
      if (success) {
        BeatView beatView = new BeatView(activity);
        beatView.setIndex(binding.linearOptionsBeats.getChildCount());
        beatView.setOnClickListener(beat -> {
          if (getConfig() != null) {
            activity.performHapticClick();
            config.setBeat(beatView.getIndex(), beatView.nextTickType());
          }
        });
        binding.linearOptionsBeats.addView(beatView);
        ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizOptionsBeats);
        updateBeatControls();
      }
    } else if (id == R.id.button_options_beats_remove) {
      ViewUtil.startIcon(binding.buttonOptionsBeatsRemove.getIcon());
      activity.performHapticClick();
      boolean success = config.removeBeat();
      if (success) {
        binding.linearOptionsBeats.removeViewAt(
            binding.linearOptionsBeats.getChildCount() - 1
        );
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizOptionsBeats, true
        );
        updateBeatControls();
      }
    } else if (id == R.id.button_options_subs_add) {
      ViewUtil.startIcon(binding.buttonOptionsSubsAdd.getIcon());
      activity.performHapticClick();
      boolean success = config.addSubdivision();
      if (success) {
        BeatView beatView = new BeatView(activity);
        beatView.setIsSubdivision(true);
        beatView.setIndex(binding.linearOptionsSubs.getChildCount());
        beatView.setOnClickListener(subdivision -> {
          if (getConfig() != null) {
            activity.performHapticClick();
            getConfig().setSubdivision(beatView.getIndex(), beatView.nextTickType());
          }
        });
        binding.linearOptionsSubs.addView(beatView);
        ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizOptionsSubs);
        updateSubControls();
      }
    } else if (id == R.id.button_options_subs_remove) {
      ViewUtil.startIcon(binding.buttonOptionsSubsRemove.getIcon());
      activity.performHapticClick();
      boolean success = config.removeSubdivision();
      if (success) {
        binding.linearOptionsSubs.removeViewAt(binding.linearOptionsSubs.getChildCount() - 1);
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizOptionsSubs, true
        );
        updateSubControls();
      }
    } else if (id == R.id.button_options_incremental_interval_decrease) {
      int valueFrom = (int) binding.sliderOptionsIncrementalInterval.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalInterval.getValueTo();
      int range = valueTo - valueFrom;
      int decreasedInterval = config.getIncrementalInterval() - range - 1;
      if (editPart) {
        config.setIncrementalInterval(decreasedInterval);
      } else {
        metronomeEngine.setIncrementalInterval(decreasedInterval);
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalIntervalDecrease.getIcon());
    } else if (id == R.id.button_options_incremental_interval_increase) {
      int valueFrom = (int) binding.sliderOptionsIncrementalInterval.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalInterval.getValueTo();
      int range = valueTo - valueFrom;
      int increasedInterval = config.getIncrementalInterval() + range + 1;
      if (editPart) {
        config.setIncrementalInterval(increasedInterval);
      } else {
        metronomeEngine.setIncrementalInterval(increasedInterval);
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalIntervalIncrease.getIcon());
    } else if (id == R.id.button_options_incremental_limit_decrease) {
      int valueFrom = (int) binding.sliderOptionsIncrementalLimit.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalLimit.getValueTo();
      int range = valueTo - valueFrom;
      int decreasedLimit = config.getIncrementalLimit() - range - 1;
      if (editPart) {
        config.setIncrementalLimit(decreasedLimit);
      } else {
        metronomeEngine.setIncrementalLimit(decreasedLimit);
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalLimitDecrease.getIcon());
    } else if (id == R.id.button_options_incremental_limit_increase) {
      int valueFrom = (int) binding.sliderOptionsIncrementalLimit.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalLimit.getValueTo();
      int range = valueTo - valueFrom;
      int increasedLimit = config.getIncrementalLimit() + range + 1;
      if (editPart) {
        config.setIncrementalLimit(increasedLimit);
      } else {
        metronomeEngine.setIncrementalLimit(increasedLimit);
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalLimitIncrease.getIcon());
    } else if (id == R.id.button_options_timer_decrease) {
      int valueFrom = (int) binding.sliderOptionsTimerDuration.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTimerDuration.getValueTo();
      int range = valueTo - valueFrom;
      int decreasedDuration = config.getTimerDuration() - range - 1;
      if (editPart) {
        config.setTimerDuration(decreasedDuration);
      } else {
        metronomeEngine.setTimerDuration(decreasedDuration, true);
      }
      updateTimer();
      if (!editPart && onTimerChanged != null) {
        onTimerChanged.run();
      }
      ViewUtil.startIcon(binding.buttonOptionsTimerDecrease.getIcon());
    } else if (id == R.id.button_options_timer_increase) {
      int valueFrom = (int) binding.sliderOptionsTimerDuration.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTimerDuration.getValueTo();
      int range = valueTo - valueFrom;
      int increasedDuration = config.getTimerDuration() + range + 1;
      if (editPart) {
        config.setTimerDuration(increasedDuration);
      } else {
        metronomeEngine.setTimerDuration(increasedDuration, true);
      }
      updateTimer();
      if (!editPart && onTimerChanged != null) {
        onTimerChanged.run();
      }
      ViewUtil.startIcon(binding.buttonOptionsTimerIncrease.getIcon());
    } else if (id == R.id.linear_options_mute_random) {
      binding.checkboxOptionsMuteRandom.toggle();
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
      }
      updateTimer();
      if (!editPart && onTimerChanged != null) {
        onTimerChanged.run();
      }
    } else if (groupId == R.id.toggle_options_mute_unit) {
      String unit = UNIT.BARS;
      if (checkedId == R.id.button_options_mute_unit_seconds) {
        unit = UNIT.SECONDS;
      }
      if (editPart) {
        config.setMuteUnit(unit);
      } else {
        metronomeEngine.setMuteUnit(unit);
      }
      updateMute();
    } else if (groupId == R.id.toggle_options_swing) {
      if (checkedId == R.id.button_options_swing_3) {
        if (editPart) {
          config.setSwing3();
        } else {
          metronomeEngine.setSwing3();
        }
      } else if (checkedId == R.id.button_options_swing_5) {
        if (editPart) {
          config.setSwing5();
        } else {
          metronomeEngine.setSwing5();
        }
      } else if (checkedId == R.id.button_options_swing_7) {
        if (editPart) {
          config.setSwing7();
        } else {
          metronomeEngine.setSwing7();
        }
      }
      updateSwing();
      updateSubdivisions();
      if (!editPart && onSubsChanged != null) {
        onSubsChanged.run();
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
        metronomeEngine.setTimerDuration((int) value, true);
        if (onTimerChanged != null) {
          onTimerChanged.run();
        }
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
    if (getMetronomeEngine() != null) {
      getMetronomeEngine().savePlayingState();
      getMetronomeEngine().stop();
    }
  }

  @Override
  public void onStopTrackingTouch(@NonNull Slider slider) {
    // listener only registered in non-editPart mode
    if (getMetronomeEngine() != null) {
      getMetronomeEngine().restorePlayingState();
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

  public interface OnPartUpdatedListener {
    void onPartUpdated(@NonNull Part part);
  }
}
