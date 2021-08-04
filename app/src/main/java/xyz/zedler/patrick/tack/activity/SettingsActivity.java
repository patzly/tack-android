package xyz.zedler.patrick.tack.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.SETTINGS;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.ActivitySettingsAppBinding;
import xyz.zedler.patrick.tack.fragment.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.util.AudioUtil;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class SettingsActivity extends AppCompatActivity
    implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,
    RadioGroup.OnCheckedChangeListener {

  private ActivitySettingsAppBinding binding;
  private SharedPreferences sharedPrefs;
  private ViewUtil viewUtil;
  private AudioUtil audioUtil;
  private HapticUtil hapticUtil;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivitySettingsAppBinding.inflate(getLayoutInflater());
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    boolean forceDarkMode = sharedPrefs.getBoolean(
        SETTINGS.DARK_MODE, Constants.DEF.DARK_MODE
    );
    AppCompatDelegate.setDefaultNightMode(
        forceDarkMode
            ? AppCompatDelegate.MODE_NIGHT_YES
            : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    );
    setContentView(binding.getRoot());

    viewUtil = new ViewUtil();
    audioUtil = new AudioUtil(this);
    hapticUtil = new HapticUtil(this);

    binding.frameSettingsBack.setOnClickListener(v -> {
      if (viewUtil.isClickEnabled()) {
        hapticUtil.click();
        finish();
      }
    });

    binding.toolbarSettings.setOnMenuItemClickListener((MenuItem item) -> {
      int itemId = item.getItemId();
      if (itemId == R.id.action_about) {
        startActivity(new Intent(this, AboutActivity.class));
      } else if (itemId == R.id.action_feedback) {
        DialogFragment fragment = new FeedbackBottomSheetDialogFragment();
        fragment.show(getSupportFragmentManager(), fragment.toString());
      }
      hapticUtil.click();
      return true;
    });

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(this);
    systemBarBehavior.setAppBar(binding.appBarSettings);
    systemBarBehavior.setScroll(binding.scrollSettings, binding.linearSettingsContainer);
    systemBarBehavior.setUp();

    new ScrollBehavior(this).setUpScroll(
        binding.appBarSettings, binding.scrollSettings, true
    );

    binding.switchSettingsDarkMode.setChecked(forceDarkMode);
    binding.imageSettingsDarkMode.setImageResource(
        forceDarkMode
            ? R.drawable.ic_round_dark_mode_to_light_mode_anim
            : R.drawable.ic_round_light_mode_to_dark_mode_anim
    );

    binding.radioGroupSettingsSound.check(getCheckedId());
    binding.radioGroupSettingsSound.setOnCheckedChangeListener(this);

    binding.switchSettingsVibrateAlways.setChecked(
        sharedPrefs.getBoolean(
            SETTINGS.VIBRATE_ALWAYS, Constants.DEF.VIBRATE_ALWAYS
        )
    );

    binding.switchSettingsHaptic.setChecked(
        sharedPrefs.getBoolean(
            SETTINGS.HAPTIC_FEEDBACK, Constants.DEF.HAPTIC_FEEDBACK
        )
    );

    if (!hapticUtil.hasVibrator()) {
      binding.linearSettingsVibrateAlways.setVisibility(View.GONE);
      binding.linearSettingsHaptic.setVisibility(View.GONE);
    }

    binding.switchSettingsSliderEmphasis.setChecked(
        sharedPrefs.getBoolean(
            SETTINGS.EMPHASIS_SLIDER, Constants.DEF.EMPHASIS_SLIDER
        )
    );

    binding.switchSettingsKeepAwake.setChecked(
        sharedPrefs.getBoolean(SETTINGS.KEEP_AWAKE, Constants.DEF.KEEP_AWAKE)
    );

    ViewUtil.setOnClickListeners(
        this,
        binding.linearSettingsDarkMode,
        binding.linearSettingsVibrateAlways,
        binding.linearSettingsHaptic,
        binding.linearSettingsSliderEmphasis,
        binding.linearSettingsKeepAwake
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.switchSettingsDarkMode,
        binding.switchSettingsVibrateAlways,
        binding.switchSettingsHaptic,
        binding.switchSettingsSliderEmphasis,
        binding.switchSettingsKeepAwake
    );
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
    audioUtil.destroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    hapticUtil.setEnabled(sharedPrefs.getBoolean(SETTINGS.HAPTIC_FEEDBACK, DEF.HAPTIC_FEEDBACK));
  }

  private int getCheckedId() {
    String sound = sharedPrefs.getString(SETTINGS.SOUND, Constants.SOUND.WOOD);
    assert sound != null;
    switch (sound) {
      case Constants.SOUND.CLICK:
        return R.id.radio_settings_sound_click;
      case Constants.SOUND.DING:
        return R.id.radio_settings_sound_ding;
      case Constants.SOUND.BEEP:
        return R.id.radio_settings_sound_beep;
      default:
        return R.id.radio_settings_sound_wood;
    }
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_settings_dark_mode && viewUtil.isClickEnabled()) {
      binding.switchSettingsDarkMode.setChecked(!binding.switchSettingsDarkMode.isChecked());
    } else if (id == R.id.linear_settings_vibrate_always) {
      binding.switchSettingsVibrateAlways.setChecked(
          !binding.switchSettingsVibrateAlways.isChecked()
      );
    } else if (id == R.id.linear_settings_haptic) {
      binding.switchSettingsHaptic.setChecked(
          !binding.switchSettingsHaptic.isChecked()
      );
    } else if (id == R.id.linear_settings_slider_emphasis) {
      binding.switchSettingsSliderEmphasis.setChecked(
          !binding.switchSettingsSliderEmphasis.isChecked()
      );
    } else if (id == R.id.linear_settings_keep_awake) {
      binding.switchSettingsKeepAwake.setChecked(
          !binding.switchSettingsKeepAwake.isChecked()
      );
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    SharedPreferences.Editor editor = sharedPrefs.edit();

    int id = buttonView.getId();
    if (id == R.id.switch_settings_dark_mode) {
      ViewUtil.startIcon(binding.imageSettingsDarkMode);
      editor.putBoolean(SETTINGS.DARK_MODE, isChecked);
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        binding.imageSettingsDarkMode.setImageResource(
            isChecked
                ? R.drawable.ic_round_dark_mode_to_light_mode_anim
                : R.drawable.ic_round_light_mode_to_dark_mode_anim

        );
        AppCompatDelegate.setDefaultNightMode(
            isChecked
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        onStart();
      }, 300);
    } else if (id == R.id.switch_settings_vibrate_always) {
      ViewUtil.startIcon(binding.imageSettingsVibrateAlways);
      editor.putBoolean(SETTINGS.VIBRATE_ALWAYS, isChecked);
    } else if (id == R.id.switch_settings_haptic) {
      ViewUtil.startIcon(binding.imageSettingsHaptic);
      hapticUtil.setEnabled(isChecked);
      editor.putBoolean(SETTINGS.HAPTIC_FEEDBACK, isChecked);
    } else if (id == R.id.switch_settings_slider_emphasis) {
      ViewUtil.startIcon(binding.imageSettingsSliderEmphasis);
      editor.putBoolean(SETTINGS.EMPHASIS_SLIDER, isChecked);
    } else if (id == R.id.switch_settings_keep_awake) {
      ViewUtil.startIcon(binding.imageSettingsKeepAwake);
      editor.putBoolean(SETTINGS.KEEP_AWAKE, isChecked);
    }

    editor.apply();
    hapticUtil.click();
  }

  @Override
  public void onCheckedChanged(RadioGroup group, int checkedId) {
    ViewUtil.startIcon(binding.imageSettingsSound);
    String sound;
    if (checkedId == R.id.radio_settings_sound_click) {
      sound = Constants.SOUND.CLICK;
    } else if (checkedId == R.id.radio_settings_sound_ding) {
      sound = Constants.SOUND.DING;
    } else if (checkedId == R.id.radio_settings_sound_beep) {
      sound = Constants.SOUND.BEEP;
    } else {
      sound = Constants.SOUND.WOOD;
    }
    sharedPrefs.edit().putString(SETTINGS.SOUND, sound).apply();
    audioUtil.play(sound, false);
  }
}
