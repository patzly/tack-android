package xyz.zedler.patrick.tack.util;

import android.os.Bundle;
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
  private String incrementalUnit = "";

  public OptionsUtil(MainActivity activity, MainFragment fragment) {
    this.activity = activity;
    this.fragment = fragment;
    dialogUtil = new DialogUtil(activity, "options");
    binding = PartialDialogOptionsBinding.inflate(activity.getLayoutInflater());
    setUpCountIn();
    setUpIncremental();
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
    binding.textOptionsCountIn.setText(
        activity.getString(R.string.options_count_in_description, barsQuantity)
    );
  }

  private void setUpIncremental() {
    binding.sliderOptionsIncrementalAmount.addOnChangeListener(this);
    binding.sliderOptionsIncrementalAmount.setLabelFormatter(
        value -> activity.getString(R.string.label_bpm_value, (int) value)
    );
    binding.toggleOptionsIncrementalDirection.addOnButtonCheckedListener(this);
    binding.sliderOptionsIncrementalInterval.addOnChangeListener(this);
    binding.sliderOptionsIncrementalInterval.setLabelFormatter(value -> {
      int interval = (int) value;
      return activity.getResources().getQuantityString(
          incrementalUnit.equals(UNIT.BARS)
              ? R.plurals.options_unit_bars
              : R.plurals.options_unit_seconds,
          interval, interval
      );
    });
    binding.toggleOptionsIncrementalUnit.addOnButtonCheckedListener(this);
  }

  private void updateIncremental() {
    if (!isBound()) {
      return;
    }
    int incrementalAmount = getMetronomeService().getIncrementalAmount();
    boolean incrementalIncrease = getMetronomeService().getIncrementalIncrease();
    binding.textOptionsIncrementalAmount.setText(activity.getString(
        incrementalIncrease
            ? R.string.options_incremental_description_increase
            : R.string.options_incremental_description_decrease,
        incrementalAmount
    ));
    binding.sliderOptionsIncrementalAmount.setValue(incrementalAmount);
    binding.toggleOptionsIncrementalDirection.removeOnButtonCheckedListener(this);
    binding.toggleOptionsIncrementalDirection.check(
        incrementalIncrease
            ? R.id.button_options_incremental_increase
            : R.id.button_options_incremental_decrease
    );
    binding.toggleOptionsIncrementalDirection.addOnButtonCheckedListener(this);

    int incrementalInterval = getMetronomeService().getIncrementalInterval();
    incrementalUnit = getMetronomeService().getIncrementalUnit();
    String unitQuantity = activity.getResources().getQuantityString(
        incrementalUnit.equals(UNIT.BARS)
            ? R.plurals.options_unit_bars
            : R.plurals.options_unit_seconds,
        incrementalInterval, incrementalInterval
    );
    binding.textOptionsIncrementalInterval.setText(
        activity.getString(R.string.options_incremental_description_interval, unitQuantity)
    );
    binding.sliderOptionsIncrementalInterval.setValue(incrementalInterval);
    binding.toggleOptionsIncrementalUnit.removeOnButtonCheckedListener(this);
    if (getMetronomeService().getIncrementalUnit().equals(UNIT.BARS)) {
      binding.toggleOptionsIncrementalUnit.check(R.id.button_options_incremental_unit_bars);
    } else {
      binding.toggleOptionsIncrementalUnit.check(R.id.button_options_incremental_unit_seconds);
    }
    binding.toggleOptionsIncrementalUnit.addOnButtonCheckedListener(this);
  }

  private void setUpSwing() {
    binding.toggleOptionsSwing.addOnButtonCheckedListener(this);
  }

  private void updateSwing() {
    if (!isBound()) {
      return;
    }
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
      }
      incrementalUnit = getMetronomeService().getIncrementalUnit();
      updateIncremental();
    } else if (groupId == R.id.toggle_options_swing) {
      if (checkedId == R.id.button_options_swing_3) {
        getMetronomeService().setSwing3();
      } else if (checkedId == R.id.button_options_swing_5) {
        getMetronomeService().setSwing5();
      } else if (checkedId == R.id.button_options_swing_7) {
        getMetronomeService().setSwing7();
      }
      fragment.updateSubs(getMetronomeService().getSubdivisions());
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
    }
  }

  private MetronomeService getMetronomeService() {
    return activity.getMetronomeService();
  }

  private boolean isBound() {
    return activity.isBound();
  }
}
