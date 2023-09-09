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
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.DialogFragment;
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
import xyz.zedler.patrick.tack.Constants.SETTINGS;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.FragmentMainAppBinding;
import xyz.zedler.patrick.tack.fragment.dialog.EmphasisBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.service.MetronomeService;
import xyz.zedler.patrick.tack.service.MetronomeService.LocalBinder;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.LogoUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.MetronomeUtil.TickListener;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.BpmPickerView;

public class MainFragment extends BaseFragment
    implements OnClickListener, ServiceConnection, TickListener {

  private static final String TAG = MainFragment.class.getSimpleName();

  private FragmentMainAppBinding binding;
  private MainActivity activity;

  private long prevTouchTime;
  private final List<Long> intervals = new ArrayList<>();

  private boolean isBound;
  private boolean hapticFeedback;
  private MetronomeService metronomeService;
  private LogoUtil logoUtil;

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
      if (id == R.id.action_about) {
        navigateToFragment(MainFragmentDirections.actionMainToAbout());
      } else if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_share) {
        ResUtil.share(activity, R.string.msg_share);
      }
      performHapticClick();
      return true;
    });
    ResUtil.tintMenuItemIcons(activity, binding.toolbarMain.getMenu());

    logoUtil = new LogoUtil(binding.imageMainLogo);
    if (!new HapticUtil(activity).hasVibrator()) {
      getSharedPrefs().edit().putBoolean(PREF.BEAT_MODE_VIBRATE, false).apply();
    }

    hapticFeedback = getSharedPrefs().getBoolean(SETTINGS.HAPTIC_FEEDBACK, DEF.HAPTIC_FEEDBACK);
    binding.textMainEmphasis.setText(
        String.valueOf(getSharedPrefs().getInt(PREF.EMPHASIS, DEF.EMPHASIS))
    );

    binding.bpmPickerMain.setOnRotationListener(new BpmPickerView.OnRotationListener() {
      @Override
      public void onRotate(int change) {
        changeBpm(change);
      }

      @Override
      public void onRotate(float change) {
        binding.circleMain.setRotation(
            binding.circleMain.getRotation() + change
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
          if (isBound) {
            if (metronomeService.getTempo() > 1) {
              changeBpm(-1);
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
          if (isBound) {
            if (metronomeService.getTempo() < 400) {
              changeBpm(1);
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
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        ViewUtil.startIcon(binding.buttonMainTempoTap.getIcon());

        long interval = System.currentTimeMillis() - prevTouchTime;
        if (prevTouchTime > 0 && interval <= 6000) {
          while (intervals.size() >= 10) {
            intervals.remove(0);
          }
          intervals.add(System.currentTimeMillis() - prevTouchTime);
          if (intervals.size() > 1) {
            setTempo((int) (60000 / getIntervalAverage()));
          }
        }
        prevTouchTime = System.currentTimeMillis();

        if (hapticFeedback
            && isBound
            && ((!metronomeService.isBeatModeVibrate() && !metronomeService.isAlwaysVibrate())
            || !metronomeService.isPlaying())
        ) {
          performHapticHeavyClick();
        }
      }
      return false;
    });

    boolean vibrateAlways = getSharedPrefs().getBoolean(SETTINGS.VIBRATE_ALWAYS, DEF.ALWAYS_VIBRATE);
    if (getSharedPrefs().getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE)) {
      binding.buttonMainBeatMode.setIconResource(
          vibrateAlways
              ? R.drawable.ic_round_volume_off_to_volume_on_anim
              : R.drawable.ic_round_vibrate_to_volume_anim
      );
    } else {
      binding.buttonMainBeatMode.setIconResource(
          vibrateAlways
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

    ViewUtil.setOnClickListeners(
        this,
        binding.buttonMainLess,
        binding.buttonMainMore,
        binding.buttonMainBeatMode,
        binding.buttonMainBookmark,
        binding.frameMainEmphasis,
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

    if (isBound) {
      activity.unbindService(this);
      isBound = false;
    }
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.fab_main_play_pause) {
      if (isBound) {
        if (metronomeService.isPlaying()) {
          metronomeService.stop();
        } else {
          metronomeService.start();
        }
      } else {
        Log.i(TAG, "onClick: hello hä");
      }
      if (hapticFeedback
          && isBound
          && (!metronomeService.isBeatModeVibrate() && !metronomeService.isAlwaysVibrate())
      ) {
        //hapticUtil.click();
      }
    } else if (id == R.id.button_main_less) {
      ViewUtil.startIcon(binding.buttonMainLess.getIcon());
      changeBpm(-1);
    } else if (id == R.id.button_main_more) {
      ViewUtil.startIcon(binding.buttonMainMore.getIcon());
      changeBpm(1);
    } else if (id == R.id.button_main_beat_mode) {
      boolean beatModeVibrateNew = !getSharedPrefs().getBoolean(
          PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE
      );
      boolean vibrateAlways = getSharedPrefs().getBoolean(SETTINGS.VIBRATE_ALWAYS, DEF.ALWAYS_VIBRATE);

      if (beatModeVibrateNew && !(new HapticUtil(activity).hasVibrator())) {
        Snackbar.make(
            binding.coordinatorContainer,
            getString(R.string.msg_vibration_unavailable),
            Snackbar.LENGTH_LONG
        ).setAnchorView(binding.fabMainPlayPause).show();
        return;
      }

      getSharedPrefs().edit().putBoolean(PREF.BEAT_MODE_VIBRATE, beatModeVibrateNew).apply();
      if (isBound) {
        //metronomeService.updateTick();
      }
      ViewUtil.startIcon(binding.buttonMainBeatMode.getIcon());
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (beatModeVibrateNew) {
          binding.buttonMainBeatMode.setIconResource(
              vibrateAlways
                  ? R.drawable.ic_round_volume_off_to_volume_on_anim
                  : R.drawable.ic_round_vibrate_to_volume_anim
          );
        } else {
          binding.buttonMainBeatMode.setIconResource(
              vibrateAlways
                  ? R.drawable.ic_round_volume_on_to_volume_off_anim
                  : R.drawable.ic_round_volume_to_vibrate_anim
          );
        }
      }, 300);
    } else if (id == R.id.button_main_bookmark) {
      ViewUtil.startIcon(binding.buttonMainBookmark.getIcon());
      if (hapticFeedback) {
        //hapticUtil.click();
      }
      if (isBound) {
        if (bookmarks.size() < 3 && !bookmarks.contains(metronomeService.getTempo())) {
          binding.chipGroupMain.addView(newChip(metronomeService.getTempo()));
          bookmarks.add(metronomeService.getTempo());
          updateBookmarks();
          refreshBookmark(true);
        } else if (bookmarks.size() >= 3) {
          Snackbar.make(
                  binding.coordinatorContainer,
                  getString(R.string.msg_bookmarks_max),
                  Snackbar.LENGTH_LONG
              ).setAnchorView(binding.fabMainPlayPause)
              .setAction(
                  getString(R.string.action_clear_all),
                  v1 -> {
                    binding.chipGroupMain.removeAllViews();
                    bookmarks.clear();
                    updateBookmarks();
                    refreshBookmark(true);
                  }
              ).show();
        }
      }
    } else if (id == R.id.frame_main_emphasis) {
      ViewUtil.startIcon(binding.imageMainEmphasis);
      if (hapticFeedback) {
        //hapticUtil.click();
      }
      if (getSharedPrefs().getBoolean(SETTINGS.EMPHASIS_SLIDER, DEF.EMPHASIS_SLIDER)) {
        DialogFragment fragment = new EmphasisBottomSheetDialogFragment();
        //fragment.show(getSupportFragmentManager(), fragment.toString());
      } else {
        setNextEmphasis();
      }
    }
  }

  @Override
  public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
    LocalBinder binder = (LocalBinder) iBinder;
    metronomeService = binder.getService();
    if (metronomeService == null) {
      return;
    }

    metronomeService.setTickListener(this);
    isBound = true;

    if (binding == null || getSharedPrefs() == null) {
      return;
    }

    if (getSharedPrefs().getBoolean(PREF.BEAT_MODE_VIBRATE, DEF.BEAT_MODE_VIBRATE)) {
      binding.buttonMainBeatMode.setIconResource(
          getSharedPrefs().getBoolean(SETTINGS.VIBRATE_ALWAYS, DEF.ALWAYS_VIBRATE)
              ? R.drawable.ic_round_volume_off_to_volume_on_anim
              : R.drawable.ic_round_vibrate_to_volume_anim
      );
    } else {
      binding.buttonMainBeatMode.setIconResource(
          getSharedPrefs().getBoolean(SETTINGS.VIBRATE_ALWAYS, DEF.ALWAYS_VIBRATE)
              ? R.drawable.ic_round_volume_on_to_volume_off_anim
              : R.drawable.ic_round_volume_to_vibrate_anim
      );
    }

    //metronomeService.updateTick();
    refreshBookmark(false);

    setTempo(metronomeService.getTempo());

    binding.fabMainPlayPause.setImageResource(
        metronomeService.isPlaying()
            ? R.drawable.ic_round_pause
            : R.drawable.ic_round_play_arrow
    );

    keepScreenAwake(metronomeService.isPlaying());
  }

  @Override
  public void onServiceDisconnected(ComponentName componentName) {
    isBound = false;
  }

  @Override
  public void onStartTicks() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        binding.fabMainPlayPause.setImageResource(R.drawable.ic_round_play_to_pause_anim);
        Drawable fabIcon = binding.fabMainPlayPause.getDrawable();
        if (fabIcon != null) {
          ((Animatable) fabIcon).start();
        }
      }
      keepScreenAwake(true);
    });
  }

  @Override
  public void onStopTicks() {
    activity.runOnUiThread(() -> {
      if (binding != null) {
        binding.fabMainPlayPause.setImageResource(R.drawable.ic_round_pause_to_play_anim);
        Drawable icon = binding.fabMainPlayPause.getDrawable();
        if (icon != null) {
          ((Animatable) icon).start();
        }
      }
      keepScreenAwake(false);
    });
  }

  @Override
  public void onTick(Tick tick) {
    activity.runOnUiThread(() -> {
      if (isBound) {
        logoUtil.nextBeat(metronomeService.getInterval());
      }
    });
  }

  public void onBpmChanged(int bpm) {
    if (isBound) {
      binding.textMainBpm.setText(String.valueOf(bpm));
      refreshBookmark(true);
    }
  }

  private void setNextEmphasis() {
    int emphasis = getSharedPrefs().getInt(PREF.EMPHASIS, DEF.EMPHASIS);
    int emphasisNew;
    if (emphasis < 6) {
      emphasisNew = emphasis + 1;
    } else {
      emphasisNew = 0;
    }
    getSharedPrefs().edit().putInt(PREF.EMPHASIS, emphasisNew).apply();
    new Handler(Looper.getMainLooper()).postDelayed(
        () -> binding.textMainEmphasis.setText(String.valueOf(emphasisNew)),
        150
    );
    if (isBound) {
      //metronomeService.updateTick();
    }
  }

  public void setEmphasis(int emphasis) {
    getSharedPrefs().edit().putInt(PREF.EMPHASIS, emphasis).apply();
    binding.textMainEmphasis.setText(String.valueOf(emphasis));
    if (hapticFeedback) {
      //hapticUtil.tick();
    }
    if (isBound) {
      //metronomeService.updateTick();
    }
  }

  private Chip newChip(int bpm) {
    Chip chip = new Chip(activity);
    chip.setCheckable(false);
    chip.setCloseIconVisible(true);
    chip.setCloseIconResource(R.drawable.ic_round_cancel);
    chip.setOnCloseIconClickListener(v -> {
      if (hapticFeedback) {
        //hapticUtil.click();
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

  public void keepScreenAwake(boolean keepAwake) {
    Window window = activity.getWindow();
    if (window == null) {
      return;
    }
    window.getDecorView().setKeepScreenOn(
        keepAwake
            && getSharedPrefs() != null
            && getSharedPrefs().getBoolean(SETTINGS.KEEP_AWAKE, DEF.KEEP_AWAKE)
    );
  }

  private void updateBookmarks() {
    StringBuilder stringBuilder = new StringBuilder();
    for (Integer bpm : bookmarks) {
      stringBuilder.append(bpm).append(",");
    }
    getSharedPrefs().edit().putString(PREF.BOOKMARKS, stringBuilder.toString()).apply();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      Collections.sort(bookmarks);
      ShortcutManager manager = (ShortcutManager) activity.getSystemService(Context.SHORTCUT_SERVICE);
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
    if (isBound) {
      binding.buttonMainBookmark.setEnabled(!bookmarks.contains(metronomeService.getTempo()));
      for (int i = 0; i < binding.chipGroupMain.getChildCount(); i++) {
        Chip chip = (Chip) binding.chipGroupMain.getChildAt(i);
        if (chip != null) {
          boolean active = Integer.parseInt(chip.getText().toString()) == metronomeService.getTempo();
          if (animated) {
            animateChip(chip, active);
          } else {
            if (active) {
              chip.setChipBackgroundColorResource(R.color.bookmark_active);
            } else {
              chip.setChipBackgroundColorResource(R.color.bookmark_inactive);
            }
          }
        }
      }
    }
  }

  private void animateChip(Chip chip, boolean active) {
    int colorFrom = Objects.requireNonNull(chip.getChipBackgroundColor()).getDefaultColor();
    int colorTo = ColorUtils.setAlphaComponent(ResUtil.getColorAttr(activity, R.attr.colorPrimaryContainer), active ? 180 : 0);
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

  private void changeBpm(int change) {
    if (isBound) {
      int bpmNew = metronomeService.getTempo() + change;
      setTempo(bpmNew);
      if (hapticFeedback
          && (!metronomeService.isPlaying()
          || (!metronomeService.isBeatModeVibrate() && !metronomeService.isAlwaysVibrate()))
          && bpmNew >= 1 && bpmNew <= 400
      ) {
        //hapticUtil.tick();
      }
    }
  }

  private void setTempo(int bpm) {
    if (isBound && bpm > 0) {
      //metronomeService.setTempo(Math.min(bpm, 400));
      refreshBookmark(true);
      binding.textMainBpm.setText(String.valueOf(metronomeService.getTempo()));
      if (metronomeService.getTempo() > 1) {
        if (!binding.buttonMainLess.isEnabled()) {
          binding.buttonMainLess.animate().alpha(1).setDuration(250).start();
        }
        binding.buttonMainLess.setEnabled(true);
      } else {
        if (binding.buttonMainLess.isEnabled()) {
          binding.buttonMainLess.animate().alpha(0.5f).setDuration(250).start();
        }
        binding.buttonMainLess.setEnabled(false);
      }
      if (metronomeService.getTempo() < 400) {
        if (!binding.buttonMainMore.isEnabled()) {
          binding.buttonMainMore.animate().alpha(1).setDuration(250).start();
        }
        binding.buttonMainMore.setEnabled(true);
      } else {
        if (binding.buttonMainMore.isEnabled()) {
          binding.buttonMainMore.animate().alpha(0.5f).setDuration(250).start();
        }
        binding.buttonMainMore.setEnabled(false);
      }
    }
  }

  private void setButtonStates() {
    if (isBound) {
      int bpm = metronomeService.getTempo();
      binding.buttonMainLess.setEnabled(bpm > 1);
      binding.buttonMainLess.setAlpha(bpm > 1 ? 1 : 0.5f);
      binding.buttonMainMore.setEnabled(bpm < 400);
      binding.buttonMainMore.setAlpha(bpm < 400 ? 1 : 0.5f);
    }
  }

  private long getIntervalAverage() {
    long sum = 0L;
    if (!intervals.isEmpty()) {
      for (long interval : intervals) {
        sum += interval;
      }
      return (long) ((double) sum / intervals.size());
    }
    return sum;
  }
}
