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
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogLatencyBinding;
import xyz.zedler.patrick.tack.fragment.SettingsFragment;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListener;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListenerAdapter;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.Tick;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class LatencyDialogUtil implements OnChangeListener, OnSliderTouchListener {

  private static final String TAG = LatencyDialogUtil.class.getSimpleName();

  private final MainActivity activity;
  private final SettingsFragment fragment;
  private final PartialDialogLatencyBinding binding;
  private final DialogUtil dialogUtil;
  private final MetronomeListener latencyListener;
  private final int colorBg, colorBgFlash;
  private boolean flashScreen;

  public LatencyDialogUtil(MainActivity activity, SettingsFragment fragment) {
    this.activity = activity;
    this.fragment = fragment;

    binding = PartialDialogLatencyBinding.inflate(activity.getLayoutInflater());

    dialogUtil = new DialogUtil(activity, "latency");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.settings_latency);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> activity.performHapticClick()
      );
    });

    colorBg = ResUtil.getColor(activity, R.attr.colorSurfaceBright);
    colorBgFlash = ResUtil.getColor(activity, R.attr.colorTertiaryContainer);

    latencyListener = new MetronomeListenerAdapter() {
      @Override
      public void onMetronomeTick(Tick tick) {
        activity.runOnUiThread(() -> {
          if (flashScreen) {
            binding.linearLatencyFlash.setBackgroundColor(colorBgFlash);
            binding.linearLatencyFlash.postDelayed(
                () -> binding.linearLatencyFlash.setBackgroundColor(colorBg), 100
            );
          }
        });
      }
    };

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

    if (getMetronomeEngine() == null) {
      return;
    }
    updateValueDisplay();

    binding.sliderLatency.removeOnChangeListener(this);
    binding.sliderLatency.setValue(getMetronomeEngine().getLatency());
    binding.sliderLatency.addOnChangeListener(this);
    binding.sliderLatency.removeOnSliderTouchListener(this);
    binding.sliderLatency.addOnSliderTouchListener(this);
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser || getMetronomeEngine() == null) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_latency) {
      getMetronomeEngine().setLatency((int) value);
      updateValueDisplay();
      fragment.updateLatencyDescription((int) value);
    }
  }

  @Override
  public void onStartTrackingTouch(@NonNull Slider slider) {
    flashScreen = true;
    if (getMetronomeEngine() != null) {
      getMetronomeEngine().savePlayingState();
      getMetronomeEngine().addListener(latencyListener);
      getMetronomeEngine().setUpLatencyCalibration();
    }
  }

  @Override
  public void onStopTrackingTouch(@NonNull Slider slider) {
    flashScreen = false;
    if (getMetronomeEngine() != null) {
      getMetronomeEngine().restorePlayingState();
      getMetronomeEngine().removeListener(latencyListener);
      getMetronomeEngine().setToPreferences();
    }
  }

  private void updateValueDisplay() {
    if (binding == null || getMetronomeEngine() == null) {
      return;
    }
    binding.textLatencyValue.setText(
        activity.getString(R.string.label_ms, String.valueOf(getMetronomeEngine().getLatency()))
    );
  }

  private void measureScrollView() {
    binding.scrollLatency.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.scrollLatency.canScrollVertically(-1)
                || binding.scrollLatency.canScrollVertically(1);
            setDividerVisibility(isScrollable);
            binding.scrollLatency.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  private void setDividerVisibility(boolean visible) {
    binding.dividerLatencyTop.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.dividerLatencyBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.linearLatencyContainer.setPadding(
        binding.linearLatencyContainer.getPaddingLeft(),
        visible ? UiUtil.dpToPx(activity, 16) : 0,
        binding.linearLatencyContainer.getPaddingRight(),
        visible ? UiUtil.dpToPx(activity, 16) : 0
    );
  }

  @Nullable
  private MetronomeEngine getMetronomeEngine() {
    return activity.getMetronomeEngine();
  }
}
