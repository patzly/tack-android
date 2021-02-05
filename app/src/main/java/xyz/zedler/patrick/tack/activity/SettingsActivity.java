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
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.databinding.ActivitySettingsNewBinding;
import xyz.zedler.patrick.tack.fragment.FeedbackBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.util.AudioUtil;
import xyz.zedler.patrick.tack.util.ClickUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class SettingsActivity extends AppCompatActivity
		implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,
		RadioGroup.OnCheckedChangeListener {

	private ActivitySettingsNewBinding binding;
	private SharedPreferences sharedPrefs;
	private ClickUtil clickUtil;
	private AudioUtil audioUtil;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivitySettingsNewBinding.inflate(getLayoutInflater());
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		boolean forceDarkMode = sharedPrefs.getBoolean(
				Constants.SETTING.DARK_MODE,Constants.DEF.DARK_MODE
		);
		AppCompatDelegate.setDefaultNightMode(
				forceDarkMode
						? AppCompatDelegate.MODE_NIGHT_YES
						: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		);
		setContentView(binding.getRoot());

		clickUtil = new ClickUtil();
		audioUtil = new AudioUtil(this);

		binding.frameSettingsBack.setOnClickListener(v -> {
			if (clickUtil.isDisabled()) return;
			finish();
		});

		binding.toolbarSettings.setOnMenuItemClickListener((MenuItem item) -> {
			if (clickUtil.isDisabled()) return false;
			int itemId = item.getItemId();
			if (itemId == R.id.action_about) {
				startActivity(new Intent(this, AboutActivity.class));
			} else if (itemId == R.id.action_feedback) {
				DialogFragment fragment = new FeedbackBottomSheetDialogFragment();
				fragment.show(getSupportFragmentManager(), fragment.toString());
			}
			return true;
		});

		new ScrollBehavior().setUpScroll(
				this,
				binding.appBarSettings,
				binding.linearSettingsAppBar,
				binding.scrollSettings,
				true
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
						Constants.SETTING.VIBRATE_ALWAYS, Constants.DEF.VIBRATE_ALWAYS
				)
		);

		binding.switchSettingsSliderEmphasis.setChecked(
				sharedPrefs.getBoolean(
						Constants.SETTING.EMPHASIS_SLIDER, Constants.DEF.EMPHASIS_SLIDER
				)
		);

		binding.switchSettingsKeepAwake.setChecked(
				sharedPrefs.getBoolean(Constants.SETTING.KEEP_AWAKE, Constants.DEF.KEEP_AWAKE)
		);

		ViewUtil.setOnClickListeners(
				this,
				binding.linearSettingsDarkMode,
				binding.linearSettingsVibrateAlways,
				binding.linearSettingsSliderEmphasis,
				binding.linearSettingsKeepAwake
		);

		ViewUtil.setOnCheckedChangedListeners(
				this,
				binding.switchSettingsDarkMode,
				binding.switchSettingsVibrateAlways,
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

	private int getCheckedId() {
		String sound = sharedPrefs.getString(Constants.SETTING.SOUND, Constants.SOUND.WOOD);
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
		if (clickUtil.isDisabled()) return;

		int id = v.getId();
		if (id == R.id.linear_settings_dark_mode) {
			binding.switchSettingsDarkMode.setChecked(!binding.switchSettingsDarkMode.isChecked());
		} else if (id == R.id.linear_settings_vibrate_always) {
			binding.switchSettingsVibrateAlways.setChecked(
					!binding.switchSettingsVibrateAlways.isChecked()
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
		int id = buttonView.getId();
		SharedPreferences.Editor editor = sharedPrefs.edit();
		if (id == R.id.switch_settings_dark_mode) {
			ViewUtil.startAnimatedIcon(binding.imageSettingsDarkMode);
			editor.putBoolean(Constants.SETTING.DARK_MODE, isChecked);
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
			//startAnimatedIcon(R.id.image_dark_mode);
			editor.putBoolean(Constants.SETTING.VIBRATE_ALWAYS, isChecked);
		} else if (id == R.id.switch_settings_slider_emphasis) {
			ViewUtil.startAnimatedIcon(binding.imageSettingsSliderEmphasis);
			editor.putBoolean(Constants.SETTING.EMPHASIS_SLIDER, isChecked);
		} else if (id == R.id.switch_settings_keep_awake) {
			//startAnimatedIcon(R.id.image_dark_mode);
			editor.putBoolean(Constants.SETTING.KEEP_AWAKE, isChecked);
		}
		editor.apply();
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		ViewUtil.startAnimatedIcon(binding.imageSettingsSound);
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
		sharedPrefs.edit().putString(Constants.SETTING.SOUND, sound).apply();
		audioUtil.play(audioUtil.getSoundId(sound));
	}
}
