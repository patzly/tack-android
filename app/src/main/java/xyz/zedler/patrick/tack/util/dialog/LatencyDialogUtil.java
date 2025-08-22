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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogLatencyBinding;
import xyz.zedler.patrick.tack.fragment.SettingsFragment;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListenerAdapter;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.ResUtil;

public class LatencyDialogUtil implements OnChangeListener {

  private static final String TAG = LatencyDialogUtil.class.getSimpleName();

  private final MainActivity activity;
  private final SettingsFragment fragment;
  private final PartialDialogLatencyBinding binding;
  private final DialogUtil dialogUtil;
  private final MetronomeListener latencyListener;
  private final int colorBgFlash;
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

    latencyListener = new MetronomeListenerAdapter() {
      @Override
      public void onMetronomeTick(Tick tick) {
        activity.runOnUiThread(() -> {
          if (flashScreen) {
            binding.linearLatencyContainer.setBackgroundColor(colorBgFlash);
            binding.linearLatencyContainer.postDelayed(
                () -> binding.linearLatencyContainer.setBackground(null), 100
            );
          }
        });
      }

      @Override
      public void onMetronomeConnectionMissing() {
        Toast.makeText(activity, R.string.msg_connection_lost, Toast.LENGTH_SHORT).show();
      }
    };

    colorBgFlash = ResUtil.getColor(activity, R.attr.colorTertiaryContainer);
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

    binding.sliderLatency.removeOnChangeListener(this);
    binding.sliderLatency.setValue(getMetronomeUtil().getLatency());
    binding.sliderLatency.addOnChangeListener(this);
    binding.sliderLatency.addOnSliderTouchListener(new OnSliderTouchListener() {
      @Override
      public void onStartTrackingTouch(@NonNull Slider slider) {
        flashScreen = true;
        new Thread(() -> {
          getMetronomeUtil().savePlayingState();
          getMetronomeUtil().addListener(latencyListener);
          getMetronomeUtil().setUpLatencyCalibration();
        }).start();
      }

      @Override
      public void onStopTrackingTouch(@NonNull Slider slider) {
        flashScreen = false;
        new Thread(() -> {
          getMetronomeUtil().restorePlayingState();
          getMetronomeUtil().removeListener(latencyListener);
          getMetronomeUtil().setToPreferences(true);
        }).start();
      }
    });
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_latency) {
      getMetronomeUtil().setLatency((int) value);
      updateValueDisplay();
      fragment.updateLatencyDescription((int) value);
    }
  }

  private void updateValueDisplay() {
    if (binding == null) {
      return;
    }
    binding.textLatencyValue.setText(
        activity.getString(R.string.label_ms, String.valueOf(getMetronomeUtil().getLatency()))
    );
  }

  private MetronomeUtil getMetronomeUtil() {
    return activity.getMetronomeUtil();
  }
}
