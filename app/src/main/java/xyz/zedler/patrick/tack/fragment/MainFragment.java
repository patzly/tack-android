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

package xyz.zedler.patrick.tack.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.graphics.ColorUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.FragmentMainBinding;
import xyz.zedler.patrick.tack.drawable.BeatsBgDrawable;
import xyz.zedler.patrick.tack.drawable.SquigglyProgressDrawable;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.LogoUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.OptionsUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ShortcutUtil;
import xyz.zedler.patrick.tack.util.TempoTapUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.BeatView;
import xyz.zedler.patrick.tack.view.TempoPickerView.OnPickListener;
import xyz.zedler.patrick.tack.view.TempoPickerView.OnRotationListener;

public class MainFragment extends BaseFragment
    implements OnClickListener, MetronomeListener {

  private static final String TAG = MainFragment.class.getSimpleName();

  private FragmentMainBinding binding;
  private MainActivity activity;
  private Bundle savedState;
  private boolean flashScreen, reduceAnimations, isRtl, isLandTablet, bigLogo, showPickerNotLogo;
  private boolean activeBeat, bigTimerSlider;
  private LogoUtil logoUtil, logoCenterUtil;
  private ValueAnimator fabAnimator;
  private float cornerSizeStop, cornerSizePlay, cornerSizeCurrent;
  private int colorFlashNormal, colorFlashStrong, colorFlashMuted;
  private DialogUtil dialogUtilGain, dialogUtilSplitScreen;
  private OptionsUtil optionsUtil;
  private ShortcutUtil shortcutUtil;
  private TempoTapUtil tempoTapUtil;
  private List<Integer> bookmarks;
  private SquigglyProgressDrawable squiggly;
  private BeatsBgDrawable beatsBgDrawable;
  private BadgeDrawable beatsCountBadge, subsCountBadge, optionsBadge;
  private ValueAnimator progressAnimator, progressTransitionAnimator;
  private ValueAnimator beatsCountBadgeAnimator, subsCountBadgeAnimator, optionsBadgeAnimator;
  private ValueAnimator pickerLogoAnimator;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentMainBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (fabAnimator != null) {
      fabAnimator.pause();
      fabAnimator.removeAllUpdateListeners();
      fabAnimator.cancel();
    }
    binding = null;
    dialogUtilGain.dismiss();
    dialogUtilSplitScreen.dismiss();
    optionsUtil.dismiss();
    tempoTapUtil.dismiss();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    savedState = savedInstanceState;
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarMain);
    systemBarBehavior.setContainer(binding.constraintMainContainer);
    systemBarBehavior.setUp();

    boolean isPortrait = UiUtil.isOrientationPortrait(activity);
    isLandTablet = UiUtil.isLandTablet(activity);
    isRtl = UiUtil.isLayoutRtl(activity);

    new ScrollBehavior().setUpScroll(binding.appBarMain, null, isPortrait);

    binding.toolbarMain.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (getViewUtil().isClickDisabled(id)) {
        return false;
      }
      performHapticClick();
      if (id == R.id.action_settings) {
        activity.navigate(MainFragmentDirections.actionMainToSettings());
      } else if (id == R.id.action_about) {
        activity.navigate(MainFragmentDirections.actionMainToAbout());
      } else if (id == R.id.action_help) {
        activity.showTextBottomSheet(R.raw.help, R.string.title_help);
      } else if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      }
      return true;
    });

    flashScreen = getSharedPrefs().getBoolean(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN);
    reduceAnimations = getSharedPrefs().getBoolean(PREF.REDUCE_ANIM, DEF.REDUCE_ANIM);
    activeBeat = getSharedPrefs().getBoolean(PREF.ACTIVE_BEAT, DEF.ACTIVE_BEAT);
    bigTimerSlider = getSharedPrefs().getBoolean(PREF.BIG_TIMER, DEF.BIG_TIMER);

    if (getSharedPrefs().getBoolean(PREF.BIG_TIME_TEXT, DEF.BIG_TIME_TEXT)) {
      binding.textMainTimerCurrent.setTextSize(32);
      binding.textMainTimerTotal.setTextSize(32);
      binding.textMainElapsedTime.setTextSize(32);
    }

    colorFlashNormal = ResUtil.getColor(activity, R.attr.colorPrimary);
    colorFlashStrong = ResUtil.getColor(activity, R.attr.colorError);
    colorFlashMuted = ResUtil.getColor(activity, R.attr.colorSurface);

    beatsCountBadge = BadgeDrawable.create(activity);
    subsCountBadge = BadgeDrawable.create(activity);
    optionsBadge = BadgeDrawable.create(activity);
    optionsBadge.setVerticalOffset(UiUtil.dpToPx(activity, 16));
    optionsBadge.setHorizontalOffset(UiUtil.dpToPx(activity, 16));

    binding.linearMainTop.post(() -> {
      if (binding == null) {
        return;
      }
      LayoutTransition transition = new LayoutTransition();
      transition.setDuration(Constants.ANIM_DURATION_LONG);
      binding.linearMainTop.setLayoutTransition(transition);
    });

    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBeats);
    binding.linearMainBeats.post(() -> {
      if (binding == null) {
        return;
      }
      LayoutTransition transition = new LayoutTransition();
      transition.setDuration(Constants.ANIM_DURATION_LONG);
      binding.linearMainBeats.setLayoutTransition(transition);
    });
    updateBeats(getSharedPrefs().getString(PREF.BEATS, DEF.BEATS).split(","));
    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainSubs);
    binding.linearMainSubs.post(() -> {
      if (binding == null) {
        return;
      }
      LayoutTransition transition = new LayoutTransition();
      transition.setDuration(Constants.ANIM_DURATION_LONG);
      binding.linearMainSubs.setLayoutTransition(transition);
    });

    updateSubs(getSharedPrefs().getString(PREF.SUBDIVISIONS, DEF.SUBDIVISIONS).split(","));

    dialogUtilGain = new DialogUtil(activity, "gain");
    dialogUtilGain.createCaution(
        R.string.msg_gain,
        R.string.msg_gain_description,
        R.string.action_play,
        () -> getMetronomeUtil().start(),
        R.string.action_deactivate_gain,
        () -> {
          getMetronomeUtil().setGain(0);
          getMetronomeUtil().start();
        });
    dialogUtilGain.showIfWasShown(savedInstanceState);

    dialogUtilSplitScreen = new DialogUtil(activity, "split_screen");
    dialogUtilSplitScreen.createClose(
        R.string.msg_split_screen, R.string.msg_split_screen_description
    );
    int screenWidthDp = UiUtil.dpFromPx(activity, UiUtil.getDisplayWidth(activity));
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
      boolean isMultiWindow = activity.isInMultiWindowMode();
      int screenHeightDp = UiUtil.dpFromPx(activity, UiUtil.getDisplayHeight(activity));
      boolean isHeightTooSmall = isPortrait && screenHeightDp < 700;
      boolean isWidthTooSmall = !isPortrait && screenWidthDp < 600;
      if (isMultiWindow && (isHeightTooSmall || isWidthTooSmall)) {
        dialogUtilSplitScreen.show();
      }
    }

    optionsUtil = new OptionsUtil(activity, this, () -> updateOptions(true));
    boolean hideOptions = isLandTablet;
    boolean hideBeatMode = !activity.getHapticUtil().hasVibrator();
    binding.buttonMainOptions.setVisibility(
        hideOptions && hideBeatMode ? View.GONE : View.VISIBLE
    );
    binding.buttonMainOptions.setEnabled(!hideOptions);
    // For symmetry
    binding.buttonMainBeatMode.setVisibility(
        hideBeatMode && hideOptions ? View.GONE : View.VISIBLE
    );

    logoUtil = new LogoUtil(binding.imageMainLogo);
    logoCenterUtil = new LogoUtil(binding.imageMainLogoCenter);
    bigLogo = getSharedPrefs().getBoolean(PREF.BIG_LOGO, DEF.BIG_LOGO);

    shortcutUtil = new ShortcutUtil(activity);
    tempoTapUtil = new TempoTapUtil(activity, this);

    beatsBgDrawable = new BeatsBgDrawable(activity);
    binding.linearMainBeatsBg.setBackground(beatsBgDrawable);

    squiggly = new SquigglyProgressDrawable(activity);
    squiggly.setReduceAnimations(reduceAnimations);
    if (bigTimerSlider) {
      binding.sliderMainTimer.addOnChangeListener((slider, value, fromUser) -> {
        if (!fromUser) {
          return;
        }
        int positions = getMetronomeUtil().getTimerDuration();
        int timerPositionCurrent = (int) (getMetronomeUtil().getTimerProgress() * positions);
        float fraction = value / binding.sliderMainTimer.getValueTo();
        int timerPositionNew = (int) (fraction * positions);
        if (timerPositionCurrent != timerPositionNew
            && timerPositionCurrent < positions
            && timerPositionNew < positions) {
          performHapticTick();
        }
        getMetronomeUtil().updateTimerHandler(fraction, true);
        updateTimerDisplay();
      });
      binding.sliderMainTimer.addOnSliderTouchListener(new OnSliderTouchListener() {
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
    } else {
      binding.seekbarMainTimer.setProgressDrawable(squiggly);
      binding.seekbarMainTimer.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
          if (!fromUser) {
            return;
          }
          int positions = getMetronomeUtil().getTimerDuration();
          int timerPositionCurrent = (int) (getMetronomeUtil().getTimerProgress() * positions);
          float fraction = (float) progress / binding.seekbarMainTimer.getMax();
          int timerPositionNew = (int) (fraction * positions);
          if (timerPositionCurrent != timerPositionNew
              && timerPositionCurrent < positions
              && timerPositionNew < positions) {
            performHapticTick();
          }
          getMetronomeUtil().updateTimerHandler(fraction, true);
          updateTimerDisplay();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
          getMetronomeUtil().savePlayingState();
          getMetronomeUtil().stop();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
          getMetronomeUtil().restorePlayingState();
        }
      });
    }

    binding.textSwitcherMainTempoTerm.setFactory(() -> {
      TextView textView = new TextView(activity);
      textView.setGravity(Gravity.CENTER_HORIZONTAL);
      textView.setTextSize(
          TypedValue.COMPLEX_UNIT_PX,
          getResources().getDimension(R.dimen.label_text_size)
      );
      textView.setTextColor(ResUtil.getColor(activity, R.attr.colorOnSecondaryContainer));
      return textView;
    });

    binding.circleMain.setReduceAnimations(reduceAnimations);
    binding.tempoPickerMain.setOnRotationListener(new OnRotationListener() {
      @Override
      public void onRotate(int tempo) {
        changeTempo(isRtl ? -tempo : tempo);
      }

      @Override
      public void onRotate(float degrees) {
        binding.circleMain.setRotation(
            binding.circleMain.getRotation() + degrees
        );
      }
    });
    binding.tempoPickerMain.setOnPickListener(new OnPickListener() {
      @Override
      public void onPickDown(float x, float y) {
        binding.circleMain.setDragged(true, x, y);
        if (bigLogo && getMetronomeUtil().isPlaying()) {
          updatePickerAndLogo(true, true);
        }
      }

      @Override
      public void onDrag(float x, float y) {
        binding.circleMain.onDrag(x, y);
      }

      @Override
      public void onPickUpOrCancel() {
        binding.circleMain.setDragged(false, 0, 0);
        if (bigLogo && getMetronomeUtil().isPlaying()) {
          updatePickerAndLogo(false, true);
        }
      }
    });

    binding.buttonMainLess.setOnTouchListener(new OnTouchListener() {
      private Handler handler;
      private int nextRun = 400;
      private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          if (getMetronomeUtil().getTempo() > Constants.TEMPO_MIN) {
            changeTempo(-1);
            handler.postDelayed(this, nextRun);
            if (nextRun > 60) {
              nextRun = (int) (nextRun * 0.9);
            }
          } else {
            handler.removeCallbacks(runnable);
            handler = null;
            nextRun = 400;
          }
        }
      };

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            if (handler != null) {
              return true;
            }
            handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(runnable, ViewConfiguration.getLongPressTimeout());
            break;
          case MotionEvent.ACTION_CANCEL:
          case MotionEvent.ACTION_UP:
            if (handler == null) {
              return true;
            }
            handler.removeCallbacks(runnable);
            handler = null;
            nextRun = 400;
            break;
        }
        return false;
      }
    });

    binding.buttonMainMore.setOnTouchListener(new OnTouchListener() {
      private Handler handler;
      private int nextRun = 400;
      private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          if (getMetronomeUtil().getTempo() < Constants.TEMPO_MAX) {
            changeTempo(1);
            handler.postDelayed(this, nextRun);
            if (nextRun > 60) {
              nextRun = (int) (nextRun * 0.9);
            }
          } else {
            handler.removeCallbacks(runnable);
            handler = null;
            nextRun = 400;
          }
        }
      };

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            if (handler != null) {
              return true;
            }
            handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(runnable, ViewConfiguration.getLongPressTimeout());
            break;
          case MotionEvent.ACTION_CANCEL:
          case MotionEvent.ACTION_UP:
            if (handler == null) {
              return true;
            }
            handler.removeCallbacks(runnable);
            handler = null;
            nextRun = 400;
            break;
        }
        return false;
      }
    });

    boolean alwaysVibrate = getSharedPrefs().getBoolean(PREF.ALWAYS_VIBRATE, DEF.ALWAYS_VIBRATE);
    if (getSharedPrefs().getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE)) {
      binding.buttonMainBeatMode.setIconResource(
          alwaysVibrate
              ? R.drawable.ic_rounded_volume_off_to_volume_up_anim
              : R.drawable.ic_rounded_vibration_to_volume_up_anim
      );
    } else {
      binding.buttonMainBeatMode.setIconResource(
          alwaysVibrate
              ? R.drawable.ic_rounded_volume_up_to_volume_off_anim
              : R.drawable.ic_rounded_volume_up_to_vibration_anim
      );
    }

    setButtonStates();

    Set<String> bookmarksSet = getSharedPrefs().getStringSet(PREF.BOOKMARKS, Set.of());
    bookmarks = new ArrayList<>();
    for (String tempo : bookmarksSet) {
      try {
        bookmarks.add(Integer.parseInt(tempo));
      } catch (NumberFormatException e) {
        Log.e(TAG, "onViewCreated: get bookmarks: ", e);
      }
    }
    Collections.sort(bookmarks);
    for (int i = 0; i < bookmarks.size(); i++) {
      binding.chipGroupMainBookmarks.addView(getBookmarkChip(bookmarks.get(i)));
    }

    boolean isWidthLargeEnough = screenWidthDp - 16 >= 344;
    boolean large = (isPortrait && isWidthLargeEnough) || isLandTablet;
    cornerSizeStop = UiUtil.dpToPx(activity, large ? 28 : 16);
    cornerSizePlay = UiUtil.dpToPx(activity, large ? 48 : 28);
    cornerSizeCurrent = cornerSizeStop;
    ViewUtil.resetAnimatedIcon(binding.fabMainPlayStop);
    binding.fabMainPlayStop.setImageResource(R.drawable.ic_rounded_play_to_stop_fill_anim);
    binding.fabMainPlayStop.setMaxImageSize(UiUtil.dpToPx(activity, large ? 36 : 24));
    binding.fabMainPlayStop.setCustomSize(UiUtil.dpToPx(activity, large ? 96 : 56));
    if (!isWidthLargeEnough) {
      // Reduce bottom controls padding for small screens or large scaling
      int padding = UiUtil.dpToPx(activity, 4);
      binding.linearMainBottomControlsStart.setPadding(padding, padding, padding, padding);
      binding.linearMainBottomControlsEnd.setPadding(padding, padding, padding, padding);
    }

    updateMetronomeControls();

    ViewUtil.setTooltipText(binding.buttonMainAddBeat, R.string.action_add_beat);
    ViewUtil.setTooltipText(binding.buttonMainRemoveBeat, R.string.action_remove_beat);
    ViewUtil.setTooltipText(binding.buttonMainAddSubdivision, R.string.action_add_sub);
    ViewUtil.setTooltipText(binding.buttonMainRemoveSubdivision, R.string.action_remove_sub);
    ViewUtil.setTooltipText(binding.buttonMainOptions, R.string.title_options);
    ViewUtil.setTooltipText(binding.buttonMainTempoTap, R.string.action_tempo_tap);
    ViewUtil.setTooltipText(binding.fabMainPlayStop, R.string.action_play_stop);
    ViewUtil.setTooltipText(binding.buttonMainBookmark, R.string.action_bookmark);
    ViewUtil.setTooltipText(binding.buttonMainBeatMode, R.string.action_beat_mode);

    ViewUtil.setOnClickListeners(
        this,
        binding.buttonMainAddBeat,
        binding.buttonMainRemoveBeat,
        binding.buttonMainAddSubdivision,
        binding.buttonMainRemoveSubdivision,
        binding.buttonMainLess,
        binding.buttonMainMore,
        binding.buttonMainBeatMode,
        binding.buttonMainBookmark,
        binding.buttonMainOptions,
        binding.buttonMainTempoTap,
        binding.fabMainPlayStop
    );
  }

  @Override
  public void onPause() {
    super.onPause();
    stopTimerProgress();
    stopTimerTransitionProgress();
    squiggly.pauseAnimation();
  }

  @Override
  public void onResume() {
    super.onResume();
    updateTimerControls();
    updateElapsedDisplay();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (dialogUtilGain != null) {
      dialogUtilGain.saveState(outState);
    }
    if (optionsUtil != null) {
      optionsUtil.saveState(outState);
    }
    if (tempoTapUtil != null) {
      tempoTapUtil.saveState(outState);
    }
  }

  public void updateMetronomeControls() {
    if (binding == null) {
      return;
    }
    getMetronomeUtil().addListener(this);
    optionsUtil.showIfWasShown(savedState);
    tempoTapUtil.showIfWasShown(savedState);
    savedState = null;

    if (getMetronomeUtil().isBeatModeVibrate()) {
      binding.buttonMainBeatMode.setIconResource(
          getMetronomeUtil().isAlwaysVibrate()
              ? R.drawable.ic_rounded_volume_off_to_volume_up_anim
              : R.drawable.ic_rounded_vibration_to_volume_up_anim
      );
    } else {
      binding.buttonMainBeatMode.setIconResource(
          getMetronomeUtil().isAlwaysVibrate()
              ? R.drawable.ic_rounded_volume_up_to_volume_off_anim
              : R.drawable.ic_rounded_volume_up_to_vibration_anim
      );
    }

    updateBeats(getMetronomeUtil().getBeats());
    updateBeatControls(false);
    updateSubs(getMetronomeUtil().getSubdivisions());
    updateSubControls(false);

    binding.scrollHorizMainBookmarks.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (binding == null) {
              return;
            }
            refreshBookmarks(true, false);
            // Kill ViewTreeObserver
            if (binding.scrollHorizMainBookmarks.getViewTreeObserver().isAlive()) {
              binding.scrollHorizMainBookmarks.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        });
    measureTimerControls(true); // calls updateTimerControls when measured
    updateElapsedDisplay();
    updateOptions(false);

    int tempo = getMetronomeUtil().getTempo();
    setTempo(tempo);
    binding.textSwitcherMainTempoTerm.setCurrentText(getTempoTerm(tempo));

    boolean showLogo = bigLogo && getMetronomeUtil().isPlaying();
    updatePickerAndLogo(!showLogo, false);

    ViewUtil.resetAnimatedIcon(binding.fabMainPlayStop);
    binding.fabMainPlayStop.setImageResource(
        getMetronomeUtil().isPlaying()
            ? R.drawable.ic_rounded_stop_fill
            : R.drawable.ic_rounded_play_arrow_fill
    );
    updateFabCornerRadius(getMetronomeUtil().isPlaying(), false);

    UiUtil.keepScreenAwake(
        activity, getMetronomeUtil().getKeepAwake() && getMetronomeUtil().isPlaying()
    );
  }

  @Override
  public void onMetronomeStart() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        beatsBgDrawable.reset();
        if (getMetronomeUtil().getCountIn() > 0) {
          beatsBgDrawable.setProgress(
              1, getMetronomeUtil().getCountInInterval(), true
          );
        }
        if (getMetronomeUtil().isTimerActive()) {
          squiggly.setAnimate(true, true);
        }
        binding.fabMainPlayStop.setImageResource(R.drawable.ic_rounded_play_to_stop_fill_anim);
        Drawable fabIcon = binding.fabMainPlayStop.getDrawable();
        if (fabIcon != null) {
          ((Animatable) fabIcon).start();
        }
        updateFabCornerRadius(true, true);
        if (bigLogo) {
          updatePickerAndLogo(false, true);
        }
      }
    });
    try {
      // Inside UI thread appears to be often not effective
      UiUtil.keepScreenAwake(activity, getMetronomeUtil().getKeepAwake());
    } catch (RuntimeException e) {
      Log.w(TAG, "onMetronomeStart: keepScreenAwake(true)", e);
    }
  }

  @Override
  public void onMetronomeStop() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        resetActiveBeats();
        beatsBgDrawable.setProgressVisible(false, true);
        if (getMetronomeUtil().isTimerActive()) {
          squiggly.setAnimate(false, true);
        }
        updateTimerDisplay();
        updateElapsedDisplay();
        binding.fabMainPlayStop.setImageResource(R.drawable.ic_rounded_stop_to_play_fill_anim);
        Drawable icon = binding.fabMainPlayStop.getDrawable();
        if (icon != null) {
          ((Animatable) icon).start();
        }
        updateFabCornerRadius(false, true);
        if (bigLogo) {
          updatePickerAndLogo(true, true);
        }
      }
      stopTimerTransitionProgress();
      stopTimerProgress();
    });
    try {
      // Inside UI thread appears to be often not effective
      UiUtil.keepScreenAwake(activity, false);
    } catch (RuntimeException e) {
      Log.w(TAG, "onMetronomeStart: keepScreenAwake(false)", e);
    }
  }

  @Override
  public void onMetronomePreTick(Tick tick) {
    activity.runOnUiThread(() -> {
      if (binding == null) {
        return;
      }
      View beat = binding.linearMainBeats.getChildAt(tick.beat - 1);
      if (beat instanceof BeatView && tick.subdivision == 1) {
        resetActiveBeats();
        ((BeatView) beat).setTickType(tick.type);
        if (activeBeat) {
          ((BeatView) beat).setActive(true);
        }
        ((BeatView) beat).beat();
      }
      View subdivision = binding.linearMainSubs.getChildAt(tick.subdivision - 1);
      if (getMetronomeUtil().getSubdivisionsUsed() && subdivision instanceof BeatView) {
        ((BeatView) subdivision).setTickType(tick.subdivision == 1 ? TICK_TYPE.MUTED : tick.type);
        ((BeatView) subdivision).beat();
      }
    });
  }

  @Override
  public void onMetronomeTick(Tick tick) {
    activity.runOnUiThread(() -> {
      if (binding == null) {
        return;
      }
      if (flashScreen) {
        int color;
        switch (tick.type) {
          case TICK_TYPE.STRONG:
            color = colorFlashStrong;
            break;
          case TICK_TYPE.SUB:
          case TICK_TYPE.MUTED:
            color = colorFlashMuted;
            break;
          default:
            color = colorFlashNormal;
            break;
        }
        if (tick.isMuted) {
          color = colorFlashMuted;
        }
        View flashContainer = isLandTablet && binding.containerMainEnd != null
            ? binding.containerMainEnd
            : binding.coordinatorContainer;
        flashContainer.setBackgroundColor(color);
        flashContainer.postDelayed(() -> {
          if (binding != null) {
            flashContainer.setBackgroundColor(colorFlashMuted);
          }
        }, 100); // flash screen for 100 milliseconds
      }
      if (tick.subdivision == 1) {
        logoUtil.nextBeat(getMetronomeUtil().getInterval());
        if (bigLogo) {
          logoCenterUtil.nextBeat(getMetronomeUtil().getInterval());
        }
        if (getMetronomeUtil().getTimerUnit().equals(UNIT.BARS)) {
          updateTimerDisplay();
        }
      }
    });
  }

  @Override
  public void onMetronomeTempoChanged(int tempoOld, int tempoNew) {
    activity.runOnUiThread(() -> setTempo(tempoOld, tempoNew));
  }

  @Override
  public void onMetronomeTimerStarted() {
    stopTimerTransitionProgress();
    stopTimerProgress();
    if (binding == null) {
      return;
    }
    int current = bigTimerSlider
        ? (int) binding.sliderMainTimer.getValue()
        : binding.seekbarMainTimer.getProgress();
    int max = bigTimerSlider
        ? (int) binding.sliderMainTimer.getValueTo()
        : binding.seekbarMainTimer.getMax();
    float currentFraction = current / (float) max;
    if (!getMetronomeUtil().equalsTimerProgress(currentFraction)) {
      // position where the timer will be at animation end
      // only if current progress is not equal to timer progress
      long animDuration = Constants.ANIM_DURATION_LONG;
      float fraction = (float) animDuration / getMetronomeUtil().getTimerInterval();
      fraction += getMetronomeUtil().getTimerProgress();
      progressTransitionAnimator = ValueAnimator.ofFloat(currentFraction, fraction);
      progressTransitionAnimator.addUpdateListener(animation -> {
        if (binding == null) {
          return;
        }
        if (bigTimerSlider) {
          binding.sliderMainTimer.setValue((int) ((float) animation.getAnimatedValue() * max));
        } else {
          binding.seekbarMainTimer.setProgress((int) ((float) animation.getAnimatedValue() * max));
          binding.seekbarMainTimer.invalidate();
        }
      });
      progressTransitionAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          stopTimerTransitionProgress();
        }
      });
      progressTransitionAnimator.setInterpolator(new FastOutSlowInInterpolator());
      progressTransitionAnimator.setDuration(animDuration);
      progressTransitionAnimator.start();
    }
    updateTimerProgress(
        1, getMetronomeUtil().getTimerIntervalRemaining(), true, true
    );
  }

  @Override
  public void onElapsedTimeSecondsChanged() {
    activity.runOnUiThread(this::updateElapsedDisplay);
  }

  @Override
  public void onTimerSecondsChanged() {
    activity.runOnUiThread(this::updateTimerDisplay);
  }

  @Override
  public void onMetronomeConnectionMissing() {
    activity.runOnUiThread(() -> showSnackbar(
        activity.getSnackbar(R.string.msg_connection_lost, Snackbar.LENGTH_SHORT)
    ));
  }

  @Override
  public void onPermissionMissing() {
    activity.requestNotificationPermission(true);
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.button_main_add_beat) {
      ViewUtil.startIcon(binding.buttonMainAddBeat.getIcon());
      performHapticClick();
      boolean success = getMetronomeUtil().addBeat();
      if (success) {
        BeatView beatView = new BeatView(activity);
        beatView.setIndex(binding.linearMainBeats.getChildCount());
        beatView.setOnClickListener(beat -> {
          performHapticClick();
          getMetronomeUtil().setBeat(beatView.getIndex(), beatView.nextTickType());
        });
        beatView.setReduceAnimations(reduceAnimations);
        binding.linearMainBeats.addView(beatView);
        ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBeats);
        updateBeatControls(true);
        updateTimerDisplay(); // Update decimals for bar unit
      }
    } else if (id == R.id.button_main_remove_beat) {
      ViewUtil.startIcon(binding.buttonMainRemoveBeat.getIcon());
      performHapticClick();
      boolean success = getMetronomeUtil().removeBeat();
      if (success) {
        binding.linearMainBeats.removeViewAt(binding.linearMainBeats.getChildCount() - 1);
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizMainBeats, true
        );
        updateBeatControls(true);
        updateTimerDisplay(); // Update decimals for bar unit
      }
    } else if (id == R.id.button_main_add_subdivision) {
      ViewUtil.startIcon(binding.buttonMainAddSubdivision.getIcon());
      performHapticClick();
      boolean success = getMetronomeUtil().addSubdivision();
      if (success) {
        BeatView beatView = new BeatView(activity);
        beatView.setIsSubdivision(true);
        beatView.setIndex(binding.linearMainSubs.getChildCount());
        beatView.setOnClickListener(subdivision -> {
          performHapticClick();
          getMetronomeUtil().setSubdivision(beatView.getIndex(), beatView.nextTickType());
        });
        beatView.setReduceAnimations(reduceAnimations);
        binding.linearMainSubs.addView(beatView);
        ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainSubs);
        updateSubControls(true);
        optionsUtil.updateSwing();
      }
    } else if (id == R.id.button_main_remove_subdivision) {
      ViewUtil.startIcon(binding.buttonMainRemoveSubdivision.getIcon());
      performHapticClick();
      boolean success = getMetronomeUtil().removeSubdivision();
      if (success) {
        binding.linearMainSubs.removeViewAt(binding.linearMainSubs.getChildCount() - 1);
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizMainSubs, true
        );
        updateSubControls(true);
        optionsUtil.updateSwing();
      }
    } else if (id == R.id.fab_main_play_stop) {
      if (getMetronomeUtil().isPlaying()) {
        performHapticClick();
        getMetronomeUtil().stop();
      } else {
        if (getMetronomeUtil().getGain() > 0 && getMetronomeUtil().neverStartedWithGainBefore()) {
          dialogUtilGain.show();
        } else {
          getMetronomeUtil().start();
        }
        performHapticClick();
      }
    } else if (id == R.id.button_main_less) {
      ViewUtil.startIcon(binding.buttonMainLess.getIcon());
      changeTempo(-1);
    } else if (id == R.id.button_main_more) {
      ViewUtil.startIcon(binding.buttonMainMore.getIcon());
      changeTempo(1);
    } else if (id == R.id.button_main_beat_mode) {
      boolean beatModeVibrateNew = !getMetronomeUtil().isBeatModeVibrate();
      if (beatModeVibrateNew && !activity.getHapticUtil().hasVibrator()) {
        showSnackbar(
            activity.getSnackbar(R.string.msg_vibration_unavailable, Snackbar.LENGTH_SHORT)
        );
        return;
      }
      if (!beatModeVibrateNew) {
        performHapticClick();
      }
      getMetronomeUtil().setBeatModeVibrate(beatModeVibrateNew);
      if (beatModeVibrateNew) {
        performHapticClick();
      }
      ViewUtil.startIcon(binding.buttonMainBeatMode.getIcon());
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (binding == null) {
          return;
        }
        if (beatModeVibrateNew) {
          binding.buttonMainBeatMode.setIconResource(
              getMetronomeUtil().isAlwaysVibrate()
                  ? R.drawable.ic_rounded_volume_off_to_volume_up_anim
                  : R.drawable.ic_rounded_vibration_to_volume_up_anim
          );
        } else {
          binding.buttonMainBeatMode.setIconResource(
              getMetronomeUtil().isAlwaysVibrate()
                  ? R.drawable.ic_rounded_volume_up_to_volume_off_anim
                  : R.drawable.ic_rounded_volume_up_to_vibration_anim
          );
        }
      }, 300);
    } else if (id == R.id.button_main_bookmark) {
      ViewUtil.startIcon(binding.buttonMainBookmark.getIcon());
      performHapticClick();
      int tempo = getMetronomeUtil().getTempo();
      if (bookmarks.size() < Constants.BOOKMARKS_MAX && !bookmarks.contains(tempo)) {
        int position = 0;
        while (position < bookmarks.size() && bookmarks.get(position) < tempo) {
          position++;
        }
        binding.chipGroupMainBookmarks.addView(getBookmarkChip(tempo), position);
        bookmarks.add(position, tempo);
        shortcutUtil.addShortcut(tempo);
        updateBookmarks();
        refreshBookmarks(false, true);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
          if (binding != null) {
            refreshBookmarks(true, true);
          }
        }, 300);
      } else if (bookmarks.size() >= Constants.BOOKMARKS_MAX) {
        Snackbar snackbar = activity.getSnackbar(R.string.msg_bookmarks_max, Snackbar.LENGTH_SHORT);
        snackbar.setAction(
            getString(R.string.action_clear_all),
            view -> {
              binding.chipGroupMainBookmarks.removeAllViews();
              bookmarks.clear();
              shortcutUtil.removeAllShortcuts();
              updateBookmarks();
              refreshBookmarks(true, true);
            }
        );
        showSnackbar(snackbar);
      }
    } else if (id == R.id.button_main_options) {
      performHapticClick();
      ViewUtil.startIcon(binding.buttonMainOptions.getIcon());
      optionsUtil.update();
      optionsUtil.show();
    } else if (id == R.id.button_main_tempo_tap) {
      performHapticClick();
      ViewUtil.startIcon(binding.buttonMainTempoTap.getIcon());
      tempoTapUtil.update();
      tempoTapUtil.show();
    }
  }

  private void updateBeats(String[] beats) {
    String[] currentBeats = new String[binding.linearMainBeats.getChildCount()];
    for (int i = 0; i < binding.linearMainBeats.getChildCount(); i++) {
      currentBeats[i] = String.valueOf(binding.linearMainBeats.getChildAt(i));
    }
    if (Arrays.equals(beats, currentBeats)) {
      return;
    }
    binding.linearMainBeats.removeAllViews();
    for (int i = 0; i < beats.length; i++) {
      String tickType = beats[i];
      BeatView beatView = new BeatView(activity);
      beatView.setTickType(tickType);
      beatView.setIndex(i);
      beatView.setOnClickListener(beat -> {
        performHapticClick();
        getMetronomeUtil().setBeat(beatView.getIndex(), beatView.nextTickType());
      });
      beatView.setReduceAnimations(reduceAnimations);
      binding.linearMainBeats.addView(beatView);
    }
    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBeats);

    updateBeatControls(true);
  }

  private void resetActiveBeats() {
    for (int i = 0; i < binding.linearMainBeats.getChildCount(); i++) {
      View beat = binding.linearMainBeats.getChildAt(i);
      if (beat instanceof BeatView) {
        ((BeatView) beat).setActive(false);
      }
    }
  }

  @OptIn(markerClass = ExperimentalBadgeUtils.class)
  private void updateBeatControls(boolean animated) {
    if (beatsCountBadgeAnimator != null) {
      beatsCountBadgeAnimator.pause();
      beatsCountBadgeAnimator.removeAllUpdateListeners();
      beatsCountBadgeAnimator.removeAllListeners();
      beatsCountBadgeAnimator.cancel();
      beatsCountBadgeAnimator = null;
    }
    int beats = getMetronomeUtil().getBeatsCount();
    binding.buttonMainAddBeat.setEnabled(beats < Constants.BEATS_MAX);
    binding.buttonMainRemoveBeat.setEnabled(beats > 1);
    beatsCountBadge.setNumber(beats);
    boolean show = beats > 4;
    if (animated) {
      beatsCountBadgeAnimator = ValueAnimator.ofInt(beatsCountBadge.getAlpha(), show ? 255 : 0);
      beatsCountBadgeAnimator.addUpdateListener(animation -> {
        if (binding == null) {
          return;
        }
        beatsCountBadge.setAlpha((int) animation.getAnimatedValue());
        float fraction = (float) ((int) animation.getAnimatedValue()) / 255;
        int colorBg = ResUtil.getColor(activity, R.attr.colorError);
        int color = ColorUtils.blendARGB(Color.TRANSPARENT, colorBg, fraction);
        beatsCountBadge.setBackgroundColor(color);
      });
      beatsCountBadgeAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          if (!show) {
            BadgeUtils.detachBadgeDrawable(beatsCountBadge, binding.linearMainBeatsBg);
          }
        }
      });
      beatsCountBadgeAnimator.setInterpolator(new FastOutSlowInInterpolator());
      beatsCountBadgeAnimator.setDuration(Constants.ANIM_DURATION_LONG);
      beatsCountBadgeAnimator.start();
      if (show) {
        BadgeUtils.attachBadgeDrawable(beatsCountBadge, binding.linearMainBeatsBg);
      }
    } else {
      beatsCountBadge.setAlpha(show ? 255 : 0);
      beatsCountBadge.setBackgroundColor(
          show ? ResUtil.getColor(activity, R.attr.colorError) : Color.TRANSPARENT
      );
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (binding == null) {
          return;
        }
        if (show) {
          BadgeUtils.attachBadgeDrawable(beatsCountBadge, binding.linearMainBeatsBg);
        } else {
          BadgeUtils.detachBadgeDrawable(beatsCountBadge, binding.linearMainBeatsBg);
        }
      }, 1);
    }
  }

  public void updateSubs(String[] subdivisions) {
    String[] currentSubs = new String[binding.linearMainSubs.getChildCount()];
    for (int i = 0; i < binding.linearMainSubs.getChildCount(); i++) {
      currentSubs[i] = String.valueOf(binding.linearMainSubs.getChildAt(i));
    }
    if (Arrays.equals(subdivisions, currentSubs)) {
      return;
    }
    binding.linearMainSubs.removeAllViews();
    for (int i = 0; i < subdivisions.length; i++) {
      String tickType = subdivisions[i];
      BeatView beatView = new BeatView(activity);
      beatView.setIsSubdivision(true);
      beatView.setTickType(i == 0 ? TICK_TYPE.MUTED : tickType);
      beatView.setIndex(i);
      if (i > 0) {
        beatView.setOnClickListener(beat -> {
          performHapticClick();
          getMetronomeUtil().setSubdivision(beatView.getIndex(), beatView.nextTickType());
        });
      }
      beatView.setReduceAnimations(reduceAnimations);
      binding.linearMainSubs.addView(beatView);
    }
    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainSubs, true);

    updateSubControls(true);
  }

  @OptIn(markerClass = ExperimentalBadgeUtils.class)
  public void updateSubControls(boolean animated) {
    if (subsCountBadgeAnimator != null) {
      subsCountBadgeAnimator.pause();
      subsCountBadgeAnimator.removeAllUpdateListeners();
      subsCountBadgeAnimator.removeAllListeners();
      subsCountBadgeAnimator.cancel();
      subsCountBadgeAnimator = null;
    }
    int subdivisions = getMetronomeUtil().getSubdivisionsCount();
    binding.buttonMainAddSubdivision.setEnabled(subdivisions < Constants.SUBS_MAX);
    binding.buttonMainRemoveSubdivision.setEnabled(subdivisions > 1);
    binding.linearMainSubsBg.setVisibility(
        getMetronomeUtil().getSubdivisionsUsed() ? View.VISIBLE : View.GONE
    );
    subsCountBadge.setNumber(subdivisions);
    boolean show = subdivisions > 4;
    if (animated) {
      subsCountBadgeAnimator = ValueAnimator.ofInt(subsCountBadge.getAlpha(), show ? 255 : 0);
      subsCountBadgeAnimator.addUpdateListener(animation -> {
        if (binding == null) {
          return;
        }
        subsCountBadge.setAlpha((int) animation.getAnimatedValue());
        float fraction = (float) ((int) animation.getAnimatedValue()) / 255;
        int colorBg = ResUtil.getColor(activity, R.attr.colorError);
        int color = ColorUtils.blendARGB(Color.TRANSPARENT, colorBg, fraction);
        subsCountBadge.setBackgroundColor(color);
      });
      subsCountBadgeAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          if (!show) {
            BadgeUtils.detachBadgeDrawable(beatsCountBadge, binding.linearMainSubsBg);
          }
        }
      });
      subsCountBadgeAnimator.setInterpolator(new FastOutSlowInInterpolator());
      subsCountBadgeAnimator.setDuration(Constants.ANIM_DURATION_LONG);
      subsCountBadgeAnimator.start();
      if (show) {
        BadgeUtils.attachBadgeDrawable(subsCountBadge, binding.linearMainSubsBg);
      }
    } else {
      subsCountBadge.setAlpha(show ? 255 : 0);
      subsCountBadge.setBackgroundColor(
          show ? ResUtil.getColor(activity, R.attr.colorError) : Color.TRANSPARENT
      );
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (binding == null) {
          return;
        }
        if (show) {
          BadgeUtils.attachBadgeDrawable(subsCountBadge, binding.linearMainSubsBg);
        } else {
          BadgeUtils.detachBadgeDrawable(subsCountBadge, binding.linearMainSubsBg);
        }
      }, 1);
    }
  }

  public void updateTimerControls() {
    boolean isPlaying = getMetronomeUtil().isPlaying();
    boolean isTimerActive = getMetronomeUtil().isTimerActive();
    binding.seekbarMainTimer.setVisibility(
        isTimerActive && !bigTimerSlider ? View.VISIBLE : View.GONE
    );
    binding.sliderMainTimer.setVisibility(
        isTimerActive && bigTimerSlider ? View.VISIBLE : View.GONE
    );
    if (bigTimerSlider) {
      binding.sliderMainTimer.setContinuousTicksCount(getMetronomeUtil().getTimerDuration() + 1);
    }
    measureTimerControls(false);
    if (isTimerActive && isPlaying) {
      squiggly.resumeAnimation();
    } else {
      squiggly.pauseAnimation();
    }
    if (isTimerActive) {
      squiggly.setAnimate(isPlaying, true);
    }
    // Check if timer is currently running
    if (isPlaying && isTimerActive && !getMetronomeUtil().isCountingIn()) {
      updateTimerProgress(
          1, getMetronomeUtil().getTimerIntervalRemaining(), true, true
      );
    } else {
      float timerProgress = getMetronomeUtil().getTimerProgress();
      updateTimerProgress(timerProgress, 0, false, false);
    }
    updateTimerDisplay();
  }

  private void measureTimerControls(boolean updateControls) {
    if (bigTimerSlider) {
      binding.sliderMainTimer.getViewTreeObserver().addOnGlobalLayoutListener(
          new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              if (binding == null) {
                return;
              }
              int width = binding.sliderMainTimer.getWidth()
                  - binding.sliderMainTimer.getTrackSidePadding() * 2;
              binding.sliderMainTimer.setValueTo(width);
              if (updateControls) {
                updateTimerControls();
              }
              if (binding.sliderMainTimer.getViewTreeObserver().isAlive()) {
                binding.sliderMainTimer.getViewTreeObserver().removeOnGlobalLayoutListener(
                    this
                );
              }
            }
          });
    } else {
      binding.seekbarMainTimer.getViewTreeObserver().addOnGlobalLayoutListener(
          new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              if (binding == null) {
                return;
              }
              int width = binding.seekbarMainTimer.getWidth()
                  - binding.seekbarMainTimer.getPaddingStart()
                  - binding.seekbarMainTimer.getPaddingEnd();
              binding.seekbarMainTimer.setMax(width);
              if (updateControls) {
                updateTimerControls();
              }
              if (binding.seekbarMainTimer.getViewTreeObserver().isAlive()) {
                binding.seekbarMainTimer.getViewTreeObserver().removeOnGlobalLayoutListener(
                    this
                );
              }
            }
          });
    }
  }

  public void updateTimerDisplay() {
    if (binding == null) {
      return;
    }
    binding.textMainTimerTotal.setText(getMetronomeUtil().getTotalTimeString());
    binding.textMainTimerCurrent.setText(getMetronomeUtil().getCurrentTimerString());
  }

  public void updateElapsedDisplay() {
    if (binding == null) {
      return;
    }
    binding.textMainElapsedTime.setText(getMetronomeUtil().getElapsedTimeString());
  }

  @OptIn(markerClass = ExperimentalBadgeUtils.class)
  public void updateOptions(boolean animated) {
    if (optionsBadgeAnimator != null) {
      optionsBadgeAnimator.pause();
      optionsBadgeAnimator.removeAllUpdateListeners();
      optionsBadgeAnimator.removeAllListeners();
      optionsBadgeAnimator.cancel();
      optionsBadgeAnimator = null;
    }
    boolean isIncremental = getMetronomeUtil().getIncrementalAmount() > 0;
    boolean isTimerActive = getMetronomeUtil().isTimerActive();
    int modifierCount = 0;
    if (isIncremental) {
      modifierCount += 1;
    }
    if (isTimerActive) {
      modifierCount += 1;
    }
    boolean show = modifierCount > 0;
    optionsBadge.setNumber(modifierCount);
    if (animated) {
      optionsBadgeAnimator = ValueAnimator.ofInt(optionsBadge.getAlpha(), show ? 255 : 0);
      optionsBadgeAnimator.addUpdateListener(animation -> {
        if (binding == null) {
          return;
        }
        optionsBadge.setAlpha((int) animation.getAnimatedValue());
        float fraction = (float) ((int) animation.getAnimatedValue()) / 255;
        int colorBg = ResUtil.getColor(activity, R.attr.colorError);
        int color = ColorUtils.blendARGB(Color.TRANSPARENT, colorBg, fraction);
        optionsBadge.setBackgroundColor(color);
      });
      optionsBadgeAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          if (!show) {
            BadgeUtils.detachBadgeDrawable(optionsBadge, binding.buttonMainOptions);
          }
        }
      });
      optionsBadgeAnimator.setInterpolator(new FastOutSlowInInterpolator());
      optionsBadgeAnimator.setDuration(Constants.ANIM_DURATION_LONG);
      optionsBadgeAnimator.start();
      if (show) {
        BadgeUtils.attachBadgeDrawable(optionsBadge, binding.buttonMainOptions);
      }
    } else {
      optionsBadge.setAlpha(show ? 255 : 0);
      optionsBadge.setBackgroundColor(
          show ? ResUtil.getColor(activity, R.attr.colorError) : Color.TRANSPARENT
      );
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (binding == null) {
          return;
        }
        if (show) {
          BadgeUtils.attachBadgeDrawable(optionsBadge, binding.buttonMainOptions);
        } else {
          BadgeUtils.detachBadgeDrawable(optionsBadge, binding.buttonMainOptions);
        }
      }, 1);
    }
  }

  private Chip getBookmarkChip(int tempo) {
    Chip chip = new Chip(activity);
    chip.setCheckable(false);
    chip.setChipIconResource(R.drawable.ic_rounded_music_note_anim);
    chip.setCloseIconResource(R.drawable.ic_rounded_close);
    chip.setCloseIconVisible(true);
    chip.setOnCloseIconClickListener(v -> {
      performHapticClick();
      binding.chipGroupMainBookmarks.removeView(chip);
      bookmarks.remove((Integer) tempo); // Integer cast required, else it would take int as index
      shortcutUtil.removeShortcut(tempo);
      updateBookmarks();
      refreshBookmarks(false, true);
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (binding != null) {
          refreshBookmarks(true, true);
        }
      }, 300);
    });
    chip.setStateListAnimator(null);
    chip.setText(getString(R.string.label_bpm_value, tempo));
    chip.setTag(tempo);
    if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
      // Crashes on API 21
      chip.setTextAppearance(R.style.TextAppearance_Tack_LabelLarge);
    }
    chip.setOnClickListener(v -> {
      performHapticClick();
      ViewUtil.startIcon(chip.getChipIcon());
      setTempo(tempo);
    });
    return chip;
  }

  private void updateBookmarks() {
    Set<String> bookmarksSet = new HashSet<>();
    for (Integer tempo : bookmarks) {
      bookmarksSet.add(String.valueOf(tempo));
    }
    getSharedPrefs().edit().putStringSet(PREF.BOOKMARKS, bookmarksSet).apply();
  }

  private void refreshBookmarks(boolean alignActiveOrCenter, boolean animated) {
    binding.buttonMainBookmark.setEnabled(!bookmarks.contains(getMetronomeUtil().getTempo()));
    for (int i = 0; i < binding.chipGroupMainBookmarks.getChildCount(); i++) {
      Chip chip = (Chip) binding.chipGroupMainBookmarks.getChildAt(i);
      if (chip == null) {
        continue;
      }
      Object tag = chip.getTag();
      boolean isActive = tag != null && ((int) tag) == getMetronomeUtil().getTempo();
      int colorBg = isActive
          ? ResUtil.getColor(activity, R.attr.colorTertiaryContainer)
          : Color.TRANSPARENT;
      int colorStroke = ResUtil.getColor(
          activity, isActive ? R.attr.colorTertiary : R.attr.colorOutline
      );
      int colorIcon = ResUtil.getColor(
          activity, isActive ? R.attr.colorOnTertiaryContainer : R.attr.colorOnSurfaceVariant
      );
      int colorText = ResUtil.getColor(
          activity, isActive ? R.attr.colorOnTertiaryContainer : R.attr.colorOnSurface
      );
      chip.setChipBackgroundColor(ColorStateList.valueOf(colorBg));
      chip.setChipStrokeColor(ColorStateList.valueOf(colorStroke));
      chip.setChipIconTint(ColorStateList.valueOf(colorIcon));
      chip.setCloseIconTint(ColorStateList.valueOf(colorIcon));
      chip.setTextColor(colorText);
      if (isActive && alignActiveOrCenter) {
        int scrollX = binding.scrollHorizMainBookmarks.getScrollX();
        int chipStart = isRtl ? chip.getRight() : chip.getLeft();
        int chipEnd = isRtl ? chip.getLeft() : chip.getRight();
        int scrollViewWidth = binding.scrollHorizMainBookmarks.getWidth();
        int margin = UiUtil.dpToPx(activity, 16);
        int start = chipStart + margin * (isRtl ? 1 : -1);
        int end = chipEnd + margin * (isRtl ? -1 : 1);
        if (start < scrollX) {
          if (animated) {
            binding.scrollHorizMainBookmarks.smoothScrollTo(start, 0);
          } else {
            binding.scrollHorizMainBookmarks.scrollTo(start, 0);
          }
        } else if (end > (scrollX + scrollViewWidth)) {
          int scrollTo = end + scrollViewWidth * (isRtl ? 1 : -1);
          if (animated) {
            binding.scrollHorizMainBookmarks.smoothScrollTo(scrollTo, 0);
          } else {
            binding.scrollHorizMainBookmarks.scrollTo(scrollTo, 0);
          }
        }
      }
    }
    if (alignActiveOrCenter) {
      ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBookmarks);
    }
  }

  private void changeTempo(int difference) {
    int tempoNew = getMetronomeUtil().getTempo() + difference;
    setTempo(tempoNew);
    if (tempoNew >= Constants.TEMPO_MIN && tempoNew <= Constants.TEMPO_MAX) {
      performHapticTick();
    }
  }

  public void setTempo(int tempo) {
    setTempo(getMetronomeUtil().getTempo(), tempo);
  }

  private void setTempo(int tempoOld, int tempoNew) {
    tempoNew = Math.min(Math.max(tempoNew, Constants.TEMPO_MIN), Constants.TEMPO_MAX);
    getMetronomeUtil().setTempo(tempoNew);
    if (binding == null) {
      return;
    }
    binding.textMainTempo.setText(String.valueOf(tempoNew));
    String termNew = getTempoTerm(tempoNew);
    if (!termNew.equals(getTempoTerm(tempoOld))) {
      boolean isFaster = tempoNew > tempoOld;
      binding.textSwitcherMainTempoTerm.setInAnimation(
          activity, isFaster ? R.anim.tempo_term_open_enter : R.anim.tempo_term_close_enter
      );
      binding.textSwitcherMainTempoTerm.setOutAnimation(
          activity, isFaster ? R.anim.tempo_term_open_exit : R.anim.tempo_term_close_exit
      );
      binding.textSwitcherMainTempoTerm.setText(termNew);
    }
    refreshBookmarks(true, true);
    setButtonStates();
  }

  private void setButtonStates() {
    int tempo = getMetronomeUtil().getTempo();
    binding.buttonMainLess.setEnabled(tempo > 1);
    binding.buttonMainMore.setEnabled(tempo < Constants.TEMPO_MAX);
  }

  private void updateTimerProgress(
      float fraction, long duration, boolean animated, boolean linear
  ) {
    stopTimerProgress();
    int max = bigTimerSlider
        ? (int) binding.sliderMainTimer.getValueTo()
        : binding.seekbarMainTimer.getMax();
    if (animated) {
      float progress = getMetronomeUtil().getTimerProgress();
      progressAnimator = ValueAnimator.ofFloat(progress, fraction);
      progressAnimator.addUpdateListener(animation -> {
        if (binding == null || progressTransitionAnimator != null) {
          return;
        }
        if (bigTimerSlider) {
          binding.sliderMainTimer.setValue((int) ((float) animation.getAnimatedValue() * max));
        } else {
          binding.seekbarMainTimer.setProgress((int) ((float) animation.getAnimatedValue() * max));
        }
      });
      progressAnimator.setInterpolator(
          linear ? new LinearInterpolator() : new FastOutSlowInInterpolator()
      );
      progressAnimator.setDuration(duration);
      progressAnimator.start();
    } else if (bigTimerSlider) {
      binding.sliderMainTimer.setValue((int) (fraction * max));
    } else {
      binding.seekbarMainTimer.setProgress((int) (fraction * max));
      binding.seekbarMainTimer.invalidate();
    }
  }

  private void stopTimerProgress() {
    if (progressAnimator != null) {
      progressAnimator.pause();
      progressAnimator.removeAllUpdateListeners();
      progressAnimator.removeAllListeners();
      progressAnimator.cancel();
      progressAnimator = null;
    }
  }

  private void stopTimerTransitionProgress() {
    if (progressTransitionAnimator != null) {
      progressTransitionAnimator.pause();
      progressTransitionAnimator.removeAllUpdateListeners();
      progressTransitionAnimator.removeAllListeners();
      progressTransitionAnimator.cancel();
      progressTransitionAnimator = null;
    }
  }

  private void updateFabCornerRadius(boolean playing, boolean animated) {
    if (fabAnimator != null) {
      fabAnimator.pause();
      fabAnimator.removeAllUpdateListeners();
      fabAnimator.cancel();
      fabAnimator = null;
    }
    if (reduceAnimations) {
      binding.fabMainPlayStop.setShapeAppearanceModel(
          binding.fabMainPlayStop.getShapeAppearanceModel().withCornerSize(cornerSizeStop)
      );
      return;
    }
    float cornerSizeNew = playing ? cornerSizePlay : cornerSizeStop;
    if (animated) {
      fabAnimator = ValueAnimator.ofFloat(cornerSizeCurrent, cornerSizeNew);
      fabAnimator.addUpdateListener(animation -> {
        cornerSizeCurrent = (float) animation.getAnimatedValue();
        binding.fabMainPlayStop.setShapeAppearanceModel(
            binding.fabMainPlayStop.getShapeAppearanceModel().withCornerSize(cornerSizeCurrent)
        );
      });
      fabAnimator.setInterpolator(new FastOutSlowInInterpolator());
      fabAnimator.setDuration(300);
      fabAnimator.start();
    } else {
      cornerSizeCurrent = cornerSizeNew;
      binding.fabMainPlayStop.setShapeAppearanceModel(
          binding.fabMainPlayStop.getShapeAppearanceModel().withCornerSize(cornerSizeNew)
      );
    }
  }

  private void updatePickerAndLogo(boolean showPicker, boolean animated) {
    showPickerNotLogo = showPicker;
    if (pickerLogoAnimator != null) {
      pickerLogoAnimator.pause();
      pickerLogoAnimator.removeAllUpdateListeners();
      pickerLogoAnimator.removeAllListeners();
      pickerLogoAnimator.cancel();
      pickerLogoAnimator = null;
    }
    float pickerAlpha = showPickerNotLogo ? 1f : 0f;
    if (animated) {
      binding.imageMainLogoCenter.setVisibility(View.VISIBLE);
      binding.imageMainLogo.setVisibility(View.VISIBLE);
      pickerLogoAnimator = ValueAnimator.ofFloat(binding.linearMainCenter.getAlpha(), pickerAlpha);
      pickerLogoAnimator.addUpdateListener(animation -> {
        float alpha = (float) animation.getAnimatedValue();
        binding.linearMainCenter.setAlpha(alpha);
        binding.imageMainLogo.setScaleX(alpha);
        binding.imageMainLogo.setScaleY(alpha);
        binding.imageMainLogoPlaceholder.setAlpha(1 - alpha);
        binding.imageMainLogoPlaceholder.setScaleX(1 - alpha);
        binding.imageMainLogoPlaceholder.setScaleY(1 - alpha);
      });
      pickerLogoAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          binding.imageMainLogoCenter.setVisibility(showPickerNotLogo ? View.GONE : View.VISIBLE);
          binding.imageMainLogo.setVisibility(showPickerNotLogo ? View.VISIBLE : View.GONE);
        }
      });
      pickerLogoAnimator.setInterpolator(new FastOutSlowInInterpolator());
      pickerLogoAnimator.setDuration(reduceAnimations ? 150 : 300);
      pickerLogoAnimator.start();
    } else {
      binding.linearMainCenter.setAlpha(showPickerNotLogo ? 1f : 0f);
      binding.imageMainLogoCenter.setVisibility(showPickerNotLogo ? View.GONE : View.VISIBLE);
      binding.imageMainLogo.setVisibility(showPickerNotLogo ? View.VISIBLE : View.GONE);
      binding.imageMainLogo.setScaleX(showPickerNotLogo ? 1f : 0f);
      binding.imageMainLogo.setScaleY(showPickerNotLogo ? 1f : 0f);
      binding.imageMainLogoPlaceholder.setAlpha(showPickerNotLogo ? 0f : 1f);
      binding.imageMainLogoPlaceholder.setScaleX(showPickerNotLogo ? 0f : 1f);
      binding.imageMainLogoPlaceholder.setScaleY(showPickerNotLogo ? 0f : 1f);
    }
  }

  public void showSnackbar(Snackbar snackbar) {
    snackbar.setAnchorView(binding.fabMainPlayStop);
    snackbar.show();
  }

  public String getTempoTerm(int tempo) {
    int resId;
    if (tempo < 60) {
      resId = R.string.label_tempo_largo;
    } else if (tempo < 66) {
      resId = R.string.label_tempo_larghetto;
    } else if (tempo < 76) {
      resId = R.string.label_tempo_adagio;
    } else if (tempo < 108) {
      resId = R.string.label_tempo_andante;
    } else if (tempo < 120) {
      resId = R.string.label_tempo_moderato;
    } else if (tempo < 168) {
      resId = R.string.label_tempo_allegro;
    } else if (tempo < 200) {
      resId = R.string.label_tempo_presto;
    } else {
      resId = R.string.label_tempo_prestissimo;
    }
    return getString(resId);
  }

  public boolean isReduceAnimations() {
    return reduceAnimations;
  }

  public FragmentMainBinding getBinding() {
    return binding;
  }
}