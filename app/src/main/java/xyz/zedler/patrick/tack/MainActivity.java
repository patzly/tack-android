package xyz.zedler.patrick.tack;

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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.fragment.EmphasisBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.fragment.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.service.MetronomeService;
import xyz.zedler.patrick.tack.util.LogoUtil;
import xyz.zedler.patrick.tack.view.BpmPickerView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        View.OnTouchListener, ServiceConnection, MetronomeService.TickListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static boolean DEBUG = false;

    private SharedPreferences sharedPrefs;
    private TextView textViewBpm, textViewEmphasis;
    private FloatingActionButton fab;
    private ChipGroup chipGroup;
    private BpmPickerView bpmPickerView;
    private FrameLayout frameLayoutLess, frameLayoutMore, frameLayoutBookmark;
    private ImageView imageViewLess;
    private ImageView imageViewMore;
    private ImageView imageViewTempoTap;
    private ImageView imageViewBeatMode;
    private ImageView imageViewEmphasis;
    private ImageView imageViewBookmark;
    private double currAngle = 0, prevAngle;
    private boolean isTouchStartedInRing;
    private long lastClick = 0, prevTouchTime;
    private float degreeStorage = 0, ringWidth;
    private final static int ROTATE_THRESHOLD = 10;
    private final List<Long> intervals = new ArrayList<>();

    private boolean isBound;
    private MetronomeService service;
    private LogoUtil logoUtil;

    //private MetronomeView metronomeView;
    //private TicksView ticksView;

    private List<Integer> bookmarks;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        AppCompatDelegate.setDefaultNightMode(
                sharedPrefs.getBoolean("force_dark_mode",false)
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        setContentView(R.layout.activity_main);

        (new ScrollBehavior()).setUpScroll(
                this,
                R.id.app_bar_main,
                R.id.linear_app_bar_main,
                0,
                true
        );

        logoUtil = new LogoUtil(findViewById(R.id.image_main_logo));

        ((Toolbar) findViewById(R.id.toolbar_main)).setOnMenuItemClickListener((MenuItem item) -> {
            if (SystemClock.elapsedRealtime() - lastClick < 1000){
                return false;
            }
            lastClick = SystemClock.elapsedRealtime();
            int itemId = item.getItemId();
            if (itemId == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (itemId == R.id.action_about) {
                startActivity(new Intent(this, AboutActivity.class));
            } else if (itemId == R.id.action_share) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.msg_share));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, null));
            } else if (itemId == R.id.action_feedback) {
                new FeedbackBottomSheetDialogFragment().show(
                        getSupportFragmentManager(),
                        "feedback"
                );
            }
            return true;
        });

        fab = findViewById(R.id.fab);
        textViewBpm = findViewById(R.id.text_bpm);
        textViewEmphasis = findViewById(R.id.text_emphasis);
        textViewEmphasis.setText(String.valueOf(sharedPrefs.getInt("emphasis", 0)));
        chipGroup = findViewById(R.id.chip_group);
        bpmPickerView = findViewById(R.id.bpm_picker);
        bpmPickerView.setOnTouchListener(this);
        frameLayoutLess = findViewById(R.id.frame_less);
        frameLayoutLess.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;
            int nextRun = 500;

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

            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isBound()) {
                        if (service.getBpm() > 1) {
                            changeBpm(-1);
                            handler.postDelayed(this, nextRun);
                            nextRun = (int) (nextRun * 0.9);
                        } else {
                            handler.removeCallbacks(runnable);
                            handler = null;
                            nextRun = 500;
                        }
                    }
                }
            };

        });

        frameLayoutMore = findViewById(R.id.frame_more);
        frameLayoutMore.setOnTouchListener(new View.OnTouchListener() {
            private Handler handler;
            int nextRun = 500;

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

            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isBound()) {
                        if (service.getBpm() < 300) {
                            changeBpm(1);
                            handler.postDelayed(this, nextRun);
                            nextRun = (int) (nextRun * 0.9);
                        } else {
                            handler.removeCallbacks(runnable);
                            handler = null;
                            nextRun = 500;
                        }
                    }
                }
            };

        });

        imageViewLess = findViewById(R.id.image_less);
        imageViewMore = findViewById(R.id.image_more);
        imageViewTempoTap = findViewById(R.id.image_tempo_tap);
        frameLayoutBookmark = findViewById(R.id.frame_bookmark);
        imageViewBookmark = findViewById(R.id.image_bookmark);
        imageViewEmphasis = findViewById(R.id.image_emphasis);

        setOnClickListeners(
                R.id.frame_less,
                R.id.frame_more,
                R.id.frame_tempo_tap,
                R.id.fab,
                R.id.frame_beat_mode,
                R.id.frame_bookmark,
                R.id.frame_emphasis
        );

        imageViewBeatMode = findViewById(R.id.image_beat_mode);
        boolean vibrateAlways = sharedPrefs.getBoolean("vibrate_always", false);
        if (sharedPrefs.getBoolean("beat_mode_vibrate", true)) {
            imageViewBeatMode.setImageResource(
                    vibrateAlways
                            ? R.drawable.ic_round_volume_off_to_volume_on_anim
                            : R.drawable.ic_round_vibrate_to_volume_anim
            );
        } else {
            imageViewBeatMode.setImageResource(
                    vibrateAlways
                            ? R.drawable.ic_round_volume_on_to_volume_off_anim
                            : R.drawable.ic_round_volume_to_vibrate_anim
            );
        }

        ringWidth = getResources().getDimension(R.dimen.dotted_ring_width);
        prevAngle = 0;

        setButtonStates();

        if (isBound()) {
            //ticksView.setTick(service.getTick());
            //metronomeView.setInterval(service.getInterval());
            //seekBar.setProgress(service.getBpm());
            //fab.setImageResource(service.isPlaying() ? R.drawable.ic_round_pause_to_play_anim : R.drawable.ic_round_play_to_pause_anim);
            //emphasisLayout.removeAllViews();
            /*for (boolean isEmphasis : service.getEmphasisList()) {
                emphasisLayout.addView(getEmphasisSwitch(isEmphasis, false));
            }*/
        }

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
            chipGroup.addView(newChip(bookmarks.get(i)));
        }

        /*touchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound()) {
                    if (prevTouchTime > 0) {
                        long interval = System.currentTimeMillis() - prevTouchTime;
                        if (interval > 200) {
                            if (interval < 20000) {
                                if (prevTouchInterval == -1)
                                    prevTouchInterval = interval;
                                else prevTouchInterval = (prevTouchInterval + interval) / 2;
                            } else prevTouchInterval = -1;
                        }

                        seekBar.setProgress((int) (60000 / prevTouchInterval));
                    }

                    prevTouchTime = System.currentTimeMillis();
                }
            }
        });*/

        /*addEmphasisView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound()) {
                    if (service.getEmphasisList().size() < 50) {
                        emphasisLayout.addView(getEmphasisSwitch(false, true));

                        List<Boolean> emphasisList = service.getEmphasisList();
                        emphasisList.add(false);
                        service.setEmphasisList(emphasisList);
                    }
                }
            }
        });

        removeEmphasisView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBound()) {
                    if (service.getEmphasisList().size() > 2) {
                        List<Boolean> emphasisList = service.getEmphasisList();
                        int position = emphasisList.size() - 1;
                        emphasisList.remove(position);
                        service.setEmphasisList(emphasisList);

                        emphasisLayout.removeViewAt(position);
                    }
                }
            }
        });*/

        /*
        ticksView.setListener(this);
        subscribe();*/

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
                () -> textViewEmphasis.setText(String.valueOf(emphasisNew)),
                150
        );
        if (isBound) service.updateTick();
    }

    public void setEmphasis(int emphasis) {
        sharedPrefs.edit().putInt("emphasis", emphasis).apply();
        textViewEmphasis.setText(String.valueOf(emphasis));
        if (isBound) service.updateTick();
    }

    private static int toBpm(long interval) {
        return (int) (60000 / interval);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fab) {
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
        } else if (id == R.id.frame_less) {
            startAnimatedIcon(imageViewLess);
            changeBpm(-1);
        } else if (id == R.id.frame_more) {
            startAnimatedIcon(imageViewMore);
            changeBpm(1);
        } else if (id == R.id.frame_tempo_tap) {
            startAnimatedIcon(imageViewTempoTap);

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
        } else if (id == R.id.frame_beat_mode) {
            boolean beatModeVibrateNew = !sharedPrefs.getBoolean("beat_mode_vibrate", true);
            boolean vibrateAlways = sharedPrefs.getBoolean("vibrate_always", false);
            sharedPrefs.edit().putBoolean("beat_mode_vibrate", beatModeVibrateNew).apply();
            if (isBound()) service.updateTick();
            startAnimatedIcon(imageViewBeatMode);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (beatModeVibrateNew) {
                    imageViewBeatMode.setImageResource(
                            vibrateAlways
                                    ? R.drawable.ic_round_volume_off_to_volume_on_anim
                                    : R.drawable.ic_round_vibrate_to_volume_anim
                    );
                } else {
                    imageViewBeatMode.setImageResource(
                            vibrateAlways
                                    ? R.drawable.ic_round_volume_on_to_volume_off_anim
                                    : R.drawable.ic_round_volume_to_vibrate_anim
                    );
                }
            }, 300);
        } else if (id == R.id.frame_bookmark) {
            startAnimatedIcon(imageViewBookmark);
            if (isBound()) {
                if (bookmarks.size() < 3 && !bookmarks.contains(service.getBpm())) {
                    chipGroup.addView(newChip(service.getBpm()));
                    bookmarks.add(service.getBpm());
                    updateBookmarks();
                    refreshBookmark(true);
                } else if (bookmarks.size() >= 3) {
                    Snackbar.make(
                            findViewById(R.id.coordinator_container),
                            getString(R.string.msg_bookmarks_max),
                            Snackbar.LENGTH_LONG
                    ).setAnchorView(fab)
                            .setActionTextColor(ContextCompat.getColor(this, R.color.secondary))
                            .setAction(
                                    getString(R.string.action_clear_all),
                                    v1 -> {
                                        chipGroup.removeAllViews();
                                        bookmarks.clear();
                                        updateBookmarks();
                                        refreshBookmark(true);
                                    }
                            ).show();
                }
            }
        } else if (id == R.id.frame_emphasis) {
            startAnimatedIcon(imageViewEmphasis);
            if (sharedPrefs.getBoolean("emphasis_slider", false)) {
                new EmphasisBottomSheetDialogFragment().show(
                        getSupportFragmentManager(), "emphasis"
                );
            } else {
                setNextEmphasis();
            }
        }
    }

    private Chip newChip(int bpm) {
        Chip chip = new Chip(this);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setCloseIconVisible(true);
        chip.setCloseIconTintResource(R.color.icon);
        chip.setCloseIconResource(R.drawable.ic_round_cancel);
        chip.setOnCloseIconClickListener(v -> {
            chipGroup.removeView(chip);
            bookmarks.remove((Integer) bpm);
            updateBookmarks();
            refreshBookmark(true);
        });
        chip.setChipBackgroundColorResource(R.color.background);
        chip.setText(String.valueOf(bpm));
        chip.setTextAppearance(R.style.TextAppearance_Tack_Chip);
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
                if (!frameLayoutBookmark.isEnabled()) {
                    if (animated) {
                        frameLayoutBookmark.animate().alpha(1).setDuration(300).start();
                    } else {
                        frameLayoutBookmark.setAlpha(1);
                    }
                }
                frameLayoutBookmark.setEnabled(true);
            } else {
                if (frameLayoutBookmark.isEnabled()) {
                    if (animated) {
                        frameLayoutBookmark.animate().alpha(0.5f).setDuration(300).start();
                    } else {
                        frameLayoutBookmark.setAlpha(0.5f);
                    }
                }
                frameLayoutBookmark.setEnabled(false);
            }
            for(int i = 0; i < chipGroup.getChildCount(); i++) {
                Chip chip = (Chip) chipGroup.getChildAt(i);
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



    @SuppressLint({"ClickableViewAccessibility"})
    @Override
    public boolean onTouch(final View v, MotionEvent event) {
        if (v.getId() == R.id.bpm_picker) {
            final float xc = (float) bpmPickerView.getWidth() / 2;
            final float yc = (float) bpmPickerView.getHeight() / 2;
            final float x = event.getX();
            final float y = event.getY();
            boolean isTouchInsideRing = isTouchInsideRing(event.getX(), event.getY());

            double angle = Math.toDegrees(Math.atan2(x - xc, yc - y));
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isTouchStartedInRing = isTouchInsideRing;
                currAngle = angle;
                bpmPickerView.setTouched(true);
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (isTouchInsideRing || isTouchStartedInRing) {
                    prevAngle = currAngle;
                    currAngle = angle;
                    animateRotation(prevAngle, currAngle);
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL
            ) {
                prevAngle = currAngle = 0;
                bpmPickerView.setTouched(false);
            }
            return true;
        } return false;
    }

    private boolean isTouchInsideRing(float x, float y) {
        float radius = (Math.min(bpmPickerView.getWidth(), bpmPickerView.getHeight()) / 2f) - ringWidth;
        double centerX = bpmPickerView.getPivotX();
        double centerY = bpmPickerView.getPivotY();
        double distanceX = x - centerX;
        double distanceY = y - centerY;
        return !((distanceX * distanceX) + (distanceY * distanceY) <= radius * radius);
    }

    private void animateRotation(double fromDegrees, double toDegrees) {
        final RotateAnimation rotate = new RotateAnimation((float) fromDegrees, (float) toDegrees,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(0);
        rotate.setFillEnabled(true);
        rotate.setFillAfter(true);
        bpmPickerView.startAnimation(rotate);
        float degreeDiff = (float) toDegrees - (float) fromDegrees;
        degreeStorage = degreeStorage + degreeDiff;
        //Log.i(TAG, "animate: difference = " + degreeDiff + ", storage = " + degreeStorage);
        if (degreeStorage > ROTATE_THRESHOLD) {
            changeBpm(1);
            degreeStorage = 0;
        } else if (degreeStorage < ROTATE_THRESHOLD * -1) {
            changeBpm(-1);
            degreeStorage = 0;
        }
    }

    private void changeBpm(int change) {
        if (isBound()) {
            setBpm(service.getBpm() + change);
        }
    }

    private void setBpm(int bpm) {
        if (isBound() && bpm > 0) {
            service.setBpm(Math.min(bpm, 300));
            refreshBookmark(true);
            textViewBpm.setText(String.valueOf(service.getBpm()));
            if (service.getBpm() > 1) {
                if (!frameLayoutLess.isEnabled()) {
                    frameLayoutLess.animate().alpha(1).setDuration(300).start();
                }
                frameLayoutLess.setEnabled(true);
            } else {
                if (frameLayoutLess.isEnabled()) {
                    frameLayoutLess.animate().alpha(0.5f).setDuration(300).start();
                }
                frameLayoutLess.setEnabled(false);
            }
            if (service.getBpm() < 300) {
                if (!frameLayoutMore.isEnabled()) {
                    frameLayoutMore.animate().alpha(1).setDuration(300).start();
                }
                frameLayoutMore.setEnabled(true);
            } else {
                if (frameLayoutMore.isEnabled()) {
                    frameLayoutMore.animate().alpha(0.5f).setDuration(300).start();
                }
                frameLayoutMore.setEnabled(false);
            }
        }
    }

    private void setButtonStates() {
        if (isBound()) {
            int bpm = service.getBpm();
            frameLayoutLess.setEnabled(bpm > 1);
            frameLayoutLess.setAlpha(bpm > 1 ? 1 : 0.5f);
            frameLayoutMore.setEnabled(bpm < 300);
            frameLayoutMore.setAlpha(bpm < 300 ? 1 : 0.5f);
        }
    }

    private boolean isBound() {
        return isBound && service != null;
    }

    /*private EmphasisSwitch getEmphasisSwitch(boolean isChecked, boolean subscribe) {
        EmphasisSwitch emphasisSwitch = new EmphasisSwitch(this);
        emphasisSwitch.setChecked(isChecked);
        emphasisSwitch.setOnCheckedChangeListener(this);
        emphasisSwitch.setLayoutParams(new LinearLayout.LayoutParams(ConversionUtils.getPixelsFromDp(40), ConversionUtils.getPixelsFromDp(40)));

        if (subscribe)
            emphasisSwitch.subscribe();

        return emphasisSwitch;
    }*/

    @Override
    protected void onStart() {
        Intent intent = new Intent(this, MetronomeService.class);
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
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MetronomeService.LocalBinder binder = (MetronomeService.LocalBinder) iBinder;
        service = binder.getService();
        service.setTickListener(this);
        isBound = true;

        if (sharedPrefs.getBoolean("beat_mode_vibrate", true)) {
            imageViewBeatMode.setImageResource(
                    sharedPrefs.getBoolean("vibrate_always", false)
                            ? R.drawable.ic_round_volume_off_to_volume_on_anim
                            : R.drawable.ic_round_vibrate_to_volume_anim
            );
        } else {
            imageViewBeatMode.setImageResource(
                    sharedPrefs.getBoolean("vibrate_always", false)
                            ? R.drawable.ic_round_volume_on_to_volume_off_anim
                            : R.drawable.ic_round_volume_to_vibrate_anim
            );
        }

        service.updateTick();
        refreshBookmark(false);

        /*if (ticksView != null)
            ticksView.setTick(service.getTick());

        if (metronomeView != null)
            metronomeView.setInterval(service.getInterval());

        if (seekBar != null)
            seekBar.setProgress(service.getBpm());*/

        setBpm(service.getBpm());

        fab.setImageResource(
                service.isPlaying()
                        ? R.drawable.ic_round_pause
                        : R.drawable.ic_round_play_arrow
        );

        keepScreenAwake(service.isPlaying());

        /*if (playView != null)
            playView.setImageResource(service.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);*/

        /*if (emphasisLayout != null) {
            emphasisLayout.removeAllViews();
            for (boolean isEmphasis : service.getEmphasisList()) {
                emphasisLayout.addView(getEmphasisSwitch(isEmphasis, true));
            }
        }*/
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
        fab.setImageResource(R.drawable.ic_round_play_to_pause_anim);
        startAnimatedFabIcon();
        keepScreenAwake(true);
    }

    @Override
    public void onTick(long interval, boolean isEmphasis, int index) {
        logoUtil.nextBeat(interval);
        /*metronomeView.onTick(isEmphasis);

        for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
            ((EmphasisSwitch) emphasisLayout.getChildAt(i)).setAccented(i == index);
        }*/
    }

    @Override
    public void onBpmChanged(int bpm) {
        if (isBound()) {
            ((TextView) findViewById(R.id.text_bpm)).setText(String.valueOf(bpm));
            refreshBookmark(true);
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

    @Override
    public void onStopTicks() {
        fab.setImageResource(R.drawable.ic_round_pause_to_play_anim);
        startAnimatedFabIcon();
        keepScreenAwake(false);
        /*playView.setImageResource(R.drawable.ic_play);

        for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
            ((EmphasisSwitch) emphasisLayout.getChildAt(i)).setAccented(false);
        }*/
    }

    /*@Override
    public void onCheckedChanged(EmphasisSwitch emphasisSwitch, boolean b) {
        if (isBound()) {
            List<Boolean> emphasisList = new ArrayList<>();
            for (int i = 0; i < emphasisLayout.getChildCount(); i++) {
                emphasisList.add(((EmphasisSwitch) emphasisLayout.getChildAt(i)).isChecked());
            }

            service.setEmphasisList(emphasisList);
        }
    }*/

    /*@Override
    public void onProgressChange(int progress) {
        if (progress > 0 && isBound())
            service.setBpm(progress);
    }*/

    private void setOnClickListeners(@IdRes int... viewIds) {
        for (int viewId : viewIds) {
            findViewById(viewId).setOnClickListener(this);
        }
    }

    private void startAnimatedIcon(View view) {
        try {
            ((Animatable) ((ImageView) view).getDrawable()).start();
        } catch (ClassCastException e) {
            if (DEBUG) Log.e(TAG, "startAnimatedIcon() requires AVD!");
        }
    }

    private void startAnimatedFabIcon() {
        try {
            ((Animatable) fab.getDrawable()).start();
        } catch (ClassCastException e) {
            if (DEBUG) Log.e(TAG, "startAnimatedIcon() requires AVD!");
        }
    }
}
