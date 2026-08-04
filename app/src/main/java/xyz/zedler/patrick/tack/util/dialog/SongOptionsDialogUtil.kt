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
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util.dialog;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogSongOptionsBinding;
import xyz.zedler.patrick.tack.fragment.SongFragment;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class SongOptionsDialogUtil implements OnClickListener, OnCheckedChangeListener,
    OnChangeListener {

  private static final String TAG = SongOptionsDialogUtil.class.getSimpleName();

  private static final String LOOPED = "looped_dialog";
  private static final String SPEED = "speed_dialog";

  private final MainActivity activity;
  private final SongFragment fragment;
  private final PartialDialogSongOptionsBinding binding;
  private final DialogUtil dialogUtil;
  private boolean looped;
  private int speed;

  public SongOptionsDialogUtil(MainActivity activity, SongFragment fragment) {
    this.activity = activity;
    this.fragment = fragment;

    binding = PartialDialogSongOptionsBinding.inflate(activity.getLayoutInflater());

    dialogUtil = new DialogUtil(activity, "song_options");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.label_song_options_dialog);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(R.string.action_apply, (dialog, which) -> {
        activity.performHapticClick();
        apply();
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> activity.performHapticClick()
      );
    });

    setDividerVisibility(!UiUtil.isOrientationPortrait(activity));
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_song_options_looped) {
      binding.switchSongOptionsLooped.toggle();
    }
  }

  @Override
  public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.switch_song_options_looped) {
      activity.performHapticClick();
      looped = isChecked;
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser || binding == null) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_song_options_speed) {
      activity.performHapticSegmentTick(slider, true);
      speed = (int) value;
      binding.textSongOptionsSpeed.setText(
          speed == 100
              ? activity.getString(R.string.label_song_speed_description_original)
              : activity.getString(R.string.label_song_speed_description, speed)
      );
    }
  }

  public void show() {
    update();
    dialogUtil.show();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    looped = state != null && state.getBoolean(LOOPED);
    speed = state != null ? state.getInt(SPEED) : 100;
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
    outState.putBoolean(LOOPED, looped);
    outState.putInt(SPEED, speed);
  }

  public void update() {
    if (binding == null) {
      return;
    }
    binding.linearSongOptionsLooped.setOnClickListener(this);

    binding.switchSongOptionsLooped.setChecked(looped);
    binding.switchSongOptionsLooped.jumpDrawablesToCurrentState();
    binding.switchSongOptionsLooped.setOnCheckedChangeListener(this);

    binding.sliderSongOptionsSpeed.removeOnChangeListener(this);
    ViewUtil.configureSliderSafely(
        binding.sliderSongOptionsSpeed, 5, 100, 5, speed
    );
    binding.sliderSongOptionsSpeed.addOnChangeListener(this);

    binding.textSongOptionsSpeed.setText(
        speed == 100
            ? activity.getString(R.string.label_song_speed_description_original)
            : activity.getString(R.string.label_song_speed_description, speed)
    );

    measureScrollView();
  }

  public void setSongOptions(boolean looped, int speed) {
    this.looped = looped;
    this.speed = speed;
    update();
  }

  private void apply() {
    fragment.setSongOptions(looped, speed);
  }

  private void measureScrollView() {
    binding.scrollSongOptions.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.scrollSongOptions.canScrollVertically(-1)
                || binding.scrollSongOptions.canScrollVertically(1);
            setDividerVisibility(isScrollable);
            binding.scrollSongOptions.getViewTreeObserver()
                .removeOnGlobalLayoutListener(this);
          }
        });
  }

  private void setDividerVisibility(boolean visible) {
    binding.dividerSongOptionsTop.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.dividerSongOptionsBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.linearSongOptionsContainer.setPadding(
        binding.linearSongOptionsContainer.getPaddingLeft(),
        visible ? UiUtil.dpToPx(activity, 16) : 0,
        binding.linearSongOptionsContainer.getPaddingRight(),
        visible ? UiUtil.dpToPx(activity, 16) : 0
    );
  }
}
