package xyz.zedler.patrick.tack.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
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
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.FragmentMainBinding;
import xyz.zedler.patrick.tack.drawable.BeatsBgDrawable;
import xyz.zedler.patrick.tack.drawable.SquigglyProgressDrawable;
import xyz.zedler.patrick.tack.service.MetronomeService.MetronomeListener;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.LogoUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.OptionsUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ShortcutUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.BeatView;
import xyz.zedler.patrick.tack.view.TempoPickerView;

public class MainFragment extends BaseFragment
    implements OnClickListener, MetronomeListener {

  private static final String TAG = MainFragment.class.getSimpleName();

  private FragmentMainBinding binding;
  private MainActivity activity;
  private long prevTouchTime;
  private final List<Long> intervals = new ArrayList<>();
  private boolean flashScreen, keepAwake, reduceAnimations;
  private LogoUtil logoUtil;
  private ValueAnimator fabAnimator;
  private float cornerSizeStop, cornerSizePlay, cornerSizeCurrent;
  private int colorFlashNormal, colorFlashStrong, colorFlashMuted;
  private DialogUtil dialogUtilGain;
  private OptionsUtil optionsUtil;
  private ShortcutUtil shortcutUtil;
  private List<Integer> bookmarks;
  private SquigglyProgressDrawable squiggly;
  private BeatsBgDrawable beatsBgDrawable;
  private ValueAnimator progressAnimator, progressTransitionAnimator;

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
      fabAnimator.cancel();
    }
    binding = null;
    dialogUtilGain.dismiss();
    optionsUtil.dismiss();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarMain);
    systemBarBehavior.setContainer(binding.constraintMainContainer);
    systemBarBehavior.setUp();

    new ScrollBehavior().setUpScroll(binding.appBarMain, null, true);

    binding.toolbarMain.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (getViewUtil().isClickDisabled(id)) {
        return false;
      }
      performHapticClick();
      if (id == R.id.action_settings) {
        activity.navigateToFragment(MainFragmentDirections.actionMainToSettings());
      } else if (id == R.id.action_about) {
        activity.navigateToFragment(MainFragmentDirections.actionMainToAbout());
      } else if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_recommend) {
        ResUtil.share(activity, R.string.msg_recommend);
      }
      return true;
    });

    logoUtil = new LogoUtil(binding.imageMainLogo);

    flashScreen = getSharedPrefs().getBoolean(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN);
    keepAwake = getSharedPrefs().getBoolean(PREF.KEEP_AWAKE, DEF.KEEP_AWAKE);
    reduceAnimations = getSharedPrefs().getBoolean(PREF.REDUCE_ANIM, DEF.REDUCE_ANIM);

    colorFlashNormal = ResUtil.getColor(activity, R.attr.colorPrimary);
    colorFlashStrong = ResUtil.getColor(activity, R.attr.colorError);
    colorFlashMuted = ResUtil.getColor(activity, android.R.attr.colorBackground);

    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBeats);
    binding.linearMainBeats.getLayoutTransition().setDuration(Constants.ANIM_DURATION_LONG);
    updateBeats(getSharedPrefs().getString(PREF.BEATS, DEF.BEATS).split(","));
    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainSubs);
    binding.linearMainSubs.getLayoutTransition().setDuration(Constants.ANIM_DURATION_LONG);
    updateSubs(getSharedPrefs().getString(PREF.SUBDIVISIONS, DEF.SUBDIVISIONS).split(","));

    dialogUtilGain = new DialogUtil(activity, "gain");
    dialogUtilGain.createCaution(
        R.string.msg_gain,
        R.string.msg_gain_description,
        R.string.action_play,
        () -> {
          if (isBound() && !getMetronomeService().isPlaying()) {
            getMetronomeService().start();
          }
        },
        R.string.action_deactivate_gain,
        () -> {
          if (isBound()) {
            getMetronomeService().setGain(0);
            if (!getMetronomeService().isPlaying()) {
              getMetronomeService().start();
            }
          }
        });
    dialogUtilGain.showIfWasShown(savedInstanceState);

    optionsUtil = new OptionsUtil(activity, this);
    optionsUtil.showIfWasShown(savedInstanceState);

    shortcutUtil = new ShortcutUtil(activity);

    beatsBgDrawable = new BeatsBgDrawable(activity);
    binding.linearMainBeatsBg.setBackground(beatsBgDrawable);

    squiggly = new SquigglyProgressDrawable(activity);
    binding.seekbarMainTimer.setProgressDrawable(squiggly);
    binding.seekbarMainTimer.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (isBound() && fromUser) {
          int positions = getMetronomeService().getTimerDuration();
          int timerPositionCurrent = (int) (getMetronomeService().getTimerProgress() * positions);
          float fraction = (float) progress / binding.seekbarMainTimer.getMax();
          int timerPositionNew = (int) (fraction * positions);
          if (timerPositionCurrent != timerPositionNew) {
            performHapticTick();
          }
          getMetronomeService().updateTimerHandler(fraction);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        if (isBound()) {
          getMetronomeService().saveState();
          getMetronomeService().stop();
        }
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        if (isBound()) {
          getMetronomeService().restoreState();
        }
      }
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

    binding.tempoPickerMain.setOnRotationListener(new TempoPickerView.OnRotationListener() {
      @Override
      public void onRotate(int tempo) {
        changeTempo(tempo);
      }

      @Override
      public void onRotate(float degrees) {
        binding.circleMain.setRotation(
            binding.circleMain.getRotation() + degrees
        );
      }
    });
    binding.tempoPickerMain.setOnPickListener(new TempoPickerView.OnPickListener() {
      @Override
      public void onPickDown(float x, float y) {
        binding.circleMain.setDragged(true, x, y);
      }

      @Override
      public void onDrag(float x, float y) {
        binding.circleMain.onDrag(x, y);
      }

      @Override
      public void onPickUpOrCancel() {
        binding.circleMain.setDragged(false, 0, 0);
      }
    });

    binding.buttonMainLess.setOnTouchListener(new View.OnTouchListener() {
      private Handler handler;
      private int nextRun = 400;
      private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          if (isBound()) {
            if (getMetronomeService().getTempo() > 1) {
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

    binding.buttonMainMore.setOnTouchListener(new View.OnTouchListener() {
      private Handler handler;
      private int nextRun = 400;
      private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          if (isBound()) {
            if (getMetronomeService().getTempo() < 400) {
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

    binding.buttonMainTempoTap.setOnTouchListener((v, event) -> {
      // TODO
      if (event.getAction() != MotionEvent.ACTION_DOWN) {
        return false;
      }
      ViewUtil.startIcon(binding.buttonMainTempoTap.getIcon());

      long currentTime = System.currentTimeMillis();
      long interval = currentTime - prevTouchTime;
      if (prevTouchTime > 0 && interval <= 3000) {
        while (intervals.size() >= 20) {
          intervals.remove(0);
        }
        intervals.add(System.currentTimeMillis() - prevTouchTime);
        if (intervals.size() > 1) {
          long sum = 0L;
          for (long e : intervals) {
            sum += e;
          }
          long intervalAverage = sum / intervals.size();
          long averageTempo = 60000 / intervalAverage;

          // Überprüfen Sie, ob das Tempo um mehr als 20% geändert wurde
          if (Math.abs(averageTempo - getMetronomeService().getTempo()) > getMetronomeService().getTempo() * 0.2) {
            intervals.clear(); // Zurücksetzen, wenn die Tempoänderung zu groß ist
          }

          // Setzen Sie das Tempo und aktualisieren Sie prevTouchTime
          if (isBoundOrShowWarning()) {
            setTempo((int) averageTempo);
          }
          prevTouchTime = currentTime;
        }
      } else {
        intervals.clear();
        prevTouchTime = currentTime;
      }

      performHapticHeavyClick();
      return true;
    });

    boolean alwaysVibrate = getSharedPrefs().getBoolean(PREF.ALWAYS_VIBRATE, DEF.ALWAYS_VIBRATE);
    if (getSharedPrefs().getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE)) {
      binding.buttonMainBeatMode.setIconResource(
          alwaysVibrate
              ? R.drawable.ic_round_volume_off_to_volume_on_anim
              : R.drawable.ic_round_vibrate_to_volume_anim
      );
    } else {
      binding.buttonMainBeatMode.setIconResource(
          alwaysVibrate
              ? R.drawable.ic_round_volume_on_to_volume_off_anim
              : R.drawable.ic_round_volume_to_vibrate_anim
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
    for (int i = 0; i < bookmarks.size(); i++) {
      binding.chipGroupMainBookmarks.addView(getBookmarkChip(bookmarks.get(i)));
    }

    ViewUtil.resetAnimatedIcon(binding.fabMainPlayStop);
    binding.fabMainPlayStop.setImageResource(R.drawable.ic_round_play_to_stop_anim);
    cornerSizeStop = UiUtil.dpToPx(activity, 28);
    cornerSizePlay = UiUtil.dpToPx(activity, 48);
    cornerSizeCurrent = cornerSizeStop;

    if (isBound()) {
      onMetronomeServiceConnected();
    }

    ViewUtil.setTooltipText(binding.buttonMainAddBeat, R.string.action_add_beat);
    ViewUtil.setTooltipText(binding.buttonMainRemoveBeat, R.string.action_remove_beat);
    ViewUtil.setTooltipText(binding.buttonMainAddSubdivision, R.string.action_add_sub);
    ViewUtil.setTooltipText(binding.buttonMainRemoveSubdivision, R.string.action_remove_sub);
    ViewUtil.setTooltipText(binding.buttonMainOptions, R.string.title_options);
    ViewUtil.setTooltipText(binding.buttonMainBeatMode, R.string.action_beat_mode);
    ViewUtil.setTooltipText(binding.fabMainPlayStop, R.string.action_play_stop);

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
        binding.fabMainPlayStop
    );
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
  }

  public void onMetronomeServiceConnected() {
    if (binding == null) {
      return;
    }
    getMetronomeService().setMetronomeListener(this);

    if (getMetronomeService().isBeatModeVibrate()) {
      binding.buttonMainBeatMode.setIconResource(
          getMetronomeService().isAlwaysVibrate()
              ? R.drawable.ic_round_volume_off_to_volume_on_anim
              : R.drawable.ic_round_vibrate_to_volume_anim
      );
    } else {
      binding.buttonMainBeatMode.setIconResource(
          getMetronomeService().isAlwaysVibrate()
              ? R.drawable.ic_round_volume_on_to_volume_off_anim
              : R.drawable.ic_round_volume_to_vibrate_anim
      );
    }

    updateBeats(getMetronomeService().getBeats());
    updateBeatControls();
    updateSubs(getMetronomeService().getSubdivisions());
    updateSubControls();
    updateTimerControls();
    refreshBookmarks();

    binding.seekbarMainTimer.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            int width = binding.seekbarMainTimer.getWidth()
                - binding.seekbarMainTimer.getPaddingStart()
                - binding.seekbarMainTimer.getPaddingEnd();
            binding.seekbarMainTimer.setMax(width);
            updateTimerProgress(
                getMetronomeService().getTimerProgress(), 0, false, false
            );
            if (binding.seekbarMainTimer.getViewTreeObserver().isAlive()) {
              binding.seekbarMainTimer.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        });

    int tempo = getMetronomeService().getTempo();
    setTempo(tempo);
    binding.textSwitcherMainTempoTerm.setCurrentText(getTempoTerm(tempo));

    ViewUtil.resetAnimatedIcon(binding.fabMainPlayStop);
    binding.fabMainPlayStop.setImageResource(
        getMetronomeService().isPlaying()
            ? R.drawable.ic_round_stop
            : R.drawable.ic_round_play_arrow
    );
    updateFabCornerRadius(getMetronomeService().isPlaying(), false);

    UiUtil.keepScreenAwake(activity, keepAwake && getMetronomeService().isPlaying());
  }

  @Override
  public void onMetronomeStart() {
    activity.runOnUiThread(() -> {
      if (!activity.hasNotificationPermission()) {
        getMetronomeService().stop();
        return;
      }
      if (binding != null) {
        beatsBgDrawable.reset();
        if (getMetronomeService().getCountIn() > 0) {
          beatsBgDrawable.setProgress(
              1, getMetronomeService().getCountInInterval(), true
          );
        }
        if (!reduceAnimations) {
          squiggly.setAnimate(true);
        }
        binding.fabMainPlayStop.setImageResource(R.drawable.ic_round_play_to_stop_anim);
        Drawable fabIcon = binding.fabMainPlayStop.getDrawable();
        if (fabIcon != null) {
          ((Animatable) fabIcon).start();
        }
        updateFabCornerRadius(true, true);
      }
      UiUtil.keepScreenAwake(activity, keepAwake);
    });
  }

  @Override
  public void onMetronomeStop() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        beatsBgDrawable.setProgressVisible(false, true);
        binding.fabMainPlayStop.setImageResource(R.drawable.ic_round_stop_to_play_anim);
        Drawable icon = binding.fabMainPlayStop.getDrawable();
        if (icon != null) {
          ((Animatable) icon).start();
        }
        updateFabCornerRadius(false, true);
        if (!reduceAnimations) {
          squiggly.setAnimate(false);
        }
      }
      stopTimerTransitionProgress();
      stopTimerProgress();
      UiUtil.keepScreenAwake(activity, false);
    });
  }

  @Override
  public void onMetronomePreTick(Tick tick) {
    if (!isBound()) {
      return;
    }
    activity.runOnUiThread(() -> {
      if (binding == null) {
        return;
      }
      View beat = binding.linearMainBeats.getChildAt(tick.beat - 1);
      if (beat instanceof BeatView && tick.subdivision == 1) {
        ((BeatView) beat).setTickType(tick.type);
        ((BeatView) beat).beat();
      }
      View subdivision = binding.linearMainSubs.getChildAt(tick.subdivision - 1);
      if (subdivision instanceof BeatView) {
        ((BeatView) subdivision).setTickType(tick.subdivision == 1 ? TICK_TYPE.MUTED : tick.type);
        ((BeatView) subdivision).beat();
      }
    });
  }

  @Override
  public void onMetronomeTick(Tick tick) {
    if (!isBound()) {
      return;
    }
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
        binding.coordinatorContainer.setBackgroundColor(color);
        binding.coordinatorContainer.postDelayed(() -> {
          if (binding != null) {
            binding.coordinatorContainer.setBackgroundColor(colorFlashMuted);
          }
        }, 100);
      }
      if (tick.subdivision == 1) {
        logoUtil.nextBeat(getMetronomeService().getInterval());

        /*int countIn = getMetronomeService().getCountIn();
        long beatIndex = tick.index / getMetronomeService().getSubsCount();
        int beatsCount = getMetronomeService().getBeatsCount() * countIn;
        long barIndex = beatIndex / getMetronomeService().getBeatsCount();
        boolean isCountIn = barIndex < getMetronomeService().getCountIn();
        float fraction = (beatIndex + 1) / (float) beatsCount;
        if (isCountIn) {
          updateProgress(fraction, getMetronomeService().getInterval(), true);
        }*/
      }
    });
  }

  @Override
  public void onTempoChanged(int tempoOld, int tempoNew) {
    activity.runOnUiThread(() -> setTempo(tempoOld, tempoNew));
  }

  @Override
  public void onTimerStarted() {
    stopTimerTransitionProgress();
    stopTimerProgress();

    int current = binding.seekbarMainTimer.getProgress();
    int max = binding.seekbarMainTimer.getMax();
    float currentFraction = current / (float) max;
    if (!getMetronomeService().equalsTimerProgress(currentFraction)) {
      // position where the timer will be at animation end
      // only if current progress is not equal to timer progress
      long animDuration = Constants.ANIM_DURATION_LONG;
      float fraction = (float) animDuration / getMetronomeService().getTimerInterval();
      fraction += getMetronomeService().getTimerProgress();
      progressTransitionAnimator = ValueAnimator.ofFloat(currentFraction, fraction);
      progressTransitionAnimator.addUpdateListener(animation -> {
        if (binding == null) {
          return;
        }
        binding.seekbarMainTimer.setProgress((int) ((float) animation.getAnimatedValue() * max));
        binding.seekbarMainTimer.invalidate();
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
        1, getMetronomeService().getTimerIntervalRemaining(), true, true
    );

    /*long delay = 0;
    int progress = binding.seekbarMainTimer.getProgress();
    int max = binding.seekbarMainTimer.getMax();
    float hello = progress / (float) max;
    if (hello == 1) {
      delay = Constants.ANIM_DURATION_LONG;
      long timerInterval = getMetronomeService().getTimerInterval();
      if (getMetronomeService().getCountIn() > 0) {
        // with count-in enough time to animate to start
        updateTimerProgress(0, true);
        delay += getMetronomeService().getCountInInterval();
      } else {
        // animate to position where the timer will be at animation end
        float fraction = (float) Constants.ANIM_DURATION_LONG / timerInterval;
        updateTimerProgress(fraction, true);
      }
    }
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      // start timer progress
      updateTimerProgress(
          1, getMetronomeService().getTimerInterval(), true, true
      );
    }, delay);*/
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (!isBoundOrShowWarning()) {
      return;
    }
    if (id == R.id.button_main_add_beat) {
      ViewUtil.startIcon(binding.buttonMainAddBeat.getIcon());
      performHapticClick();
      boolean success = getMetronomeService().addBeat();
      if (success) {
        BeatView beatView = new BeatView(activity);
        beatView.setIndex(binding.linearMainBeats.getChildCount());
        beatView.setOnClickListener(beat -> {
          performHapticClick();
          getMetronomeService().setBeat(beatView.getIndex(), beatView.nextTickType());
        });
        binding.linearMainBeats.addView(beatView);
        ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBeats);
        updateBeatControls();
      }
    } else if (id == R.id.button_main_remove_beat) {
      ViewUtil.startIcon(binding.buttonMainRemoveBeat.getIcon());
      performHapticClick();
      boolean success = getMetronomeService().removeBeat();
      if (success) {
        binding.linearMainBeats.removeViewAt(binding.linearMainBeats.getChildCount() - 1);
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizMainBeats, true
        );
        updateBeatControls();
      }
    } else if (id == R.id.button_main_add_subdivision) {
      ViewUtil.startIcon(binding.buttonMainAddSubdivision.getIcon());
      performHapticClick();
      boolean success = getMetronomeService().addSubdivision();
      if (success) {
        BeatView beatView = new BeatView(activity);
        beatView.setIsSubdivision(true);
        beatView.setIndex(binding.linearMainSubs.getChildCount());
        beatView.setOnClickListener(subdivision -> {
          performHapticClick();
          getMetronomeService().setSubdivision(beatView.getIndex(), beatView.nextTickType());
        });
        binding.linearMainSubs.addView(beatView);
        ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainSubs);
        updateSubControls();
      }
    } else if (id == R.id.button_main_remove_subdivision) {
      ViewUtil.startIcon(binding.buttonMainRemoveSubdivision.getIcon());
      performHapticClick();
      boolean success = getMetronomeService().removeSubdivision();
      if (success) {
        binding.linearMainSubs.removeViewAt(binding.linearMainSubs.getChildCount() - 1);
        ViewUtil.centerScrollContentIfNotFullWidth(
            binding.scrollHorizMainSubs, true
        );
        updateSubControls();
      }
    } else if (id == R.id.fab_main_play_stop) {
      if (getMetronomeService().isPlaying()) {
        performHapticClick();
        getMetronomeService().stop();
      } else {
        if (getMetronomeService().getGain() > 0) {
          dialogUtilGain.show();
        } else {
          getMetronomeService().start();
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
      boolean beatModeVibrateNew = !getMetronomeService().isBeatModeVibrate();
      if (beatModeVibrateNew && !activity.getHapticUtil().hasVibrator()) {
        showSnackbar(
            activity.getSnackbar(R.string.msg_vibration_unavailable, Snackbar.LENGTH_SHORT)
        );
        return;
      }
      if (!beatModeVibrateNew) {
        performHapticClick();
      }
      getMetronomeService().setBeatModeVibrate(beatModeVibrateNew);
      if (beatModeVibrateNew) {
        performHapticClick();
      }
      ViewUtil.startIcon(binding.buttonMainBeatMode.getIcon());
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (beatModeVibrateNew) {
          binding.buttonMainBeatMode.setIconResource(
              getMetronomeService().isAlwaysVibrate()
                  ? R.drawable.ic_round_volume_off_to_volume_on_anim
                  : R.drawable.ic_round_vibrate_to_volume_anim
          );
        } else {
          binding.buttonMainBeatMode.setIconResource(
              getMetronomeService().isAlwaysVibrate()
                  ? R.drawable.ic_round_volume_on_to_volume_off_anim
                  : R.drawable.ic_round_volume_to_vibrate_anim
          );
        }
      }, 300);
    } else if (id == R.id.button_main_bookmark) {
      ViewUtil.startIcon(binding.buttonMainBookmark.getIcon());
      performHapticClick();
      int tempo = getMetronomeService().getTempo();
      if (bookmarks.size() < Constants.BOOKMARKS_MAX && !bookmarks.contains(tempo)) {
        binding.chipGroupMainBookmarks.addView(getBookmarkChip(tempo));
        bookmarks.add(tempo);
        shortcutUtil.addShortcut(tempo);
        updateBookmarks();
        refreshBookmarks();
      } else if (bookmarks.size() >= Constants.BOOKMARKS_MAX) {
        Snackbar snackbar = activity.getSnackbar(R.string.msg_bookmarks_max, Snackbar.LENGTH_SHORT);
        snackbar.setAction(
            getString(R.string.action_clear_all),
            view -> {
              binding.chipGroupMainBookmarks.removeAllViews();
              bookmarks.clear();
              shortcutUtil.removeAllShortcuts();
              updateBookmarks();
              refreshBookmarks();
            }
        );
        showSnackbar(snackbar);
      }
    } else if (id == R.id.button_main_options) {
      performHapticClick();
      if (getMetronomeService().isPlaying()) {
        getMetronomeService().stop();
      }
      ViewUtil.startIcon(binding.buttonMainOptions.getIcon());
      optionsUtil.update();
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
        performHapticClick();
        getMetronomeService().setBeat(beatView.getIndex(), beatView.nextTickType());
      });
      beatView.setReduceAnimations(reduceAnimations);
      binding.linearMainBeats.addView(beatView);
    }
    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBeats);
    updateBeatControls();
  }

  private void updateBeatControls() {
    if (isBound()) {
      int beats = getMetronomeService().getBeatsCount();
      binding.buttonMainAddBeat.setEnabled(beats < Constants.BEATS_MAX);
      binding.buttonMainRemoveBeat.setEnabled(beats > 1);
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
          getMetronomeService().setSubdivision(beatView.getIndex(), beatView.nextTickType());
        });
      }
      binding.linearMainSubs.addView(beatView);
    }
    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainSubs, true);
    updateSubControls();
  }

  public void updateSubControls() {
    if (isBound()) {
      int subdivisions = getMetronomeService().getSubsCount();
      binding.buttonMainAddSubdivision.setEnabled(subdivisions < Constants.SUBS_MAX);
      binding.buttonMainRemoveSubdivision.setEnabled(subdivisions > 1);
      binding.linearMainSubsBg.setVisibility(
          getMetronomeService().getSubdivisionsUsed() ? View.VISIBLE : View.GONE
      );
    }
  }

  public void updateTimerControls() {
    if (isBound()) {
      binding.seekbarMainTimer.setVisibility(
          getMetronomeService().isTimerActive() ? View.VISIBLE : View.GONE
      );
    }
  }

  private Chip getBookmarkChip(int tempo) {
    Chip chip = new Chip(activity);
    chip.setCheckable(false);
    chip.setChipIconResource(R.drawable.ic_round_audiotrack_anim);
    chip.setCloseIconResource(R.drawable.ic_round_cancel);
    chip.setCloseIconVisible(true);
    chip.setOnCloseIconClickListener(v -> {
      performHapticClick();
      binding.chipGroupMainBookmarks.removeView(chip);
      bookmarks.remove((Integer) tempo); // Integer cast required, else it would take int as index
      shortcutUtil.removeShortcut(tempo);
      updateBookmarks();
      refreshBookmarks();
    });
    chip.setStateListAnimator(null);
    chip.setText(getString(R.string.label_bpm_value, tempo));
    chip.setTag(tempo);
    chip.setTextAppearance(R.style.TextAppearance_Tack_LabelLarge);
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

  private void refreshBookmarks() {
    if (!isBound()) {
      return;
    }
    binding.buttonMainBookmark.setEnabled(!bookmarks.contains(getMetronomeService().getTempo()));
    for (int i = 0; i < binding.chipGroupMainBookmarks.getChildCount(); i++) {
      Chip chip = (Chip) binding.chipGroupMainBookmarks.getChildAt(i);
      if (chip == null) {
        continue;
      }
      Object tag = chip.getTag();
      boolean isActive = tag != null && ((int) tag) == getMetronomeService().getTempo();
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
      if (isActive) {
        boolean isRtl = UiUtil.isLayoutRtl(activity);
        int scrollX = binding.scrollHorizMainBookmarks.getScrollX();
        int chipStart = isRtl ? chip.getRight() : chip.getLeft();
        int chipEnd = isRtl ? chip.getLeft() : chip.getRight();
        int scrollViewWidth = binding.scrollHorizMainBookmarks.getWidth();
        int margin = UiUtil.dpToPx(activity, 16);
        if (chipStart - margin < scrollX) {
          int scrollTo = chipStart - margin;
          binding.scrollHorizMainBookmarks.smoothScrollTo(scrollTo, 0);
        } else if (chipEnd + margin > (scrollX + scrollViewWidth)) {
          int scrollTo = chipEnd + margin - scrollViewWidth;
          binding.scrollHorizMainBookmarks.smoothScrollTo(scrollTo, 0);
        }
      }
    }
    ViewUtil.centerScrollContentIfNotFullWidth(binding.scrollHorizMainBookmarks);
  }

  private void changeTempo(int difference) {
    if (!isBound()) {
      return;
    }
    int tempoNew = getMetronomeService().getTempo() + difference;
    setTempo(tempoNew);
    if (tempoNew >= Constants.TEMPO_MIN && tempoNew <= Constants.TEMPO_MAX) {
      performHapticTick();
    }
  }

  private void setTempo(int tempo) {
    if (!isBound()) {
      return;
    }
    setTempo(getMetronomeService().getTempo(), tempo);
  }

  private void setTempo(int tempoOld, int tempoNew) {
    if (!isBound()) {
      return;
    }
    getMetronomeService().setTempo(
        Math.min(Math.max(tempoNew, Constants.TEMPO_MIN), Constants.TEMPO_MAX)
    );
    binding.textMainTempo.setText(String.valueOf(getMetronomeService().getTempo()));
    String termNew = getTempoTerm(tempoNew);
    if (!termNew.equals(getTempoTerm(tempoOld))) {
      boolean isFaster = getMetronomeService().getTempo() > tempoOld;
      binding.textSwitcherMainTempoTerm.setInAnimation(
          activity, isFaster ? R.anim.tempo_term_open_enter : R.anim.tempo_term_close_enter
      );
      binding.textSwitcherMainTempoTerm.setOutAnimation(
          activity, isFaster ? R.anim.tempo_term_open_exit : R.anim.tempo_term_close_exit
      );
      binding.textSwitcherMainTempoTerm.setText(termNew);
    }
    refreshBookmarks();
    setButtonStates();
  }

  private void setButtonStates() {
    if (isBound()) {
      int tempo = getMetronomeService().getTempo();
      binding.buttonMainLess.setEnabled(tempo > 1);
      binding.buttonMainMore.setEnabled(tempo < Constants.TEMPO_MAX);
    }
  }

  private void updateTimerProgress(
      float fraction, long duration, boolean animated, boolean linear
  ) {
    stopTimerProgress();
    if (!isBound()) {
      return;
    }
    int max = binding.seekbarMainTimer.getMax();
    if (animated) {
      float progress = getMetronomeService().getTimerProgress();
      progressAnimator = ValueAnimator.ofFloat(progress, fraction);
      progressAnimator.addUpdateListener(animation -> {
        if (binding == null || progressTransitionAnimator != null) {
          return;
        }
        binding.seekbarMainTimer.setProgress((int) ((float) animation.getAnimatedValue() * max));
        binding.seekbarMainTimer.invalidate();
      });
      progressAnimator.setInterpolator(
          linear ? new LinearInterpolator() : new FastOutSlowInInterpolator()
      );
      progressAnimator.setDuration(duration);
      progressAnimator.start();
    } else {
      binding.seekbarMainTimer.setProgress((int) (fraction * max));
      binding.seekbarMainTimer.invalidate();
    }
  }

  private void stopTimerProgress() {
    if (progressAnimator != null) {
      progressAnimator.pause();
      progressAnimator.cancel();
      progressAnimator.removeAllUpdateListeners();
      progressAnimator = null;
    }
  }

  private void stopTimerTransitionProgress() {
    if (progressTransitionAnimator != null) {
      progressTransitionAnimator.pause();
      progressTransitionAnimator.removeAllUpdateListeners();
      progressTransitionAnimator.cancel();
      progressTransitionAnimator = null;
    }
  }

  private void updateFabCornerRadius(boolean playing, boolean animated) {
    if (fabAnimator != null) {
      fabAnimator.pause();
      fabAnimator.cancel();
      fabAnimator = null;
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

  private boolean isBoundOrShowWarning() {
    boolean isBound = isBound();
    if (!isBound) {
      showSnackbar(activity.getSnackbar(R.string.msg_connection_lost, Snackbar.LENGTH_SHORT));
    }
    return isBound;
  }

  public void showSnackbar(Snackbar snackbar) {
    snackbar.setAnchorView(binding.fabMainPlayStop);
    snackbar.show();
  }

  public String getTempoTerm(int tempo) {
    String[] terms = getResources().getStringArray(R.array.label_tempo_terms);
    if (tempo < 60) {
      return terms[0];
    } else if (tempo < 66) {
      return terms[1];
    } else if (tempo < 76) {
      return terms[2];
    } else if (tempo < 108) {
      return terms[3];
    } else if (tempo < 120) {
      return terms[4];
    } else if (tempo < 168) {
      return terms[5];
    } else if (tempo < 200) {
      return terms[6];
    } else {
      return terms[7];
    }
  }
}