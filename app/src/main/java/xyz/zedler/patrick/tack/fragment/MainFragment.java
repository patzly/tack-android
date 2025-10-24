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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.AutoTransition;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.BEAT_MODE;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.FLASH_SCREEN;
import xyz.zedler.patrick.tack.Constants.KEEP_AWAKE;
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
import xyz.zedler.patrick.tack.fragment.MainFragmentDirections.ActionMainToSong;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.MetronomeListener;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine.Tick;
import xyz.zedler.patrick.tack.model.MetronomeConfig;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.LogoUtil;
import xyz.zedler.patrick.tack.util.NotificationUtil;
import xyz.zedler.patrick.tack.util.OptionsUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.util.dialog.BackupDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.PartsDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.TempoDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.UnlockDialogUtil;
import xyz.zedler.patrick.tack.view.BeatView;
import xyz.zedler.patrick.tack.view.SongPickerView.SongPickerListener;
import xyz.zedler.patrick.tack.view.TempoPickerView.OnPickListener;
import xyz.zedler.patrick.tack.view.TempoPickerView.OnRotationListener;
import xyz.zedler.patrick.tack.view.TimerView.TimerListener;

public class MainFragment extends BaseFragment implements OnClickListener, MetronomeListener {

  private static final String TAG = MainFragment.class.getSimpleName();

  private FragmentMainBinding binding;
  private MainActivity activity;
  private Bundle savedState;
  private boolean flashScreen, reduceAnimations, isRtl, isPortrait, isLandTablet, bigLogo;
  private boolean showPickerNotLogo, activeBeat;
  private LogoUtil logoUtil, logoCenterUtil;
  private ValueAnimator playStopButtonAnimator;
  private float playStopButtonFraction;
  private int colorFlashNormal, colorFlashStrong, colorFlashMuted;
  private int songPickerAvailableHeight, topControlsBottomMin;
  private DialogUtil dialogUtilGain, dialogUtilSplitScreen, dialogUtilTimer, dialogUtilElapsed;
  private DialogUtil dialogUtilPermission, dialogUtilBeatMode, dialogUtilIntro;
  private UnlockDialogUtil unlockDialogUtil;
  private OptionsUtil optionsUtil;
  private PartsDialogUtil partsDialogUtil;
  private TempoDialogUtil tempoDialogUtil;
  private BackupDialogUtil backupDialogUtil;
  private BeatsBgDrawable beatsBgDrawable;
  private BadgeDrawable beatsCountBadge, subsCountBadge, optionsBadge;
  private ValueAnimator beatsCountBadgeAnimator, subsCountBadgeAnimator, optionsBadgeAnimator;
  private ValueAnimator pickerLogoAnimator;
  private List<SongWithParts> songsWithParts;

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

    if (getMetronomeEngine() != null) {
      getMetronomeEngine().removeListener(this);
    }
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
    unlockDialogUtil.dismiss();
    backupDialogUtil.dismiss();
    dialogUtilIntro.dismiss();
    optionsUtil.dismiss();
    partsDialogUtil.dismiss();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    savedState = savedInstanceState;
    activity = (MainActivity) requireActivity();

    isPortrait = UiUtil.isOrientationPortrait(activity);
    isLandTablet = UiUtil.isLandTablet(activity);
    isRtl = UiUtil.isLayoutRtl(activity);

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarMain);
    if (isPortrait || isLandTablet) {
      systemBarBehavior.setContainer(binding.constraintMainContainer);
    } else if (binding.scrollMainStart != null) {
      systemBarBehavior.setContainer(binding.constraintMainContainer);
      systemBarBehavior.setScroll(binding.scrollMainStart, binding.linearMainTop);
      systemBarBehavior.setMultiColumnLayout(true);
      if (binding.containerMainEnd != null) {
        SystemBarBehavior.applyBottomInset(binding.containerMainEnd);
      }
    }
    systemBarBehavior.setUp();

    if (isPortrait || isLandTablet) {
      int liftMode = isLandTablet ? ScrollBehavior.ALWAYS_LIFTED : ScrollBehavior.NEVER_LIFTED;
      new ScrollBehavior().setUpScroll(binding.appBarMain, null, liftMode);
    } else {
      new ScrollBehavior().setUpScroll(
          binding.appBarMain, binding.scrollMainStart, ScrollBehavior.LIFT_ON_SCROLL
      );
    }

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
          activity.showHelp();
        } else if (id == R.id.action_feedback) {
          activity.showFeedback();
        }
        return true;
      });
    });
    ViewUtil.setTooltipText(binding.buttonMainMenu, R.string.action_more);

    reduceAnimations = getSharedPrefs().getBoolean(PREF.REDUCE_ANIM, DEF.REDUCE_ANIM);
    activeBeat = getSharedPrefs().getBoolean(PREF.ACTIVE_BEAT, DEF.ACTIVE_BEAT);
    boolean bigText = getSharedPrefs().getBoolean(PREF.BIG_TIME_TEXT, DEF.BIG_TIME_TEXT);

    beatsCountBadge = BadgeDrawable.create(activity);
    subsCountBadge = BadgeDrawable.create(activity);
    optionsBadge = BadgeDrawable.create(activity);
    optionsBadge.setVerticalOffset(UiUtil.dpToPx(activity, 16));
    optionsBadge.setHorizontalOffset(UiUtil.dpToPx(activity, 16));

    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBeats);
    updateBeats(getSharedPrefs().getString(PREF.BEATS, DEF.BEATS).split(","));

    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainSubs);
    updateSubs(getSharedPrefs().getString(PREF.SUBDIVISIONS, DEF.SUBDIVISIONS).split(","));

    binding.timerMain.setMainActivity(activity);
    binding.timerMain.setChangeHeightOfChips(!isPortrait && !isLandTablet);
    binding.timerMain.setBigText(bigText);
    binding.timerMain.setListener(new TimerListener() {
      @Override
      public void onCurrentTimeClick() {
        dialogUtilTimer.show();
        performHapticClick();
      }

      @Override
      public void onElapsedTimeClick() {
        dialogUtilElapsed.show();
        performHapticClick();
      }

      @Override
      public void onTotalTimeClick() {
        dialogUtilTimer.show();
        performHapticClick();
      }

      @Override
      public void onHeightChanged() {
        if (isPortrait || isLandTablet) {
          updateTempoPickerTranslationAndScale();
        }
      }
    });

    dialogUtilGain = new DialogUtil(activity, "gain");
    dialogUtilGain.createDialogError(builder -> {
      builder.setTitle(R.string.msg_gain);
      builder.setMessage(R.string.msg_gain_description);
      builder.setPositiveButton(R.string.action_play, (dialog, which) -> {
        if (getMetronomeEngine() != null) {
          performHapticClick();
          getMetronomeEngine().start();
        }
      });
      builder.setNegativeButton(
          R.string.action_deactivate,
          (dialog, which) -> {
            if (getMetronomeEngine() != null) {
              performHapticClick();
              getMetronomeEngine().setGain(0);
              getMetronomeEngine().start();
            }
          });
    });
    dialogUtilGain.showIfWasShown(savedInstanceState);

    dialogUtilPermission = new DialogUtil(activity, "notification_permission");
    dialogUtilPermission.createDialog(builder -> {
      builder.setTitle(R.string.msg_notification_permission);
      builder.setMessage(R.string.msg_notification_permission_description);
      builder.setPositiveButton(R.string.action_next, (dialog, which) -> {
        if (getMetronomeEngine() != null) {
          performHapticClick();
          getMetronomeEngine().start();
        }
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
        if (getMetronomeEngine() != null) {
          performHapticClick();
          getMetronomeEngine().resetTimerNow();
        }
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
        if (getMetronomeEngine() != null) {
          performHapticClick();
          getMetronomeEngine().resetElapsed();
        }
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilElapsed.showIfWasShown(savedInstanceState);

    unlockDialogUtil = new UnlockDialogUtil(activity);
    unlockDialogUtil.showIfWasShown(savedInstanceState);

    backupDialogUtil = new BackupDialogUtil(activity, this);
    backupDialogUtil.showIfWasShown(savedInstanceState);

    dialogUtilIntro = new DialogUtil(activity, "songs_intro");
    dialogUtilIntro.createDialog(builder -> {
      builder.setTitle(R.string.msg_songs_intro);
      builder.setMessage(R.string.msg_songs_intro_description);
      builder.setPositiveButton(
          R.string.action_close,
          (dialog, which) -> {
            performHapticClick();
            getSharedPrefs().edit().putBoolean(PREF.SONGS_INTRO_SHOWN, true).apply();
          });
      builder.setOnCancelListener(dialog -> {
        performHapticClick();
        getSharedPrefs().edit().putBoolean(PREF.SONGS_INTRO_SHOWN, true).apply();
      });
    });
    dialogUtilIntro.showIfWasShown(savedInstanceState);

    dialogUtilBeatMode = new DialogUtil(activity, "beat_mode");

    tempoDialogUtil = new TempoDialogUtil(activity, this, tempo -> {
      if (getMetronomeEngine() != null) {
        updateTempoDisplay(getMetronomeEngine().getConfig().getTempo(), tempo);
      }
    });

    optionsUtil = new OptionsUtil(
        activity, binding,
        () -> updateOptions(true),
        () -> binding.timerMain.updateControls(
            true, true, true
        ),
        () -> {
          if (getMetronomeEngine() != null) {
            updateSubs(getMetronomeEngine().getConfig().getSubdivisions());
            updateSubControls(true);
          }
        }
    );
    binding.buttonMainOptions.setEnabled(!isLandTablet);

    logoUtil = new LogoUtil(binding.imageMainLogo);
    logoCenterUtil = new LogoUtil(binding.imageMainLogoCenter);
    bigLogo = getSharedPrefs().getBoolean(PREF.BIG_LOGO, DEF.BIG_LOGO);

    partsDialogUtil = new PartsDialogUtil(activity);

    beatsBgDrawable = new BeatsBgDrawable(activity);
    binding.linearMainBeatsBg.setBackground(beatsBgDrawable);

    binding.textSwitcherMainTempoTerm.setFactory(() -> {
      TextView textView = new TextView(activity);
      textView.setGravity(Gravity.CENTER_HORIZONTAL);
      textView.setTextSize(
          TypedValue.COMPLEX_UNIT_PX,
          getResources().getDimension(R.dimen.label_text_size)
      );
      Typeface typeface = ResourcesCompat.getFont(activity, R.font.nunito_bold);
      textView.setTypeface(typeface);
      textView.setTextColor(ResUtil.getColor(activity, R.attr.colorOnPrimaryContainer));
      return textView;
    });

    binding.circleMain.setReduceAnimations(reduceAnimations);
    binding.circleMain.setOnDragAnimListener(fraction -> {
      if (VERSION.SDK_INT >= VERSION_CODES.O) {
        binding.textMainTempo.setFontVariationSettings("'wght' " + (600 + (fraction * 300)));
      }
    });

    binding.tempoPickerMain.setOnRotationListener(new OnRotationListener() {
      @Override
      public void onRotate(int tempo) {
        changeTempo(isRtl ? -tempo : tempo);
        activity.performHapticSegmentTick(binding.tempoPickerMain, false);
      }

      @Override
      public void onRotate(float degrees) {
        binding.circleMain.setRotation(binding.circleMain.getRotation() + degrees);
      }
    });
    binding.tempoPickerMain.setOnPickListener(new OnPickListener() {
      @Override
      public void onPickDown(float x, float y) {
        binding.circleMain.setDragged(true, x, y);
        if (bigLogo && getMetronomeEngine() != null && getMetronomeEngine().isPlaying()) {
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
        if (bigLogo && getMetronomeEngine() != null && getMetronomeEngine().isPlaying()) {
          updateTempoPickerAndLogo(false, true);
        }
      }
    });
    binding.tempoPickerMain.setOnClickListener(v -> {
      tempoDialogUtil.show();
      performHapticClick();
    });

    measureSongPicker();
    binding.songPickerMain.setListener(new SongPickerListener() {
      @Override
      public void onCurrentSongChanged(@NonNull String currentSongId) {
        if (getMetronomeEngine() != null) {
          getMetronomeEngine().setCurrentSong(currentSongId, 0);
          performHapticClick();
        }
      }

      @Override
      public void onCurrentSongClicked() {
        if (getMetronomeEngine() != null) {
          ActionMainToSong action = MainFragmentDirections.actionMainToSong();
          action.setSongId(getMetronomeEngine().getCurrentSongId());
          activity.navigate(action);
          performHapticClick();
        }
      }

      @Override
      public void onCurrentPartClicked() {
        partsDialogUtil.show();
        performHapticClick();
      }

      @Override
      public void onPreviousPartClicked() {
        if (getMetronomeEngine() != null) {
          int currentPartIndex = getMetronomeEngine().getCurrentPartIndex();
          getMetronomeEngine().setCurrentPartIndex(currentPartIndex - 1);
          performHapticClick();
        }
      }

      @Override
      public void onNextPartClicked() {
        if (getMetronomeEngine() != null) {
          int currentPartIndex = getMetronomeEngine().getCurrentPartIndex();
          getMetronomeEngine().setCurrentPartIndex(currentPartIndex + 1);
          performHapticClick();
        }
      }

      @Override
      public void onSongLongClicked(@NonNull String songId) {
        ActionMainToSong action = MainFragmentDirections.actionMainToSong();
        action.setSongId(songId);
        activity.navigate(action);
      }

      @Override
      public void onExpandCollapseClicked(boolean expand) {
        performHapticClick();
        if (expand && !getSharedPrefs().getBoolean(PREF.SONGS_INTRO_SHOWN, false)) {
          new Handler(Looper.getMainLooper()).postDelayed(
              () -> dialogUtilIntro.show(), 200
          );
        }
      }

      @Override
      public void onOpenSongsClicked() {
        performHapticClick();
        int visitCount = getSharedPrefs().getInt(PREF.SONGS_VISIT_COUNT, 0);
        if (visitCount != -1) { // no widget created and no dialog shown yet
          visitCount++;
          getSharedPrefs().edit().putInt(PREF.SONGS_VISIT_COUNT, visitCount).apply();
        }
        activity.navigate(MainFragmentDirections.actionMainToSongs());
      }

      @Override
      public void onMenuOrMenuItemClicked() {
        performHapticClick();
      }

      @Override
      public void onBackupClicked() {
        // haptic already performed in onMenuOrMenuItemClicked
        backupDialogUtil.show();
      }

      @Override
      public void onSortOrderChanged(int sortOrder) {
        if (getMetronomeEngine() != null) {
          getMetronomeEngine().setSongsOrder(sortOrder);
        }
      }

      @Override
      public void onAddSongClicked() {
        performHapticClick();
        if (activity.isUnlocked() || songsWithParts.size() < 3) {
          activity.navigate(MainFragmentDirections.actionMainToSong());
        } else {
          unlockDialogUtil.show();
        }
      }

      @Override
      public void onHeightChanged() {
        if (isPortrait || isLandTablet) {
          updateTempoPickerTranslationAndScale();
        }
      }

      @Override
      public void onExpandChanged(boolean expanded) {
        if (getMetronomeEngine() != null) {
          getMetronomeEngine().setSongPickerExpanded(expanded);
        }
      }
    });

    ViewUtil.resetAnimatedIcon(binding.buttonMainPlayStop);
    binding.buttonMainPlayStop.setIconResource(
        R.drawable.ic_rounded_play_to_stop_fill_anim
    );
    binding.buttonMainPlayStop.setOnTouchListener(
        (v, event) -> {
          if (getMetronomeEngine() == null) {
            return false;
          }
          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (getMetronomeEngine().isPlaying()) {
              performHapticClick();
              getMetronomeEngine().stop();
            } else {
              if (getMetronomeEngine().getGain() > 0 &&
                  getMetronomeEngine().neverStartedWithGainBefore()
              ) {
                dialogUtilGain.show();
              } else {
                boolean permissionDenied = getSharedPrefs().getBoolean(
                    PREF.PERMISSION_DENIED, false
                );
                if (NotificationUtil.hasPermission(activity) || permissionDenied) {
                  getMetronomeEngine().start();
                } else {
                  dialogUtilPermission.show();
                }
              }
              performHapticClick();
            }
          } else if (event.getAction() == MotionEvent.ACTION_UP) {
            v.performClick();
          }
          // Only false allowed for button animations to work
          return false;
        });

    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      Typeface variableTypeface = ResourcesCompat.getFont(activity, R.font.nunito_variable_wght);
      binding.textMainTempo.setTypeface(variableTypeface);
      binding.textMainTempo.setFontVariationSettings("'wght' 600");
    }
    updateMetronomeControls(true);

    ViewUtil.setTooltipText(binding.buttonMainAddBeat, R.string.action_add_beat);
    ViewUtil.setTooltipText(binding.buttonMainRemoveBeat, R.string.action_remove_beat);
    ViewUtil.setTooltipText(binding.buttonMainAddSubdivision, R.string.action_add_sub);
    ViewUtil.setTooltipText(binding.buttonMainRemoveSubdivision, R.string.action_remove_sub);
    ViewUtil.setTooltipText(binding.buttonMainOptions, R.string.title_options);
    ViewUtil.setTooltipText(binding.buttonMainBeatMode, R.string.action_beat_mode);

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
        binding.buttonMainBeatMode,
        binding.buttonMainOptions
    );
  }

  @Override
  public void onPause() {
    super.onPause();
    binding.timerMain.stopProgress();
    binding.timerMain.stopProgressTransition();
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
    if (dialogUtilIntro != null) {
      dialogUtilIntro.saveState(outState);
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
    if (unlockDialogUtil != null) {
      unlockDialogUtil.saveState(outState);
    }
    if (backupDialogUtil != null) {
      backupDialogUtil.saveState(outState);
    }
  }

  @Override
  public void updateMetronomeControls(boolean init) {
    if (binding == null) {
      return;
    }
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    MetronomeConfig metronomeConfig = metronomeEngine != null
        ? metronomeEngine.getConfig()
        : new MetronomeConfig(getSharedPrefs());

    optionsUtil.maybeInit();
    optionsUtil.showIfWasShown(savedState);
    tempoDialogUtil.showIfWasShown(savedState);
    partsDialogUtil.showIfWasShown(savedState);
    savedState = null;

    updateBeats(metronomeConfig.getBeats());
    updateBeatControls(false);
    updateSubs(metronomeConfig.getSubdivisions());
    updateSubControls(false);

    if (init) {
      binding.timerMain.measureControls();
    } else {
      binding.timerMain.updateControls(true, true, true);
    }

    updateOptions(false);

    int tempo = metronomeConfig.getTempo();
    updateTempoDisplay(tempo, tempo);
    binding.textSwitcherMainTempoTerm.setCurrentText(getTempoTerm(tempo));

    if (metronomeEngine == null) {
      // Below only stuff that only works with metronome engine
      return;
    }

    metronomeEngine.addListener(this);

    binding.buttonMainBeatMode.setIconResource(
        metronomeEngine.getBeatMode().equals(BEAT_MODE.VIBRATION)
            ? R.drawable.ic_rounded_vibration_to_volume_up_anim
            : R.drawable.ic_rounded_volume_up_to_vibration_anim
    );

    boolean showLogo = bigLogo && metronomeEngine.isPlaying();
    updateTempoPickerAndLogo(!showLogo, false);

    if (metronomeEngine.isCountingIn()) {
      beatsBgDrawable.reset();
      if (metronomeConfig.getCountIn() > 0) {
        beatsBgDrawable.setProgress(metronomeEngine.getCountInProgress(), 0);
        beatsBgDrawable.setProgress(1, metronomeEngine.getCountInIntervalRemaining());
      }
    }

    activity.getSongViewModel().getAllSongsWithPartsLive().removeObservers(getViewLifecycleOwner());
    activity.getSongViewModel().getAllSongsWithPartsLive().observe(
        getViewLifecycleOwner(), songs -> {
          songsWithParts = new ArrayList<>(songs);
          for (SongWithParts songWithParts : songsWithParts) {
            // Remove default song from song picker
            if (songWithParts.getSong().getId().equals(Constants.SONG_ID_DEFAULT)) {
              songsWithParts.remove(songWithParts);
              break;
            }
          }
          if (!binding.songPickerMain.isInitialized() && getMetronomeEngine() != null) {
            binding.songPickerMain.init(
                getMetronomeEngine().getCurrentSongId(),
                getMetronomeEngine().getCurrentPartIndex(),
                songsWithParts,
                getMetronomeEngine().getSongsOrder(),
                getMetronomeEngine().isSongPickerExpanded()
            );
          }
          binding.songPickerMain.setSongs(songsWithParts);
        }
    );

    ViewUtil.resetAnimatedIcon(binding.buttonMainPlayStop);
    binding.buttonMainPlayStop.setIconResource(
        metronomeEngine.isPlaying()
            ? R.drawable.ic_rounded_stop_fill
            : R.drawable.ic_rounded_play_arrow_fill
    );
    updatePlayStopButton(metronomeEngine.isPlaying(), false);

    binding.buttonMainBeatMode.setIconResource(
        metronomeEngine.getBeatMode().equals(BEAT_MODE.VIBRATION)
            ? R.drawable.ic_rounded_vibration_to_volume_up_anim
            : R.drawable.ic_rounded_volume_up_to_vibration_anim
    );
    setButtonStates(metronomeConfig.getTempo());

    Map<String, String> beatModeLabels = new LinkedHashMap<>();
    beatModeLabels.put(BEAT_MODE.ALL, getString(R.string.label_beat_mode_all));
    beatModeLabels.put(BEAT_MODE.SOUND, getString(R.string.label_beat_mode_sound));
    beatModeLabels.put(BEAT_MODE.VIBRATION, getString(R.string.label_beat_mode_vibration));
    ArrayList<String> beatModes = new ArrayList<>(beatModeLabels.keySet());
    String[] items = beatModeLabels.values().toArray(new String[]{});
    int initItem = beatModes.indexOf(metronomeEngine.getBeatMode());
    if (initItem == -1) {
      initItem = 0;
      getSharedPrefs().edit().remove(PREF.BEAT_MODE).apply();
    }
    int initItemFinal = initItem;
    dialogUtilBeatMode.createDialog(builder -> {
      builder.setTitle(R.string.action_beat_mode);
      if (activity.getHapticUtil().hasVibrator()) {
        builder.setSingleChoiceItems(
            items, initItemFinal, (dialog, which) -> {
              if (getMetronomeEngine() == null) {
                return;
              }
              String beatModePrev = getMetronomeEngine().getBeatMode();
              String beatMode = beatModes.get(which);
              if (beatMode.equals(BEAT_MODE.SOUND)) {
                performHapticClick();
              }
              getMetronomeEngine().setBeatMode(beatMode);
              if (!beatMode.equals(BEAT_MODE.SOUND)) {
                performHapticClick();
              }

              if (beatModePrev.equals(BEAT_MODE.VIBRATION)
                  && !beatMode.equals(BEAT_MODE.VIBRATION)) {
                binding.buttonMainBeatMode.setIconResource(
                    R.drawable.ic_rounded_vibration_to_volume_up_anim
                );
                ViewUtil.startIcon(binding.buttonMainBeatMode.getIcon());
              } else if (!beatModePrev.equals(BEAT_MODE.VIBRATION)
                  && beatMode.equals(BEAT_MODE.VIBRATION)) {
                binding.buttonMainBeatMode.setIconResource(
                    R.drawable.ic_rounded_volume_up_to_vibration_anim
                );
                ViewUtil.startIcon(binding.buttonMainBeatMode.getIcon());
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

    String flashScreenMode = metronomeEngine.getFlashScreen();
    flashScreen = !flashScreenMode.equals(FLASH_SCREEN.OFF);

    colorFlashMuted = ResUtil.getColor(activity, R.attr.colorSurface);
    if (flashScreenMode.equals(FLASH_SCREEN.SUBTLE)) {
      float mixRatio = 0.7f;
      colorFlashNormal = ResUtil.getColor(activity, R.attr.colorPrimaryContainer);
      colorFlashNormal = ColorUtils.blendARGB(colorFlashMuted, colorFlashNormal, mixRatio);
      colorFlashStrong = ResUtil.getColor(activity, R.attr.colorErrorContainer);
      colorFlashStrong = ColorUtils.blendARGB(colorFlashMuted, colorFlashStrong, mixRatio);
    } else {
      colorFlashNormal = ResUtil.getColor(activity, R.attr.colorPrimary);
      colorFlashStrong = ResUtil.getColor(activity, R.attr.colorError);
    }

    String keepAwake = metronomeEngine.getKeepAwake();
    boolean keepAwakeNow = keepAwake.equals(KEEP_AWAKE.ALWAYS)
        || (keepAwake.equals(KEEP_AWAKE.WHILE_PLAYING) && metronomeEngine.isPlaying());
    UiUtil.keepScreenAwake(activity, keepAwakeNow);
  }

  @Override
  public void onMetronomeStart() {
    activity.runOnUiThread(() -> {
      if (binding == null || getMetronomeEngine() == null) {
        return;
      }
      beatsBgDrawable.reset();
      if (getMetronomeEngine().getConfig().getCountIn() > 0) {
        beatsBgDrawable.setProgress(1, getMetronomeEngine().getCountInInterval());
      }
      binding.buttonMainPlayStop.setIconResource(R.drawable.ic_rounded_play_to_stop_fill_anim);
      Drawable startStopIcon = binding.buttonMainPlayStop.getIcon();
      if (startStopIcon != null) {
        ((Animatable) startStopIcon).start();
      }
      updatePlayStopButton(true, !reduceAnimations);
      if (bigLogo) {
        updateTempoPickerAndLogo(false, true);
      }
      String keepAwake = getMetronomeEngine().getKeepAwake();
      UiUtil.keepScreenAwake(activity, !keepAwake.equals(KEEP_AWAKE.NEVER));
    });
  }

  @Override
  public void onMetronomeStop() {
    activity.runOnUiThread(() -> {
      if (binding == null || getMetronomeEngine() == null) {
        return;
      }
      resetActiveBeats();
      beatsBgDrawable.setProgressVisible(false, true);
      binding.timerMain.updateDisplay();
      binding.buttonMainPlayStop.setIconResource(R.drawable.ic_rounded_stop_to_play_fill_anim);
      Drawable icon = binding.buttonMainPlayStop.getIcon();
      if (icon != null) {
        ((Animatable) icon).start();
      }
      updatePlayStopButton(false, !reduceAnimations);
      if (bigLogo) {
        updateTempoPickerAndLogo(true, true);
      }
      binding.timerMain.stopProgressTransition();
      binding.timerMain.stopProgress();
      String keepAwake = getMetronomeEngine().getKeepAwake();
      UiUtil.keepScreenAwake(activity, keepAwake.equals(KEEP_AWAKE.ALWAYS));
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
      if (!(subdivision instanceof BeatView)) {
        return;
      }
      ((BeatView) subdivision).setTickType(tick.subdivision == 1 ? TICK_TYPE.MUTED : tick.type);
      ((BeatView) subdivision).beat();
    });
  }

  @Override
  public void onMetronomeTick(Tick tick) {
    activity.runOnUiThread(() -> {
      if (binding == null || getMetronomeEngine() == null) {
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
        if (isLandTablet && binding.cardMainContainerEnd != null) {
          binding.cardMainContainerEnd.setCardBackgroundColor(color);
          binding.cardMainContainerEnd.postDelayed(() -> {
            if (binding != null) {
              binding.cardMainContainerEnd.setCardBackgroundColor(colorFlashMuted);
            }
          }, 100); // flash screen for 100 milliseconds
        } else {
          binding.coordinatorContainer.setBackgroundColor(color);
          binding.coordinatorContainer.postDelayed(() -> {
            if (binding != null) {
              binding.coordinatorContainer.setBackgroundColor(colorFlashMuted);
            }
          }, 100); // flash screen for 100 milliseconds
        }
      }
      if (tick.subdivision == 1) {
        if (!reduceAnimations) {
          logoUtil.nextBeat(getMetronomeEngine().getInterval());
        }
        if (bigLogo) {
          logoCenterUtil.nextBeat(getMetronomeEngine().getInterval());
        }
      }
      if (getMetronomeEngine().getConfig().getTimerUnit().equals(UNIT.BARS)) {
        binding.timerMain.updateDisplay();
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
      if (binding == null) {
        return;
      }
      binding.timerMain.stopProgressTransition();
      binding.timerMain.stopProgress();
      binding.timerMain.updateControls(true, true, true);
    });
  }

  @Override
  public void onMetronomeElapsedTimeSecondsChanged() {
    activity.runOnUiThread(() -> binding.timerMain.updateDisplay());
  }

  @Override
  public void onMetronomeTimerSecondsChanged() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        binding.timerMain.updateDisplay();
      }
    });
  }

  @Override
  public void onMetronomeTimerProgressOneTime(boolean withTransition) {
    activity.runOnUiThread(() -> {
      if (binding == null) {
        return;
      }
      binding.timerMain.updateControls(true, true, withTransition);
    });
  }

  @Override
  public void onMetronomeConfigChanged() {
    activity.runOnUiThread(() -> {
      if (binding == null || getMetronomeEngine() == null) {
        return;
      }
      // tempo is updated in onMetronomeTempoChanged
      updateBeats(getMetronomeEngine().getConfig().getBeats());
      updateBeatControls(true);
      updateSubs(getMetronomeEngine().getConfig().getSubdivisions());
      updateSubControls(true);

      // no timer updateControls here, as it is called from onMetronomeTimerProgressOneTime
      //binding.timerMain.updateControls(true, true, true);

      updateOptions(true);
    });
  }

  @Override
  public void onMetronomeSongOrPartChanged(@Nullable SongWithParts song, int partIndex) {
    activity.runOnUiThread(() -> {
      if (song != null && binding != null) {
        partsDialogUtil.update();
        if (!song.getSong().getId().equals(Constants.SONG_ID_DEFAULT)) {
          // Only if not closing current song, else the user sees a switch to part 1 during anim
          binding.songPickerMain.setPartIndex(partIndex);
        }
      }
    });
  }

  @Override
  public void onMetronomePermissionMissing() {
    activity.runOnUiThread(() -> activity.requestNotificationPermission(true));
  }

  @Override
  public void onClick(View v) {
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    if (binding == null || metronomeEngine == null) {
      return;
    }
    MetronomeConfig config = metronomeEngine.getConfig();
    int id = v.getId();
    if (id == R.id.button_main_add_beat) {
      ViewUtil.startIcon(binding.buttonMainAddBeat.getIcon());
      performHapticClick();
      boolean success = metronomeEngine.addBeat();
      if (success) {
        if (config.isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
          metronomeEngine.restartIfPlaying(false);
        }
        metronomeEngine.maybeUpdateDefaultSong();

        Transition transition = new AutoTransition();
        transition.setDuration(Constants.ANIM_DURATION_SHORT);
        TransitionManager.beginDelayedTransition(binding.linearMainBeats, transition);

        BeatView beatView = new BeatView(activity);
        beatView.setIndex(binding.linearMainBeats.getChildCount());
        beatView.setOnClickListener(beat -> {
          if (getMetronomeEngine() != null) {
            performHapticClick();
            getMetronomeEngine().setBeat(beatView.getIndex(), beatView.nextTickType());
            getMetronomeEngine().maybeUpdateDefaultSong();
          }
        });
        beatView.setReduceAnimations(reduceAnimations);

        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizMainBeats, UiUtil.dpToPx(activity, 48)
        );

        binding.linearMainBeats.addView(beatView);
        updateBeatControls(true);
        binding.timerMain.updateDisplay(); // Update decimals for bar unit
      }
    } else if (id == R.id.button_main_remove_beat) {
      ViewUtil.startIcon(binding.buttonMainRemoveBeat.getIcon());
      performHapticClick();
      boolean success = metronomeEngine.removeBeat();
      if (success) {
        if (config.isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
          metronomeEngine.restartIfPlaying(false);
        }
        metronomeEngine.maybeUpdateDefaultSong();

        Transition transition = new ChangeBounds();
        transition.setDuration(Constants.ANIM_DURATION_SHORT);
        TransitionManager.beginDelayedTransition(binding.linearMainBeats, transition);

        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizMainBeats, -UiUtil.dpToPx(activity, 48)
        );

        binding.linearMainBeats.removeViewAt(binding.linearMainBeats.getChildCount() - 1);
        updateBeatControls(true);
        binding.timerMain.updateDisplay(); // Update decimals for bar unit
      }
    } else if (id == R.id.button_main_add_subdivision) {
      ViewUtil.startIcon(binding.buttonMainAddSubdivision.getIcon());
      performHapticClick();
      boolean success = metronomeEngine.addSubdivision();
      if (success) {
        if (config.isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
          metronomeEngine.restartIfPlaying(false);
        }
        metronomeEngine.maybeUpdateDefaultSong();

        Transition transition = new AutoTransition();
        transition.setDuration(Constants.ANIM_DURATION_SHORT);
        TransitionManager.beginDelayedTransition(binding.linearMainSubs, transition);

        BeatView beatView = new BeatView(activity);
        beatView.setIsSubdivision(true);
        beatView.setIndex(binding.linearMainSubs.getChildCount());
        beatView.setOnClickListener(subdivision -> {
          if (getMetronomeEngine() != null) {
            performHapticClick();
            getMetronomeEngine().setSubdivision(beatView.getIndex(), beatView.nextTickType());
            getMetronomeEngine().maybeUpdateDefaultSong();
          }
        });
        beatView.setReduceAnimations(reduceAnimations);

        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizMainSubs, UiUtil.dpToPx(activity, 48)
        );

        binding.linearMainSubs.addView(beatView);
        updateSubControls(true);
        optionsUtil.updateSwing();
      }
    } else if (id == R.id.button_main_remove_subdivision) {
      ViewUtil.startIcon(binding.buttonMainRemoveSubdivision.getIcon());
      performHapticClick();
      boolean success = metronomeEngine.removeSubdivision();
      if (success) {
        if (config.isTimerActive() && config.getTimerUnit().equals(UNIT.BARS)) {
          metronomeEngine.restartIfPlaying(false);
        }
        metronomeEngine.maybeUpdateDefaultSong();

        Transition transition = new ChangeBounds();
        transition.setDuration(Constants.ANIM_DURATION_SHORT);
        TransitionManager.beginDelayedTransition(binding.linearMainSubs, transition);

        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizMainSubs, -UiUtil.dpToPx(activity, 48)
        );

        binding.linearMainSubs.removeViewAt(binding.linearMainSubs.getChildCount() - 1);
        updateSubControls(true);
        optionsUtil.updateSwing();
      }
    } else if (id == R.id.button_main_less_1) {
      ViewUtil.startIcon(binding.buttonMainLess1.getIcon());
      changeTempo(-1);
      performHapticClick();
    } else if (id == R.id.button_main_less_5) {
      ViewUtil.startIcon(binding.buttonMainLess5.getIcon());
      changeTempo(-5);
      performHapticClick();
    } else if (id == R.id.button_main_less_10) {
      ViewUtil.startIcon(binding.buttonMainLess10.getIcon());
      changeTempo(-10);
      performHapticClick();
    } else if (id == R.id.button_main_more_1) {
      ViewUtil.startIcon(binding.buttonMainMore1.getIcon());
      changeTempo(1);
      performHapticClick();
    } else if (id == R.id.button_main_more_5) {
      ViewUtil.startIcon(binding.buttonMainMore5.getIcon());
      changeTempo(5);
      performHapticClick();
    } else if (id == R.id.button_main_more_10) {
      ViewUtil.startIcon(binding.buttonMainMore10.getIcon());
      changeTempo(10);
      performHapticClick();
    } else if (id == R.id.button_main_beat_mode) {
      performHapticClick();
      dialogUtilBeatMode.show();
      if (metronomeEngine.getBeatMode().equals(BEAT_MODE.VIBRATION)) {
        // Use available animated icon for click
        binding.buttonMainBeatMode.setIconResource(
            R.drawable.ic_rounded_vibration_anim
        );
        ViewUtil.startIcon(binding.buttonMainBeatMode.getIcon());
      }
    } else if (id == R.id.button_main_options) {
      performHapticClick();
      ViewUtil.startIcon(binding.buttonMainOptions.getIcon());
      optionsUtil.show();
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
        if (getMetronomeEngine() != null) {
          performHapticClick();
          getMetronomeEngine().setBeat(beatView.getIndex(), beatView.nextTickType());
          getMetronomeEngine().maybeUpdateDefaultSong();
        }
      });
      beatView.setReduceAnimations(reduceAnimations);
      binding.linearMainBeats.addView(beatView);
    }
    binding.linearMainBeats.post(
        () -> ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBeats)
    );

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
    if (binding == null || getMetronomeEngine() == null) {
      return;
    }
    int beats = getMetronomeEngine().getConfig().getBeatsCount();
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

  private void updateSubs(String[] subdivisions) {
    if (binding == null) {
      return;
    }
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
          if (getMetronomeEngine() != null) {
            performHapticClick();
            getMetronomeEngine().setSubdivision(beatView.getIndex(), beatView.nextTickType());
            getMetronomeEngine().maybeUpdateDefaultSong();
          }
        });
      }
      beatView.setReduceAnimations(reduceAnimations);
      binding.linearMainSubs.addView(beatView);
    }
    binding.linearMainSubs.post(
        () -> ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainSubs)
    );

    updateSubControls(true);
  }

  @OptIn(markerClass = ExperimentalBadgeUtils.class)
  private void updateSubControls(boolean animated) {
    if (binding == null || getMetronomeEngine() == null) {
      return;
    }
    if (subsCountBadgeAnimator != null) {
      subsCountBadgeAnimator.pause();
      subsCountBadgeAnimator.removeAllUpdateListeners();
      subsCountBadgeAnimator.removeAllListeners();
      subsCountBadgeAnimator.cancel();
      subsCountBadgeAnimator = null;
    }
    int subdivisions = getMetronomeEngine().getConfig().getSubdivisionsCount();
    binding.buttonMainAddSubdivision.setEnabled(subdivisions < Constants.SUBS_MAX);
    binding.buttonMainRemoveSubdivision.setEnabled(subdivisions > 1);
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

  private void measureSongPicker() {
    binding.coordinatorContainer.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (binding == null) {
              return;
            }
            songPickerAvailableHeight =
                binding.frameMainBottom.getTop() - binding.frameMainCenter.getBottom();

            ViewGroup songPickerParent = binding.constraintMainContainer;
            if (isLandTablet) {
              songPickerParent = binding.cardMainContainerEnd;
            } else if (!isPortrait) {
              songPickerParent = binding.linearMainTop;
            }
            if (songPickerParent != null) {
              binding.songPickerMain.setParentWidth(songPickerParent.getWidth());
            }

            // bottom of top controls where timer height will be added to
            topControlsBottomMin =
                binding.linearMainSubsBg.getBottom() + UiUtil.dpToPx(activity, 24);

            if (binding.coordinatorContainer.getViewTreeObserver().isAlive()) {
              binding.coordinatorContainer.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        });
  }

  private void updateTempoPickerTranslationAndScale() {
    if (binding == null || binding.frameMainBottom.getTop() == 0) {
      return;
    }
    float fraction = binding.songPickerMain.getExpandFraction();
    int songPickerHeightExpanded = binding.songPickerMain.getHeightExpanded();
    if (songPickerHeightExpanded > songPickerAvailableHeight) {
      int songPickerOverlap = songPickerHeightExpanded - songPickerAvailableHeight;
      int timerSliderHeight = binding.timerMain.getSliderHeightExpanded();
      int timerSliderHeightCurrent = (int) (timerSliderHeight *
          binding.timerMain.getTimerExpandFraction());
      int timerDisplayHeight = binding.timerMain.getDisplayHeightExpanded();
      int timerDisplayHeightCurrent = (int) Math.max(
          timerDisplayHeight * binding.timerMain.getTimerExpandFraction(),
          timerDisplayHeight * binding.timerMain.getElapsedExpandFraction()
      );
      int topControlsBottom =
          topControlsBottomMin + timerSliderHeightCurrent + timerDisplayHeightCurrent;
      int currentWidth = binding.frameMainCenter.getWidth();
      int currentHeight = binding.frameMainCenter.getHeight();
      int targetHeight =
          binding.frameMainBottom.getTop() - topControlsBottom - songPickerHeightExpanded;
      if (targetHeight > currentHeight) {
        targetHeight = currentHeight;
      }

      float scale = 1 + (((float) targetHeight / currentHeight) - 1) * fraction;
      if (Float.isNaN(scale)) {
        scale = 1f;
      }
      int scaleCompensation = (currentHeight - targetHeight) / 2;
      float translationY = (-songPickerOverlap + scaleCompensation) * fraction;

      binding.frameMainCenter.setScaleX(scale);
      binding.frameMainCenter.setScaleY(scale);
      binding.frameMainCenter.setTranslationY(translationY);

      binding.buttonGroupMainLess.setScaleX(scale);
      binding.buttonGroupMainLess.setScaleY(scale);
      binding.buttonGroupMainLess.setTranslationY(translationY);
      int targetWidth = (int) (currentWidth * scale);
      int translationX = (currentWidth - targetWidth) / 4;
      binding.buttonGroupMainLess.setTranslationX(isRtl ? -translationX : translationX);

      binding.buttonGroupMainMore.setScaleX(scale);
      binding.buttonGroupMainMore.setScaleY(scale);
      binding.buttonGroupMainMore.setTranslationY(translationY);
      binding.buttonGroupMainMore.setTranslationX(isRtl ? translationX : -translationX);

      binding.songPickerMain.setTranslationY(-songPickerOverlap * 0.5f * fraction);
    }
  }

  @OptIn(markerClass = ExperimentalBadgeUtils.class)
  private void updateOptions(boolean animated) {
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

  private int getModifierCount() {
    if (getMetronomeEngine() == null) {
      return 0;
    }
    MetronomeConfig config = getMetronomeEngine().getConfig();
    return (config.isCountInActive() ? 1 : 0) +
        (config.isIncrementalActive() ? 1 : 0) +
        (config.isTimerActive() ? 1 : 0) +
        (config.isMuteActive() ? 1 : 0);
  }

  private void changeTempo(int difference) {
    if (getMetronomeEngine() == null) {
      return;
    }
    int tempoNew = getMetronomeEngine().getConfig().getTempo() + difference;
    if (tempoNew >= Constants.TEMPO_MIN && tempoNew <= Constants.TEMPO_MAX) {
      updateTempoDisplay(getMetronomeEngine().getConfig().getTempo(), tempoNew);
      getMetronomeEngine().setTempo(tempoNew);
      getMetronomeEngine().maybeUpdateDefaultSong();
    }
  }

  private void updateTempoDisplay(int tempoOld, int tempoNew) {
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

  private void updatePlayStopButton(boolean playing, boolean animated) {
    binding.buttonMainPlayStop.setChecked(playing);
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
        binding.buttonMainPlayStop.setBackgroundColor(
            ColorUtils.blendARGB(colorBgStopped, colorBgPlaying, playStopButtonFraction)
        );
        binding.buttonMainPlayStop.setIconTint(
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
      binding.buttonMainPlayStop.setBackgroundColor(
          ColorUtils.blendARGB(colorBgStopped, colorBgPlaying, playStopButtonFraction)
      );
      binding.buttonMainPlayStop.setIconTint(
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
        float fraction = (float) animation.getAnimatedValue();
        binding.frameMainCenter.setAlpha(fraction);
        binding.imageMainLogoCenter.setAlpha(1 - fraction);
        binding.imageMainLogo.setScaleX(fraction);
        binding.imageMainLogo.setScaleY(fraction);
        binding.imageMainLogoPlaceholder.setAlpha(1 - fraction);
        binding.imageMainLogoPlaceholder.setScaleX(1 - fraction);
        binding.imageMainLogoPlaceholder.setScaleY(1 - fraction);
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

  public void showSnackbar(Snackbar snackbar) {
    snackbar.setAnchorView(binding.buttonMainPlayStop);
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
}