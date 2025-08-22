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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogGainBinding;
import xyz.zedler.patrick.tack.fragment.SettingsFragment;

public class GainDialogUtil implements OnChangeListener {

  private static final String TAG = GainDialogUtil.class.getSimpleName();

  private final MainActivity activity;
  private final SettingsFragment fragment;
  private final PartialDialogGainBinding binding;
  private final DialogUtil dialogUtil;

  public GainDialogUtil(MainActivity activity, SettingsFragment fragment) {
    this.activity = activity;
    this.fragment = fragment;

    binding = PartialDialogGainBinding.inflate(activity.getLayoutInflater());

    dialogUtil = new DialogUtil(activity, "gain");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.settings_gain);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(
          R.string.action_cancel, (dialog, which) -> activity.performHapticClick()
      );
    });
  }

  public void show() {
    update();
    dialogUtil.show();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    update();
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
    if (binding == null) {
      return;
    }

    updateValueDisplay();

    binding.sliderGain.removeOnChangeListener(this);
    binding.sliderGain.setValue(getMetronomeUtil().getGain());
    binding.sliderGain.addOnChangeListener(this);
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_gain) {
      getMetronomeUtil().setGain((int) value);
      activity.performHapticSegmentTick(slider, false);
      updateValueDisplay();
      fragment.updateGainDescription((int) value);
    }
  }

  private void updateValueDisplay() {
    if (binding == null) {
      return;
    }
    int gain = getMetronomeUtil().getGain();
    binding.textGainValue.setText(
        activity.getString(
            R.string.label_db_signed,
            gain > 0 ? "+" + gain : String.valueOf(gain)
        )
    );
  }

  private MetronomeUtil getMetronomeUtil() {
    return activity.getMetronomeUtil();
  }
}
