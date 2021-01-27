package xyz.zedler.patrick.tack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.wearable.input.WearableButtons;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.SwipeDismissFrameLayout;

import java.util.ArrayList;
import java.util.List;

import xyz.zedler.patrick.tack.databinding.ActivityMainBinding;
import xyz.zedler.patrick.tack.util.Constants;
import xyz.zedler.patrick.tack.util.VibratorUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.BpmPickerView;

public class MainActivity extends FragmentActivity
        implements View.OnClickListener, Runnable,
        AmbientModeSupport.AmbientCallbackProvider {

    private final static String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding binding;
    private SharedPreferences sharedPrefs;
    private int bpm, emphasis, emphasisIndex, rotaryFactorIndex = 0, rotatedPrev = 0;
    private long lastClick = 0, prevTouchTime = 0, interval;
    private Drawable drawableFabBg;
    private SoundPool soundPool;
    private Handler handler;
    private int soundId = -1;
    private boolean animations, vibrateAlways, wristGestures, hidePicker;
    private boolean isFirstRotation, isFirstButtonPress, isPlaying = false;
    private final List<Long> intervals = new ArrayList<>();

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        hidePicker = sharedPrefs.getBoolean(Constants.PREF.HIDE_PICKER, false);
        isFirstRotation = sharedPrefs.getBoolean(Constants.PREF.FIRST_ROTATION, true);
        isFirstButtonPress = sharedPrefs.getBoolean(Constants.PREF.FIRST_PRESS, true);
        interval = sharedPrefs.getLong(Constants.PREF.INTERVAL, 500);
        bpm = toBpm(interval);

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                ).build();

        handler = new Handler(Looper.getMainLooper());

        initViews();

        if (sharedPrefs.getBoolean(Constants.PREF.FIRST_START, true)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            sharedPrefs.edit().putBoolean(Constants.PREF.FIRST_START, false).apply();
        }

        AmbientModeSupport.attach(this).setAmbientOffloadEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        handler.removeCallbacks(this);
    }

    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    private void initViews() {
        binding.textBpm.setText(String.valueOf(bpm));
        //binding.textBpm.setText(String.format(Locale.getDefault(), "%1$d", bpm));

        binding.imagePlayPause.setImageResource(R.drawable.ic_round_play_arrow);

        drawableFabBg = binding.framePlayPause.getBackground();

        binding.textEmphasis.setText(String.valueOf(
                sharedPrefs.getInt(Constants.PREF.EMPHASIS, 0))
        );

        binding.swipeDismiss.addCallback(new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                layout.setVisibility(View.GONE);
                finish();
            }
        });
        binding.swipeDismiss.setSwipeable(true);

        ViewUtil.setOnClickListeners(
                this,
                binding.frameSettings,
                binding.frameTempoTap,
                binding.framePlayPause,
                binding.frameBeatMode,
                binding.frameEmphasis,
                binding.frameBookmark
        );

        binding.bpmPicker.setOnRotationListener(this::changeBpm);
        binding.bpmPicker.setOnPickListener(new BpmPickerView.OnPickListener() {
            @Override
            public void onPickDown(boolean canBeDismiss) {
                binding.swipeDismiss.setSwipeable(canBeDismiss);
                binding.bpmPicker.setTouched(true, animations);
            }

            @Override
            public void onPickUpOrCancel() {
                binding.swipeDismiss.setSwipeable(true);
                binding.bpmPicker.setTouched(false, animations);
            }
        });
        binding.bpmPicker.setOnRotaryInputListener(change -> {
            if (change != rotatedPrev) {
                // change immediately after direction change
                changeBpm(change);
                rotatedPrev = change;
            } else if (rotaryFactorIndex == 0) {
                // enough rotated for next value change
                changeBpm(change);
                rotaryFactorIndex++;
            } else {
                // more rotation needed for bpm to change again
                rotaryFactorIndex = rotaryFactorIndex < 5 ? rotaryFactorIndex + 1 : 0;
            }

            if (isFirstRotation && !hidePicker) {
                isFirstRotation = false;
                Toast.makeText(
                        this, R.string.msg_hide_picker, Toast.LENGTH_LONG
                ).show();
                sharedPrefs.edit().putBoolean(
                        Constants.PREF.FIRST_ROTATION, isFirstRotation
                ).apply();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, 0);
        hidePicker = sharedPrefs.getBoolean(Constants.PREF.HIDE_PICKER, false);
        vibrateAlways = sharedPrefs.getBoolean(Constants.PREF.VIBRATE_ALWAYS, false);
        wristGestures = sharedPrefs.getBoolean(Constants.PREF.WRIST_GESTURES, true);
        animations = sharedPrefs.getBoolean(Constants.PREF.ANIMATIONS, true);

        if (sharedPrefs.getBoolean(Constants.PREF.BEAT_MODE_VIBRATE, true)) {
            binding.imageBeatMode.setImageResource(
                    vibrateAlways
                            ? R.drawable.ic_round_volume_off_to_volume_on_anim
                            : R.drawable.ic_round_vibrate_to_volume_anim
            );
            soundId = -1;
        } else {
            binding.imageBeatMode.setImageResource(
                    vibrateAlways
                            ? R.drawable.ic_round_volume_on_to_volume_off_anim
                            : R.drawable.ic_round_volume_to_vibrate_anim
            );
            soundId = soundPool.load(this, getSoundId(), 1);
        }

        // LAYOUT

        binding.bpmPicker.setDotsVisible(!hidePicker);
        ViewUtil.setViewsSize(
                getResources().getDimensionPixelSize(
                        hidePicker ? R.dimen.icon_size_no_picker : R.dimen.icon_size
                ),
                binding.imageTempoTap,
                binding.imageEmphasis,
                binding.imageSettings,
                binding.imageBookmark,
                binding.imageBeatMode,
                binding.imagePlayPause
        );
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new AmbientModeSupport.AmbientCallback() {

            public void onEnterAmbient(Bundle ambientDetails) {
                ViewUtil.setVisibility(
                        View.GONE,
                        binding.imageTempoTap,
                        binding.imageEmphasis,
                        binding.imageSettings,
                        binding.imageBookmark,
                        binding.imageBeatMode,
                        binding.framePlayPause,
                        binding.textEmphasis,
                        binding.bpmPicker
                );
                binding.textBpm.setTextSize(40);
                binding.textLabel.setTextSize(15);
                binding.textBpm.setTextColor(getColor(R.color.on_background_secondary));
                binding.textLabel.setTextColor(getColor(R.color.on_background_secondary));
            }

            public void onExitAmbient() {
                ViewUtil.setVisibility(
                        View.VISIBLE,
                        binding.imageTempoTap,
                        binding.imageEmphasis,
                        binding.imageSettings,
                        binding.imageBookmark,
                        binding.imageBeatMode,
                        binding.framePlayPause,
                        binding.textEmphasis,
                        binding.bpmPicker
                );
                binding.bpmPicker.requestFocus();
                binding.textBpm.setTextSize(30);
                binding.textLabel.setTextSize(13);
                binding.textBpm.setTextColor(getColor(R.color.on_background));
                binding.textLabel.setTextColor(getColor(R.color.on_background_secondary));
            }
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_NAVIGATE_NEXT:
                if (wristGestures) {
                    changeBpm(1);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_NAVIGATE_PREVIOUS:
                if (wristGestures) {
                    changeBpm(-1);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_STEM_1:
                if (WearableButtons.getButtonCount(this) >= 2) {
                    onButtonPress();
                    changeBpm(1);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_STEM_2:
                if (WearableButtons.getButtonCount(this) >= 2) {
                    onButtonPress();
                    changeBpm(-1);
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void onButtonPress() {
        if (!isFirstButtonPress) return;
        isFirstButtonPress = false;
        Toast.makeText(this, R.string.msg_long_press, Toast.LENGTH_LONG).show();
        sharedPrefs.edit().putBoolean(Constants.PREF.FIRST_PRESS, isFirstButtonPress).apply();
    }

    private void setNextEmphasis() {
        int emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, 0);
        int emphasisNew = emphasis < 6 ? emphasis + 1 : 0;
        this.emphasis = emphasisNew;
        sharedPrefs.edit().putInt(Constants.PREF.EMPHASIS, emphasisNew).apply();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> binding.textEmphasis.setText(String.valueOf(emphasisNew)),
                animations ? 150 : 0
        );
    }

    @Override
    public void run() {
        if (isPlaying) {
            handler.postDelayed(this, interval);

            boolean isEmphasis = emphasis != 0 && emphasisIndex == 0;
            if (emphasis != 0) emphasisIndex = emphasisIndex < emphasis - 1 ? emphasisIndex + 1 : 0;

            if (soundId != -1) {
                soundPool.play(
                        soundId, 1, 1, 0, 0, isEmphasis ? 1.5f : 1
                );
                if (vibrateAlways) VibratorUtil.vibrate(this, isEmphasis);
            } else {
                VibratorUtil.vibrate(this, isEmphasis);
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
        if ((change > 0 && bpmNew <= 300) || (change < 0 && bpmNew >= 1)) {
            setBpm(bpmNew);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.frame_settings) {
            if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
            lastClick = SystemClock.elapsedRealtime();
            if (animations) ViewUtil.startAnimatedIcon(binding.imageSettings);

            if (isPlaying) {
                isPlaying = false;
                keepScreenOn(false);
                binding.imagePlayPause.setImageResource(R.drawable.ic_round_play_arrow);
            }
            handler.removeCallbacks(this);

            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.frame_tempo_tap) {
            if (animations) ViewUtil.startAnimatedIcon(binding.imageTempoTap);

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
                binding.imagePlayPause.setImageResource(
                        isPlaying
                                ? R.drawable.ic_round_play_to_pause_anim
                                : R.drawable.ic_round_pause_to_play_anim
                );
                ViewUtil.startAnimatedIcon(binding.imagePlayPause);
            } else {
                binding.imagePlayPause.setImageResource(
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
            if (animations) ViewUtil.startAnimatedIcon(binding.imageBeatMode);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (beatModeVibrateNew) {
                    binding.imageBeatMode.setImageResource(
                            vibrateAlways
                                    ? R.drawable.ic_round_volume_off_to_volume_on_anim
                                    : R.drawable.ic_round_vibrate_to_volume_anim
                    );
                } else {
                    binding.imageBeatMode.setImageResource(
                            vibrateAlways
                                    ? R.drawable.ic_round_volume_on_to_volume_off_anim
                                    : R.drawable.ic_round_volume_to_vibrate_anim
                    );
                }
            }, animations ? 300 : 0);
        } else if (id == R.id.frame_emphasis) {
            if (animations) ViewUtil.startAnimatedIcon(binding.imageEmphasis);
            setNextEmphasis();
        } else if (id == R.id.frame_bookmark) {
            if (animations) ViewUtil.startAnimatedIcon(binding.imageBookmark);
            int bookmark = sharedPrefs.getInt(Constants.PREF.BOOKMARK, -1);
            if (bookmark == -1) {
                Toast.makeText(
                        this, R.string.msg_bookmark, Toast.LENGTH_LONG
                ).show();
                bookmark = bpm;
            }
            sharedPrefs.edit().putInt(Constants.PREF.BOOKMARK, bpm).apply();
            int finalBookmark = bookmark;
            binding.textBpm.animate().alpha(0).setDuration(150).start();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                setBpm(finalBookmark);
                binding.textBpm.animate().alpha(isPlaying ? 0.35f : 1).setDuration(150).start();
            }, animations ? 150 : 0);
        }
    }

    private void setBpm(int bpm) {
        if (bpm > 0) {
            this.bpm = Math.min(bpm, 300);
            binding.textBpm.setText(String.valueOf(this.bpm));
            interval = toInterval(this.bpm);
            sharedPrefs.edit().putLong(Constants.PREF.INTERVAL, interval).apply();
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

    private void keepScreenOn(boolean keepOn) {
        float iconAlpha = 0.5f;
        if (keepOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (animations) {
                ViewUtil.animateBackgroundTint(binding.framePlayPause, R.color.retro_dark);
                binding.imagePlayPause.animate().alpha(0.5f).setDuration(300).start();
                binding.textBpm.animate().alpha(0.35f).setDuration(300).start();
                ViewUtil.animateViewsAlpha(
                        iconAlpha,
                        binding.textLabel,
                        binding.imageBeatMode,
                        binding.imageTempoTap,
                        binding.imageSettings,
                        binding.imageEmphasis,
                        binding.textEmphasis,
                        binding.bpmPicker,
                        binding.imageBookmark
                );
            } else {
                drawableFabBg.setTint(getColor(R.color.retro_dark));
                binding.imagePlayPause.setAlpha(0.5f);
                binding.textBpm.setAlpha(0.35f);
                ViewUtil.setViewsAlpha(
                        iconAlpha,
                        binding.textLabel,
                        binding.imageBeatMode,
                        binding.imageTempoTap,
                        binding.imageSettings,
                        binding.imageEmphasis,
                        binding.textEmphasis,
                        binding.bpmPicker,
                        binding.imageBookmark
                );
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (animations) {
                ViewUtil.animateBackgroundTint(binding.framePlayPause, R.color.secondary);
                ViewUtil.animateViewsAlpha(
                        1,
                        binding.textBpm,
                        binding.textLabel,
                        binding.imagePlayPause,
                        binding.imageBeatMode,
                        binding.imageTempoTap,
                        binding.imageSettings,
                        binding.imageEmphasis,
                        binding.textEmphasis,
                        binding.bpmPicker,
                        binding.imageBookmark
                );
            } else {
                drawableFabBg.setTint(getColor(R.color.secondary));
                ViewUtil.setViewsAlpha(
                        1,
                        binding.textBpm,
                        binding.textLabel,
                        binding.imagePlayPause,
                        binding.imageBeatMode,
                        binding.imageTempoTap,
                        binding.imageSettings,
                        binding.imageEmphasis,
                        binding.textEmphasis,
                        binding.bpmPicker,
                        binding.imageBookmark
                );
            }
        }
    }

    private static int toBpm(long interval) {
        return (int) (60000 / interval);
    }

    private static long toInterval(int bpm) {
        return (long) 60000 / bpm;
    }
}