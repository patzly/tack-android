package xyz.zedler.patrick.tack.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.TICK_TYPE;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.FragmentMainAppBinding;
import xyz.zedler.patrick.tack.service.MetronomeService;
import xyz.zedler.patrick.tack.service.MetronomeService.LocalBinder;
import xyz.zedler.patrick.tack.service.MetronomeService.MetronomeListener;
import xyz.zedler.patrick.tack.util.LogoUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.BpmPickerView;

public class MainFragment extends BaseFragment
    implements OnClickListener, ServiceConnection, MetronomeListener {

  private static final String TAG = MainFragment.class.getSimpleName();

  private FragmentMainAppBinding binding;
  private MainActivity activity;
  private MetronomeService metronomeService;
  private boolean bound;
  private long prevTouchTime;
  private final List<Long> intervals = new ArrayList<>();
  private boolean flashScreen, keepAwake;
  private LogoUtil logoUtil;
  private ValueAnimator fabAnimator;
  private float cornerSizePause, cornerSizePlay, cornerSizeCurrent;
  private int colorFlashNormal, colorFlashStrong, colorFlashSub, colorFlashMuted;
  private List<Integer> bookmarks;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentMainAppBinding.inflate(inflater, container, false);
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
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarMain);
    systemBarBehavior.setContainer(binding.frameMainContainer);
    systemBarBehavior.setUp();

    new ScrollBehavior().setUpScroll(binding.appBarMain, null, false);

    binding.toolbarMain.setNavigationOnClickListener(v -> {
      if (getViewUtil().isClickEnabled(v.getId())) {
        performHapticClick();
        navigateUp();
      }
    });
    binding.toolbarMain.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (id == R.id.action_settings) {
        activity.navigateToFragment(MainFragmentDirections.actionMainToSettings());
      } else if (id == R.id.action_about) {
        navigateToFragment(MainFragmentDirections.actionMainToAbout());
      } else if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_recommend) {
        ResUtil.share(activity, R.string.msg_recommend);
      }
      performHapticClick();
      return true;
    });

    logoUtil = new LogoUtil(binding.imageMainLogo);

    flashScreen = getSharedPrefs().getBoolean(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN);
    keepAwake = getSharedPrefs().getBoolean(PREF.KEEP_AWAKE, DEF.KEEP_AWAKE);
    flashScreen = false;

    colorFlashNormal = ResUtil.getColorAttr(activity, R.attr.colorPrimary);
    colorFlashStrong = ResUtil.getColorAttr(activity, R.attr.colorError);
    colorFlashSub = ResUtil.getColorAttr(activity, R.attr.colorPrimaryContainer);
    colorFlashMuted = ResUtil.getColorAttr(activity, android.R.attr.colorBackground);

    binding.bpmPickerMain.setOnRotationListener(new BpmPickerView.OnRotationListener() {
      @Override
      public void onRotate(int bpm) {
        changeTempo(bpm);
      }

      @Override
      public void onRotate(float degrees) {
        binding.circleMain.setRotation(
            binding.circleMain.getRotation() + degrees
        );
      }
    });
    binding.bpmPickerMain.setOnPickListener(new BpmPickerView.OnPickListener() {
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
          if (bound) {
            if (metronomeService.getTempo() > 1) {
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
          if (bound) {
            if (metronomeService.getTempo() < 400) {
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
      if (event.getAction() != MotionEvent.ACTION_DOWN) {
        return false;
      }
      ViewUtil.startIcon(binding.buttonMainTempoTap.getIcon());

      long interval = System.currentTimeMillis() - prevTouchTime;
      if (prevTouchTime > 0 && interval <= 5000) {
        while (intervals.size() >= 10) {
          intervals.remove(0);
        }
        intervals.add(System.currentTimeMillis() - prevTouchTime);
        if (intervals.size() > 1) {
          long intervalAverage;
          long sum = 0L;
          for (long e : intervals) {
            sum += e;
          }
          intervalAverage = (long) ((double) sum / intervals.size());
          if (isBound()) {
            setTempo((int) (60000 / intervalAverage));
          }
        }
      }
      prevTouchTime = System.currentTimeMillis();

      if (areHapticsAllowed()) {
        performHapticHeavyClick();
      }
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

    String prefBookmarks = getSharedPrefs().getString(PREF.BOOKMARKS, null);
    List<String> bookmarksArray;
    if (prefBookmarks != null) {
      bookmarksArray = Arrays.asList(prefBookmarks.split(","));
    } else {
      bookmarksArray = new ArrayList<>();
    }
    bookmarks = new ArrayList<>(bookmarksArray.size());
    for (int i = 0; i < bookmarksArray.size(); i++) {
      if (!bookmarksArray.get(i).isEmpty()) {
        bookmarks.add(Integer.parseInt(bookmarksArray.get(i)));
      }
    }
    for (int i = 0; i < bookmarks.size(); i++) {
      binding.chipGroupMain.addView(newChip(bookmarks.get(i)));
    }

    ViewUtil.resetAnimatedIcon(binding.fabMainPlayPause);
    binding.fabMainPlayPause.setImageResource(R.drawable.ic_round_play_to_pause_anim);
    cornerSizePause = UiUtil.dpToPx(activity, 28);
    cornerSizePlay = UiUtil.dpToPx(activity, 48);
    cornerSizeCurrent = cornerSizePause;

    ViewUtil.setOnClickListeners(
        this,
        binding.buttonMainLess,
        binding.buttonMainMore,
        binding.buttonMainBeatMode,
        binding.buttonMainBookmark,
        binding.buttonMainOptions,
        binding.fabMainPlayPause
    );
  }

  @Override
  public void onStart() {
    super.onStart();

    Intent intent = new Intent(activity, MetronomeService.class);
    try {
      activity.startService(intent);
      activity.bindService(intent, this, Context.BIND_AUTO_CREATE);
    } catch (IllegalStateException e) {
      Log.e(TAG, "onStart: cannot start service because app is in background");
    }
  }

  @Override
  public void onStop() {
    super.onStop();

    if (bound) {
      activity.unbindService(this);
      bound = false;
    }
  }

  @Override
  public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
    LocalBinder binder = (LocalBinder) iBinder;
    metronomeService = binder.getService();
    if (metronomeService == null || binding == null || getSharedPrefs() == null) {
      return;
    }
    bound = true;
    metronomeService.setMetronomeListener(this);

    if (metronomeService.isBeatModeVibrate()) {
      binding.buttonMainBeatMode.setIconResource(
          metronomeService.isAlwaysVibrate()
              ? R.drawable.ic_round_volume_off_to_volume_on_anim
              : R.drawable.ic_round_vibrate_to_volume_anim
      );
    } else {
      binding.buttonMainBeatMode.setIconResource(
          metronomeService.isAlwaysVibrate()
              ? R.drawable.ic_round_volume_on_to_volume_off_anim
              : R.drawable.ic_round_volume_to_vibrate_anim
      );
    }

    //metronomeService.updateTick();
    refreshBookmark(false);

    setTempo(metronomeService.getTempo());

    ViewUtil.resetAnimatedIcon(binding.fabMainPlayPause);
    binding.fabMainPlayPause.setImageResource(
        metronomeService.isPlaying()
            ? R.drawable.ic_round_pause
            : R.drawable.ic_round_play_arrow
    );
    updateFabCornerRadius(metronomeService.isPlaying(), false);

    UiUtil.keepScreenAwake(activity, keepAwake && metronomeService.isPlaying());
  }

  @Override
  public void onServiceDisconnected(ComponentName componentName) {
    bound = false;
  }

  @Override
  public void onMetronomeStart() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        binding.fabMainPlayPause.setImageResource(R.drawable.ic_round_play_to_pause_anim);
        Drawable fabIcon = binding.fabMainPlayPause.getDrawable();
        if (fabIcon != null) {
          ((Animatable) fabIcon).start();
        }
        updateFabCornerRadius(true, true);
      }
    });
    UiUtil.keepScreenAwake(activity, keepAwake);
  }

  @Override
  public void onMetronomeStop() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        binding.fabMainPlayPause.setImageResource(R.drawable.ic_round_pause_to_play_anim);
        Drawable icon = binding.fabMainPlayPause.getDrawable();
        if (icon != null) {
          ((Animatable) icon).start();
        }
        updateFabCornerRadius(false, true);
      }
    });
    UiUtil.keepScreenAwake(activity, false);
  }

  @Override
  public void onMetronomeTick(Tick tick) {
    if (!bound) {
      return;
    }
    activity.runOnUiThread(() -> {
      if (flashScreen) {
        int color;
        switch (tick.type) {
          case TICK_TYPE.STRONG:
            color = colorFlashStrong;
            break;
          case TICK_TYPE.SUB:
            color = colorFlashSub;
            break;
          case TICK_TYPE.MUTED:
            color = colorFlashMuted;
            break;
          default:
            color = colorFlashNormal;
            break;
        }
        binding.coordinatorContainer.setBackgroundColor(color);
        binding.coordinatorContainer.postDelayed(
            () -> binding.coordinatorContainer.setBackgroundColor(colorFlashMuted),
            100
        );
      }
      if (!tick.type.equals(TICK_TYPE.SUB)) {
        logoUtil.nextBeat(metronomeService.getInterval());
      }
    });
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (!isBound()) {
      return;
    }
    if (id == R.id.fab_main_play_pause) {
      if (metronomeService.isPlaying()) {
        if (areHapticsAllowed()) {
          performHapticClick();
        }
        metronomeService.stop();
      } else {
        metronomeService.start();
        if (areHapticsAllowed()) {
          performHapticClick();
        }
      }
    } else if (id == R.id.button_main_less) {
      ViewUtil.startIcon(binding.buttonMainLess.getIcon());
      changeTempo(-1);
    } else if (id == R.id.button_main_more) {
      ViewUtil.startIcon(binding.buttonMainMore.getIcon());
      changeTempo(1);
    } else if (id == R.id.button_main_beat_mode) {
      boolean beatModeVibrateNew = !metronomeService.isBeatModeVibrate();
      if (beatModeVibrateNew && !activity.getHapticUtil().hasVibrator()) {
        showSnackbar(
            activity.getSnackbar(R.string.msg_vibration_unavailable, Snackbar.LENGTH_SHORT)
        );
        return;
      }
      if (!beatModeVibrateNew && areHapticsAllowed()) {
        performHapticClick();
      }
      metronomeService.setBeatModeVibrate(beatModeVibrateNew);
      if (beatModeVibrateNew && areHapticsAllowed()) {
        performHapticClick();
      }
      ViewUtil.startIcon(binding.buttonMainBeatMode.getIcon());
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (beatModeVibrateNew) {
          binding.buttonMainBeatMode.setIconResource(
              metronomeService.isAlwaysVibrate()
                  ? R.drawable.ic_round_volume_off_to_volume_on_anim
                  : R.drawable.ic_round_vibrate_to_volume_anim
          );
        } else {
          binding.buttonMainBeatMode.setIconResource(
              metronomeService.isAlwaysVibrate()
                  ? R.drawable.ic_round_volume_on_to_volume_off_anim
                  : R.drawable.ic_round_volume_to_vibrate_anim
          );
        }
      }, 300);
    } else if (id == R.id.button_main_bookmark) {
      ViewUtil.startIcon(binding.buttonMainBookmark.getIcon());
      if (areHapticsAllowed()) {
        performHapticClick();
      }
      if (bookmarks.size() < 3 && !bookmarks.contains(metronomeService.getTempo())) {
        binding.chipGroupMain.addView(newChip(metronomeService.getTempo()));
        bookmarks.add(metronomeService.getTempo());
        updateBookmarks();
        refreshBookmark(true);
      } else if (bookmarks.size() >= 3) {
        Snackbar snackbar = activity.getSnackbar(
            R.string.msg_bookmarks_max, Snackbar.LENGTH_SHORT
        );
        snackbar.setAction(
            getString(R.string.action_clear_all),
            view -> {
              binding.chipGroupMain.removeAllViews();
              bookmarks.clear();
              updateBookmarks();
              refreshBookmark(true);
            }
        );
        showSnackbar(snackbar);
      }
    } else if (id == R.id.button_main_options) {
      if (areHapticsAllowed()) {
        performHapticClick();
      }
    }
  }

  public void onBpmChanged(int bpm) {
    if (bound) {
      binding.textMainBpm.setText(String.valueOf(bpm));
      refreshBookmark(true);
    }
  }

  private Chip newChip(int bpm) {
    Chip chip = new Chip(activity);
    chip.setCheckable(false);
    chip.setCloseIconVisible(true);
    chip.setCloseIconResource(R.drawable.ic_round_cancel);
    chip.setOnCloseIconClickListener(v -> {
      if (areHapticsAllowed()) {
        performHapticClick();
      }
      binding.chipGroupMain.removeView(chip);
      bookmarks.remove((Integer) bpm); // Integer cast required
      updateBookmarks();
      refreshBookmark(true);
    });
    chip.setStateListAnimator(null);
    chip.setText(String.valueOf(bpm));
    chip.setTypeface(ResourcesCompat.getFont(activity, R.font.jost_medium));
    chip.setChipIconVisible(false);
    chip.setOnClickListener(v -> setTempo(bpm));
    return chip;
  }

  private void updateBookmarks() {
    StringBuilder stringBuilder = new StringBuilder();
    for (Integer bpm : bookmarks) {
      stringBuilder.append(bpm).append(",");
    }
    getSharedPrefs().edit().putString(PREF.BOOKMARKS, stringBuilder.toString()).apply();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      Collections.sort(bookmarks);
      ShortcutManager manager = (ShortcutManager) activity.getSystemService(
          Context.SHORTCUT_SERVICE);
      if (manager != null) {
        List<ShortcutInfo> shortcuts = new ArrayList<>();
        for (int bpm : bookmarks) {
          shortcuts.add(
              new ShortcutInfo.Builder(activity, String.valueOf(bpm))
                  .setShortLabel(getString(R.string.label_bpm_number, String.valueOf(bpm)))
                  .setIcon(Icon.createWithResource(activity, R.mipmap.ic_shortcut))
//                  .setIntent(new Intent(activity, ShortcutActivity.class)
//                      .setAction(OldMetronomeService.ACTION_START)
//                      .putExtra(OldMetronomeService.EXTRA_BPM, bpm)
//                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                  .build()
          );
        }

        manager.setDynamicShortcuts(shortcuts);
      }
    }
  }

  private void refreshBookmark(boolean animated) {
    if (bound) {
      binding.buttonMainBookmark.setEnabled(!bookmarks.contains(metronomeService.getTempo()));
      for (int i = 0; i < binding.chipGroupMain.getChildCount(); i++) {
        Chip chip = (Chip) binding.chipGroupMain.getChildAt(i);
        if (chip != null) {
          boolean active =
              Integer.parseInt(chip.getText().toString()) == metronomeService.getTempo();
          if (animated) {
            animateChip(chip, active);
          } else {
            if (active) {
              chip.setChipBackgroundColor(
                  ColorStateList.valueOf(ResUtil.getColorAttr(activity, R.attr.colorTertiary)));
            } else {
              chip.setChipBackgroundColor(
                  ColorStateList.valueOf(ResUtil.getColorAttr(activity, R.attr.colorOutline)));
            }
          }
        }
      }
    }
  }

  private void animateChip(Chip chip, boolean active) {
    int colorFrom = Objects.requireNonNull(chip.getChipBackgroundColor()).getDefaultColor();
    int colorTo = ColorUtils.setAlphaComponent(
        ResUtil.getColorAttr(activity, R.attr.colorPrimaryContainer), active ? 180 : 0);
    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
    colorAnimation.addUpdateListener(
        animator -> chip.setChipBackgroundColor(
            ColorStateList.valueOf((int) animator.getAnimatedValue())
        )
    );
    colorAnimation.setDuration(250);
    colorAnimation.setInterpolator(new FastOutSlowInInterpolator());
    colorAnimation.start();
  }

  private void changeTempo(int change) {
    if (bound) {
      int bpmNew = metronomeService.getTempo() + change;
      setTempo(bpmNew);
      if (areHapticsAllowed() && bpmNew >= 1 && bpmNew <= 500) {
        performHapticTick();
      }
    }
  }

  private void setTempo(int bpm) {
    if (bound && bpm > 0) {
      metronomeService.setTempo(Math.min(bpm, 500));
      refreshBookmark(true);
      binding.textMainBpm.setText(String.valueOf(metronomeService.getTempo()));
      setButtonStates();
    }
  }

  private void setButtonStates() {
    if (bound) {
      int bpm = metronomeService.getTempo();
      binding.buttonMainLess.setEnabled(bpm > 1);
      binding.buttonMainMore.setEnabled(bpm < 500);
    }
  }

  private void updateFabCornerRadius(boolean playing, boolean animated) {
    if (fabAnimator != null) {
      fabAnimator.pause();
      fabAnimator.cancel();
      fabAnimator = null;
    }
    float cornerSizeNew = playing ? cornerSizePlay : cornerSizePause;
    if (animated) {
      fabAnimator = ValueAnimator.ofFloat(cornerSizeCurrent, cornerSizeNew);
      fabAnimator.addUpdateListener(animation -> {
        cornerSizeCurrent = (float) animation.getAnimatedValue();
        binding.fabMainPlayPause.setShapeAppearanceModel(
            binding.fabMainPlayPause.getShapeAppearanceModel().withCornerSize(cornerSizeCurrent)
        );
      });
      fabAnimator.setInterpolator(new FastOutSlowInInterpolator());
      fabAnimator.setDuration(300);
      fabAnimator.start();
    } else {
      cornerSizeCurrent = cornerSizeNew;
      binding.fabMainPlayPause.setShapeAppearanceModel(
          binding.fabMainPlayPause.getShapeAppearanceModel().withCornerSize(cornerSizeNew)
      );
    }
  }

  private boolean areHapticsAllowed() {
    return bound && metronomeService.areHapticEffectsPossible();
  }

  private boolean isBound() {
    if (!bound) {
      showSnackbar(activity.getSnackbar(R.string.msg_connection_lost, Snackbar.LENGTH_SHORT));
    }
    return bound;
  }

  private void showSnackbar(Snackbar snackbar) {
    snackbar.setAnchorView(binding.fabMainPlayPause);
    activity.showSnackbar(snackbar);
  }
}
