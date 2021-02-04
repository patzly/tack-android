package xyz.zedler.patrick.tack.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.fragment.FeedbackBottomSheetDialogFragment;

public class SettingsActivity extends AppCompatActivity
		implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

	private final static String TAG = SettingsActivity.class.getSimpleName();
	private final static boolean DEBUG = false;

	private long lastClick = 0;
	private SharedPreferences sharedPrefs;
	private ImageView imageViewDarkMode;
	private SwitchMaterial switchDarkMode;
	private SwitchMaterial switchVibrateAlways;
	private SwitchMaterial switchSlider;
	private SwitchMaterial switchKeepAwake;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		AppCompatDelegate.setDefaultNightMode(
				sharedPrefs.getBoolean("force_dark_mode",false)
						? AppCompatDelegate.MODE_NIGHT_YES
						: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		);
		setContentView(R.layout.activity_settings);

		findViewById(R.id.frame_back_settings).setOnClickListener(v -> {
			if (SystemClock.elapsedRealtime() - lastClick < 1000) return;
			lastClick = SystemClock.elapsedRealtime();
			finish();
		});

		((Toolbar) findViewById(R.id.toolbar_settings)).setOnMenuItemClickListener((MenuItem item) -> {
			if (SystemClock.elapsedRealtime() - lastClick < 1000) return false;
			lastClick = SystemClock.elapsedRealtime();
			int itemId = item.getItemId();
			if (itemId == R.id.action_about) {
				startActivity(new Intent(this, AboutActivity.class));
			} else if (itemId == R.id.action_feedback) {
				new FeedbackBottomSheetDialogFragment().show(
						getSupportFragmentManager(),
						"feedback"
				);
			}
			return true;
		});

		new ScrollBehavior().setUpScroll(
				this,
				R.id.app_bar_settings,
				R.id.linear_app_bar_settings,
				R.id.scroll_settings,
				true
		);

		switchDarkMode = findViewById(R.id.switch_dark_mode);
		switchDarkMode.setChecked(sharedPrefs.getBoolean("force_dark_mode",false));
		switchDarkMode.setOnCheckedChangeListener(this);

		RadioGroup radioGroupSound = findViewById(R.id.radio_group_sound);
		radioGroupSound.check(getCheckedId());
		radioGroupSound.setOnCheckedChangeListener((RadioGroup group, int checkedId) -> {
			startAnimatedIcon(R.id.image_sound);
			String sound = "wood";
			if (checkedId == R.id.radio_sound_wood) {
				sound = "wood";
			} else if (checkedId == R.id.radio_sound_click) {
				sound = "click";
			} else if (checkedId == R.id.radio_sound_ding) {
				sound = "ding";
			} else if (checkedId == R.id.radio_sound_beep) {
				sound = "beep";
			}
			sharedPrefs.edit().putString("sound", sound).apply();
		});

		switchVibrateAlways = findViewById(R.id.switch_vibrate_always);
		switchVibrateAlways.setChecked(sharedPrefs.getBoolean("vibrate_always",false));
		switchVibrateAlways.setOnCheckedChangeListener(this);

		switchSlider = findViewById(R.id.switch_slider_emphasis);
		switchSlider.setChecked(sharedPrefs.getBoolean("emphasis_slider",false));
		switchSlider.setOnCheckedChangeListener(this);

		switchKeepAwake = findViewById(R.id.switch_keep_awake);
		switchKeepAwake.setChecked(sharedPrefs.getBoolean("keep_awake",true));
		switchKeepAwake.setOnCheckedChangeListener(this);

		imageViewDarkMode = findViewById(R.id.image_dark_mode);
		imageViewDarkMode.setImageResource(
				sharedPrefs.getBoolean("force_dark_mode",false)
						? R.drawable.ic_round_dark_mode_off_anim
						: R.drawable.ic_round_dark_mode_on_anim
		);

		setOnClickListeners(
				R.id.linear_dark_mode,
				R.id.linear_vibrate_always,
				R.id.linear_slider_emphasis,
				R.id.linear_keep_awake
		);
	}

	private int getCheckedId() {
		String sound = sharedPrefs.getString("sound", "wood");
		assert sound != null;
		switch (sound) {
			case "click":
				return R.id.radio_sound_click;
			case "ding":
				return R.id.radio_sound_ding;
			case "beep":
				return R.id.radio_sound_beep;
			default:
				return R.id.radio_sound_wood;
		}
	}

	private void setOnClickListeners(@IdRes int... viewIds) {
		for (int viewId : viewIds) {
			findViewById(viewId).setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {

		if (SystemClock.elapsedRealtime() - lastClick < 400){
			return;
		}
		lastClick = SystemClock.elapsedRealtime();

		int id = v.getId();
		if (id == R.id.linear_dark_mode) {
			switchDarkMode.setChecked(!switchDarkMode.isChecked());
		} else if (id == R.id.linear_vibrate_always) {
			switchVibrateAlways.setChecked(!switchVibrateAlways.isChecked());
		} else if (id == R.id.linear_slider_emphasis) {
			switchSlider.setChecked(!switchSlider.isChecked());
		} else if (id == R.id.linear_keep_awake) {
			switchKeepAwake.setChecked(!switchKeepAwake.isChecked());
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int id = buttonView.getId();
		if (id == R.id.switch_dark_mode) {
			startAnimatedIcon(R.id.image_dark_mode);
			sharedPrefs.edit().putBoolean("force_dark_mode", isChecked).apply();
			new Handler(Looper.getMainLooper()).postDelayed(() -> {
				imageViewDarkMode.setImageResource(
						isChecked
								? R.drawable.ic_round_dark_mode_off_anim
								: R.drawable.ic_round_dark_mode_on_anim

				);
				AppCompatDelegate.setDefaultNightMode(
						isChecked
								? AppCompatDelegate.MODE_NIGHT_YES
								: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
				);
				onStart();
			}, 300);
		} else if (id == R.id.switch_vibrate_always) {//startAnimatedIcon(R.id.image_dark_mode);
			sharedPrefs.edit().putBoolean("vibrate_always", isChecked).apply();
		} else if (id == R.id.switch_slider_emphasis) {
			startAnimatedIcon(R.id.image_slider_emphasis);
			sharedPrefs.edit().putBoolean("emphasis_slider", isChecked).apply();
		} else if (id == R.id.switch_keep_awake) {//startAnimatedIcon(R.id.image_dark_mode);
			sharedPrefs.edit().putBoolean("keep_awake", isChecked).apply();
		}
	}

	private void startAnimatedIcon(int viewId) {
		try {
			((Animatable) ((ImageView) findViewById(viewId)).getDrawable()).start();
		} catch (ClassCastException e) {
			if (DEBUG) Log.e(TAG, "startAnimatedIcon() requires AVD!");
		}
	}
}
