package xyz.zedler.patrick.tack;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.input.RotaryEncoder;
import android.support.wearable.input.WearableButtons;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.IdRes;
import androidx.preference.PreferenceManager;
import androidx.wear.widget.SwipeDismissFrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xyz.zedler.patrick.tack.util.Constants;
import xyz.zedler.patrick.tack.view.BpmPickerView;

public class MainActivity extends WearableActivity
        implements View.OnClickListener, Runnable, View.OnTouchListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static boolean DEBUG = false;

    private SharedPreferences sharedPrefs;
    private Vibrator vibrator;
    private int bpm, emphasis, emphasisIndex, rotaryFactorIndex = 0, rotatedPrev = 0;
    private long lastClick = 0, prevTouchTime = 0, interval;
    private ImageView imageViewSettings, imageViewPlayPause, imageViewTempoTap;
    private ImageView imageViewBeatMode, imageViewEmphasis, imageViewBookmark;
    private Drawable drawableFabBg;
    private SwipeDismissFrameLayout swipeDismissFrameLayout;
    private BpmPickerView bpmPickerView;
    private TextView textViewBpm, textViewLabel, textViewEmphasis;
    private FrameLayout frameLayoutPlayPause;
    private SoundPool soundPool;
    private Handler handler;
    private int soundId = -1;
    private float degreeStorage = 0;
    private boolean animations, vibrateAlways, wristGestures, hidePicker, isTouchStartedInRing;
    private boolean isFirstRotation, isFirstButtonPress, isPlaying = false;
    private double currAngle = 0, prevAngle, ringWidth, edgeWidth;
    private final List<Long> intervals = new ArrayList<>();

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        hidePicker = sharedPrefs.getBoolean(Constants.PREF.HIDE_PICKER, false);
        isFirstRotation = sharedPrefs.getBoolean(Constants.PREF.FIRST_ROTATION, true);
        isFirstButtonPress = sharedPrefs.getBoolean(Constants.PREF.FIRST_PRESS, true);
        interval = sharedPrefs.getLong(Constants.PREF.INTERVAL, 500);
        bpm = toBpm(interval);

        setContentView(hidePicker ? R.layout.activity_main : R.layout.activity_main_picker);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                .build();

        handler = new Handler(Looper.getMainLooper());

        ringWidth = (float) getResources().getDimensionPixelSize(R.dimen.dotted_ring_width);
        edgeWidth = (float) getResources().getDimensionPixelSize(R.dimen.edge_width);
        prevAngle = 0;

        initViews();

        if(sharedPrefs.getBoolean(Constants.PREF.FIRST_START, true)) {
            startActivity(new Intent(this, WelcomeActivity.class));
            sharedPrefs.edit().putBoolean(Constants.PREF.FIRST_START, false).apply();
        }
    }

    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    private void initViews() {
        textViewBpm = findViewById(R.id.text_bpm);
        textViewBpm.setText(String.valueOf(bpm));
        //textViewBpm.setText(String.format(Locale.getDefault(), "%1$d", bpm));

        imageViewPlayPause = findViewById(R.id.image_play_pause);
        imageViewPlayPause.setImageResource(R.drawable.ic_round_play_arrow);

        textViewLabel = findViewById(R.id.text_label);
        imageViewSettings = findViewById(R.id.image_settings);
        imageViewTempoTap = findViewById(R.id.image_tempo_tap);
        imageViewBookmark = findViewById(R.id.image_bookmark);
        frameLayoutPlayPause = findViewById(R.id.frame_play_pause);
        drawableFabBg = frameLayoutPlayPause.getBackground();
        imageViewEmphasis = findViewById(R.id.image_emphasis);
        imageViewBeatMode = findViewById(R.id.image_beat_mode); // resource is set in onResume()

        textViewEmphasis = findViewById(R.id.text_emphasis);
        textViewEmphasis.setText(String.valueOf(
                sharedPrefs.getInt(Constants.PREF.EMPHASIS, 0))
        );

        swipeDismissFrameLayout = findViewById(R.id.swipe_dismiss);
        swipeDismissFrameLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                layout.setVisibility(View.GONE);
                finish();
            }
        });
        swipeDismissFrameLayout.setSwipeable(true);

        setOnClickListeners(
                R.id.frame_settings,
                R.id.frame_tempo_tap,
                R.id.frame_play_pause,
                R.id.frame_beat_mode,
                R.id.frame_emphasis,
                R.id.frame_bookmark
        );

        bpmPickerView = findViewById(R.id.bpm_picker);
        bpmPickerView.setOnTouchListener(hidePicker ? null : this);
        bpmPickerView.setDotsVisible(!hidePicker);
        bpmPickerView.setOnGenericMotionListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_SCROLL
                    && RotaryEncoder.isFromRotaryEncoder(ev)
            ) {
                @SuppressWarnings("deprecation")
                float delta = -RotaryEncoder.getRotaryAxisValue(ev)
                        * (RotaryEncoder.getScaledScrollFactor(this) / 5);
                v.setRotation(v.getRotation() + delta);
                int rotated = -RotaryEncoder.getRotaryAxisValue(ev) > 0 ? 1 : -1;

                if(rotated != rotatedPrev) {
                    changeBpm(rotated);
                    rotatedPrev = rotated;
                } else {
                    if(rotaryFactorIndex == 0) {
                        changeBpm(rotated);
                        rotaryFactorIndex++;
                    } else {
                        if(rotaryFactorIndex < 5) {
                            rotaryFactorIndex++;
                        } else {
                            rotaryFactorIndex = 0;
                        }
                    }
                }
                if(isFirstRotation && !hidePicker) {
                    isFirstRotation = false;
                    Toast.makeText(
                            this, R.string.msg_hide_picker, Toast.LENGTH_LONG
                    ).show();
                    sharedPrefs.edit().putBoolean(
                            Constants.PREF.FIRST_ROTATION, isFirstRotation
                    ).apply();
                }
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(hidePicker != sharedPrefs.getBoolean(Constants.PREF.HIDE_PICKER, false)) {
            recreate();
        }

        emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, 0);
        vibrateAlways = sharedPrefs.getBoolean(Constants.PREF.VIBRATE_ALWAYS, false);
        wristGestures = sharedPrefs.getBoolean(Constants.PREF.WRIST_GESTURES, true);
        animations = sharedPrefs.getBoolean(Constants.PREF.ANIMATIONS, true);

        if(sharedPrefs.getBoolean(Constants.PREF.BEAT_MODE_VIBRATE, true)) {
            imageViewBeatMode.setImageResource(
                    vibrateAlways
                            ? R.drawable.ic_round_volume_off_to_volume_on_anim
                            : R.drawable.ic_round_vibrate_to_volume_anim
            );
            soundId = -1;
        } else {
            imageViewBeatMode.setImageResource(
                    vibrateAlways
                            ? R.drawable.ic_round_volume_on_to_volume_off_anim
                            : R.drawable.ic_round_volume_to_vibrate_anim
            );
            soundId = soundPool.load(this, getSoundId(), 1);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                if(wristGestures) {
                    changeBpm(1);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                if(wristGestures) {
                    changeBpm(-1);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_STEM_1:
                if(WearableButtons.getButtonCount(this) >= 2) {
                    onButtonPress();
                    changeBpm(1);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_STEM_2:
                if(WearableButtons.getButtonCount(this) >= 2) {
                    onButtonPress();
                    changeBpm(-1);
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void onButtonPress() {
        if(isFirstButtonPress) {
            isFirstButtonPress = false;
            Toast.makeText(this, R.string.msg_long_press, Toast.LENGTH_LONG).show();
            sharedPrefs.edit().putBoolean(
                    Constants.PREF.FIRST_PRESS, isFirstButtonPress
            ).apply();
        }
    }

    private void setNextEmphasis() {
        int emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, 0);
        int emphasisNew;
        if(emphasis < 6) {
            emphasisNew = emphasis + 1;
        } else {
            emphasisNew = 0;
        }
        this.emphasis = emphasisNew;
        sharedPrefs.edit().putInt(Constants.PREF.EMPHASIS, emphasisNew).apply();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> textViewEmphasis.setText(String.valueOf(emphasisNew)),
                animations ? 150 : 0
        );
    }

    @SuppressLint({"ClickableViewAccessibility", "RestrictedApi"})
    @Override
    public boolean onTouch(final View v, MotionEvent event) {
        if(v.getId() == R.id.bpm_picker) {
            final float xc = (float) bpmPickerView.getWidth() / 2;
            final float yc = (float) bpmPickerView.getHeight() / 2;
            final float x = event.getX();
            final float y = event.getY();
            boolean isTouchInsideRing = isTouchInsideRing(event.getX(), event.getY());

            double angle = Math.toDegrees(Math.atan2(x - xc, yc - y));
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                isTouchStartedInRing = isTouchInsideRing;
                swipeDismissFrameLayout.setSwipeable(
                        !(isTouchInsideRing && !isTouchEdge(event.getX()))
                );
                currAngle = angle;
                bpmPickerView.setTouched(true, animations);
            } else if(event.getAction() == MotionEvent.ACTION_MOVE) {
                if(isTouchInsideRing || isTouchStartedInRing) {
                    prevAngle = currAngle;
                    currAngle = angle;
                    animate(prevAngle, currAngle);
                }
            } else if(event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL
            ) {
                prevAngle = currAngle = 0;
                swipeDismissFrameLayout.setSwipeable(true);
                bpmPickerView.setTouched(false, animations);
            }
            return true;
        } return false;
    }

    private boolean isTouchInsideRing(float x, float y) {
        float radius = (Math.min(
                bpmPickerView.getWidth(), bpmPickerView.getHeight()
        ) / 2f) - (float) ringWidth;
        double centerX = bpmPickerView.getPivotX();
        double centerY = bpmPickerView.getPivotY();
        double distanceX = x - centerX;
        double distanceY = y - centerY;
        return !((distanceX * distanceX) + (distanceY * distanceY) <= radius * radius);
    }

    private boolean isTouchEdge(float x) {
        return x <= edgeWidth;
    }

    private void animate(double fromDegrees, double toDegrees) {
        final RotateAnimation rotate = new RotateAnimation((float) fromDegrees, (float) toDegrees,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(0);
        rotate.setFillEnabled(true);
        rotate.setFillAfter(true);
        bpmPickerView.startAnimation(rotate);
        float degreeDiff = (float) toDegrees - (float) fromDegrees;
        degreeStorage = degreeStorage + degreeDiff;
        if(degreeStorage > 12) {
            changeBpm(1);
            degreeStorage = 0;
        } else if(degreeStorage < -12) {
            changeBpm(-1);
            degreeStorage = 0;
        }
    }

    @Override
    public void run() {
        if (isPlaying) {
            handler.postDelayed(this, interval);

            boolean isEmphasis = emphasis != 0 && emphasisIndex == 0;
            if(emphasis != 0) {
                if(emphasisIndex < emphasis - 1) {
                    emphasisIndex++;
                } else emphasisIndex = 0;
            }

            if (soundId != -1) {
                soundPool.play(
                        soundId, 1, 1,
                        0, 0, isEmphasis ? 1.5f : 1
                );
                if(vibrateAlways) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(VibrationEffect.createOneShot(
                                isEmphasis ? 50 : 20,
                                VibrationEffect.DEFAULT_AMPLITUDE)
                        );
                    } else {
                        vibrator.vibrate(isEmphasis ? 50 : 20);
                    }
                }
            } else if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        isEmphasis ? 50 : 20,
                        VibrationEffect.DEFAULT_AMPLITUDE)
                );
            } else {
                vibrator.vibrate(isEmphasis ? 50 : 20);
            }
        }
    }

    private int getSoundId() {
        String sound = sharedPrefs.getString(Constants.PREF.SOUND, Constants.SOUND.WOOD);
        assert sound != null;
        switch (sound) {
            case Constants.SOUND.CLICK:
                return R.raw.click;
            case Constants.SOUND.DING:
                return R.raw.ding;
            case Constants.SOUND.BEEP:
                return R.raw.beep;
            default:
                return R.raw.wood;
        }
    }

    private void changeBpm(int change) {
        int bpmNew = bpm + change;
        if((change > 0 && bpmNew <= 300) || (change < 0 && bpmNew >= 1)) {
            setBpm(bpmNew);
        }
    }

    private void setOnClickListeners(@IdRes int... viewIds) {
        for (int viewId : viewIds) {
            findViewById(viewId).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.frame_settings) {
            if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
            lastClick = SystemClock.elapsedRealtime();
            if (animations) startAnimatedIcon(imageViewSettings);

            if (isPlaying) {
                isPlaying = false;
                keepScreenOn(false);
                imageViewPlayPause.setImageResource(R.drawable.ic_round_play_arrow);
            }
            handler.removeCallbacks(this);

            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.frame_tempo_tap) {
            if (animations) startAnimatedIcon(imageViewTempoTap);

            long interval = System.currentTimeMillis() - prevTouchTime;
            if (prevTouchTime > 0 && interval <= 6000) {
                if (intervals.size() >= 5) {
                    intervals.remove(0);
                }
                intervals.add(System.currentTimeMillis() - prevTouchTime);
                if (intervals.size() > 1) {
                    setBpm(toBpm(getIntervalAverage()));
                }
            }
            prevTouchTime = System.currentTimeMillis();
        } else if (id == R.id.frame_play_pause) {
            emphasisIndex = 0;
            isPlaying = !isPlaying;
            if (isPlaying) {
                handler.post(MainActivity.this);
            } else {
                handler.removeCallbacks(MainActivity.this);
            }
            keepScreenOn(isPlaying);
            if (animations) {
                imageViewPlayPause.setImageResource(
                        isPlaying
                                ? R.drawable.ic_round_play_to_pause_anim
                                : R.drawable.ic_round_pause_to_play_anim
                );
                startAnimatedIcon(imageViewPlayPause);
            } else {
                imageViewPlayPause.setImageResource(
                        isPlaying
                                ? R.drawable.ic_round_pause
                                : R.drawable.ic_round_play_arrow
                );
            }
        } else if (id == R.id.frame_beat_mode) {
            boolean beatModeVibrateNew = !sharedPrefs.getBoolean(
                    Constants.PREF.BEAT_MODE_VIBRATE, true
            );
            sharedPrefs.edit().putBoolean(
                    Constants.PREF.BEAT_MODE_VIBRATE, beatModeVibrateNew
            ).apply();
            if (!beatModeVibrateNew) {
                soundId = soundPool.load(this, getSoundId(), 1);
            } else soundId = -1;
            if (animations) startAnimatedIcon(imageViewBeatMode);
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
            }, animations ? 300 : 0);
        } else if (id == R.id.frame_emphasis) {
            if (animations) startAnimatedIcon(imageViewEmphasis);
            setNextEmphasis();
        } else if (id == R.id.frame_bookmark) {
            if (animations) startAnimatedIcon(imageViewBookmark);
            int bookmark = sharedPrefs.getInt(Constants.PREF.BOOKMARK, -1);
            if (bookmark == -1) {
                Toast.makeText(
                        this, R.string.msg_bookmark, Toast.LENGTH_LONG
                ).show();
                bookmark = bpm;
            }
            sharedPrefs.edit().putInt(Constants.PREF.BOOKMARK, bpm).apply();
            int finalBookmark = bookmark;
            textViewBpm.animate().alpha(0).setDuration(150).start();
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> {
                        setBpm(finalBookmark);
                        textViewBpm.animate()
                                .alpha(isPlaying ? 0.35f : 1)
                                .setDuration(150)
                                .start();
                    },
                    animations ? 150 : 0
            );
        }
    }

    private void setBpm(int bpm) {
        if(bpm > 0) {
            this.bpm = Math.min(bpm, 300);
            textViewBpm.setText(String.valueOf(this.bpm));
            interval = toInterval(this.bpm);
            sharedPrefs.edit().putLong(Constants.PREF.INTERVAL, interval).apply();
        }
    }

    private long getIntervalAverage() {
        long sum = 0L;
        if(!intervals.isEmpty()) {
            for (long interval : intervals) {
                sum += interval;
            }
            return (long) ((double) sum / intervals.size());
        }
        return sum;
    }

    private void keepScreenOn(boolean keepOn) {
        float iconAlpha = 0.5f;
        if(keepOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if(animations) {
                animateActionButtonTint(R.color.retro_dark);
                imageViewPlayPause.animate().alpha(0.5f).setDuration(300).start();
                textViewBpm.animate().alpha(0.35f).setDuration(300).start();
                animateViewsAlpha(
                        iconAlpha,
                        textViewLabel,
                        imageViewBeatMode,
                        imageViewTempoTap,
                        imageViewSettings,
                        imageViewEmphasis,
                        textViewEmphasis,
                        bpmPickerView,
                        imageViewBookmark
                );
            } else {
                drawableFabBg.setTint(getColor(R.color.retro_dark));
                imageViewPlayPause.setAlpha(0.5f);
                textViewBpm.setAlpha(0.35f);
                setViewsAlpha(
                        iconAlpha,
                        textViewLabel,
                        imageViewBeatMode,
                        imageViewTempoTap,
                        imageViewSettings,
                        imageViewEmphasis,
                        textViewEmphasis,
                        bpmPickerView,
                        imageViewBookmark
                );
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if(animations) {
                animateActionButtonTint(R.color.secondary);
                animateViewsAlpha(
                        1,
                        textViewBpm,
                        textViewLabel,
                        imageViewPlayPause,
                        imageViewBeatMode,
                        imageViewTempoTap,
                        imageViewSettings,
                        imageViewEmphasis,
                        textViewEmphasis,
                        bpmPickerView,
                        imageViewBookmark
                );
            } else {
                drawableFabBg.setTint(getColor(R.color.secondary));
                setViewsAlpha(
                        1,
                        textViewBpm,
                        textViewLabel,
                        imageViewPlayPause,
                        imageViewBeatMode,
                        imageViewTempoTap,
                        imageViewSettings,
                        imageViewEmphasis,
                        textViewEmphasis,
                        bpmPickerView,
                        imageViewBookmark
                );
            }
        }
    }

    private void animateActionButtonTint(@ColorRes int color) {
        int colorFrom = Objects.requireNonNull(
                frameLayoutPlayPause.getBackgroundTintList()
        ).getDefaultColor();
        int colorTo = getColor(color);
        ValueAnimator colorAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(), colorFrom, colorTo
        );
        colorAnimation.setDuration(300);
        colorAnimation.addUpdateListener(
                animator -> drawableFabBg.setTint((int) animator.getAnimatedValue())
        );
        colorAnimation.start();
    }

    private void animateViewsAlpha(float alpha, View... views) {
        for (View view : views) {
            view.animate().alpha(alpha).setDuration(300).start();
        }
    }

    private void setViewsAlpha(float alpha, View... views) {
        for (View view : views) {
            view.setAlpha(alpha);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(this);
    }

    private static int toBpm(long interval) {
        return (int) (60000 / interval);
    }

    private static long toInterval(int bpm) {
        return (long) 60000 / bpm;
    }

    private void startAnimatedIcon(View view) {
        try {
            ((Animatable) ((ImageView) view).getDrawable()).start();
        } catch (ClassCastException e) {
            if(DEBUG) Log.e(TAG, "startAnimatedIcon() requires AVD!");
        }
    }
}