package xyz.zedler.patrick.tack.activity;

import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.databinding.ActivityMainNewBinding;
import xyz.zedler.patrick.tack.fragment.EmphasisBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.fragment.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.service.MetronomeService;
import xyz.zedler.patrick.tack.util.ClickUtil;
import xyz.zedler.patrick.tack.util.LogoUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.VibratorUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.BpmPickerView;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, ServiceConnection,
        MetronomeService.TickListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static boolean DEBUG = false;

    private ActivityMainNewBinding binding;
    private SharedPreferences sharedPrefs;
    private long prevTouchTime;
    private final List<Long> intervals = new ArrayList<>();

    private boolean isBound;
    private MetronomeService service;
    private LogoUtil logoUtil;
    private ClickUtil clickUtil;
    private VibratorUtil vibratorUtil;

    private List<Integer> bookmarks;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainNewBinding.inflate(getLayoutInflater());
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        AppCompatDelegate.setDefaultNightMode(
                sharedPrefs.getBoolean("force_dark_mode",false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        setContentView(binding.getRoot());

        new ScrollBehavior().setUpScroll(
                this,
                binding.appBarMain,
                binding.linearMainAppBar,
                null,
                true
        );

        logoUtil = new LogoUtil(binding.imageMainLogo);
        clickUtil = new ClickUtil();
        vibratorUtil = new VibratorUtil(this);

        binding.toolbarMain.setOnMenuItemClickListener((MenuItem item) -> {
            if (clickUtil.isDisabled()) return false;

            int itemId = item.getItemId();
            if (itemId == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (itemId == R.id.action_about) {
                startActivity(new Intent(this, AboutActivity.class));
            } else if (itemId == R.id.action_share) {
                ResUtil.share(this, R.string.msg_share);
            } else if (itemId == R.id.action_feedback) {
                new FeedbackBottomSheetDialogFragment().show(
                        getSupportFragmentManager(),
                        "feedback"
                );
            }
            return true;
        });

        binding.textMainEmphasis.setText(
                String.valueOf(sharedPrefs.getInt("emphasis", 0))
        );

        binding.bpmPickerMain.setOnRotationListener(new BpmPickerView.OnRotationListener() {
            @Override
            public void onRotate(int change) {
                changeBpm(change);
            }

            @Override
            public void onRotate(float change) {
                binding.dottedCircleMain.setRotation(
                        binding.dottedCircleMain.getRotation() + change
                );
            }
        });
        binding.bpmPickerMain.setOnPickListener(new BpmPickerView.OnPickListener() {
            @Override
            public void onPickDown() {
                binding.dottedCircleMain.setHighlighted(true);
            }

            @Override
            public void onPickUpOrCancel() {
                binding.dottedCircleMain.setHighlighted(false);
            }
        });

        binding.frameMainLess.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;
            private int nextRun = 500;
            private final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isBound()) {
                        if (service.getBpm() > 1) {
                            changeBpm(-1);
                            handler.postDelayed(this, nextRun);
                            if (nextRun > 50) nextRun = (int) (nextRun * 0.9);
                        } else {
                            handler.removeCallbacks(runnable);
                            handler = null;
                            nextRun = 500;
                        }
                    }
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (handler != null) return true;
                        handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(runnable, ViewConfiguration.getLongPressTimeout());
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        if (handler == null) return true;
                        handler.removeCallbacks(runnable);
                        handler = null;
                        nextRun = 500;
                        break;
                }
                return false;
            }
        });

        binding.frameMainMore.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;
            private int nextRun = 500;
            private final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isBound()) {
                        if (service.getBpm() < 300) {
                            changeBpm(1);
                            handler.postDelayed(this, nextRun);
                            if (nextRun > 50) nextRun = (int) (nextRun * 0.9);
                        } else {
                            handler.removeCallbacks(runnable);
                            handler = null;
                            nextRun = 500;
                        }
                    }
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (handler != null) return true;
                        handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(runnable, ViewConfiguration.getLongPressTimeout());
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        if (handler == null) return true;
                        handler.removeCallbacks(runnable);
                        handler = null;
                        nextRun = 500;
                        break;
                }
                return false;
            }
        });

        ViewUtil.setOnClickListeners(
                this,
                binding.frameMainLess,
                binding.frameMainMore,
                binding.frameMainTempoTap,
                binding.frameMainBeatMode,
                binding.frameMainBookmark,
                binding.frameMainEmphasis,
                binding.fabMain
        );

        boolean vibrateAlways = sharedPrefs.getBoolean("vibrate_always", false);
        if (sharedPrefs.getBoolean("beat_mode_vibrate", true)) {
            binding.imageMainBeatMode.setImageResource(
                    vibrateAlways
                            ? R.drawable.ic_round_volume_off_to_volume_on_anim
                            : R.drawable.ic_round_vibrate_to_volume_anim
            );
        } else {
            binding.imageMainBeatMode.setImageResource(
                    vibrateAlways
                            ? R.drawable.ic_round_volume_on_to_volume_off_anim
                            : R.drawable.ic_round_volume_to_vibrate_anim
            );
        }

        setButtonStates();

        String prefBookmarks = sharedPrefs.getString("bookmarks", null);
        List<String> bookmarksArray;
        if (prefBookmarks != null) {
            bookmarksArray = Arrays.asList(prefBookmarks.split(","));
        } else {
            bookmarksArray = new ArrayList<>();
        }
        bookmarks = new ArrayList<>(bookmarksArray.size());
        for(int i = 0; i < bookmarksArray.size(); i++) {
            if (!bookmarksArray.get(i).equals("")) {
                bookmarks.add(Integer.parseInt(bookmarksArray.get(i)));
            }
        }
        for(int i = 0; i < bookmarks.size(); i++) {
            binding.chipGroupMain.addView(newChip(bookmarks.get(i)));
        }

        int feedback = sharedPrefs.getInt("feedback_pop_up", 1);
        if (feedback > 0) {
            if (feedback < 5) {
                sharedPrefs.edit().putInt("feedback_pop_up", feedback + 1).apply();
            } else {
                new FeedbackBottomSheetDialogFragment().show(
                        getSupportFragmentManager(),
                        "feedback"
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    protected void onStart() {
        Intent intent = new Intent(this, MetronomeService.class);

        // sometimes throws exception because "app is in the background"
        startService(intent);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        super.onStart();
    }

    @Override
    protected void onStop() {
        if (isBound) {
            unbindService(this);
            isBound = false;
        }
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab_main) {
            if (isBound()) {
                if (service.isPlaying())
                    service.pause();
                else service.play();
                    /*startAnimatedFabIcon();
                    new Handler().postDelayed(
                            () -> fab.setImageResource(
                                    service.isPlaying()
                                            ? R.drawable.ic_round_pause_to_play_anim
                                            : R.drawable.ic_round_play_to_pause_anim
                            ), 3000);*/
            }
        } else if (id == R.id.frame_main_less) {
            ViewUtil.startAnimatedIcon(binding.imageMainLess);
            changeBpm(-1);
        } else if (id == R.id.frame_main_more) {
            ViewUtil.startAnimatedIcon(binding.imageMainMore);
            changeBpm(1);
        } else if (id == R.id.frame_main_tempo_tap) {
            ViewUtil.startAnimatedIcon(binding.imageMainTempoTap);

            long interval = System.currentTimeMillis() - prevTouchTime;
            if (prevTouchTime > 0 && interval <= 6000) {
                if (intervals.size() == 4) {
                    intervals.remove(0);
                }
                intervals.add(System.currentTimeMillis() - prevTouchTime);
                if (intervals.size() > 1) {
                    setBpm(toBpm(getIntervalAverage()));
                }
            }
            prevTouchTime = System.currentTimeMillis();
        } else if (id == R.id.frame_main_beat_mode) {
            boolean beatModeVibrateNew = !sharedPrefs.getBoolean("beat_mode_vibrate", true);
            boolean vibrateAlways = sharedPrefs.getBoolean("vibrate_always", false);
            sharedPrefs.edit().putBoolean("beat_mode_vibrate", beatModeVibrateNew).apply();
            if (isBound()) service.updateTick();
            ViewUtil.startAnimatedIcon(binding.imageMainBeatMode);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (beatModeVibrateNew) {
                    binding.imageMainBeatMode.setImageResource(
                            vibrateAlways
                                    ? R.drawable.ic_round_volume_off_to_volume_on_anim
                                    : R.drawable.ic_round_vibrate_to_volume_anim
                    );
                } else {
                    binding.imageMainBeatMode.setImageResource(
                            vibrateAlways
                                    ? R.drawable.ic_round_volume_on_to_volume_off_anim
                                    : R.drawable.ic_round_volume_to_vibrate_anim
                    );
                }
            }, 300);
        } else if (id == R.id.frame_main_bookmark) {
            ViewUtil.startAnimatedIcon(binding.imageMainBookmark);
            if (isBound()) {
                if (bookmarks.size() < 3 && !bookmarks.contains(service.getBpm())) {
                    binding.chipGroupMain.addView(newChip(service.getBpm()));
                    bookmarks.add(service.getBpm());
                    updateBookmarks();
                    refreshBookmark(true);
                } else if (bookmarks.size() >= 3) {
                    Snackbar.make(
                            binding.coordinatorContainer,
                            getString(R.string.msg_bookmarks_max),
                            Snackbar.LENGTH_LONG
                    ).setAnchorView(binding.fabMain)
                            .setActionTextColor(ContextCompat.getColor(this, R.color.secondary))
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
            ViewUtil.startAnimatedIcon(binding.imageMainEmphasis);
            if (sharedPrefs.getBoolean("emphasis_slider", false)) {
                new EmphasisBottomSheetDialogFragment().show(
                        getSupportFragmentManager(), "emphasis"
                );
            } else {
                setNextEmphasis();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MetronomeService.LocalBinder binder = (MetronomeService.LocalBinder) iBinder;
        service = binder.getService();
        service.setTickListener(this);
        isBound = true;

        if (sharedPrefs.getBoolean("beat_mode_vibrate", true)) {
            binding.imageMainBeatMode.setImageResource(
                    sharedPrefs.getBoolean("vibrate_always", false)
                            ? R.drawable.ic_round_volume_off_to_volume_on_anim
                            : R.drawable.ic_round_vibrate_to_volume_anim
            );
        } else {
            binding.imageMainBeatMode.setImageResource(
                    sharedPrefs.getBoolean("vibrate_always", false)
                            ? R.drawable.ic_round_volume_on_to_volume_off_anim
                            : R.drawable.ic_round_volume_to_vibrate_anim
            );
        }

        service.updateTick();
        refreshBookmark(false);

        setBpm(service.getBpm());

        binding.fabMain.setImageResource(
                service.isPlaying()
                        ? R.drawable.ic_round_pause
                        : R.drawable.ic_round_play_arrow
        );

        keepScreenAwake(service.isPlaying());
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        isBound = false;
    }

    @Override
    public void onStartTicks() {
        /*if (service.isPlaying()) service.pause();
        else service.play();
        */
        binding.fabMain.setImageResource(R.drawable.ic_round_play_to_pause_anim);
        ((Animatable) binding.fabMain.getDrawable()).start();
        keepScreenAwake(true);
    }

    @Override
    public void onStopTicks() {
        binding.fabMain.setImageResource(R.drawable.ic_round_pause_to_play_anim);
        ((Animatable) binding.fabMain.getDrawable()).start();
        keepScreenAwake(false);
    }

    @Override
    public void onTick(long interval, boolean isEmphasis, int index) {
        logoUtil.nextBeat(interval);
    }

    @Override
    public void onBpmChanged(int bpm) {
        if (isBound()) {
            binding.textMainBpm.setText(String.valueOf(bpm));
            refreshBookmark(true);
        }
    }

    private void setNextEmphasis() {
        int emphasis = sharedPrefs.getInt("emphasis", 0);
        int emphasisNew;
        if (emphasis < 6) {
            emphasisNew = emphasis + 1;
        } else {
            emphasisNew = 0;
        }
        sharedPrefs.edit().putInt("emphasis", emphasisNew).apply();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> binding.textMainEmphasis.setText(String.valueOf(emphasisNew)),
                150
        );
        if (isBound) service.updateTick();
    }

    public void setEmphasis(int emphasis) {
        sharedPrefs.edit().putInt("emphasis", emphasis).apply();
        binding.textMainEmphasis.setText(String.valueOf(emphasis));
        if (isBound) service.updateTick();
    }

    private static int toBpm(long interval) {
        return (int) (60000 / interval);
    }

    private Chip newChip(int bpm) {
        Chip chip = new Chip(this);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setCloseIconVisible(true);
        chip.setCloseIconTintResource(R.color.icon);
        chip.setCloseIconResource(R.drawable.ic_round_cancel);
        chip.setOnCloseIconClickListener(v -> {
            binding.chipGroupMain.removeView(chip);
            bookmarks.remove((Integer) bpm);
            updateBookmarks();
            refreshBookmark(true);
        });
        chip.setChipBackgroundColorResource(R.color.background);
        chip.setText(String.valueOf(bpm));
        chip.setTextAppearance(R.style.TextAppearance_Tack_Chip);
        chip.setTypeface(ResourcesCompat.getFont(this, R.font.text_bold));
        chip.setChipIconVisible(false);
        chip.setChipStrokeWidth(getResources().getDimension(R.dimen.chip_stroke_width));
        chip.setChipStrokeColorResource(R.color.stroke_primary);
        chip.setRippleColor(null);
        chip.setOnClickListener(v -> setBpm(bpm));
        return chip;
    }

    public void keepScreenAwake(boolean keepAwake) {
        if (keepAwake && sharedPrefs.getBoolean("keep_awake", true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void updateBookmarks() {
        StringBuilder stringBuilder = new StringBuilder();
        for(Integer bpm : bookmarks) {
            stringBuilder.append(bpm).append(",");
        }
        sharedPrefs.edit().putString("bookmarks", stringBuilder.toString()).apply();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Collections.sort(bookmarks);
            ShortcutManager manager = (ShortcutManager) getSystemService(Context.SHORTCUT_SERVICE);
            if (manager != null) {
                List<ShortcutInfo> shortcuts = new ArrayList<>();
                for (int bpm : bookmarks) {
                    shortcuts.add(
                            new ShortcutInfo.Builder(this, String.valueOf(bpm))
                                    .setShortLabel(getString(R.string.title_bpm, String.valueOf(bpm)))
                                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_shortcut))
                                    .setIntent(new Intent(this, ShortcutActivity.class)
                                            .setAction(MetronomeService.ACTION_START)
                                            .putExtra(MetronomeService.EXTRA_BPM, bpm)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    .build()
                    );
                }

                manager.setDynamicShortcuts(shortcuts);
            }
        }
    }

    private void refreshBookmark(boolean animated) {
        if (isBound()) {
            if (!bookmarks.contains(service.getBpm())) {
                if (!binding.frameMainBookmark.isEnabled()) {
                    if (animated) {
                        binding.frameMainBookmark.animate().alpha(1).setDuration(300).start();
                    } else {
                        binding.frameMainBookmark.setAlpha(1);
                    }
                }
                binding.frameMainBookmark.setEnabled(true);
            } else {
                if (binding.frameMainBookmark.isEnabled()) {
                    if (animated) {
                        binding.frameMainBookmark.animate().alpha(0.5f).setDuration(300).start();
                    } else {
                        binding.frameMainBookmark.setAlpha(0.5f);
                    }
                }
                binding.frameMainBookmark.setEnabled(false);
            }
            for(int i = 0; i < binding.chipGroupMain.getChildCount(); i++) {
                Chip chip = (Chip) binding.chipGroupMain.getChildAt(i);
                if (chip != null) {
                    boolean active = Integer.parseInt(chip.getText().toString()) == service.getBpm();
                    if (animated) {
                        animateChip(chip, active);
                    } else {
                        if (active) {
                            chip.setChipStrokeColorResource(R.color.bookmark_active);
                            chip.setChipStrokeWidthResource(R.dimen.chip_stroke_width_active);
                        } else {
                            chip.setChipStrokeColorResource(R.color.stroke_primary);
                            chip.setChipStrokeWidthResource(R.dimen.chip_stroke_width);
                        }
                    }
                }
            }
        }
    }

    private void animateChip(Chip chip, boolean active) {
        int colorFrom = Objects.requireNonNull(chip.getChipStrokeColor()).getDefaultColor();
        int colorTo = ContextCompat.getColor(
                this,
                active ? R.color.bookmark_active : R.color.stroke_primary
        );
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(300);
        colorAnimation.addUpdateListener(
                animator -> chip.setChipStrokeColor(
                        new ColorStateList(
                                new int[][]{
                                        new int[]{android.R.attr.state_enabled}
                                },
                                new int[] {
                                        (int) animator.getAnimatedValue()
                                }
                        ))
        );
        colorAnimation.start();
        float widthFrom = chip.getChipStrokeWidth();
        float widthTo = getResources().getDimension(active ? R.dimen.chip_stroke_width_active : R.dimen.chip_stroke_width);
        ValueAnimator widthAnimation = ValueAnimator.ofObject(new FloatEvaluator(), widthFrom, widthTo);
        widthAnimation.setDuration(300);
        widthAnimation.addUpdateListener(
                animator -> chip.setChipStrokeWidth(
                        (float) animator.getAnimatedValue()
                )
        );
        widthAnimation.start();
    }

    private void changeBpm(int change) {
        if (isBound()) {
            setBpm(service.getBpm() + change);
            vibratorUtil.vibrate(VibratorUtil.TAP);
            /*if (hapticFeedback && (!isPlaying || (!isBeatModeVibrate && !vibrateAlways))) {
                vibratorUtil.vibrate(VibratorUtil.TAP);
            }*/
        }
    }

    private void setBpm(int bpm) {
        if (isBound() && bpm > 0) {
            service.setBpm(Math.min(bpm, 300));
            refreshBookmark(true);
            binding.textMainBpm.setText(String.valueOf(service.getBpm()));
            if (service.getBpm() > 1) {
                if (!binding.frameMainLess.isEnabled()) {
                    binding.frameMainLess.animate().alpha(1).setDuration(300).start();
                }
                binding.frameMainLess.setEnabled(true);
            } else {
                if (binding.frameMainLess.isEnabled()) {
                    binding.frameMainLess.animate().alpha(0.5f).setDuration(300).start();
                }
                binding.frameMainLess.setEnabled(false);
            }
            if (service.getBpm() < 300) {
                if (!binding.frameMainMore.isEnabled()) {
                    binding.frameMainMore.animate().alpha(1).setDuration(300).start();
                }
                binding.frameMainMore.setEnabled(true);
            } else {
                if (binding.frameMainMore.isEnabled()) {
                    binding.frameMainMore.animate().alpha(0.5f).setDuration(300).start();
                }
                binding.frameMainMore.setEnabled(false);
            }
        }
    }

    private void setButtonStates() {
        if (isBound()) {
            int bpm = service.getBpm();
            binding.frameMainLess.setEnabled(bpm > 1);
            binding.frameMainLess.setAlpha(bpm > 1 ? 1 : 0.5f);
            binding.frameMainMore.setEnabled(bpm < 300);
            binding.frameMainMore.setAlpha(bpm < 300 ? 1 : 0.5f);
        }
    }

    private boolean isBound() {
        return isBound && service != null;
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
