package xyz.zedler.patrick.tack;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.preference.PreferenceManager;

import xyz.zedler.patrick.tack.util.Constants;

public class SettingsActivity extends WearableActivity
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    final static String TAG = SettingsActivity.class.getSimpleName();

    private SharedPreferences sharedPrefs;
    private Switch switchVibrateAlways, switchWristGestures, switchHidePicker, switchAnimations;
    private TextView textViewSound;
    private boolean animations;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        animations = sharedPrefs.getBoolean(Constants.PREF.ANIMATIONS, true);

        textViewSound = findViewById(R.id.text_setting_sound);
        textViewSound.setText(getSound());

        switchVibrateAlways = findViewById(R.id.switch_setting_vibrate_always);
        switchVibrateAlways.setChecked(
                sharedPrefs.getBoolean(Constants.PREF.VIBRATE_ALWAYS, false)
        );
        switchVibrateAlways.setOnCheckedChangeListener(this);

        switchWristGestures = findViewById(R.id.switch_setting_wrist_gestures);
        switchWristGestures.setChecked(
                sharedPrefs.getBoolean(Constants.PREF.WRIST_GESTURES, true)
        );
        switchWristGestures.setOnCheckedChangeListener(this);

        switchHidePicker = findViewById(R.id.switch_setting_hide_picker);
        switchHidePicker.setChecked(
                sharedPrefs.getBoolean(Constants.PREF.HIDE_PICKER, false)
        );
        switchHidePicker.setOnCheckedChangeListener(this);

        switchAnimations = findViewById(R.id.switch_setting_animations);
        switchAnimations.setChecked(
                sharedPrefs.getBoolean(Constants.PREF.ANIMATIONS, true)
        );
        switchAnimations.setOnCheckedChangeListener(this);

        setOnClickListeners(
                R.id.linear_setting_sound,
                R.id.linear_setting_vibrate_always,
                R.id.linear_setting_wrist_gestures,
                R.id.linear_setting_hide_picker,
                R.id.linear_setting_animations,
                R.id.linear_changelog,
                R.id.linear_rate
        );
    }

    private void setOnClickListeners(@IdRes int... viewIds) {
        for (int viewId : viewIds) {
            findViewById(viewId).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.linear_setting_sound:
                startAnimatedIcon(R.id.image_sound);
                setNextSound();
                break;
            case R.id.linear_setting_vibrate_always:
                switchVibrateAlways.setChecked(!switchVibrateAlways.isChecked());
                break;
            case R.id.linear_setting_wrist_gestures:
                switchWristGestures.setChecked(!switchWristGestures.isChecked());
                break;
            case R.id.linear_setting_hide_picker:
                switchHidePicker.setChecked(!switchHidePicker.isChecked());
                break;
            case R.id.linear_setting_animations:
                switchAnimations.setChecked(!switchAnimations.isChecked());
                break;
            case R.id.linear_changelog:
                startAnimatedIcon(R.id.image_changelog);
                startActivity(new Intent(this, ChangelogActivity.class));
                break;
            case R.id.linear_rate:
                startAnimatedIcon(R.id.image_rate);
                Uri uri = Uri.parse(
                        "market://details?id=" + getApplicationContext().getPackageName()
                );
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                new Handler().postDelayed(() -> {
                    try {
                        startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                "http://play.google.com/store/apps/details?id="
                                        + getApplicationContext().getPackageName()
                        )));
                    }
                }, 300);
                break;
        }
    }

    private String getSound() {
        String sound = sharedPrefs.getString(Constants.PREF.SOUND, Constants.SOUND.WOOD);
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
        String sound = sharedPrefs.getString(Constants.PREF.SOUND, Constants.SOUND.WOOD);
        assert sound != null;
        SharedPreferences.Editor editor = sharedPrefs.edit();
        switch (sound) {
            case Constants.SOUND.CLICK:
                textViewSound.setText(R.string.setting_sound_ding);
                editor.putString(Constants.PREF.SOUND, Constants.SOUND.DING);
                break;
            case Constants.SOUND.DING:
                textViewSound.setText(R.string.setting_sound_beep);
                editor.putString(Constants.PREF.SOUND, Constants.SOUND.BEEP);
                break;
            case Constants.SOUND.BEEP:
                textViewSound.setText(R.string.setting_sound_wood);
                editor.putString(Constants.PREF.SOUND, Constants.SOUND.WOOD);
                break;
            default:
                textViewSound.setText(R.string.setting_sound_click);
                editor.putString(Constants.PREF.SOUND, Constants.SOUND.CLICK);
                break;
        }
        editor.apply();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch_setting_vibrate_always:
                sharedPrefs.edit().putBoolean(Constants.PREF.VIBRATE_ALWAYS, isChecked).apply();
                break;
            case R.id.switch_setting_wrist_gestures:
                sharedPrefs.edit().putBoolean(Constants.PREF.WRIST_GESTURES, isChecked).apply();
                break;
            case R.id.switch_setting_hide_picker:
                sharedPrefs.edit().putBoolean(Constants.PREF.HIDE_PICKER, isChecked).apply();
                break;
            case R.id.switch_setting_animations:
                sharedPrefs.edit().putBoolean(Constants.PREF.ANIMATIONS, isChecked).apply();
                animations = isChecked;
                break;
        }
    }

    private void startAnimatedIcon(@IdRes int viewId) {
        if(animations) {
            try {
                ((Animatable) ((ImageView) findViewById(viewId)).getDrawable()).start();
            } catch (ClassCastException ignored) { }
        }
    }
}
