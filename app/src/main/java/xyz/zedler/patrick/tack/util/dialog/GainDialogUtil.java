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

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogGainBinding;
import xyz.zedler.patrick.tack.fragment.SettingsFragment;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.util.UiUtil;

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
          R.string.action_close, (dialog, which) -> activity.performHapticClick()
      );
    });

    setDividerVisibility(!UiUtil.isOrientationPortrait(activity));
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

  private void update() {
    if (binding == null) {
      return;
    }

    measureScrollView();

    if (getMetronomeEngine() != null) {
      updateValueDisplay();
      binding.sliderGain.removeOnChangeListener(this);
      binding.sliderGain.setValue(getMetronomeEngine().getGain());
      binding.sliderGain.addOnChangeListener(this);
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser || getMetronomeEngine() == null) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_gain) {
      getMetronomeEngine().setGain((int) value);
      activity.performHapticSegmentTick(slider, false);
      updateValueDisplay();
      fragment.updateGainDescription((int) value);
    }
  }

  private void updateValueDisplay() {
    if (binding == null || getMetronomeEngine() == null) {
      return;
    }
    int gain = getMetronomeEngine().getGain();
    binding.textGainValue.setText(
        activity.getString(
            R.string.label_db_signed,
            gain > 0 ? "+" + gain : String.valueOf(gain)
        )
    );
  }

  private void measureScrollView() {
    binding.scrollGain.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.scrollGain.canScrollVertically(-1)
                || binding.scrollGain.canScrollVertically(1);
            setDividerVisibility(isScrollable);
            binding.scrollGain.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  private void setDividerVisibility(boolean visible) {
    binding.dividerGainTop.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.dividerGainBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.linearGainContainer.setPadding(
        binding.linearGainContainer.getPaddingLeft(),
        visible ? UiUtil.dpToPx(activity, 16) : 0,
        binding.linearGainContainer.getPaddingRight(),
        visible ? UiUtil.dpToPx(activity, 16) : 0
    );
  }

  @Nullable
  private MetronomeEngine getMetronomeEngine() {
    return activity.getMetronomeEngine();
  }
}
