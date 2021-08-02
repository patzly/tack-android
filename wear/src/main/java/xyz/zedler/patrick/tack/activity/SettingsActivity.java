package xyz.zedler.patrick.tack.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CompoundButton;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.SETTINGS;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.ActivitySettingsWearBinding;
import xyz.zedler.patrick.tack.util.AudioUtil;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class SettingsActivity extends FragmentActivity
    implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

  final static String TAG = SettingsActivity.class.getSimpleName();

  private ActivitySettingsWearBinding binding;
  private SharedPreferences sharedPrefs;
  private ViewUtil viewUtil;
  private HapticUtil hapticUtil;
  private AudioUtil audioUtil;
  private boolean animations;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivitySettingsWearBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    animations = sharedPrefs.getBoolean(SETTINGS.ANIMATIONS, Constants.DEF.ANIMATIONS);
    viewUtil = new ViewUtil();
    audioUtil = new AudioUtil(this);
    hapticUtil = new HapticUtil(this);
    hapticUtil.setEnabled(sharedPrefs.getBoolean(SETTINGS.HAPTIC_FEEDBACK, DEF.HAPTIC_FEEDBACK));

    binding.textSettingSound.setText(getSound());

    binding.switchSettingsHeavyVibration.setChecked(
        sharedPrefs.getBoolean(
            SETTINGS.HEAVY_VIBRATION, Constants.DEF.HEAVY_VIBRATION
        )
    );

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

    binding.switchSettingsWristGestures.setChecked(
        sharedPrefs.getBoolean(
            SETTINGS.WRIST_GESTURES, Constants.DEF.WRIST_GESTURES
        )
    );

    binding.switchSettingsHidePicker.setChecked(
        sharedPrefs.getBoolean(SETTINGS.HIDE_PICKER, Constants.DEF.HIDE_PICKER)
    );

    binding.switchSettingsAnimations.setChecked(
        sharedPrefs.getBoolean(SETTINGS.ANIMATIONS, Constants.DEF.ANIMATIONS)
    );

    ViewUtil.setOnClickListeners(
        this,
        binding.linearSettingsSound,
        binding.linearSettingsHeavyVibration,
        binding.linearSettingsVibrateAlways,
        binding.linearSettingsHaptic,
        binding.linearSettingsWristGestures,
        binding.linearSettingsHidePicker,
        binding.linearSettingsAnimations,
        binding.linearSettingsChangelog,
        binding.linearSettingsRate
    );

    ViewUtil.setOnCheckedChangedListeners(
        this,
        binding.switchSettingsHeavyVibration,
        binding.switchSettingsVibrateAlways,
        binding.switchSettingsHaptic,
        binding.switchSettingsWristGestures,
        binding.switchSettingsHidePicker,
        binding.switchSettingsAnimations
    );
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
    audioUtil.destroy();
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_settings_sound) {
      if (animations) {
        ViewUtil.startIcon(binding.imageSound);
      }
      hapticUtil.tick();
      setNextSound();
      audioUtil.play(sharedPrefs.getString(SETTINGS.SOUND, Constants.DEF.SOUND), false);
    } else if (id == R.id.linear_settings_heavy_vibration) {
      binding.switchSettingsHeavyVibration.setChecked(
          !binding.switchSettingsHeavyVibration.isChecked()
      );
    } else if (id == R.id.linear_settings_vibrate_always) {
      binding.switchSettingsVibrateAlways.setChecked(
          !binding.switchSettingsVibrateAlways.isChecked()
      );
    } else if (id == R.id.linear_settings_haptic) {
      binding.switchSettingsHaptic.setChecked(!binding.switchSettingsHaptic.isChecked());
    } else if (id == R.id.linear_settings_wrist_gestures) {
      binding.switchSettingsWristGestures.setChecked(
          !binding.switchSettingsWristGestures.isChecked()
      );
    } else if (id == R.id.linear_settings_hide_picker) {
      binding.switchSettingsHidePicker.setChecked(
          !binding.switchSettingsHidePicker.isChecked()
      );
    } else if (id == R.id.linear_settings_animations) {
      binding.switchSettingsAnimations.setChecked(
          !binding.switchSettingsAnimations.isChecked()
      );
    } else if (id == R.id.linear_settings_changelog && viewUtil.isClickEnabled()) {
      if (animations) {
        ViewUtil.startIcon(binding.imageChangelog);
      }
      hapticUtil.tick();
      startActivity(new Intent(this, ChangelogActivity.class));
    } else if (id == R.id.linear_settings_rate && viewUtil.isClickEnabled()) {
      if (animations) {
        ViewUtil.startIcon(binding.imageRate);
      }
      hapticUtil.tick();
      Uri uri = Uri.parse(
          "market://details?id=" + getApplicationContext().getPackageName()
      );
      Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
      goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
          Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
          Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        try {
          startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
              "http://play.google.com/store/apps/details?id="
                  + getApplicationContext().getPackageName()
          )));
        }
      }, 400);
    }
  }

  private String getSound() {
    String sound = sharedPrefs.getString(SETTINGS.SOUND, Constants.DEF.SOUND);
    assert sound != null;
    switch (sound) {
      case Constants.SOUND.CLICK:
        return getString(R.string.setting_sound_click);
      case Constants.SOUND.DING:
        return getString(R.string.setting_sound_ding);
      case Constants.SOUND.BEEP:
        return getString(R.string.setting_sound_beep);
      default:
        return getString(R.string.setting_sound_wood);
    }
  }

  private void setNextSound() {
    SharedPreferences.Editor editor = sharedPrefs.edit();
    String sound = sharedPrefs.getString(SETTINGS.SOUND, Constants.DEF.SOUND);
    assert sound != null;
    switch (sound) {
      case Constants.SOUND.CLICK:
        binding.textSettingSound.setText(R.string.setting_sound_ding);
        editor.putString(SETTINGS.SOUND, Constants.SOUND.DING);
        break;
      case Constants.SOUND.DING:
        binding.textSettingSound.setText(R.string.setting_sound_beep);
        editor.putString(SETTINGS.SOUND, Constants.SOUND.BEEP);
        break;
      case Constants.SOUND.BEEP:
        binding.textSettingSound.setText(R.string.setting_sound_wood);
        editor.putString(SETTINGS.SOUND, Constants.SOUND.WOOD);
        break;
      default:
        binding.textSettingSound.setText(R.string.setting_sound_click);
        editor.putString(SETTINGS.SOUND, Constants.SOUND.CLICK);
        break;
    }
    editor.apply();
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    SharedPreferences.Editor editor = sharedPrefs.edit();
    int id = buttonView.getId();
    if (id == R.id.switch_settings_heavy_vibration) {
      editor.putBoolean(SETTINGS.HEAVY_VIBRATION, isChecked);
    } else if (id == R.id.switch_settings_vibrate_always) {
      editor.putBoolean(SETTINGS.VIBRATE_ALWAYS, isChecked);
    } else if (id == R.id.switch_settings_haptic) {
      editor.putBoolean(SETTINGS.HAPTIC_FEEDBACK, isChecked);
      hapticUtil.setEnabled(isChecked);
    } else if (id == R.id.switch_settings_wrist_gestures) {
      editor.putBoolean(SETTINGS.WRIST_GESTURES, isChecked);
    } else if (id == R.id.switch_settings_hide_picker) {
      editor.putBoolean(SETTINGS.HIDE_PICKER, isChecked);
    } else if (id == R.id.switch_settings_animations) {
      editor.putBoolean(SETTINGS.ANIMATIONS, isChecked);
      animations = isChecked;
    }
    editor.apply();
    hapticUtil.tick();
  }
}
