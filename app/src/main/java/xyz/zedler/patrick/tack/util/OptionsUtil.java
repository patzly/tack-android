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
import xyz.zedler.patrick.tack.databinding.PartialDialogOptionsBinding;
import xyz.zedler.patrick.tack.databinding.PartialOptionsBinding;
import xyz.zedler.patrick.tack.fragment.MainFragment;

public class OptionsUtil implements OnClickListener, OnButtonCheckedListener,
    OnChangeListener, OnSliderTouchListener {

  private static final String TAG = OptionsUtil.class.getSimpleName();

  private final MainActivity activity;
  private final MainFragment fragment;
  private final PartialOptionsBinding binding;
  private final boolean useDialog, hideSubControls;
  private final Runnable onModifiersCountChanged;
  private boolean isCountInActive, isIncrementalActive, isTimerActive;
  private boolean isMuteActive, isSubdivisionActive;
  private DialogUtil dialogUtil;
  private PartialDialogOptionsBinding bindingDialog;

  public OptionsUtil(
      MainActivity activity, MainFragment fragment, Runnable onModifiersCountChanged
  ) {
    this.activity = activity;
    this.fragment = fragment;
    this.onModifiersCountChanged = onModifiersCountChanged;

    useDialog = !UiUtil.isLandTablet(activity);
    if (useDialog) {
      bindingDialog = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
      dialogUtil = new DialogUtil(activity, "options");
    }
    binding = useDialog ? bindingDialog.partialOptions : fragment.getBinding().partialOptions;

    isCountInActive = getMetronomeUtil().isCountInActive();
    isIncrementalActive = getMetronomeUtil().isIncrementalActive();
    isTimerActive = getMetronomeUtil().isTimerActive();
    isMuteActive = getMetronomeUtil().isMuteActive();
    isSubdivisionActive = getMetronomeUtil().isSubdivisionActive();

    hideSubControls = activity.getSharedPrefs().getBoolean(
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

  public void show() {
    update();
    if (useDialog) {
      dialogUtil.show();
    }
  }

  public void showIfWasShown(@Nullable Bundle state) {
    update();
    if (useDialog) {
      dialogUtil.showIfWasShown(state);
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
    }
  }

  public void update() {
    if (useDialog) {
      bindingDialog.scrollOptions.scrollTo(0, 0);
    }
    updateCountIn();
    updateIncremental();
    updateTimer();
    updateMute();
    updateSubdivisions();
    updateSwing();
  }

  private void updateCountIn() {
    boolean isCountInActive = getMetronomeUtil().isCountInActive();
    if (this.isCountInActive != isCountInActive) {
      this.isCountInActive = isCountInActive;
      onModifiersCountChanged.run();
    }
    int countIn = getMetronomeUtil().getCountIn();
    binding.sliderOptionsCountIn.removeOnChangeListener(this);
    binding.sliderOptionsCountIn.setValue(countIn);
    binding.sliderOptionsCountIn.addOnChangeListener(this);
    binding.sliderOptionsCountIn.setLabelFormatter(
        value -> activity.getResources().getQuantityString(
            R.plurals.options_unit_bars, (int) value, (int) value
        )
    );
    if (getMetronomeUtil().isCountInActive()) {
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
    int incrementalAmount = getMetronomeUtil().getIncrementalAmount();
    boolean incrementalIncrease = getMetronomeUtil().isIncrementalIncrease();
    boolean isIncrementalActive = getMetronomeUtil().isIncrementalActive();
    if (this.isIncrementalActive != isIncrementalActive) {
      this.isIncrementalActive = isIncrementalActive;
      onModifiersCountChanged.run();
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

    int incrementalInterval = getMetronomeUtil().getIncrementalInterval();
    String incrementalUnit = getMetronomeUtil().getIncrementalUnit();
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

    binding.sliderOptionsIncrementalInterval.removeOnChangeListener(this);
    binding.sliderOptionsIncrementalInterval.setValue(incrementalInterval);
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

    int incrementalLimit = getMetronomeUtil().getIncrementalLimit();
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
    int timerDuration = getMetronomeUtil().getTimerDuration();
    boolean isTimerActive = getMetronomeUtil().isTimerActive();
    if (this.isTimerActive != isTimerActive) {
      this.isTimerActive = isTimerActive;
      onModifiersCountChanged.run();
    }
    String timerUnit = getMetronomeUtil().getTimerUnit();
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
    int mutePlay = getMetronomeUtil().getMutePlay();
    int muteMute = getMetronomeUtil().getMuteMute();
    String muteUnit = getMetronomeUtil().getMuteUnit();
    boolean muteRandom = getMetronomeUtil().isMuteRandom();
    boolean isMuteActive = getMetronomeUtil().isMuteActive();
    if (this.isMuteActive != isMuteActive) {
      this.isMuteActive = isMuteActive;
      onModifiersCountChanged.run();
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
    binding.checkboxOptionsMuteRandom.setOnCheckedChangeListener((buttonView, isChecked) -> {
      getMetronomeUtil().setMuteRandom(isChecked);
      updateMute();
    });
    binding.checkboxOptionsMuteRandom.setEnabled(isMuteActive);

    boolean visibleControls = isMuteActive || !useDialog;
    binding.linearOptionsMuteContainer.setVisibility(visibleControls ? View.VISIBLE : View.GONE);
  }

  public void updateSubdivisions(boolean animated) {
    // Only show if user decided to hide subdivisions when not in use
    binding.linearOptionsSubdivisionsContainer.setVisibility(
        hideSubControls ? View.VISIBLE : View.GONE
    );

    int subdivisionsCount = getMetronomeUtil().getSubdivisionsCount();
    boolean isSubdivisionActive = getMetronomeUtil().isSubdivisionActive();
    if (this.isSubdivisionActive != isSubdivisionActive) {
      this.isSubdivisionActive = isSubdivisionActive;
      onModifiersCountChanged.run();
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
    boolean isSwingActive = getMetronomeUtil().isSwingActive();

    binding.textOptionsSwing.setText(
        isSwingActive ? R.string.options_swing_description : R.string.options_inactive
    );

    binding.toggleOptionsSwing.removeOnButtonCheckedListener(this);
    if (getMetronomeUtil().isSwing3()) {
      binding.toggleOptionsSwing.check(R.id.button_options_swing_3);
    } else if (getMetronomeUtil().isSwing5()) {
      binding.toggleOptionsSwing.check(R.id.button_options_swing_5);
    } else if (getMetronomeUtil().isSwing7()) {
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
    if (id == R.id.button_options_incremental_limit_decrease) {
      int valueFrom = (int) binding.sliderOptionsIncrementalLimit.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalLimit.getValueTo();
      int range = valueTo - valueFrom;
      getMetronomeUtil().setIncrementalLimit(getMetronomeUtil().getIncrementalLimit() - range - 1);
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalLimitDecrease.getIcon());
    } else if (id == R.id.button_options_incremental_limit_increase) {
      int valueFrom = (int) binding.sliderOptionsIncrementalLimit.getValueFrom();
      int valueTo = (int) binding.sliderOptionsIncrementalLimit.getValueTo();
      int range = valueTo - valueFrom;
      getMetronomeUtil().setIncrementalLimit(getMetronomeUtil().getIncrementalLimit() + range + 1);
      updateIncremental();
      ViewUtil.startIcon(binding.buttonOptionsIncrementalLimitIncrease.getIcon());
    } else if (id == R.id.button_options_timer_decrease) {
      int valueFrom = (int) binding.sliderOptionsTimerDuration.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTimerDuration.getValueTo();
      int range = valueTo - valueFrom;
      getMetronomeUtil().setTimerDuration(
          getMetronomeUtil().getTimerDuration() - range - 1, true
      );
      updateTimer();
      fragment.updateTimerControls(true, true);
      ViewUtil.startIcon(binding.buttonOptionsTimerDecrease.getIcon());
    } else if (id == R.id.button_options_timer_increase) {
      int valueFrom = (int) binding.sliderOptionsTimerDuration.getValueFrom();
      int valueTo = (int) binding.sliderOptionsTimerDuration.getValueTo();
      int range = valueTo - valueFrom;
      getMetronomeUtil().setTimerDuration(
          getMetronomeUtil().getTimerDuration() + range + 1, true
      );
      updateTimer();
      fragment.updateTimerControls(true, true);
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
      getMetronomeUtil().setIncrementalIncrease(
          checkedId == R.id.button_options_incremental_increase
      );
      updateIncremental();
    } else if (groupId == R.id.toggle_options_incremental_unit) {
      if (checkedId == R.id.button_options_incremental_unit_bars) {
        getMetronomeUtil().setIncrementalUnit(UNIT.BARS);
      } else if (checkedId == R.id.button_options_incremental_unit_seconds) {
        getMetronomeUtil().setIncrementalUnit(UNIT.SECONDS);
      } else if (checkedId == R.id.button_options_incremental_unit_minutes) {
        getMetronomeUtil().setIncrementalUnit(UNIT.MINUTES);
      }
      updateIncremental();
    } else if (groupId == R.id.toggle_options_timer_unit) {
      if (checkedId == R.id.button_options_timer_unit_bars) {
        getMetronomeUtil().setTimerUnit(UNIT.BARS);
      } else if (checkedId == R.id.button_options_timer_unit_seconds) {
        getMetronomeUtil().setTimerUnit(UNIT.SECONDS);
      } else if (checkedId == R.id.button_options_timer_unit_minutes) {
        getMetronomeUtil().setTimerUnit(UNIT.MINUTES);
      }
      updateTimer();
      fragment.updateTimerDisplay();
    } else if (groupId == R.id.toggle_options_mute_unit) {
      if (checkedId == R.id.button_options_mute_unit_seconds) {
        getMetronomeUtil().setMuteUnit(UNIT.SECONDS);
      } else if (checkedId == R.id.button_options_mute_unit_bars) {
        getMetronomeUtil().setMuteUnit(UNIT.BARS);
      }
      updateMute();
    } else if (groupId == R.id.toggle_options_swing) {
      if (checkedId == R.id.button_options_swing_3) {
        getMetronomeUtil().setSwing3();
      } else if (checkedId == R.id.button_options_swing_5) {
        getMetronomeUtil().setSwing5();
      } else if (checkedId == R.id.button_options_swing_7) {
        getMetronomeUtil().setSwing7();
      }
      updateSubdivisions(true);
      updateSwing();
      fragment.updateSubs(getMetronomeUtil().getSubdivisions());
      fragment.updateSubControls(true);
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
      getMetronomeUtil().setCountIn((int) value);
      updateCountIn();
    } else if (id == R.id.slider_options_incremental_amount) {
      activity.performHapticSegmentTick(slider, true);
      getMetronomeUtil().setIncrementalAmount((int) value);
      updateIncremental();
    } else if (id == R.id.slider_options_incremental_interval) {
      activity.performHapticSegmentTick(slider, true);
      getMetronomeUtil().setIncrementalInterval((int) value);
      updateIncremental();
    } else if (id == R.id.slider_options_incremental_limit) {
      activity.performHapticSegmentTick(slider, true);
      getMetronomeUtil().setIncrementalLimit((int) value);
      updateIncremental();
    } else if (id == R.id.slider_options_timer_duration) {
      activity.performHapticSegmentTick(slider, true);
      getMetronomeUtil().setTimerDuration((int) value, true);
      updateTimer();
      fragment.updateTimerControls(true, true);
    } else if (id == R.id.slider_options_mute_play) {
      activity.performHapticSegmentTick(slider, true);
      getMetronomeUtil().setMutePlay((int) value);
      updateMute();
    } else if (id == R.id.slider_options_mute_mute) {
      activity.performHapticSegmentTick(slider, true);
      getMetronomeUtil().setMuteMute((int) value);
      updateMute();
    } else if (id == R.id.slider_options_subdivisions) {
      activity.performHapticSegmentTick(slider, true);
      int oldCount = getMetronomeUtil().getSubdivisionsCount();
      int newCount = (int) value;
      int diff = newCount - oldCount;
      for (int i = 0; i < Math.abs(diff); i++) {
        if (diff > 0) {
          getMetronomeUtil().addSubdivision();
        } else {
          getMetronomeUtil().removeSubdivision();
        }
      }
      updateSubdivisions();
      updateSwing();
      fragment.updateSubs(getMetronomeUtil().getSubdivisions());
      fragment.updateSubControls(true);
    }
  }

  @Override
  public void onStartTrackingTouch(@NonNull Slider slider) {
    getMetronomeUtil().savePlayingState();
    getMetronomeUtil().stop();
  }

  @Override
  public void onStopTrackingTouch(@NonNull Slider slider) {
    getMetronomeUtil().restorePlayingState();
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
}
