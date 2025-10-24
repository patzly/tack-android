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
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.motion.MotionUtils;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.ViewTimerBinding;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.model.MetronomeConfig;
import xyz.zedler.patrick.tack.util.UiUtil;

public class TimerView extends FrameLayout {

  private static final String TAG = TimerView.class.getSimpleName();

  private static final boolean TEST_ANIMATIONS = false;

  private final ViewTimerBinding binding;
  private final int sliderHeightExpanded;
  private final List<Rect> exclusionRects = new ArrayList<>();
  private final Rect exclusionRect = new Rect();
  private final Rect globalRect = new Rect();
  private final int[] locationOnScreen = new int[2];
  private MainActivity activity;
  private TimerListener listener;
  private ValueAnimator progressAnimator, progressTransitionAnimator;
  private int displayHeightExpanded;
  private float timerExpandFraction, elapsedExpandFraction;
  private boolean timerExpanded, elapsedExpanded, changeHeightOfChips;
  private SpringAnimation springAnimationTimerExpand, springAnimationElapsedExpand;

  public TimerView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    binding = ViewTimerBinding.inflate(
        LayoutInflater.from(context), this, true
    );

    binding.sliderTimer.addOnChangeListener((slider, value, fromUser) -> {
      if (!fromUser || getMetronomeEngine() == null) {
        return;
      }
      int positions = getMetronomeEngine().getConfig().getTimerDuration();
      int timerPositionCurrent = (int) (getMetronomeEngine().getTimerProgress() * positions);
      float fraction = value / binding.sliderTimer.getValueTo();
      int timerPositionNew = (int) (fraction * positions);
      if (timerPositionCurrent != timerPositionNew
          && timerPositionCurrent < positions
          && timerPositionNew < positions
      ) {
        activity.performHapticSegmentTick(binding.sliderTimer, false);
      }
      getMetronomeEngine().updateTimerHandler(fraction, true);
      updateDisplay();
    });
    binding.sliderTimer.addOnSliderTouchListener(new OnSliderTouchListener() {
      @Override
      public void onStartTrackingTouch(@NonNull Slider slider) {
        if (getMetronomeEngine() != null) {
          getMetronomeEngine().savePlayingState();
          getMetronomeEngine().stop();
        }
      }

      @Override
      public void onStopTrackingTouch(@NonNull Slider slider) {
        if (getMetronomeEngine() != null) {
          getMetronomeEngine().restorePlayingState();
        }
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

    sliderHeightExpanded = UiUtil.dpToPx(context, 48);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    binding.sliderTimer.getHitRect(exclusionRect);
    exclusionRects.clear();
    exclusionRects.add(exclusionRect);
    ViewCompat.setSystemGestureExclusionRects(this, exclusionRects);
  }

  public void setMainActivity(MainActivity activity) {
    this.activity = activity;
  }

  public void setListener(TimerListener listener) {
    this.listener = listener;
  }

  public void setChangeHeightOfChips(boolean change) {
    changeHeightOfChips = change;
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
            displayHeightExpanded = binding.frameTimerDisplayContainer.getHeight();
            MetronomeEngine metronomeEngine = getMetronomeEngine();
            updateControls(
                false,
                metronomeEngine != null &&
                    metronomeEngine.isPlaying() && metronomeEngine.getConfig().isTimerActive(),
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

  public void updateControls(
      boolean animateVisibility, boolean animateProgress, boolean withTransition
  ) {
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    MetronomeConfig metronomeConfig = metronomeEngine != null
        ? metronomeEngine.getConfig()
        : new MetronomeConfig(activity.getSharedPrefs());
    boolean isPlaying = metronomeEngine != null && metronomeEngine.isPlaying();
    boolean isTimerActive = metronomeConfig.isTimerActive();
    setTimerExpanded(isTimerActive, animateVisibility);

    int tickCount = metronomeConfig.getTimerDuration();
    int tickSpacingPx = (int) (binding.sliderTimer.getValueTo() / tickCount);
    int tickSpacingDp = UiUtil.dpFromPx(activity, tickSpacingPx);
    if (tickSpacingDp < 16) {
      // reduce tick count to only show every tenth tick
      tickCount = (int) Math.ceil(tickCount / 10f);
      tickSpacingPx = (int) (binding.sliderTimer.getValueTo() / tickCount);
      tickSpacingDp = UiUtil.dpFromPx(activity, tickSpacingPx);
      if (tickSpacingDp < 16) {
        // reduce tick count again to only show every hundredth tick
        tickCount = (int) Math.ceil(tickCount / 10f);
      }
    }
    binding.sliderTimer.setContinuousModeTickCount(tickCount + 1);

    if (metronomeEngine == null) {
      return;
    }

    // Check if timer is currently running
    if (isPlaying && isTimerActive && !metronomeEngine.isCountingIn()) {
      if (withTransition) {
        long timerInterval = metronomeEngine.getTimerInterval();
        float fraction = (float) Constants.ANIM_DURATION_LONG / timerInterval;
        fraction += metronomeEngine.getTimerProgress();
        startProgressTransition(fraction);
      }
      updateProgress(
          1, metronomeEngine.getTimerIntervalRemaining(), animateProgress, true
      );
    } else {
      float timerProgress = metronomeEngine.getTimerProgress();
      if (animateProgress) {
        startProgressTransition(timerProgress);
      } else  {
        updateProgress(timerProgress, 0, false, false);
      }
    }
    updateDisplay();
  }

  public void updateDisplay() {
    if (binding == null || getMetronomeEngine() == null) {
      return;
    }
    String totalTime = getMetronomeEngine().getTotalTimeString();
    if (!totalTime.isEmpty()) {
      binding.chipTimerTotal.textChipNumbers.setText(getMetronomeEngine().getTotalTimeString());
    }
    String currentTime = getMetronomeEngine().getCurrentTimerString();
    if (!currentTime.isEmpty()) {
      binding.chipTimerCurrent.textChipNumbers.setText(getMetronomeEngine().getCurrentTimerString());
    }

    boolean isElapsedActive = getMetronomeEngine().isElapsedActive();
    setElapsedExpanded(isElapsedActive, false);
    binding.chipTimerElapsed.textChipNumbers.setText(getMetronomeEngine().getElapsedTimeString());
  }

  private void updateProgress(float fraction, long duration, boolean animated, boolean linear) {
    stopProgress();
    int max = (int) binding.sliderTimer.getValueTo();
    if (animated) {
      if (getMetronomeEngine() == null) {
        return;
      }
      progressAnimator = ValueAnimator.ofFloat(getMetronomeEngine().getTimerProgress(), fraction);
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
    if (getMetronomeEngine() == null) {
      return;
    }
    int current = (int) binding.sliderTimer.getValue();
    int max = (int) binding.sliderTimer.getValueTo();
    float currentFraction = current / (float) max;
    int currentFractionPx = (int) (currentFraction * binding.sliderTimer.getValueTo());
    int currentFractionDp = UiUtil.dpFromPx(activity, currentFractionPx);
    float currentProgress = getMetronomeEngine().getTimerProgress();
    int currentProgressPx = (int) (currentProgress * binding.sliderTimer.getValueTo());
    int currentProgressDp = UiUtil.dpFromPx(activity, currentProgressPx);
    int diffDp = Math.abs(currentFractionDp - currentProgressDp);
    if (diffDp < 2) {
      // only if current progress is not nearly equal to timer progress
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

  @SuppressLint("PrivateResource")
  private void setTimerExpanded(boolean expanded, boolean animated) {
    this.timerExpanded = expanded;
    if (springAnimationTimerExpand != null) {
      springAnimationTimerExpand.cancel();
    }
    if (animated) {
      binding.sliderTimer.setVisibility(VISIBLE);
      binding.chipTimerCurrent.frameChipNumbersContainer.setVisibility(VISIBLE);
      binding.chipTimerTotal.frameChipNumbersContainer.setVisibility(VISIBLE);
      if (springAnimationTimerExpand == null) {
        springAnimationTimerExpand =
            new SpringAnimation(this, TIMER_EXPAND_FRACTION)
                .setSpring(
                    MotionUtils.resolveThemeSpringForce(
                        getContext(),
                        R.attr.motionSpringDefaultSpatial,
                        R.style.Motion_Material3_Spring_Standard_Default_Spatial)
                )
                .setMinimumVisibleChange(0.01f)
                .addEndListener(
                    (animation, canceled, value, velocity) -> {
                      if (!canceled) {
                        setTimerExpandAnimationEndState();
                      }
                    });
        if (TEST_ANIMATIONS) {
          springAnimationTimerExpand.setSpring(
              new SpringForce().setStiffness(20f).setDampingRatio(0.9f)
          );
        }
      }
      springAnimationTimerExpand.animateToFinalPosition(expanded ? 1 : 0);
    } else {
      setTimerExpandFraction(expanded ? 1 : 0);
      setTimerExpandAnimationEndState();
    }
  }

  private void setTimerExpandAnimationEndState() {
    binding.sliderTimer.setAlpha(timerExpanded ? 1 : 0);
    binding.sliderTimer.setVisibility(timerExpanded ? VISIBLE : GONE);
    binding.chipTimerCurrent.frameChipNumbersContainer.setAlpha(timerExpanded ? 1 : 0);
    binding.chipTimerCurrent.frameChipNumbersContainer.setVisibility(
        timerExpanded ? VISIBLE : INVISIBLE
    );
    binding.chipTimerCurrent.frameChipNumbersContainer.setClickable(timerExpanded);
    binding.chipTimerTotal.frameChipNumbersContainer.setAlpha(timerExpanded ? 1 : 0);
    binding.chipTimerTotal.frameChipNumbersContainer.setVisibility(
        timerExpanded ? VISIBLE : INVISIBLE
    );
    binding.chipTimerTotal.frameChipNumbersContainer.setClickable(timerExpanded);
  }

  public float getTimerExpandFraction() {
    return timerExpandFraction;
  }

  public void setTimerExpandFraction(float fraction) {
    timerExpandFraction = fraction;

    binding.sliderTimer.setAlpha(fraction);
    binding.sliderTimer.setPivotY(0);
    binding.sliderTimer.setScaleY(fraction);
    ViewGroup.LayoutParams lp = binding.frameTimerSliderContainer.getLayoutParams();
    lp.height = (int) (sliderHeightExpanded * fraction);
    binding.frameTimerSliderContainer.setLayoutParams(lp);

    binding.chipTimerCurrent.frameChipNumbersContainer.setAlpha(fraction);
    if (changeHeightOfChips) {
      binding.chipTimerCurrent.frameChipNumbersContainer.setPivotY(0);
      binding.chipTimerCurrent.frameChipNumbersContainer.setScaleY(fraction);
      ViewGroup.LayoutParams lpCurrent =
          binding.chipTimerCurrent.frameChipNumbersContainer.getLayoutParams();
      lpCurrent.height = (int) (displayHeightExpanded * fraction);
      binding.chipTimerCurrent.frameChipNumbersContainer.setLayoutParams(lpCurrent);
    }

    binding.chipTimerTotal.frameChipNumbersContainer.setAlpha(fraction);
    if (changeHeightOfChips) {
      binding.chipTimerTotal.frameChipNumbersContainer.setPivotY(0);
      binding.chipTimerTotal.frameChipNumbersContainer.setScaleY(fraction);
      ViewGroup.LayoutParams lpTotal =
          binding.chipTimerTotal.frameChipNumbersContainer.getLayoutParams();
      lpTotal.height = (int) (displayHeightExpanded * fraction);
      binding.chipTimerTotal.frameChipNumbersContainer.setLayoutParams(lpTotal);
    }

    if (listener != null) {
      listener.onHeightChanged();
    }
  }

  public int getSliderHeightExpanded() {
    return sliderHeightExpanded;
  }

  public int getMaxHeight() {
    return sliderHeightExpanded + displayHeightExpanded;
  }

  public int getTargetHeight() {
    int height = 0;
    if (timerExpanded) {
      height += sliderHeightExpanded;
      height += displayHeightExpanded;
    }
    if (!timerExpanded && elapsedExpanded) {
      height += displayHeightExpanded;
    }
    return height;
  }

  @SuppressLint("PrivateResource")
  private void setElapsedExpanded(boolean expanded, boolean animated) {
    this.elapsedExpanded = expanded;
    if (springAnimationElapsedExpand != null) {
      springAnimationElapsedExpand.cancel();
    }
    if (animated) {
      binding.chipTimerElapsed.frameChipNumbersContainer.setVisibility(VISIBLE);
      if (springAnimationElapsedExpand == null) {
        springAnimationElapsedExpand =
            new SpringAnimation(this, ELAPSED_EXPAND_FRACTION)
                .setSpring(
                    MotionUtils.resolveThemeSpringForce(
                        getContext(),
                        R.attr.motionSpringDefaultSpatial,
                        R.style.Motion_Material3_Spring_Standard_Default_Spatial)
                )
                .setMinimumVisibleChange(0.01f)
                .addEndListener(
                    (animation, canceled, value, velocity) -> {
                      if (!canceled) {
                        setElapsedExpandAnimationEndState();
                      }
                    });
        if (TEST_ANIMATIONS) {
          springAnimationElapsedExpand.setSpring(
              new SpringForce().setStiffness(30f).setDampingRatio(0.9f)
          );
        }
      }
      springAnimationElapsedExpand.animateToFinalPosition(expanded ? 1 : 0);
    } else {
      setElapsedExpandFraction(expanded ? 1 : 0);
      setElapsedExpandAnimationEndState();
    }
  }

  private void setElapsedExpandAnimationEndState() {
    binding.chipTimerElapsed.frameChipNumbersContainer.setAlpha(elapsedExpanded ? 1 : 0);
    binding.chipTimerElapsed.frameChipNumbersContainer.setVisibility(
        elapsedExpanded ? VISIBLE : INVISIBLE
    );
    binding.chipTimerElapsed.frameChipNumbersContainer.setClickable(elapsedExpanded);
  }

  public float getElapsedExpandFraction() {
    return elapsedExpandFraction;
  }

  public void setElapsedExpandFraction(float fraction) {
    elapsedExpandFraction = fraction;

    binding.chipTimerElapsed.frameChipNumbersContainer.setAlpha(fraction);
    if (changeHeightOfChips) {
      binding.chipTimerElapsed.frameChipNumbersContainer.setPivotY(0);
      binding.chipTimerElapsed.frameChipNumbersContainer.setScaleY(fraction);
      ViewGroup.LayoutParams lpTotal =
          binding.chipTimerElapsed.frameChipNumbersContainer.getLayoutParams();
      lpTotal.height = (int) (displayHeightExpanded * fraction);
      binding.chipTimerElapsed.frameChipNumbersContainer.setLayoutParams(lpTotal);
    }

    if (listener != null) {
      listener.onHeightChanged();
    }
  }

  public int getDisplayHeightExpanded() {
    return displayHeightExpanded;
  }

  @Nullable
  private MetronomeEngine getMetronomeEngine() {
    return activity.getMetronomeEngine();
  }

  public interface TimerListener {
    void onCurrentTimeClick();
    void onElapsedTimeClick();
    void onTotalTimeClick();
    void onHeightChanged();
  }

  private static final FloatPropertyCompat<TimerView> TIMER_EXPAND_FRACTION =
      new FloatPropertyCompat<>("timerExpandFraction") {
        @Override
        public float getValue(TimerView delegate) {
          return delegate.getTimerExpandFraction();
        }

        @Override
        public void setValue(TimerView delegate, float value) {
          delegate.setTimerExpandFraction(value);
        }
      };
  private static final FloatPropertyCompat<TimerView> ELAPSED_EXPAND_FRACTION =
      new FloatPropertyCompat<>("elapsedExpandFraction") {
        @Override
        public float getValue(TimerView delegate) {
          return delegate.getElapsedExpandFraction();
        }

        @Override
        public void setValue(TimerView delegate, float value) {
          delegate.setElapsedExpandFraction(value);
        }
      };
}
