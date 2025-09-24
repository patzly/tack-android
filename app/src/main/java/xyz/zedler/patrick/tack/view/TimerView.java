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

package xyz.zedler.patrick.tack.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.ViewTimerBinding;
import xyz.zedler.patrick.tack.util.MetronomeUtil;

public class TimerView extends FrameLayout {

  private final ViewTimerBinding binding;
  private MainActivity activity;
  private TimerListener listener;
  private ValueAnimator progressAnimator, progressTransitionAnimator;

  public TimerView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    binding = ViewTimerBinding.inflate(
        LayoutInflater.from(context), this, true
    );

    binding.sliderTimer.addOnChangeListener((slider, value, fromUser) -> {
      if (!fromUser) {
        return;
      }
      int positions = getMetronomeUtil().getTimerDuration();
      int timerPositionCurrent = (int) (getMetronomeUtil().getTimerProgress() * positions);
      float fraction = value / binding.sliderTimer.getValueTo();
      int timerPositionNew = (int) (fraction * positions);
      if (timerPositionCurrent != timerPositionNew
          && timerPositionCurrent < positions
          && timerPositionNew < positions
      ) {
        activity.performHapticSegmentTick(binding.sliderTimer, false);
      }
      getMetronomeUtil().updateTimerHandler(fraction, true);
      updateDisplay();
    });
    binding.sliderTimer.addOnSliderTouchListener(new OnSliderTouchListener() {
      @Override
      public void onStartTrackingTouch(@NonNull Slider slider) {
        getMetronomeUtil().savePlayingState();
        getMetronomeUtil().stop();
      }

      @Override
      public void onStopTrackingTouch(@NonNull Slider slider) {
        getMetronomeUtil().restorePlayingState();
      }
    });

    binding.chipTimerCurrent.frameChipNumbersContainer.setOnClickListener(v -> {
      if (listener != null) {
        listener.onCurrentTimeClick();
      }
    });
    binding.chipTimerElapsed.frameChipNumbersContainer.setOnClickListener(v -> {
      if (listener != null) {
        listener.onElapsedTimeClick();
      }
    });
    binding.chipTimerTotal.frameChipNumbersContainer.setOnClickListener(v -> {
      if (listener != null) {
        listener.onTotalTimeClick();
      }
    });
  }

  public void setListener(TimerListener listener) {
    this.listener = listener;
  }

  public void setMainActivity(MainActivity activity) {
    this.activity = activity;
  }

  public void setBigText(boolean bigText) {
    if (bigText) {
      Typeface typeface = ResourcesCompat.getFont(activity, R.font.nunito_medium);
      binding.chipTimerCurrent.textChipNumbers.setTextSize(28);
      binding.chipTimerCurrent.textChipNumbers.setTypeface(typeface);
      binding.chipTimerElapsed.textChipNumbers.setTextSize(28);
      binding.chipTimerElapsed.textChipNumbers.setTypeface(typeface);
      binding.chipTimerTotal.textChipNumbers.setTextSize(28);
      binding.chipTimerTotal.textChipNumbers.setTypeface(typeface);
    } else {
      binding.chipTimerCurrent.imageChipNumbers.setImageResource(R.drawable.ic_rounded_timer_anim);
      binding.chipTimerCurrent.imageChipNumbers.setVisibility(View.VISIBLE);
      binding.chipTimerElapsed.imageChipNumbers.setImageResource(
          R.drawable.ic_rounded_schedule_anim
      );
      binding.chipTimerElapsed.imageChipNumbers.setVisibility(View.VISIBLE);
    }
  }

  public void measureControls() {
    binding.linearTimerContainer.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            int width =
                binding.sliderTimer.getWidth() - binding.sliderTimer.getTrackSidePadding() * 2;
            float valueFrom = binding.sliderTimer.getValueFrom();
            float valueTo = Math.max(valueFrom, width);
            if (valueFrom < valueTo) {
              binding.sliderTimer.setValueTo(valueTo);
            }
            updateControls(
                getMetronomeUtil().isPlaying() && getMetronomeUtil().isTimerActive(),
                true
            );
            if (binding.linearTimerContainer.getViewTreeObserver().isAlive()) {
              binding.linearTimerContainer.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        });
  }

  public void updateControls(boolean animated, boolean withTransition) {
    boolean isPlaying = getMetronomeUtil().isPlaying();
    boolean isTimerActive = getMetronomeUtil().isTimerActive();
    int visibility = isTimerActive ? View.VISIBLE : View.GONE;
    binding.chipTimerCurrent.frameChipNumbersContainer.setVisibility(visibility);
    binding.chipTimerTotal.frameChipNumbersContainer.setVisibility(visibility);
    binding.sliderTimer.setVisibility(visibility);
    binding.sliderTimer.setContinuousModeTickCount(getMetronomeUtil().getTimerDuration() + 1);
    // Check if timer is currently running and if metronome is from service
    if (!getMetronomeUtil().isFromService()) {
      return;
    }
    if (isPlaying && isTimerActive && !getMetronomeUtil().isCountingIn()) {
      if (withTransition) {
        long timerInterval = getMetronomeUtil().getTimerInterval();
        float fraction = (float) Constants.ANIM_DURATION_LONG / timerInterval;
        fraction += getMetronomeUtil().getTimerProgress();
        startProgressTransition(fraction);
      }
      updateProgress(
          1, getMetronomeUtil().getTimerIntervalRemaining(), animated, true
      );
    } else {
      float timerProgress = getMetronomeUtil().getTimerProgress();
      if (animated && getMetronomeUtil().isFromService()) {
        startProgressTransition(timerProgress);
      } else if (getMetronomeUtil().isFromService()) {
        updateProgress(timerProgress, 0, false, false);
      }
    }
    updateDisplay();
  }

  public void updateDisplay() {
    if (binding == null) {
      return;
    }
    binding.chipTimerTotal.textChipNumbers.setText(getMetronomeUtil().getTotalTimeString());
    binding.chipTimerCurrent.textChipNumbers.setText(getMetronomeUtil().getCurrentTimerString());

    boolean isElapsedActive = getMetronomeUtil().isElapsedActive();
    binding.chipTimerElapsed.frameChipNumbersContainer.setVisibility(
        isElapsedActive ? View.VISIBLE : View.GONE
    );
    binding.chipTimerElapsed.textChipNumbers.setText(getMetronomeUtil().getElapsedTimeString());
  }

  private void updateProgress(float fraction, long duration, boolean animated, boolean linear) {
    stopProgress();
    int max = (int) binding.sliderTimer.getValueTo();
    if (animated) {
      float progress = getMetronomeUtil().getTimerProgress();
      progressAnimator = ValueAnimator.ofFloat(progress, fraction);
      progressAnimator.addUpdateListener(animation -> {
        if (progressTransitionAnimator != null) {
          return;
        }
        binding.sliderTimer.setValue((int) ((float) animation.getAnimatedValue() * max));
      });
      progressAnimator.setInterpolator(
          linear ? new LinearInterpolator() : new FastOutSlowInInterpolator()
      );
      progressAnimator.setDuration(duration);
      progressAnimator.start();
    } else {
      binding.sliderTimer.setValue((int) (fraction * max));
    }
  }

  public void stopProgress() {
    if (progressAnimator != null) {
      progressAnimator.pause();
      progressAnimator.removeAllUpdateListeners();
      progressAnimator.removeAllListeners();
      progressAnimator.cancel();
      progressAnimator = null;
    }
  }

  private void startProgressTransition(float fractionTo) {
    int current = (int) binding.sliderTimer.getValue();
    int max = (int) binding.sliderTimer.getValueTo();
    float currentFraction = current / (float) max;
    if (getMetronomeUtil().equalsTimerProgress(currentFraction)) {
      // only if current progress is not equal to timer progress
      return;
    }
    progressTransitionAnimator = ValueAnimator.ofFloat(currentFraction, fractionTo);
    progressTransitionAnimator.addUpdateListener(
        animation -> binding.sliderTimer.setValue(
            (int) ((float) animation.getAnimatedValue() * max)
        )
    );
    progressTransitionAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        stopProgressTransition();
      }
    });
    progressTransitionAnimator.setInterpolator(new FastOutSlowInInterpolator());
    progressTransitionAnimator.setDuration(Constants.ANIM_DURATION_LONG);
    progressTransitionAnimator.start();
  }

  public void stopProgressTransition() {
    if (progressTransitionAnimator != null) {
      progressTransitionAnimator.pause();
      progressTransitionAnimator.removeAllUpdateListeners();
      progressTransitionAnimator.removeAllListeners();
      progressTransitionAnimator.cancel();
      progressTransitionAnimator = null;
    }
  }

  private MetronomeUtil getMetronomeUtil() {
    return activity.getMetronomeUtil();
  }

  public interface TimerListener {
    void onCurrentTimeClick();
    void onElapsedTimeClick();
    void onTotalTimeClick();
  }
}
