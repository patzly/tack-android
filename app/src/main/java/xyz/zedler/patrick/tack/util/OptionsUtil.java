package xyz.zedler.patrick.tack.util;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogOptionsBinding;
import xyz.zedler.patrick.tack.fragment.MainFragment;
import xyz.zedler.patrick.tack.service.MetronomeService;

public class OptionsUtil implements OnButtonCheckedListener, OnChangeListener {

  private final MainActivity activity;
  private final MainFragment fragment;
  private final DialogUtil dialogUtil;
  private final PartialDialogOptionsBinding binding;

  public OptionsUtil(MainActivity activity, MainFragment fragment) {
    this.activity = activity;
    this.fragment = fragment;
    dialogUtil = new DialogUtil(activity, "options");
    binding = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
    setUpCountIn();
    setUpIncremental();
    setUpTimer();
    setUpSwing();
    dialogUtil.createCloseCustom(R.string.title_options, binding.getRoot());
  }

  public void show() {
    dialogUtil.show();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    dialogUtil.showIfWasShown(state);
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
    binding.scrollOptions.scrollTo(0, 0);
    updateCountIn();
    updateIncremental();
    updateTimer();
    updateSwing();
  }

  private void setUpCountIn() {
    binding.sliderOptionsCountIn.addOnChangeListener(this);
    binding.sliderOptionsCountIn.setLabelFormatter(
        value -> activity.getResources().getQuantityString(
            R.plurals.options_unit_bars, (int) value, (int) value
        )
    );
  }

  private void updateCountIn() {
    if (!isBound()) {
      return;
    }
    int countIn = getMetronomeService().getCountIn();
    binding.sliderOptionsCountIn.setValue(countIn);
    String barsQuantity = activity.getResources().getQuantityString(
        R.plurals.options_unit_bars, countIn, countIn
    );
    if (getMetronomeService().isCountInActive()) {
      binding.textOptionsCountIn.setText(
          activity.getString(R.string.options_count_in_description, barsQuantity)
      );
    } else {
      binding.textOptionsCountIn.setText(activity.getString(R.string.options_inactive));
    }
  }

  private void setUpIncremental() {
    binding.sliderOptionsIncrementalAmount.addOnChangeListener(this);
    binding.sliderOptionsIncrementalAmount.setLabelFormatter(
        value -> activity.getString(R.string.label_bpm_value, (int) value)
    );
    binding.toggleOptionsIncrementalDirection.addOnButtonCheckedListener(this);
    binding.sliderOptionsIncrementalInterval.addOnChangeListener(this);
    binding.toggleOptionsIncrementalUnit.addOnButtonCheckedListener(this);
  }

  private void updateIncremental() {
    if (!isBound()) {
      return;
    }
    int incrementalAmount = getMetronomeService().getIncrementalAmount();
    boolean incrementalIncrease = getMetronomeService().getIncrementalIncrease();
    boolean isIncrementalActive = getMetronomeService().isIncrementalActive();
    if (isIncrementalActive) {
      binding.textOptionsIncrementalAmount.setText(activity.getString(
          incrementalIncrease
              ? R.string.options_incremental_amount_increase
              : R.string.options_incremental_amount_decrease,
          incrementalAmount
      ));
    } else {
      binding.textOptionsIncrementalAmount.setText(activity.getString(R.string.options_inactive));
    }
    binding.sliderOptionsIncrementalAmount.setValue(incrementalAmount);
    binding.toggleOptionsIncrementalDirection.removeOnButtonCheckedListener(this);
    binding.toggleOptionsIncrementalDirection.check(
        incrementalIncrease
            ? R.id.button_options_incremental_increase
            : R.id.button_options_incremental_decrease
    );
    binding.toggleOptionsIncrementalDirection.addOnButtonCheckedListener(this);
    binding.toggleOptionsIncrementalDirection.setEnabled(isIncrementalActive);

    int incrementalInterval = getMetronomeService().getIncrementalInterval();
    String incrementalUnit = getMetronomeService().getIncrementalUnit();
    int unitResId, checkedId;
    switch (incrementalUnit) {
      case UNIT.SECONDS:
        unitResId = R.plurals.options_unit_seconds;
        checkedId = R.id.button_options_incremental_unit_seconds;
        break;
      case UNIT.MINUTES:
        unitResId = R.plurals.options_unit_minutes;
        checkedId = R.id.button_options_incremental_unit_minutes;
        break;
      default:
        unitResId = R.plurals.options_unit_bars;
        checkedId = R.id.button_options_incremental_unit_bars;
        break;
    }
    String unitQuantity = activity.getResources().getQuantityString(
        unitResId, incrementalInterval, incrementalInterval
    );
    binding.textOptionsIncrementalInterval.setText(
        activity.getString(R.string.options_incremental_interval, unitQuantity)
    );
    binding.textOptionsIncrementalInterval.setAlpha(isIncrementalActive ? 1 : 0.5f);
    binding.sliderOptionsIncrementalInterval.setValue(incrementalInterval);
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

    // Update timer unit selection, see below for an explanation
    updateTimer();
  }

  private void setUpTimer() {
    binding.sliderOptionsTimerDuration.addOnChangeListener(this);
    binding.toggleOptionsTimerUnit.addOnButtonCheckedListener(this);
  }

  private void updateTimer() {
    if (!isBound()) {
      return;
    }
    int timerDuration = getMetronomeService().getTimerDuration();
    boolean isTimerActive = getMetronomeService().isTimerActive();
    String timerUnit = getMetronomeService().getTimerUnit();
    int unitResId, checkedId;
    switch (timerUnit) {
      case UNIT.SECONDS:
        unitResId = R.plurals.options_unit_seconds;
        checkedId = R.id.button_options_timer_unit_seconds;
        break;
      case UNIT.MINUTES:
        unitResId = R.plurals.options_unit_minutes;
        checkedId = R.id.button_options_timer_unit_minutes;
        break;
      default:
        unitResId = R.plurals.options_unit_bars;
        checkedId = R.id.button_options_timer_unit_bars;
        break;
    }
    String unitQuantity = activity.getResources().getQuantityString(
        unitResId, timerDuration, timerDuration
    );
    if (isTimerActive) {
      binding.textOptionsTimerDuration.setText(
          activity.getString(R.string.options_timer_description, unitQuantity)
      );
    } else {
      binding.textOptionsTimerDuration.setText(activity.getString(R.string.options_inactive));
    }
    binding.sliderOptionsTimerDuration.setValue(timerDuration);
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
    // Single-selection mode with auto de-selection only effective if buttons enabled
    binding.buttonOptionsTimerUnitBars.setEnabled(true);
    binding.toggleOptionsTimerUnit.removeOnButtonCheckedListener(this);
    binding.toggleOptionsTimerUnit.check(checkedId);
    binding.toggleOptionsTimerUnit.addOnButtonCheckedListener(this);
    binding.toggleOptionsTimerUnit.setEnabled(isTimerActive);

    // Bars unit not supported for timer in connection with incremental tempo change!
    // On tempo changes with units different to bars, beats would not start at bar start
    // Disable bars unit button for timer
    boolean isIncrementalActive = getMetronomeService().isIncrementalActive();
    boolean convertToNonBarUnit = timerUnit.equals(UNIT.BARS) && isIncrementalActive;
    binding.buttonOptionsTimerUnitBars.setEnabled(
        isTimerActive && !isIncrementalActive
    );
    if (convertToNonBarUnit) {
      long timerInterval = getMetronomeService().getTimerInterval();
      int intervalSeconds = (int) (timerInterval / 1000);
      if (intervalSeconds > binding.sliderOptionsTimerDuration.getValueTo()) {
        getMetronomeService().setTimerUnit(UNIT.MINUTES);
        getMetronomeService().setTimerDuration(intervalSeconds / 60);
      } else {
        getMetronomeService().setTimerUnit(UNIT.SECONDS);
        getMetronomeService().setTimerDuration(intervalSeconds);
      }
      updateTimer();
    }
    binding.textOptionsTimerUnsupported.setVisibility(
        isIncrementalActive ? View.VISIBLE : View.GONE
    );
  }

  private void setUpSwing() {
    binding.toggleOptionsSwing.addOnButtonCheckedListener(this);
  }

  private void updateSwing() {
    if (!isBound()) {
      return;
    }
    binding.textOptionsSwing.setText(activity.getString(
        getMetronomeService().isSwingActive()
            ? R.string.options_swing_description
            : R.string.options_inactive
    ));
    binding.toggleOptionsSwing.removeOnButtonCheckedListener(this);
    if (getMetronomeService().isSwing3()) {
      binding.toggleOptionsSwing.check(R.id.button_options_swing_3);
    } else if (getMetronomeService().isSwing5()) {
      binding.toggleOptionsSwing.check(R.id.button_options_swing_5);
    } else if (getMetronomeService().isSwing7()) {
      binding.toggleOptionsSwing.check(R.id.button_options_swing_7);
    } else {
      binding.toggleOptionsSwing.clearChecked();
    }
    binding.toggleOptionsSwing.addOnButtonCheckedListener(this);
  }

  @Override
  public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
    if (!isChecked) {
      return;
    }
    activity.performHapticClick();
    int groupId = group.getId();
    if (groupId == R.id.toggle_options_incremental_direction) {
      getMetronomeService().setIncrementalIncrease(
          checkedId == R.id.button_options_incremental_increase
      );
      updateIncremental();
    } else if (groupId == R.id.toggle_options_incremental_unit) {
      if (checkedId == R.id.button_options_incremental_unit_bars) {
        getMetronomeService().setIncrementalUnit(UNIT.BARS);
      } else if (checkedId == R.id.button_options_incremental_unit_seconds) {
        getMetronomeService().setIncrementalUnit(UNIT.SECONDS);
      } else if (checkedId == R.id.button_options_incremental_unit_minutes) {
        getMetronomeService().setIncrementalUnit(UNIT.MINUTES);
      }
      updateIncremental();
    } else if (groupId == R.id.toggle_options_timer_unit) {
      if (checkedId == R.id.button_options_timer_unit_bars) {
        getMetronomeService().setTimerUnit(UNIT.BARS);
      } else if (checkedId == R.id.button_options_timer_unit_seconds) {
        getMetronomeService().setTimerUnit(UNIT.SECONDS);
      } else if (checkedId == R.id.button_options_timer_unit_minutes) {
        getMetronomeService().setTimerUnit(UNIT.MINUTES);
      }
      updateTimer();
    } else if (groupId == R.id.toggle_options_swing) {
      if (checkedId == R.id.button_options_swing_3) {
        getMetronomeService().setSwing3();
      } else if (checkedId == R.id.button_options_swing_5) {
        getMetronomeService().setSwing5();
      } else if (checkedId == R.id.button_options_swing_7) {
        getMetronomeService().setSwing7();
      }
      updateSwing();
      getMetronomeService().setSubdivisionsUsed(true);
      fragment.updateSubs(getMetronomeService().getSubdivisions());
      fragment.updateSubControls();
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser || !isBound()) {
      return;
    }
    activity.performHapticTick();
    int id = slider.getId();
    if (id == R.id.slider_options_count_in) {
      getMetronomeService().setCountIn((int) value);
      updateCountIn();
    } else if (id == R.id.slider_options_incremental_amount) {
      getMetronomeService().setIncrementalAmount((int) value);
      updateIncremental();
    } else if (id == R.id.slider_options_incremental_interval) {
      getMetronomeService().setIncrementalInterval((int) value);
      updateIncremental();
    } else if (id == R.id.slider_options_timer_duration) {
      getMetronomeService().setTimerDuration((int) value);
      updateTimer();
      fragment.updateTimerControls();
    }
  }

  private MetronomeService getMetronomeService() {
    return activity.getMetronomeService();
  }

  private boolean isBound() {
    return activity.isBound();
  }
}
