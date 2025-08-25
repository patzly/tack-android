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
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.BEAT_MODE;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.FragmentMainBinding;
import xyz.zedler.patrick.tack.drawable.BeatsBgDrawable;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.LogoUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.NotificationUtil;
import xyz.zedler.patrick.tack.util.OptionsUtil;
import xyz.zedler.patrick.tack.util.dialog.PartsDialogUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.dialog.TempoDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.TempoTapDialogUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.BeatView;
import xyz.zedler.patrick.tack.view.SongPickerView.SongPickerListener;
import xyz.zedler.patrick.tack.view.TempoPickerView.OnPickListener;
import xyz.zedler.patrick.tack.view.TempoPickerView.OnRotationListener;

public class MainFragment extends BaseFragment
    implements OnClickListener, MetronomeListener {

  private static final String TAG = MainFragment.class.getSimpleName();

  private FragmentMainBinding binding;
  private MainActivity activity;
  private Bundle savedState;
  private boolean flashScreen, reduceAnimations, isRtl, isLandTablet, bigLogo, showPickerNotLogo;
  private boolean hideSubControls, activeBeat;
  private LogoUtil logoUtil, logoCenterUtil;
  private ValueAnimator playStopButtonAnimator;
  private float playStopButtonFraction;
  private int colorFlashNormal, colorFlashStrong, colorFlashMuted;
  private DialogUtil dialogUtilGain, dialogUtilSplitScreen, dialogUtilTimer, dialogUtilElapsed;
  private DialogUtil dialogUtilPermission, dialogUtilBeatMode;
  private OptionsUtil optionsUtil;
  private PartsDialogUtil partsDialogUtil;
  private TempoTapDialogUtil tempoTapDialogUtil;
  private TempoDialogUtil tempoDialogUtil;
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

    if (playStopButtonAnimator != null) {
      playStopButtonAnimator.pause();
      playStopButtonAnimator.removeAllUpdateListeners();
      playStopButtonAnimator.cancel();
    }
    binding = null;
    dialogUtilGain.dismiss();
    dialogUtilSplitScreen.dismiss();
    dialogUtilTimer.dismiss();
    dialogUtilElapsed.dismiss();
    dialogUtilPermission.dismiss();
    dialogUtilBeatMode.dismiss();
    tempoDialogUtil.dismiss();
    optionsUtil.dismiss();
    partsDialogUtil.dismiss();
    tempoTapDialogUtil.dismiss();
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

    int liftMode = isLandTablet ? ScrollBehavior.ALWAYS_LIFTED : ScrollBehavior.NEVER_LIFTED;
    new ScrollBehavior().setUpScroll(binding.appBarMain, null, liftMode);

    binding.buttonMainMenu.setOnClickListener(v -> {
      performHapticClick();
      ViewUtil.showMenu(v, R.menu.menu_main, item -> {
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
    });
    ViewUtil.setTooltipText(binding.buttonMainMenu, R.string.action_more);

    flashScreen = getSharedPrefs().getBoolean(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN);
    reduceAnimations = getSharedPrefs().getBoolean(PREF.REDUCE_ANIM, DEF.REDUCE_ANIM);
    hideSubControls = getSharedPrefs().getBoolean(PREF.HIDE_SUB_CONTROLS, DEF.HIDE_SUB_CONTROLS);
    activeBeat = getSharedPrefs().getBoolean(PREF.ACTIVE_BEAT, DEF.ACTIVE_BEAT);

    if (getSharedPrefs().getBoolean(PREF.BIG_TIME_TEXT, DEF.BIG_TIME_TEXT)) {
      Typeface typeface = ResourcesCompat.getFont(activity, R.font.nunito_medium);
      binding.chipMainTimerCurrent.textChipNumbers.setTextSize(28);
      binding.chipMainTimerCurrent.textChipNumbers.setTypeface(typeface);
      binding.chipMainElapsedTime.textChipNumbers.setTextSize(28);
      binding.chipMainElapsedTime.textChipNumbers.setTypeface(typeface);
      binding.chipMainTimerTotal.textChipNumbers.setTextSize(28);
      binding.chipMainTimerTotal.textChipNumbers.setTypeface(typeface);
    } else {
      binding.chipMainTimerCurrent.imageChipNumbers.setImageResource(
          R.drawable.ic_rounded_timer_anim
      );
      binding.chipMainTimerCurrent.imageChipNumbers.setVisibility(View.VISIBLE);
      binding.chipMainElapsedTime.imageChipNumbers.setImageResource(
          R.drawable.ic_rounded_schedule_anim
      );
      binding.chipMainElapsedTime.imageChipNumbers.setVisibility(View.VISIBLE);
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
      transition.setDuration(Constants.ANIM_DURATION_SHORT);
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
    dialogUtilGain.createDialogError(builder -> {
      builder.setTitle(R.string.msg_gain);
      builder.setMessage(R.string.msg_gain_description);
      builder.setPositiveButton(R.string.action_play, (dialog, which) -> {
        performHapticClick();
        getMetronomeUtil().start();
      });
      builder.setNegativeButton(
          R.string.action_deactivate_gain,
          (dialog, which) -> {
            performHapticClick();
            getMetronomeUtil().setGain(0);
            getMetronomeUtil().start();
          });
    });
    dialogUtilGain.showIfWasShown(savedInstanceState);

    dialogUtilPermission = new DialogUtil(activity, "notification_permission");
    dialogUtilPermission.createDialog(builder -> {
      builder.setTitle(R.string.msg_notification_permission);
      builder.setMessage(R.string.msg_notification_permission_description);
      builder.setPositiveButton(R.string.action_next, (dialog, which) -> {
        performHapticClick();
        getMetronomeUtil().start();
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilPermission.showIfWasShown(savedInstanceState);

    dialogUtilSplitScreen = new DialogUtil(activity, "split_screen");
    dialogUtilSplitScreen.createDialog(builder -> {
      builder.setTitle(R.string.msg_split_screen);
      builder.setMessage(R.string.msg_split_screen_description);
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> performHapticClick()
      );
    });
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

    dialogUtilTimer = new DialogUtil(activity, "timer_reset");
    dialogUtilTimer.createDialog(builder -> {
      builder.setTitle(R.string.msg_reset_timer);
      builder.setMessage(R.string.msg_reset_timer_description);
      builder.setPositiveButton(R.string.action_reset, (dialog, which) -> {
        performHapticClick();
        getMetronomeUtil().resetTimerNow();
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilTimer.showIfWasShown(savedInstanceState);

    dialogUtilElapsed = new DialogUtil(activity, "elapsed_reset");
    dialogUtilElapsed.createDialog(builder -> {
      builder.setTitle(R.string.msg_reset_elapsed);
      builder.setMessage(R.string.msg_reset_elapsed_description);
      builder.setPositiveButton(R.string.action_reset, (dialog, which) -> {
        performHapticClick();
        getMetronomeUtil().resetElapsed();
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilElapsed.showIfWasShown(savedInstanceState);

    dialogUtilBeatMode = new DialogUtil(activity, "beat_mode");

    tempoDialogUtil = new TempoDialogUtil(activity, this);
    tempoDialogUtil.showIfWasShown(savedInstanceState);

    optionsUtil = new OptionsUtil(activity, this, () -> updateOptions(true));
    boolean hideOptions = isLandTablet;
    binding.controlsMainBottom.buttonMainOptions.setEnabled(!hideOptions);

    logoUtil = new LogoUtil(binding.imageMainLogo);
    logoCenterUtil = new LogoUtil(binding.imageMainLogoCenter);
    bigLogo = getSharedPrefs().getBoolean(PREF.BIG_LOGO, DEF.BIG_LOGO);

    tempoTapDialogUtil = new TempoTapDialogUtil(activity, this);

    partsDialogUtil = new PartsDialogUtil(activity);
    partsDialogUtil.showIfWasShown(savedInstanceState);

    beatsBgDrawable = new BeatsBgDrawable(activity);
    binding.linearMainBeatsBg.setBackground(beatsBgDrawable);

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
    binding.chipMainTimerCurrent.frameChipNumbersContainer.setOnClickListener(v -> {
      dialogUtilTimer.show();
      performHapticClick();
    });
    binding.chipMainTimerTotal.frameChipNumbersContainer.setOnClickListener(v -> {
      dialogUtilTimer.show();
      performHapticClick();
    });
    binding.chipMainElapsedTime.frameChipNumbersContainer.setOnClickListener(v -> {
      dialogUtilElapsed.show();
      performHapticClick();
    });

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
    binding.circleMain.setOnDragAnimListener(fraction -> {
      if (VERSION.SDK_INT >= VERSION_CODES.O) {
        binding.textMainTempo.setFontVariationSettings("'wght' " + (600 + (fraction * 100)));
      }
    });

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
          updateTempoPickerAndLogo(true, true);
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
          updateTempoPickerAndLogo(false, true);
        }
      }
    });
    binding.tempoPickerMain.setOnClickListener(v -> {
      tempoDialogUtil.show();
      performHapticClick();
    });

    if (binding.controlsMainBottom.buttonMainBeatMode != null) {
      binding.controlsMainBottom.buttonMainBeatMode.setIconResource(
          getSharedPrefs().getString(PREF.BEAT_MODE, DEF.BEAT_MODE).equals(BEAT_MODE.VIBRATION)
              ? R.drawable.ic_rounded_vibration_to_volume_up_anim
              : R.drawable.ic_rounded_volume_up_to_vibration_anim
      );
    }

    setButtonStates(getMetronomeUtil().getTempo());

    binding.songPickerMain.setListener(new SongPickerListener() {
      @Override
      public void onCurrentSongChanged(@NonNull String currentSongId) {
        getMetronomeUtil().setCurrentSong(currentSongId, 0, true);
        performHapticClick();
      }

      @Override
      public void onCurrentSongClicked() {
        partsDialogUtil.show();
        performHapticClick();
      }
    });
    activity.getSongViewModel().getAllSongsWithPartsLive().observe(
        getViewLifecycleOwner(), songs -> {
          List<SongWithParts> songsWithParts = new ArrayList<>(songs);
          for (SongWithParts songWithParts : songsWithParts) {
            // Remove default song from song picker
            if (songWithParts.getSong().getId().equals(Constants.SONG_ID_DEFAULT)) {
              songsWithParts.remove(songWithParts);
              break;
            }
          }
          if (!binding.songPickerMain.isInitialized()) {
            binding.songPickerMain.init(
                getSharedPrefs().getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER),
                getMetronomeUtil().getCurrentSongId(),
                songsWithParts
            );
          }
          binding.songPickerMain.setSongs(songsWithParts);
          if (!isPortrait && !isLandTablet) {
            // Hide song picker in landscape mode if empty to make place for other controls
            binding.frameMainSongsContainer.setVisibility(
                songsWithParts.isEmpty() ? View.GONE : View.VISIBLE
            );
            updateSongPickerDividerVisibility();
          } else {
            binding.frameMainSongsContainer.setVisibility(View.VISIBLE);
          }
        }
    );

    ViewUtil.resetAnimatedIcon(binding.controlsMainBottom.buttonMainPlayStop);
    binding.controlsMainBottom.buttonMainPlayStop.setIconResource(
        R.drawable.ic_rounded_play_to_stop_fill_anim
    );
    binding.controlsMainBottom.buttonMainPlayStop.setOnTouchListener(
        (v, event) -> {
          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getMetronomeUtil().isPlaying()) {
              performHapticClick();
              getMetronomeUtil().stop();
            } else {
              if (getMetronomeUtil().getGain() > 0 && getMetronomeUtil().neverStartedWithGainBefore()) {
                dialogUtilGain.show();
              } else {
                boolean permissionDenied = getSharedPrefs().getBoolean(
                    PREF.PERMISSION_DENIED, false
                );
                if (NotificationUtil.hasPermission(activity) || permissionDenied) {
                  getMetronomeUtil().start();
                } else {
                  dialogUtilPermission.show();
                }
              }
              performHapticClick();
            }
            return false;
          }
          return false;
        });

    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      Typeface variableTypeface = ResourcesCompat.getFont(activity, R.font.nunito_variable_wght);
      binding.textMainTempo.setTypeface(variableTypeface);
      binding.textMainTempo.setFontVariationSettings("'wght' 600");
    }
    updateMetronomeControls();

    ViewUtil.setTooltipText(binding.buttonMainAddBeat, R.string.action_add_beat);
    ViewUtil.setTooltipText(binding.buttonMainRemoveBeat, R.string.action_remove_beat);
    ViewUtil.setTooltipText(binding.buttonMainAddSubdivision, R.string.action_add_sub);
    ViewUtil.setTooltipText(binding.buttonMainRemoveSubdivision, R.string.action_remove_sub);
    ViewUtil.setTooltipText(binding.controlsMainBottom.buttonMainOptions, R.string.title_options);
    if (binding.controlsMainBottom.buttonMainMenuBottom != null) {
      ViewUtil.setTooltipText(
          binding.controlsMainBottom.buttonMainMenuBottom, R.string.action_more
      );
    }
    if (binding.controlsMainBottom.buttonMainTempoTap != null) {
      ViewUtil.setTooltipText(
          binding.controlsMainBottom.buttonMainTempoTap, R.string.action_tempo_tap
      );
    }
    if (binding.controlsMainBottom.buttonMainSongs != null) {
      ViewUtil.setTooltipText(
          binding.controlsMainBottom.buttonMainSongs, R.string.title_songs
      );
    }
    if (binding.controlsMainBottom.buttonMainBeatMode != null) {
      ViewUtil.setTooltipText(
          binding.controlsMainBottom.buttonMainBeatMode, R.string.action_beat_mode
      );
    }

    ViewUtil.setTooltipTextAndContentDescription(
        binding.buttonMainLess1,
        getString(R.string.options_incremental_amount_decrease, 1)
    );
    ViewUtil.setTooltipTextAndContentDescription(
        binding.buttonMainLess5,
        getString(R.string.options_incremental_amount_decrease, 5)
    );
    ViewUtil.setTooltipTextAndContentDescription(
        binding.buttonMainLess10,
        getString(R.string.options_incremental_amount_decrease, 10)
    );
    ViewUtil.setTooltipTextAndContentDescription(
        binding.buttonMainMore1,
        getString(R.string.options_incremental_amount_increase, 1)
    );
    ViewUtil.setTooltipTextAndContentDescription(
        binding.buttonMainMore5,
        getString(R.string.options_incremental_amount_increase, 5)
    );
    ViewUtil.setTooltipTextAndContentDescription(
        binding.buttonMainMore10,
        getString(R.string.options_incremental_amount_increase, 10)
    );

    ViewUtil.setOnClickListeners(
        this,
        binding.buttonMainAddBeat,
        binding.buttonMainRemoveBeat,
        binding.buttonMainAddSubdivision,
        binding.buttonMainRemoveSubdivision,
        binding.buttonMainLess1, binding.buttonMainLess5, binding.buttonMainLess10,
        binding.buttonMainMore1, binding.buttonMainMore5, binding.buttonMainMore10,
        binding.controlsMainBottom.buttonMainBeatMode,
        binding.controlsMainBottom.buttonMainSongs,
        binding.controlsMainBottom.buttonMainOptions,
        binding.controlsMainBottom.buttonMainTempoTap,
        binding.controlsMainBottom.buttonMainMenuBottom
    );
  }

  @Override
  public void onPause() {
    super.onPause();
    stopTimerProgress();
    stopTimerProgressTransition();
  }

  @Override
  public void onResume() {
    super.onResume();
    updateTimerControls(true, true);
    updateElapsedDisplay();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (dialogUtilGain != null) {
      dialogUtilGain.saveState(outState);
    }
    if (dialogUtilPermission != null) {
      dialogUtilPermission.saveState(outState);
    }
    if (dialogUtilTimer != null) {
      dialogUtilTimer.saveState(outState);
    }
    if (dialogUtilElapsed != null) {
      dialogUtilElapsed.saveState(outState);
    }
    if (dialogUtilBeatMode != null) {
      dialogUtilBeatMode.saveState(outState);
    }
    if (partsDialogUtil != null) {
      partsDialogUtil.saveState(outState);
    }
    if (optionsUtil != null) {
      optionsUtil.saveState(outState);
    }
    if (tempoDialogUtil != null) {
      tempoDialogUtil.saveState(outState);
    }
    if (tempoTapDialogUtil != null) {
      tempoTapDialogUtil.saveState(outState);
    }
  }

  public void updateMetronomeControls() {
    if (binding == null) {
      return;
    }
    getMetronomeUtil().addListener(this);
    optionsUtil.showIfWasShown(savedState);
    tempoTapDialogUtil.showIfWasShown(savedState);

    savedState = null;

    if (binding.controlsMainBottom.buttonMainBeatMode != null) {
      binding.controlsMainBottom.buttonMainBeatMode.setIconResource(
          getMetronomeUtil().getBeatMode().equals(BEAT_MODE.VIBRATION)
              ? R.drawable.ic_rounded_vibration_to_volume_up_anim
              : R.drawable.ic_rounded_volume_up_to_vibration_anim
      );
    }

    updateBeats(getMetronomeUtil().getBeats());
    updateBeatControls(false);
    updateSubs(getMetronomeUtil().getSubdivisions());
    updateSubControls(false);

    measureTimerControls(true); // calls updateTimerControls when measured
    updateElapsedDisplay();
    updateOptions(false);

    int tempo = getMetronomeUtil().getTempo();
    updateTempoDisplay(tempo, tempo);
    binding.textSwitcherMainTempoTerm.setCurrentText(getTempoTerm(tempo));

    boolean showLogo = bigLogo && getMetronomeUtil().isPlaying();
    updateTempoPickerAndLogo(!showLogo, false);

    if (getMetronomeUtil().isCountingIn()) {
      beatsBgDrawable.reset();
      if (getMetronomeUtil().getCountIn() > 0) {
        beatsBgDrawable.setProgress(getMetronomeUtil().getCountInProgress(), 0);
        beatsBgDrawable.setProgress(1, getMetronomeUtil().getCountInIntervalRemaining());
      }
    }

    ViewUtil.resetAnimatedIcon(binding.controlsMainBottom.buttonMainPlayStop);
    binding.controlsMainBottom.buttonMainPlayStop.setIconResource(
        getMetronomeUtil().isPlaying()
            ? R.drawable.ic_rounded_stop_fill
            : R.drawable.ic_rounded_play_arrow_fill
    );
    updatePlayStopButton(getMetronomeUtil().isPlaying(), false);

    Map<String, String> beatModeLabels = new LinkedHashMap<>();
    beatModeLabels.put(BEAT_MODE.ALL, getString(R.string.label_beat_mode_all));
    beatModeLabels.put(BEAT_MODE.SOUND, getString(R.string.label_beat_mode_sound));
    beatModeLabels.put(BEAT_MODE.VIBRATION, getString(R.string.label_beat_mode_vibration));
    ArrayList<String> beatModes = new ArrayList<>(beatModeLabels.keySet());
    String[] items = beatModeLabels.values().toArray(new String[]{});
    int init = beatModes.indexOf(getMetronomeUtil().getBeatMode());
    if (init == -1) {
      init = 0;
      getSharedPrefs().edit().remove(PREF.BEAT_MODE).apply();
    }
    int initFinal = init;
    dialogUtilBeatMode.createDialog(builder -> {
      builder.setTitle(R.string.action_beat_mode);
      if (activity.getHapticUtil().hasVibrator()) {
        builder.setSingleChoiceItems(
            items, initFinal, (dialog, which) -> {
              String beatModePrev = getMetronomeUtil().getBeatMode();
              String beatMode = beatModes.get(which);
              if (beatMode.equals(BEAT_MODE.SOUND)) {
                performHapticClick();
              }
              getMetronomeUtil().setBeatMode(beatMode);
              if (!beatMode.equals(BEAT_MODE.SOUND)) {
                performHapticClick();
              }

              if (beatModePrev.equals(BEAT_MODE.VIBRATION)
                  && !beatMode.equals(BEAT_MODE.VIBRATION)) {
                binding.controlsMainBottom.buttonMainBeatMode.setIconResource(
                    R.drawable.ic_rounded_vibration_to_volume_up_anim
                );
                ViewUtil.startIcon(binding.controlsMainBottom.buttonMainBeatMode.getIcon());
              } else if (!beatModePrev.equals(BEAT_MODE.VIBRATION)
                  && beatMode.equals(BEAT_MODE.VIBRATION)) {
                binding.controlsMainBottom.buttonMainBeatMode.setIconResource(
                    R.drawable.ic_rounded_volume_up_to_vibration_anim
                );
                ViewUtil.startIcon(binding.controlsMainBottom.buttonMainBeatMode.getIcon());
              }
            }
        );
      } else {
        builder.setMessage(R.string.msg_vibration_unavailable);
      }
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilBeatMode.showIfWasShown(savedState);

    boolean keepAwake = getMetronomeUtil().getKeepAwake() && getMetronomeUtil().isPlaying();
    UiUtil.keepScreenAwake(activity, keepAwake);
  }

  @Override
  public void onMetronomeStart() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        beatsBgDrawable.reset();
        if (getMetronomeUtil().getCountIn() > 0) {
          beatsBgDrawable.setProgress(1, getMetronomeUtil().getCountInInterval());
        }
        binding.controlsMainBottom.buttonMainPlayStop.setIconResource(
            R.drawable.ic_rounded_play_to_stop_fill_anim
        );
        Drawable startStopIcon = binding.controlsMainBottom.buttonMainPlayStop.getIcon();
        if (startStopIcon != null) {
          ((Animatable) startStopIcon).start();
        }
        updatePlayStopButton(true, !reduceAnimations);
        if (bigLogo) {
          updateTempoPickerAndLogo(false, true);
        }
      }
      UiUtil.keepScreenAwake(activity, getMetronomeUtil().getKeepAwake());
    });
  }

  @Override
  public void onMetronomeStop() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        resetActiveBeats();
        beatsBgDrawable.setProgressVisible(false, true);
        updateTimerDisplay();
        updateElapsedDisplay();
        binding.controlsMainBottom.buttonMainPlayStop.setIconResource(
            R.drawable.ic_rounded_stop_to_play_fill_anim
        );
        Drawable icon = binding.controlsMainBottom.buttonMainPlayStop.getIcon();
        if (icon != null) {
          ((Animatable) icon).start();
        }
        updatePlayStopButton(false, !reduceAnimations);
        if (bigLogo) {
          updateTempoPickerAndLogo(true, true);
        }
      }
      stopTimerProgressTransition();
      stopTimerProgress();
      UiUtil.keepScreenAwake(activity, false);
    });
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
      if ((getMetronomeUtil().isSubdivisionActive() || !hideSubControls)) {
        if (!(subdivision instanceof BeatView)) {
          return;
        }
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
        if (!reduceAnimations) {
          logoUtil.nextBeat(getMetronomeUtil().getInterval());
        }
        if (bigLogo) {
          logoCenterUtil.nextBeat(getMetronomeUtil().getInterval());
        }
      }
      if (getMetronomeUtil().getTimerUnit().equals(UNIT.BARS)) {
        updateTimerDisplay();
      }
    });
  }

  @Override
  public void onMetronomeTempoChanged(int tempoOld, int tempoNew) {
    activity.runOnUiThread(() -> updateTempoDisplay(tempoOld, tempoNew));
  }

  @Override
  public void onMetronomeTimerStarted() {
    activity.runOnUiThread(() -> {
      stopTimerProgressTransition();
      stopTimerProgress();
      if (binding == null) {
        return;
      }
      updateTimerControls(true, true);
    });
  }

  @Override
  public void onMetronomeElapsedTimeSecondsChanged() {
    activity.runOnUiThread(this::updateElapsedDisplay);
  }

  @Override
  public void onMetronomeTimerSecondsChanged() {
    activity.runOnUiThread(this::updateTimerDisplay);
  }

  @Override
  public void onMetronomeTimerProgressOneTime(boolean withTransition) {
    activity.runOnUiThread(() -> {
      if (binding == null) {
        return;
      }
      updateTimerControls(true, withTransition);
    });
  }

  @Override
  public void onMetronomeConfigChanged() {
    activity.runOnUiThread(() -> {
      if (binding == null) {
        return;
      }
      // tempo is updated in onMetronomeTempoChanged
      updateBeats(getMetronomeUtil().getBeats());
      updateBeatControls(true);
      updateSubs(getMetronomeUtil().getSubdivisions());
      updateSubControls(true);

      updateTimerControls(true, true);
      updateElapsedDisplay();
      updateOptions(true);
    });
  }

  @Override
  public void onMetronomeSongOrPartChanged(@Nullable SongWithParts song, int partIndex) {
    activity.runOnUiThread(() -> {
      if (song != null && binding != null) {
        partsDialogUtil.update();
      }
    });
  }

  @Override
  public void onMetronomeConnectionMissing() {
    activity.runOnUiThread(() -> showSnackbar(
        activity.getSnackbar(R.string.msg_connection_lost, Snackbar.LENGTH_SHORT)
    ));
  }

  @Override
  public void onMetronomePermissionMissing() {
    activity.runOnUiThread(() -> activity.requestNotificationPermission(true));
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
        optionsUtil.updateSubdivisions();
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
        optionsUtil.updateSubdivisions();
        optionsUtil.updateSwing();
      }
    } else if (id == R.id.button_main_less_1) {
      ViewUtil.startIcon(binding.buttonMainLess1.getIcon());
      changeTempo(-1);
    } else if (id == R.id.button_main_less_5) {
      ViewUtil.startIcon(binding.buttonMainLess5.getIcon());
      changeTempo(-5);
    } else if (id == R.id.button_main_less_10) {
      ViewUtil.startIcon(binding.buttonMainLess10.getIcon());
      changeTempo(-10);
    } else if (id == R.id.button_main_more_1) {
      ViewUtil.startIcon(binding.buttonMainMore1.getIcon());
      changeTempo(1);
    } else if (id == R.id.button_main_more_5) {
      ViewUtil.startIcon(binding.buttonMainMore5.getIcon());
      changeTempo(5);
    } else if (id == R.id.button_main_more_10) {
      ViewUtil.startIcon(binding.buttonMainMore10.getIcon());
      changeTempo(10);
    } else if (id == R.id.button_main_beat_mode) {
      performHapticClick();
      dialogUtilBeatMode.show();
      if (getMetronomeUtil().getBeatMode().equals(BEAT_MODE.VIBRATION)) {
        // Use available animated icon for click
        if (binding.controlsMainBottom.buttonMainBeatMode != null) {
          binding.controlsMainBottom.buttonMainBeatMode.setIconResource(
              R.drawable.ic_rounded_vibration_anim
          );
          ViewUtil.startIcon(binding.controlsMainBottom.buttonMainBeatMode.getIcon());
        }
      }
    } else if (id == R.id.button_main_songs) {
      performHapticClick();
      int visitCount = getSharedPrefs().getInt(PREF.SONGS_VISIT_COUNT, 0);
      if (visitCount != -1) { // no widget created and no dialog shown yet
        visitCount++;
        getSharedPrefs().edit().putInt(PREF.SONGS_VISIT_COUNT, visitCount).apply();
      }
      activity.navigate(MainFragmentDirections.actionMainToSongs());
    } else if (id == R.id.button_main_options) {
      performHapticClick();
      ViewUtil.startIcon(binding.controlsMainBottom.buttonMainOptions.getIcon());
      optionsUtil.show();
    } else if (id == R.id.button_main_tempo_tap) {
      performHapticClick();
      if (binding.controlsMainBottom.buttonMainTempoTap != null) {
        ViewUtil.startIcon(binding.controlsMainBottom.buttonMainTempoTap.getIcon());
      }
      tempoTapDialogUtil.update();
      tempoTapDialogUtil.show();
    } else if (id == R.id.button_main_menu_bottom) {
      performHapticClick();
      ViewUtil.showMenu(v, R.menu.menu_main_bottom_collapsed, item -> {
        int itemId = item.getItemId();
        if (getViewUtil().isClickDisabled(itemId)) {
          return false;
        }
        performHapticClick();
        if (itemId == R.id.action_songs) {
          activity.navigate(MainFragmentDirections.actionMainToSongs());
        } else if (itemId == R.id.action_tempo_tap) {
          tempoTapDialogUtil.update();
          tempoTapDialogUtil.show();
        } else if (itemId == R.id.action_beat_mode) {
          dialogUtilBeatMode.show();
        }
        return true;
      }, Gravity.CENTER);
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
          if (!show && binding != null) {
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
        getMetronomeUtil().isSubdivisionActive() || !hideSubControls ? View.VISIBLE : View.GONE
    );
    updateSongPickerDividerVisibility();
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
          if (!show && binding != null) {
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

  public void updateTimerControls(boolean animated, boolean withTransition) {
    boolean isPlaying = getMetronomeUtil().isPlaying();
    boolean isTimerActive = getMetronomeUtil().isTimerActive();
    int visibility = isTimerActive ? View.VISIBLE : View.GONE;
    binding.chipMainTimerCurrent.frameChipNumbersContainer.setVisibility(visibility);
    binding.chipMainTimerTotal.frameChipNumbersContainer.setVisibility(visibility);
    binding.sliderMainTimer.setVisibility(visibility);
    updateSongPickerDividerVisibility();
    binding.sliderMainTimer.setContinuousModeTickCount(getMetronomeUtil().getTimerDuration() + 1);
    measureTimerControls(false);
    // Check if timer is currently running and if metronome is from service
    if (!getMetronomeUtil().isFromService()) {
      return;
    }
    if (isPlaying && isTimerActive && !getMetronomeUtil().isCountingIn()) {
      if (withTransition) {
        long timerInterval = getMetronomeUtil().getTimerInterval();
        float fraction = (float) Constants.ANIM_DURATION_LONG / timerInterval;
        fraction += getMetronomeUtil().getTimerProgress();
        startTimerProgressTransition(fraction);
      }
      updateTimerProgress(
          1, getMetronomeUtil().getTimerIntervalRemaining(), animated, true
      );
    } else {
      float timerProgress = getMetronomeUtil().getTimerProgress();
      if (animated && getMetronomeUtil().isFromService()) {
        startTimerProgressTransition(timerProgress);
      } else if (getMetronomeUtil().isFromService()) {
        updateTimerProgress(timerProgress, 0, false, false);
      }
    }
    updateTimerDisplay();
  }

  private void measureTimerControls(boolean updateControls) {
    binding.sliderMainTimer.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (binding == null) {
              return;
            }
            int width = binding.sliderMainTimer.getWidth()
                - binding.sliderMainTimer.getTrackSidePadding() * 2;
            float valueFrom = binding.sliderMainTimer.getValueFrom();
            float valueTo = Math.max(valueFrom, width);
            if (valueFrom < valueTo) {
              binding.sliderMainTimer.setValueTo(valueTo);
            }
            if (updateControls) {
              updateTimerControls(
                  getMetronomeUtil().isPlaying() && getMetronomeUtil().isTimerActive(),
                  true
              );
            }
            if (binding.sliderMainTimer.getViewTreeObserver().isAlive()) {
              binding.sliderMainTimer.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        });
  }

  public void updateTimerDisplay() {
    if (binding == null) {
      return;
    }
    binding.chipMainTimerTotal.textChipNumbers.setText(getMetronomeUtil().getTotalTimeString());
    binding.chipMainTimerCurrent.textChipNumbers.setText(
        getMetronomeUtil().getCurrentTimerString()
    );
  }

  public void updateElapsedDisplay() {
    if (binding == null) {
      return;
    }
    boolean isElapsedActive = getMetronomeUtil().isElapsedActive();
    binding.chipMainElapsedTime.frameChipNumbersContainer.setVisibility(
        isElapsedActive ? View.VISIBLE : View.GONE
    );
    binding.chipMainElapsedTime.textChipNumbers.setText(getMetronomeUtil().getElapsedTimeString());
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
    int modifierCount = getModifierCount();
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
          if (!show && binding != null) {
            BadgeUtils.detachBadgeDrawable(
                optionsBadge, binding.controlsMainBottom.buttonMainOptions
            );
          }
        }
      });
      optionsBadgeAnimator.setInterpolator(new FastOutSlowInInterpolator());
      optionsBadgeAnimator.setDuration(Constants.ANIM_DURATION_LONG);
      optionsBadgeAnimator.start();
      if (show) {
        BadgeUtils.attachBadgeDrawable(optionsBadge, binding.controlsMainBottom.buttonMainOptions);
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
          BadgeUtils.attachBadgeDrawable(
              optionsBadge, binding.controlsMainBottom.buttonMainOptions
          );
        } else {
          BadgeUtils.detachBadgeDrawable(
              optionsBadge, binding.controlsMainBottom.buttonMainOptions
          );
        }
      }, 1);
    }
  }

  private int getModifierCount() {
    MetronomeUtil metronome = getMetronomeUtil();
    return (metronome.isCountInActive() ? 1 : 0) +
        (metronome.isIncrementalActive() ? 1 : 0) +
        (metronome.isTimerActive() ? 1 : 0) +
        (metronome.isMuteActive() ? 1 : 0) +
        (metronome.isSubdivisionActive() && hideSubControls ? 1 : 0);
  }

  private void changeTempo(int difference) {
    int tempoNew = getMetronomeUtil().getTempo() + difference;
    if (tempoNew >= Constants.TEMPO_MIN && tempoNew <= Constants.TEMPO_MAX) {
      updateTempoDisplay(getMetronomeUtil().getTempo(), tempoNew);
      getMetronomeUtil().setTempo(tempoNew);
      performHapticTick();
    }
  }

  public void updateTempoDisplay(int tempoOld, int tempoNew) {
    tempoNew = Math.min(Math.max(tempoNew, Constants.TEMPO_MIN), Constants.TEMPO_MAX);
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
    setButtonStates(tempoNew);
  }

  private void setButtonStates(int tempo) {
    binding.buttonMainLess1.setEnabled(tempo > 1);
    binding.buttonMainLess5.setEnabled(tempo > 5);
    binding.buttonMainLess10.setEnabled(tempo > 10);
    binding.buttonMainMore1.setEnabled(tempo < Constants.TEMPO_MAX);
    binding.buttonMainMore5.setEnabled(tempo <= Constants.TEMPO_MAX - 5);
    binding.buttonMainMore10.setEnabled(tempo <= Constants.TEMPO_MAX - 10);
  }

  private void updateTimerProgress(
      float fraction, long duration, boolean animated, boolean linear
  ) {
    stopTimerProgress();
    int max = (int) binding.sliderMainTimer.getValueTo();
    if (animated) {
      float progress = getMetronomeUtil().getTimerProgress();
      progressAnimator = ValueAnimator.ofFloat(progress, fraction);
      progressAnimator.addUpdateListener(animation -> {
        if (binding == null || progressTransitionAnimator != null) {
          return;
        }
        binding.sliderMainTimer.setValue((int) ((float) animation.getAnimatedValue() * max));
      });
      progressAnimator.setInterpolator(
          linear ? new LinearInterpolator() : new FastOutSlowInInterpolator()
      );
      progressAnimator.setDuration(duration);
      progressAnimator.start();
    } else {
      binding.sliderMainTimer.setValue((int) (fraction * max));
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

  private void startTimerProgressTransition(float fractionTo) {
    int current = (int) binding.sliderMainTimer.getValue();
    int max = (int) binding.sliderMainTimer.getValueTo();
    float currentFraction = current / (float) max;
    if (getMetronomeUtil().equalsTimerProgress(currentFraction)) {
      // only if current progress is not equal to timer progress
      return;
    }
    progressTransitionAnimator = ValueAnimator.ofFloat(currentFraction, fractionTo);
    progressTransitionAnimator.addUpdateListener(animation -> {
      if (binding == null) {
        return;
      }
      binding.sliderMainTimer.setValue((int) ((float) animation.getAnimatedValue() * max));
    });
    progressTransitionAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        stopTimerProgressTransition();
      }
    });
    progressTransitionAnimator.setInterpolator(new FastOutSlowInInterpolator());
    progressTransitionAnimator.setDuration(Constants.ANIM_DURATION_LONG);
    progressTransitionAnimator.start();
  }

  private void stopTimerProgressTransition() {
    if (progressTransitionAnimator != null) {
      progressTransitionAnimator.pause();
      progressTransitionAnimator.removeAllUpdateListeners();
      progressTransitionAnimator.removeAllListeners();
      progressTransitionAnimator.cancel();
      progressTransitionAnimator = null;
    }
  }

  private void updatePlayStopButton(boolean playing, boolean animated) {
    binding.controlsMainBottom.buttonMainPlayStop.setChecked(playing);
    if (playStopButtonAnimator != null) {
      playStopButtonAnimator.pause();
      playStopButtonAnimator.removeAllUpdateListeners();
      playStopButtonAnimator.cancel();
      playStopButtonAnimator = null;
    }
    int colorBgPlaying = ResUtil.getColor(activity, R.attr.colorTertiary);
    int colorBgStopped = ResUtil.getColor(activity, R.attr.colorPrimary);
    int colorFgPlaying = ResUtil.getColor(activity, R.attr.colorOnTertiary);
    int colorFgStopped = ResUtil.getColor(activity, R.attr.colorOnPrimary);
    float targetFraction = playing ? 1f : 0f;
    if (animated) {
      playStopButtonAnimator = ValueAnimator.ofFloat(playStopButtonFraction, targetFraction);
      playStopButtonAnimator.addUpdateListener(animation -> {
        playStopButtonFraction = (float) animation.getAnimatedValue();
        binding.controlsMainBottom.buttonMainPlayStop.setBackgroundColor(
            ColorUtils.blendARGB(colorBgStopped, colorBgPlaying, playStopButtonFraction)
        );
        binding.controlsMainBottom.buttonMainPlayStop.setIconTint(
            ColorStateList.valueOf(
                ColorUtils.blendARGB(colorFgStopped, colorFgPlaying, playStopButtonFraction)
            )
        );
      });
      playStopButtonAnimator.setInterpolator(new FastOutSlowInInterpolator());
      playStopButtonAnimator.setDuration(300);
      playStopButtonAnimator.start();
    } else {
      playStopButtonFraction = targetFraction;
      binding.controlsMainBottom.buttonMainPlayStop.setBackgroundColor(
          ColorUtils.blendARGB(colorBgStopped, colorBgPlaying, playStopButtonFraction)
      );
      binding.controlsMainBottom.buttonMainPlayStop.setIconTint(
          ColorStateList.valueOf(
              ColorUtils.blendARGB(colorFgStopped, colorFgPlaying, playStopButtonFraction)
          )
      );
    }
  }

  private void updateTempoPickerAndLogo(boolean showPicker, boolean animated) {
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
      pickerLogoAnimator = ValueAnimator.ofFloat(binding.frameMainCenter.getAlpha(), pickerAlpha);
      pickerLogoAnimator.addUpdateListener(animation -> {
        float alpha = (float) animation.getAnimatedValue();
        binding.frameMainCenter.setAlpha(alpha);
        binding.imageMainLogo.setScaleX(alpha);
        binding.imageMainLogo.setScaleY(alpha);
        binding.imageMainLogoPlaceholder.setAlpha(1 - alpha);
        binding.imageMainLogoPlaceholder.setScaleX(1 - alpha);
        binding.imageMainLogoPlaceholder.setScaleY(1 - alpha);
      });
      pickerLogoAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          if (binding != null) {
            binding.imageMainLogoCenter.setVisibility(showPickerNotLogo ? View.GONE : View.VISIBLE);
            binding.imageMainLogo.setVisibility(showPickerNotLogo ? View.VISIBLE : View.GONE);
          }
        }
      });
      pickerLogoAnimator.setInterpolator(new FastOutSlowInInterpolator());
      pickerLogoAnimator.setDuration(reduceAnimations ? 150 : 300);
      pickerLogoAnimator.start();
    } else {
      binding.frameMainCenter.setAlpha(showPickerNotLogo ? 1f : 0f);
      binding.imageMainLogoCenter.setVisibility(showPickerNotLogo ? View.GONE : View.VISIBLE);
      binding.imageMainLogo.setVisibility(showPickerNotLogo ? View.VISIBLE : View.GONE);
      binding.imageMainLogo.setScaleX(showPickerNotLogo ? 1f : 0f);
      binding.imageMainLogo.setScaleY(showPickerNotLogo ? 1f : 0f);
      binding.imageMainLogoPlaceholder.setAlpha(showPickerNotLogo ? 0f : 1f);
      binding.imageMainLogoPlaceholder.setScaleX(showPickerNotLogo ? 0f : 1f);
      binding.imageMainLogoPlaceholder.setScaleY(showPickerNotLogo ? 0f : 1f);
    }
  }

  private void updateSongPickerDividerVisibility() {
    if (binding == null || binding.scrollMainTop == null) {
      return; // not landscape phone
    }
    binding.scrollMainTop.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (binding == null || binding.frameMainSongsContainer.getVisibility() == View.GONE) {
              return;
            }
            int scrollViewHeight = binding.scrollMainTop.getMeasuredHeight();
            int scrollContentHeight = binding.linearMainTop.getHeight();
            boolean showDivider = scrollContentHeight > scrollViewHeight;
            if (binding.dividerMainSongs != null) {
              binding.dividerMainSongs.setVisibility(showDivider ? View.VISIBLE : View.GONE);
            }
            if (binding.scrollMainTop.getViewTreeObserver().isAlive()) {
              binding.scrollMainTop.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
          }
        });
  }

  public void showSnackbar(Snackbar snackbar) {
    snackbar.setAnchorView(binding.controlsMainBottom.buttonMainPlayStop);
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