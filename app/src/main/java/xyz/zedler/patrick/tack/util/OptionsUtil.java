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

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.databinding.PartialDialogOptionsBinding;
import xyz.zedler.patrick.tack.databinding.PartialOptionsBinding;
import xyz.zedler.patrick.tack.fragment.MainFragment;
import xyz.zedler.patrick.tack.model.MetronomeConfig;

public class OptionsUtil implements OnClickListener, OnButtonCheckedListener,
    OnChangeListener, OnSliderTouchListener {

  private static final String TAG = OptionsUtil.class.getSimpleName();

  private static final String PART = "part_dialog";

  private final MainActivity activity;
  @Nullable
  private final MainFragment fragment;
  private final boolean useDialog, hideSubControlsIfUnused, editPart;
  private Runnable onModifiersCountChanged;
  private OnPartUpdatedListener onPartUpdatedListener;
  private boolean isCountInActive, isIncrementalActive, isTimerActive;
  private boolean isMuteActive, isSubdivisionActive;
  private DialogUtil dialogUtil;
  private PartialOptionsBinding binding;
  private PartialDialogOptionsBinding bindingDialog;
  private Part part;
  private MetronomeConfig config;

  public OptionsUtil(
      MainActivity activity, @NonNull MainFragment fragment, Runnable onModifiersCountChanged
  ) {
    this.activity = activity;
    this.fragment = fragment;
    this.onModifiersCountChanged = onModifiersCountChanged;

    editPart = false;
    useDialog = !UiUtil.isLandTablet(activity);
    if (useDialog) {
      bindingDialog = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
      dialogUtil = new DialogUtil(activity, "options");
    }
    binding = useDialog ? bindingDialog.partialOptions : fragment.getBinding().partialOptions;

    MetronomeConfig config = getConfig();
    isCountInActive = config.isCountInActive();
    isIncrementalActive = config.isIncrementalActive();
    isTimerActive = config.isTimerActive();
    isMuteActive = config.isMuteActive();
    isSubdivisionActive = config.isSubdivisionActive();

    hideSubControlsIfUnused = activity.getSharedPrefs().getBoolean(
        PREF.HIDE_SUB_CONTROLS, DEF.HIDE_SUB_CONTROLS
    );

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
    this.fragment = null;
    this.onPartUpdatedListener = onPartUpdatedListener;

    editPart = true;
    useDialog = true;
    dialogUtil = new DialogUtil(activity, "edit_part");
    bindingDialog = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
    binding = bindingDialog.partialOptions;

    // Hide slider because of separate subdivision controls at the top
    hideSubControlsIfUnused = false;
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
        R.string.label_part_unnamed, part.getPartIndex() + 1
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
    binding.buttonOptionsUseCurrentConfig.setOnClickListener(this);
    binding.buttonOptionsUseCurrentConfig.setVisibility(editPart ? View.VISIBLE : View.GONE);

    updateCountIn();
    updateIncremental();
    updateTimer();
    updateMute();
    updateSubdivisions();
    updateSwing();
  }

  private void updateCountIn() {
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
  }

  private void updateIncremental() {
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
    updateSurfaceToggleButtons(binding.toggleOptionsIncrementalDirection);
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
    updateSurfaceToggleButtons(binding.toggleOptionsIncrementalUnit);
    binding.toggleOptionsIncrementalUnit.setEnabled(isIncrementalActive);

    int incrementalLimit = getConfig().getIncrementalLimit();
    /* When slider should be automatically adjusted to tempo
    int tempo = getMetronomeUtil().getTempo();
    if ((incrementalIncrease && incrementalLimit < tempo)
        || (!incrementalIncrease && incrementalLimit > tempo)
    ) {
      //getMetronomeUtil().setIncrementalLimit(tempo);
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
    updateSurfaceToggleButtons(binding.toggleOptionsTimerUnit);
    binding.toggleOptionsTimerUnit.setEnabled(isTimerActive);
  }

  private void updateMute() {
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
    updateSurfaceToggleButtons(binding.toggleOptionsMuteUnit);
    binding.toggleOptionsMuteUnit.setEnabled(isMuteActive);

    binding.linearOptionsMuteRandom.setOnClickListener(this);
    binding.linearOptionsMuteRandom.setEnabled(isMuteActive);
    binding.linearOptionsMuteRandom.setBackgroundResource(
        useDialog
            ? R.drawable.ripple_list_item_surface_container_high
            : R.drawable.ripple_list_item_bg
    );
    binding.textOptionsMuteRandom.setAlpha(isMuteActive ? 1 : 0.5f);
    binding.checkboxOptionsMuteRandom.setOnCheckedChangeListener(null);
    binding.checkboxOptionsMuteRandom.setChecked(muteRandom);
    binding.checkboxOptionsMuteRandom.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          if (editPart) {
            getConfig().setMuteRandom(isChecked);
          } else {
            getMetronomeUtil().setMuteRandom(isChecked);
          }
          updateMute();
        });
    binding.checkboxOptionsMuteRandom.setEnabled(isMuteActive);

    boolean visibleControls = isMuteActive || !useDialog;
    binding.linearOptionsMuteContainer.setVisibility(visibleControls ? View.VISIBLE : View.GONE);
  }

  public void updateSubdivisions(boolean animated) {
    // Only show if user decided to hide subdivisions when not in use
    binding.linearOptionsSubdivisionsContainer.setVisibility(
        hideSubControlsIfUnused ? View.VISIBLE : View.GONE
    );

    int subdivisionsCount = getConfig().getSubdivisionsCount();
    boolean isSubdivisionActive = getConfig().isSubdivisionActive();
    if (this.isSubdivisionActive != isSubdivisionActive) {
      this.isSubdivisionActive = isSubdivisionActive;
      if (onModifiersCountChanged != null) {
        onModifiersCountChanged.run();
      }
    }

    if (isSubdivisionActive) {
      binding.textOptionsSubdivisions.setText(
          activity.getResources().getQuantityString(
              R.plurals.options_subdivisions_description, subdivisionsCount, subdivisionsCount
          )
      );
    } else {
      binding.textOptionsSubdivisions.setText(R.string.options_inactive);
    }

    binding.sliderOptionsSubdivisions.removeOnChangeListener(this);
    //binding.sliderOptionsSubdivisions.setAnimateNonUserValueChange(animated);
    binding.sliderOptionsSubdivisions.setValue(subdivisionsCount);
    binding.sliderOptionsSubdivisions.addOnChangeListener(this);
    binding.sliderOptionsSubdivisions.setLabelFormatter(value -> {
      int count = (int) value;
      return activity.getResources().getQuantityString(
          R.plurals.options_unit_subdivisions, count, count
      );
    });
  }

  public void updateSubdivisions() {
    updateSubdivisions(false);
  }

  public void updateSwing() {
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
    updateSurfaceToggleButtons(binding.toggleOptionsSwing);
  }

  @Override
  public void onClick(View v) {
    activity.performHapticClick();
    int id = v.getId();
    if (id == R.id.button_options_use_current_config) {
      config = new MetronomeConfig(getMetronomeUtil().getConfig());
      update();
    } else if (id == R.id.button_options_incremental_interval_decrease) {
      int valueFrom = (int) binding.sliderOptionsIncrementalInterval.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalInterval.getValueTo();
      int range = valueTo - valueFrom;
      int decreasedInterval = getConfig().getIncrementalInterval() - range - 1;
      if (editPart) {
        getConfig().setIncrementalInterval(decreasedInterval);
      } else {
        getMetronomeUtil().setIncrementalInterval(decreasedInterval);
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalIntervalDecrease.getIcon());
    } else if (id == R.id.button_options_incremental_interval_increase) {
      int valueFrom = (int) binding.sliderOptionsIncrementalInterval.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalInterval.getValueTo();
      int range = valueTo - valueFrom;
      int increasedInterval = getConfig().getIncrementalInterval() + range + 1;
      if (editPart) {
        getConfig().setIncrementalInterval(increasedInterval);
      } else {
        getMetronomeUtil().setIncrementalInterval(increasedInterval);
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalIntervalIncrease.getIcon());
    } else if (id == R.id.button_options_incremental_limit_decrease) {
      int valueFrom = (int) binding.sliderOptionsIncrementalLimit.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalLimit.getValueTo();
      int range = valueTo - valueFrom;
      int decreasedLimit = getConfig().getIncrementalLimit() - range - 1;
      if (editPart) {
        getConfig().setIncrementalLimit(decreasedLimit);
      } else {
        getMetronomeUtil().setIncrementalLimit(decreasedLimit);
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalLimitDecrease.getIcon());
    } else if (id == R.id.button_options_incremental_limit_increase) {
      int valueFrom = (int) binding.sliderOptionsIncrementalLimit.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalLimit.getValueTo();
      int range = valueTo - valueFrom;
      int increasedLimit = getConfig().getIncrementalLimit() + range + 1;
      if (editPart) {
        getConfig().setIncrementalLimit(increasedLimit);
      } else {
        getMetronomeUtil().setIncrementalLimit(increasedLimit);
      }
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalLimitIncrease.getIcon());
    } else if (id == R.id.button_options_timer_decrease) {
      int valueFrom = (int) binding.sliderOptionsTimerDuration.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTimerDuration.getValueTo();
      int range = valueTo - valueFrom;
      int decreasedDuration = getConfig().getTimerDuration() - range - 1;
      if (editPart) {
        getConfig().setTimerDuration(decreasedDuration);
      } else {
        getMetronomeUtil().setTimerDuration(decreasedDuration, true);
      }
      updateTimer();
      if (!editPart && fragment != null) {
        fragment.updateTimerControls(true, true);
      }
      ViewUtil.startIcon(binding.buttonOptionsTimerDecrease.getIcon());
    } else if (id == R.id.button_options_timer_increase) {
      int valueFrom = (int) binding.sliderOptionsTimerDuration.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTimerDuration.getValueTo();
      int range = valueTo - valueFrom;
      int increasedDuration = getConfig().getTimerDuration() + range + 1;
      if (editPart) {
        getConfig().setTimerDuration(increasedDuration);
      } else {
        getMetronomeUtil().setTimerDuration(increasedDuration, true);
      }
      updateTimer();
      if (!editPart && fragment != null) {
        fragment.updateTimerControls(true, true);
      }
      ViewUtil.startIcon(binding.buttonOptionsTimerIncrease.getIcon());
    } else if (id == R.id.linear_options_mute_random) {
      binding.checkboxOptionsMuteRandom.toggle();
    }
  }

  @Override
  public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
    if (!isChecked) {
      return;
    }
    activity.performHapticClick();
    int groupId = group.getId();
    if (groupId == R.id.toggle_options_incremental_direction) {
      boolean incrementalIncrease = checkedId == R.id.button_options_incremental_increase;
      if (editPart) {
        getConfig().setIncrementalIncrease(incrementalIncrease);
      } else {
        getMetronomeUtil().setIncrementalIncrease(incrementalIncrease);
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
        getConfig().setIncrementalUnit(unit);
      } else {
        getMetronomeUtil().setIncrementalUnit(unit);
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
        getConfig().setTimerUnit(unit);
      } else {
        getMetronomeUtil().setTimerUnit(unit);
      }
      updateTimer();
      if (!editPart && fragment != null) {
        fragment.updateTimerDisplay();
      }
    } else if (groupId == R.id.toggle_options_mute_unit) {
      String unit = UNIT.BARS;
      if (checkedId == R.id.button_options_timer_unit_seconds) {
        unit = UNIT.SECONDS;
      }
      if (editPart) {
        getConfig().setMuteUnit(unit);
      } else {
        getMetronomeUtil().setMuteUnit(unit);
      }
      updateMute();
    } else if (groupId == R.id.toggle_options_swing) {
      if (checkedId == R.id.button_options_swing_3) {
        if (editPart) {
          getConfig().setSwing3();
        } else {
          getMetronomeUtil().setSwing3();
        }
      } else if (checkedId == R.id.button_options_swing_5) {
        if (editPart) {
          getConfig().setSwing5();
        } else {
          getMetronomeUtil().setSwing5();
        }
      } else if (checkedId == R.id.button_options_swing_7) {
        if (editPart) {
          getConfig().setSwing7();
        } else {
          getMetronomeUtil().setSwing7();
        }
      }
      updateSubdivisions(true);
      updateSwing();
      if (!editPart && fragment != null) {
        fragment.updateSubs(getConfig().getSubdivisions());
        fragment.updateSubControls(true);
      }
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_options_count_in) {
      activity.performHapticSegmentTick(slider, false);
      if (editPart) {
        getConfig().setCountIn((int) value);
      } else {
        getMetronomeUtil().setCountIn((int) value);
      }
      updateCountIn();
    } else if (id == R.id.slider_options_incremental_amount) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        getConfig().setIncrementalAmount((int) value);
      } else {
        getMetronomeUtil().setIncrementalAmount((int) value);
      }
      updateIncremental();
    } else if (id == R.id.slider_options_incremental_interval) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        getConfig().setIncrementalInterval((int) value);
      } else {
        getMetronomeUtil().setIncrementalInterval((int) value);
      }
      updateIncremental();
    } else if (id == R.id.slider_options_incremental_limit) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        getConfig().setIncrementalLimit((int) value);
      } else {
        getMetronomeUtil().setIncrementalLimit((int) value);
      }
      updateIncremental();
    } else if (id == R.id.slider_options_timer_duration) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        getConfig().setTimerDuration((int) value);
      } else if (fragment != null) {
        getMetronomeUtil().setTimerDuration((int) value, true);
        fragment.updateTimerControls(true, true);
      }
      updateTimer();
    } else if (id == R.id.slider_options_mute_play) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        getConfig().setMutePlay((int) value);
      } else {
        getMetronomeUtil().setMutePlay((int) value);
      }
      updateMute();
    } else if (id == R.id.slider_options_mute_mute) {
      activity.performHapticSegmentTick(slider, true);
      if (editPart) {
        getConfig().setMuteMute((int) value);
      } else {
        getMetronomeUtil().setMuteMute((int) value);
      }
      updateMute();
    } else if (id == R.id.slider_options_subdivisions) {
      activity.performHapticSegmentTick(slider, true);
      int oldCount = getConfig().getSubdivisionsCount();
      int newCount = (int) value;
      int diff = newCount - oldCount;
      for (int i = 0; i < Math.abs(diff); i++) {
        if (diff > 0) {
          // only MetronomeUtil required as subdivision slider is hidden in editPart mode
          getMetronomeUtil().addSubdivision();
        } else {
          getMetronomeUtil().removeSubdivision();
        }
      }
      updateSubdivisions();
      updateSwing();
      if (!editPart && fragment != null) {
        fragment.updateSubs(getConfig().getSubdivisions());
        fragment.updateSubControls(true);
      }
    }
  }

  @Override
  public void onStartTrackingTouch(@NonNull Slider slider) {
    // listener only registered in non-editPart mode
    getMetronomeUtil().savePlayingState();
    getMetronomeUtil().stop();
  }

  @Override
  public void onStopTrackingTouch(@NonNull Slider slider) {
    // listener only registered in non-editPart mode
    getMetronomeUtil().restorePlayingState();
  }

  private MetronomeConfig getConfig() {
    if (editPart) {
      return config;
    } else {
      return getMetronomeUtil().getConfig();
    }
  }

  private MetronomeUtil getMetronomeUtil() {
    return activity.getMetronomeUtil();
  }

  private void updateSurfaceToggleButtons(MaterialButtonToggleGroup toggleGroup) {
    if (useDialog) {
      int strokeColor = ResUtil.getColor(activity, R.attr.colorOutlineVariant);
      int bgColorChecked = ResUtil.getColor(activity, R.attr.colorPrimary);
      int bgColorUnchecked = ResUtil.getColor(activity, R.attr.colorSurfaceContainerHigh);
      for (int i = 0; i < toggleGroup.getChildCount(); i++) {
        View child = toggleGroup.getChildAt(i);
        MaterialButton button = (MaterialButton) child;
        button.setStrokeColor(ColorStateList.valueOf(strokeColor));
        if (button.getId() != toggleGroup.getCheckedButtonId()) {
          button.setStrokeWidth(UiUtil.dpToPx(activity, 1));
          button.setBackgroundTintList(ColorStateList.valueOf(bgColorUnchecked));
        } else {
          button.setStrokeWidth(0);
          button.setBackgroundTintList(ColorStateList.valueOf(bgColorChecked));
        }
      }
    }
  }

  public interface OnPartUpdatedListener {
    void onPartUpdated(@NonNull Part part);
  }
}
