package xyz.zedler.patrick.tack.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.wearable.input.WearableButtons;
import android.util.Log;
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

import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.ActivityMainNewBinding;
import xyz.zedler.patrick.tack.util.AudioUtil;
import xyz.zedler.patrick.tack.util.Constants;
import xyz.zedler.patrick.tack.util.VibratorUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.BpmPickerView;

public class MainActivity extends FragmentActivity
        implements View.OnClickListener, Runnable, AmbientModeSupport.AmbientCallbackProvider {

    private final static String TAG = MainActivity.class.getSimpleName();

    private ActivityMainNewBinding binding;
    private SharedPreferences sharedPrefs;
    private AudioUtil audioUtil;
    private List<Long> intervals;
    private Handler handler;
    private int bpm;
    private int soundId = -1;
    private int emphasis;
    private int emphasisIndex;
    private int rotaryFactorIndex;
    private int rotatedPrev;
    private long lastClick;
    private long prevTouchTime;
    private long interval;
    private boolean animations;
    private boolean isBeatModeVibrate;
    private boolean vibrateAlways;
    private boolean wristGestures;
    private boolean hidePicker;
    private boolean isFirstRotation;
    private boolean isFirstButtonPress;
    private boolean isPlaying = false;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainNewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        isFirstRotation = sharedPrefs.getBoolean(
                Constants.PREF.FIRST_ROTATION, Constants.DEF.FIRST_ROTATION
        );
        isFirstButtonPress = sharedPrefs.getBoolean(
                Constants.PREF.FIRST_PRESS, Constants.DEF.FIRST_PRESS);
        interval = sharedPrefs.getLong(Constants.PREF.INTERVAL, Constants.DEF.INTERVAL
        );
        bpm = (int) (60000 / interval);

        audioUtil = new AudioUtil(this);

        isBeatModeVibrate = sharedPrefs.getBoolean(
                Constants.PREF.BEAT_MODE_VIBRATE, Constants.DEF.BEAT_MODE_VIBRATE
        );
        vibrateAlways = sharedPrefs.getBoolean(
                Constants.SETTING.VIBRATE_ALWAYS, Constants.DEF.VIBRATE_ALWAYS
        );
        hidePicker = sharedPrefs.getBoolean(
                Constants.SETTING.HIDE_PICKER, Constants.DEF.HIDE_PICKER
        );
        updatePickerVisibility();
        updateBeatMode();

        handler = new Handler(Looper.getMainLooper());
        intervals = new ArrayList<>();

        // VIEWS

        binding.textBpm.setText(String.valueOf(bpm));

        binding.imagePlayPause.setImageResource(R.drawable.ic_round_play_arrow);

        binding.textEmphasis.setText(String.valueOf(
                sharedPrefs.getInt(Constants.PREF.EMPHASIS, Constants.DEF.EMPHASIS))
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

        // ONBOARDING

        if (sharedPrefs.getBoolean(Constants.PREF.FIRST_START, Constants.DEF.FIRST_START)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            sharedPrefs.edit().putBoolean(Constants.PREF.FIRST_START, false).apply();
        }

        // AMBIENT MODE

        AmbientModeSupport.attach(this).setAmbientOffloadEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        handler.removeCallbacks(this);
        audioUtil.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, Constants.DEF.EMPHASIS);
        wristGestures = sharedPrefs.getBoolean(
                Constants.SETTING.WRIST_GESTURES, Constants.DEF.WRIST_GESTURES
        );
        animations = sharedPrefs.getBoolean(
                Constants.SETTING.ANIMATIONS, Constants.DEF.ANIMATIONS
        );
        vibrateAlways = sharedPrefs.getBoolean(
                Constants.SETTING.VIBRATE_ALWAYS, Constants.DEF.VIBRATE_ALWAYS
        );

        boolean hidePickerNew = sharedPrefs.getBoolean(
                Constants.SETTING.HIDE_PICKER, Constants.DEF.HIDE_PICKER
        );
        if(hidePicker != hidePickerNew) {
            hidePicker = hidePickerNew;
            updatePickerVisibility();
        }

        boolean isBeatModeVibrateNew = sharedPrefs.getBoolean(
                Constants.PREF.BEAT_MODE_VIBRATE, Constants.DEF.BEAT_MODE_VIBRATE
        );
        if(isBeatModeVibrate != isBeatModeVibrateNew) {
            isBeatModeVibrate = isBeatModeVibrateNew;
            updateBeatMode();
        }
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
                ViewUtil.setTextSize(binding.textBpm, R.dimen.text_size_bpm_ambient);
                ViewUtil.setTextSize(binding.textLabel, R.dimen.text_size_label_ambient);
                ViewUtil.setAlpha(0.5f, binding.textBpm, binding.textLabel);
                ViewUtil.setMarginBottom(binding.textBpm, R.dimen.text_bpm_margin_bottom_ambient);
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
                binding.bpmPicker.setDotsVisible(!hidePicker);
                ViewUtil.setTextSize(
                        binding.textBpm,
                        hidePicker ? R.dimen.text_size_bpm : R.dimen.text_size_bpm_picker
                );
                ViewUtil.setTextSize(
                        binding.textLabel,
                        hidePicker ? R.dimen.text_size_label : R.dimen.text_size_label_picker
                );
                ViewUtil.setAlpha(1, binding.textBpm, binding.textLabel);
                ViewUtil.setMarginBottom(
                        binding.textBpm,
                        hidePicker
                                ? R.dimen.text_bpm_margin_bottom
                                : R.dimen.text_bpm_margin_bottom_picker
                );
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

    @Override
    public void run() {
        if (!isPlaying) return;

        handler.postDelayed(this, interval);

        boolean isEmphasis = emphasis != 0 && emphasisIndex == 0;
        if (emphasis != 0) emphasisIndex = emphasisIndex < emphasis - 1 ? emphasisIndex + 1 : 0;

        if (soundId != -1) {
            audioUtil.play(soundId, isEmphasis);
            if (vibrateAlways) VibratorUtil.vibrate(this, isEmphasis);
        } else {
            VibratorUtil.vibrate(this, isEmphasis);
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
                    setBpm((int) (60000 / getIntervalAverage()));
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
            SharedPreferences.Editor editor = sharedPrefs.edit();
            if (!audioUtil.isSpeakerAvailable() && sharedPrefs.getBoolean(
                    Constants.PREF.FIRST_SPEAKER_MODE, Constants.DEF.FIRST_SPEAKER_MODE
            )) {
                Toast.makeText(this, R.string.msg_no_speaker, Toast.LENGTH_LONG).show();
                editor.putBoolean(Constants.PREF.FIRST_SPEAKER_MODE, false).apply();
            }
            isBeatModeVibrate = !isBeatModeVibrate;
            editor.putBoolean(
                    Constants.PREF.BEAT_MODE_VIBRATE, isBeatModeVibrate
            ).apply();
            if (animations) ViewUtil.startAnimatedIcon(binding.imageBeatMode);
            new Handler(Looper.getMainLooper()).postDelayed(
                    this::updateBeatMode, animations ? 300 : 0
            );
        } else if (id == R.id.frame_emphasis) {
            if (animations) ViewUtil.startAnimatedIcon(binding.imageEmphasis);
            setNextEmphasis();
        } else if (id == R.id.frame_bookmark) {
            if (animations) ViewUtil.startAnimatedIcon(binding.imageBookmark);
            int bookmark = sharedPrefs.getInt(Constants.PREF.BOOKMARK, Constants.DEF.BOOKMARK);
            if (bookmark == -1) {
                Toast.makeText(
                        this, R.string.msg_bookmark, Toast.LENGTH_LONG
                ).show();
                bookmark = bpm;
            }
            sharedPrefs.edit().putInt(Constants.PREF.BOOKMARK, bpm).apply();
            int finalBookmark = bookmark;
            binding.textBpm.animate().alpha(0).withEndAction(() -> {
                setBpm(finalBookmark);
                binding.textBpm.animate()
                        .alpha(isPlaying ? 0.35f : 1)
                        .setDuration(animations ? 150 : 0)
                        .start();
            }).setDuration(animations ? 150 : 0).start();
        }
    }

    private void onButtonPress() {
        if (!isFirstButtonPress) return;
        isFirstButtonPress = false;
        Toast.makeText(this, R.string.msg_long_press, Toast.LENGTH_LONG).show();
        sharedPrefs.edit().putBoolean(Constants.PREF.FIRST_PRESS, isFirstButtonPress).apply();
    }

    private void setNextEmphasis() {
        int emphasis = sharedPrefs.getInt(Constants.PREF.EMPHASIS, Constants.DEF.EMPHASIS);
        int emphasisNew = emphasis < 6 ? emphasis + 1 : 0;
        this.emphasis = emphasisNew;
        sharedPrefs.edit().putInt(Constants.PREF.EMPHASIS, emphasisNew).apply();
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> binding.textEmphasis.setText(String.valueOf(emphasisNew)),
                animations ? 150 : 0
        );
    }

    private void updateBeatMode() {
        if (isBeatModeVibrate) {
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
            soundId = audioUtil.getCurrentSoundId();
        }
    }

    private void updatePickerVisibility() {
        binding.bpmPicker.setDotsVisible(!hidePicker);

        ViewUtil.setSize(
                hidePicker ? R.dimen.icon_size : R.dimen.icon_size_picker,
                binding.imageTempoTap,
                binding.imageEmphasis,
                binding.imageSettings,
                binding.imageBookmark,
                binding.imageBeatMode,
                binding.imagePlayPause
        );
        ViewUtil.setSize(
                hidePicker ? R.dimen.action_button_size : R.dimen.action_button_size_picker,
                binding.framePlayPause
        );

        binding.imageTempoTap.setImageResource(R.drawable.ic_round_tempo_tap_anim);
        binding.imageEmphasis.setImageResource(R.drawable.ic_round_emphasis_anim);
        binding.imageSettings.setImageResource(R.drawable.ic_round_settings_anim);
        binding.imageBookmark.setImageResource(R.drawable.ic_round_bookmark_anim);
        binding.imagePlayPause.setImageResource(
                isPlaying
                        ? R.drawable.ic_round_pause
                        : R.drawable.ic_round_play_arrow
        );

        ViewUtil.setTextSize(
                binding.textBpm, hidePicker ? R.dimen.text_size_bpm : R.dimen.text_size_bpm_picker
        );
        ViewUtil.setTextSize(
                binding.textLabel,
                hidePicker ? R.dimen.text_size_label : R.dimen.text_size_label_picker
        );

        ViewUtil.setMargin(
                binding.linearControlsContainer,
                hidePicker ? 0 : getResources().getDimensionPixelSize(R.dimen.picker_ring_width)
        );
        ViewUtil.setMarginTop(
                binding.frameSettings,
                hidePicker
                        ? R.dimen.settings_vertical_offset
                        : R.dimen.settings_vertical_offset_picker
        );
        ViewUtil.setMarginBottom(
                binding.textBpm,
                hidePicker ? R.dimen.text_bpm_margin_bottom : R.dimen.text_bpm_margin_bottom_picker
        );

        ViewUtil.setHorizontalMargins(
                binding.frameTempoTap,
                hidePicker
                        ? R.dimen.control_horizontal_offset
                        : R.dimen.control_horizontal_offset_picker,
                -1
        );
        ViewUtil.setHorizontalMargins(
                binding.frameBeatMode,
                -1,
                hidePicker
                        ? R.dimen.control_horizontal_offset
                        : R.dimen.control_horizontal_offset_picker
        );

        Log.i(TAG, "updatePickerVisibility: " + getResources().getConfiguration().screenHeightDp);
    }

    private void changeBpm(int change) {
        int bpmNew = bpm + change;
        if ((change > 0 && bpmNew <= 300) || (change < 0 && bpmNew >= 1)) {
            setBpm(bpmNew);
        }
    }

    private void setBpm(int bpm) {
        if (bpm <= 0) return;
        this.bpm = Math.min(bpm, 300);
        binding.textBpm.setText(String.valueOf(this.bpm));
        interval = 60000 / bpm;
        sharedPrefs.edit().putLong(Constants.PREF.INTERVAL, interval).apply();
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
                ViewUtil.animateAlpha(
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
                binding.framePlayPause.getBackground().setTint(getColor(R.color.retro_dark));
                binding.imagePlayPause.setAlpha(0.5f);
                binding.textBpm.setAlpha(0.35f);
                ViewUtil.setAlpha(
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
                ViewUtil.animateAlpha(
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
                binding.framePlayPause.getBackground().setTint(getColor(R.color.secondary));
                ViewUtil.setAlpha(
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
}