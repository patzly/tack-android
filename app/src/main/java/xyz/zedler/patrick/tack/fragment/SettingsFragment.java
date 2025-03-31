/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.fragment;

import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import xyz.zedler.patrick.tack.Constants.CONTRAST;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SOUND;
import xyz.zedler.patrick.tack.Constants.THEME;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.FragmentSettingsBinding;
import xyz.zedler.patrick.tack.service.MetronomeService;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.LocaleUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListenerAdapter;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.ShortcutUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.ThemeSelectionCardView;

public class SettingsFragment extends BaseFragment
    implements OnClickListener, OnCheckedChangeListener, OnChangeListener {

  private static final String TAG = SettingsFragment.class.getSimpleName();

  private FragmentSettingsBinding binding;
  private MainActivity activity;
  private Bundle savedState;
  private DialogUtil dialogUtilReset, dialogUtilSound;
  private Drawable itemBgFlash;
  private boolean flashScreen;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentSettingsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    dialogUtilReset.dismiss();
    dialogUtilSound.dismiss();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    savedState = savedInstanceState;
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarSettings);
    systemBarBehavior.setScroll(binding.scrollSettings, binding.linearSettingsContainer);
    systemBarBehavior.setUp();

    new ScrollBehavior().setUpScroll(
        binding.appBarSettings, binding.scrollSettings, true
    );

    binding.toolbarSettings.setNavigationOnClickListener(getNavigationOnClickListener());
    binding.toolbarSettings.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (getViewUtil().isClickDisabled(id)) {
        return false;
      }
      performHapticClick();
      if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_about) {
        activity.navigate(SettingsFragmentDirections.actionSettingsToAbout());
      } else if (id == R.id.action_help) {
        activity.showTextBottomSheet(R.raw.help, R.string.title_help);
      } else if (id == R.id.action_log) {
        activity.navigate(SettingsFragmentDirections.actionSettingsToLog());
      }
      return true;
    });

    binding.textSettingsLanguage.setText(
        LocaleUtil.followsSystem()
            ? getString(R.string.settings_language_system)
            : LocaleUtil.getLocaleName()
    );

    setUpThemeSelection();

    int idMode;
    switch (getSharedPrefs().getInt(PREF.UI_MODE, DEF.UI_MODE)) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        idMode = R.id.button_settings_theme_light;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
        idMode = R.id.button_settings_theme_dark;
        break;
      default:
        idMode = R.id.button_settings_theme_auto;
        break;
    }
    binding.toggleSettingsTheme.check(idMode);
    binding.toggleSettingsTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (!isChecked) {
        return;
      }
      int pref;
      if (checkedId == R.id.button_settings_theme_light) {
        pref = AppCompatDelegate.MODE_NIGHT_NO;
      } else if (checkedId == R.id.button_settings_theme_dark) {
        pref = AppCompatDelegate.MODE_NIGHT_YES;
      } else {
        pref = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
      }
      getSharedPrefs().edit().putInt(PREF.UI_MODE, pref).apply();
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsTheme);
      activity.restartToApply(0, getInstanceState(), true, false);
    });

    int idContrast;
    switch (getSharedPrefs().getString(PREF.UI_CONTRAST, DEF.UI_CONTRAST)) {
      case CONTRAST.MEDIUM:
        idContrast = R.id.button_other_contrast_medium;
        break;
      case CONTRAST.HIGH:
        idContrast = R.id.button_other_contrast_high;
        break;
      default:
        idContrast = R.id.button_other_contrast_standard;
        break;
    }
    binding.toggleOtherContrast.check(idContrast);
    binding.toggleOtherContrast.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (!isChecked) {
        return;
      }
      String pref;
      if (checkedId == R.id.button_other_contrast_medium) {
        pref = CONTRAST.MEDIUM;
      } else if (checkedId == R.id.button_other_contrast_high) {
        pref = CONTRAST.HIGH;
      } else {
        pref = CONTRAST.STANDARD;
      }
      getSharedPrefs().edit().putString(PREF.UI_CONTRAST, pref).apply();
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsContrast);
      activity.restartToApply(0, getInstanceState(), true, false);
    });
    String currentTheme = getSharedPrefs().getString(PREF.THEME, DEF.THEME);
    boolean isDynamic;
    boolean hasDynamic = DynamicColors.isDynamicColorAvailable();
    if (currentTheme.isEmpty()) {
      isDynamic = hasDynamic;
    } else {
      isDynamic = currentTheme.equals(THEME.DYNAMIC);
    }
    binding.toggleOtherContrast.setEnabled(!isDynamic);
    binding.textSettingsContrastDynamic.setVisibility(isDynamic ? View.VISIBLE : View.GONE);
    binding.textSettingsContrastDynamic.setText(
        VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE
            ? R.string.settings_contrast_dynamic
            : R.string.settings_contrast_dynamic_unsupported
    );

    binding.switchSettingsHaptic.setChecked(
        getSharedPrefs().getBoolean(PREF.HAPTIC, HapticUtil.areSystemHapticsTurnedOn(activity))
    );
    binding.switchSettingsHaptic.jumpDrawablesToCurrentState();
    binding.linearSettingsHaptic.setVisibility(
        activity.getHapticUtil().hasVibrator() ? View.VISIBLE : View.GONE
    );

    binding.switchSettingsReduceAnimations.setChecked(
        getSharedPrefs().getBoolean(PREF.REDUCE_ANIM, DEF.REDUCE_ANIM)
    );
    binding.switchSettingsReduceAnimations.jumpDrawablesToCurrentState();

    binding.switchSettingsHideSubControls.setChecked(
        getSharedPrefs().getBoolean(PREF.HIDE_SUB_CONTROLS, DEF.HIDE_SUB_CONTROLS)
    );
    binding.switchSettingsHideSubControls.jumpDrawablesToCurrentState();

    binding.switchSettingsActiveBeat.setChecked(
        getSharedPrefs().getBoolean(PREF.ACTIVE_BEAT, DEF.ACTIVE_BEAT)
    );
    binding.switchSettingsActiveBeat.jumpDrawablesToCurrentState();

    binding.switchSettingsBigTimeText.setChecked(
        getSharedPrefs().getBoolean(PREF.BIG_TIME_TEXT, DEF.BIG_TIME_TEXT)
    );
    binding.switchSettingsBigTimeText.jumpDrawablesToCurrentState();

    binding.switchSettingsBigLogo.setChecked(
        getSharedPrefs().getBoolean(PREF.BIG_LOGO, DEF.BIG_LOGO)
    );
    binding.switchSettingsBigLogo.jumpDrawablesToCurrentState();

    dialogUtilReset = new DialogUtil(activity, "reset");
    dialogUtilReset.createDialogError(builder -> {
      builder.setTitle(R.string.msg_reset);
      builder.setMessage(R.string.msg_reset_description);
      builder.setPositiveButton(R.string.action_reset, (dialog, which) -> {
        performHapticClick();
        getMetronomeUtil().stop();
        getSharedPrefs().edit().clear().apply();
        activity.getSongViewModel().deleteAll();
        new ShortcutUtil(activity).removeAllShortcuts();
        activity.restartToApply(100, getInstanceState(), false, true);
      });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilReset.showIfWasShown(savedInstanceState);

    dialogUtilSound = new DialogUtil(activity, "sound");

    MetronomeListener latencyListener = new MetronomeListenerAdapter() {
      @Override
      public void onMetronomeTick(Tick tick) {
        activity.runOnUiThread(() -> {
          if (binding != null && flashScreen) {
            binding.linearSettingsLatency.setBackground(itemBgFlash);
            binding.linearSettingsLatency.postDelayed(() -> {
              if (binding != null) {
                binding.linearSettingsLatency.setBackground(null);
              }
            }, 100);
          }
        });
      }

      @Override
      public void onMetronomeConnectionMissing() {
        activity.runOnUiThread(() -> {
          if (binding != null) {
            activity.showSnackbar(R.string.msg_connection_lost);
          }
        });
      }
    };

    binding.sliderSettingsLatency.addOnSliderTouchListener(new OnSliderTouchListener() {
      @Override
      public void onStartTrackingTouch(@NonNull Slider slider) {
        flashScreen = true;
        new Thread(() -> {
          getMetronomeUtil().savePlayingState();
          getMetronomeUtil().addListener(latencyListener);
          getMetronomeUtil().setUpLatencyCalibration();
        }).start();
      }

      @Override
      public void onStopTrackingTouch(@NonNull Slider slider) {
        flashScreen = false;
        new Thread(() -> {
          getMetronomeUtil().restorePlayingState();
          getMetronomeUtil().removeListener(latencyListener);
          getMetronomeUtil().setToPreferences();
        }).start();
      }
    });
    binding.sliderSettingsLatency.setLabelFormatter(
        value -> getString(
            R.string.label_ms, String.format(activity.getLocale(), "%.0f", value)
        )
    );
    itemBgFlash = ViewUtil.getBgListItemSelected(activity, R.attr.colorTertiaryContainer);

    binding.sliderSettingsGain.setLabelFormatter(
        value -> getString(R.string.label_db, (int) value)
    );

    binding.linearSettingsAlwaysVibrate.setVisibility(
        activity.getHapticUtil().hasVibrator() ? View.VISIBLE : View.GONE
    );

    updateMetronomeSettings();

    ViewUtil.setOnClickListeners(
        this,
        binding.linearSettingsLanguage,
        binding.linearSettingsHaptic,
        binding.linearSettingsReduceAnimations,
        binding.linearSettingsReset,
        binding.linearSettingsSound,
        binding.linearSettingsIgnoreFocus,
        binding.linearSettingsHideSubControls,
        binding.linearSettingsActiveBeat,
        binding.linearSettingsPermNotification,
        binding.linearSettingsAlwaysVibrate,
        binding.linearSettingsElapsed,
        binding.linearSettingsResetTimer,
        binding.linearSettingsBigTimeText,
        binding.linearSettingsFlashScreen,
        binding.linearSettingsKeepAwake,
        binding.linearSettingsBigLogo
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.switchSettingsHaptic,
        binding.switchSettingsReduceAnimations,
        binding.switchSettingsIgnoreFocus,
        binding.switchSettingsHideSubControls,
        binding.switchSettingsActiveBeat,
        binding.switchSettingsPermNotification,
        binding.switchSettingsAlwaysVibrate,
        binding.switchSettingsElapsed,
        binding.switchSettingsResetTimer,
        binding.switchSettingsBigTimeText,
        binding.switchSettingsFlashScreen,
        binding.switchSettingsKeepAwake,
        binding.switchSettingsBigLogo
    );
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (dialogUtilReset != null) {
      dialogUtilReset.saveState(outState);
    }
    if (dialogUtilSound != null) {
      dialogUtilSound.saveState(outState);
    }
  }

  public void updateMetronomeSettings() {
    Map<String, String> labels = new LinkedHashMap<>();
    labels.put(SOUND.SINE, getString(R.string.settings_sound_sine));
    labels.put(SOUND.WOOD, getString(R.string.settings_sound_wood));
    labels.put(SOUND.MECHANICAL, getString(R.string.settings_sound_mechanical));
    labels.put(SOUND.BEATBOXING_1, getString(R.string.settings_sound_beatboxing_1));
    labels.put(SOUND.BEATBOXING_2, getString(R.string.settings_sound_beatboxing_2));
    labels.put(SOUND.HANDS, getString(R.string.settings_sound_hands));
    labels.put(SOUND.FOLDING, getString(R.string.settings_sound_folding));
    ArrayList<String> sounds = new ArrayList<>(labels.keySet());
    String[] items = labels.values().toArray(new String[]{});
    int init = sounds.indexOf(getMetronomeUtil().getSound());
    if (init == -1) {
      init = 0;
      getSharedPrefs().edit().remove(PREF.SOUND).apply();
    }
    int initFinal = init;
    binding.textSettingsSound.setText(items[initFinal]);
    dialogUtilSound.createDialog(builder -> {
      builder.setTitle(R.string.settings_sound);
      builder.setSingleChoiceItems(
          items, initFinal, (dialog, which) -> {
            performHapticClick();
            getMetronomeUtil().setSound(sounds.get(which));
            binding.textSettingsSound.setText(items[which]);
          }
      );
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilSound.showIfWasShown(savedState);

    binding.sliderSettingsLatency.removeOnChangeListener(this);
    binding.sliderSettingsLatency.setValue(getMetronomeUtil().getLatency());
    binding.sliderSettingsLatency.addOnChangeListener(this);

    binding.switchSettingsIgnoreFocus.setOnCheckedChangeListener(null);
    binding.switchSettingsIgnoreFocus.setChecked(getMetronomeUtil().getIgnoreAudioFocus());
    binding.switchSettingsIgnoreFocus.jumpDrawablesToCurrentState();
    binding.switchSettingsIgnoreFocus.setOnCheckedChangeListener(this);

    binding.sliderSettingsGain.removeOnChangeListener(this);
    binding.sliderSettingsGain.setValue(getMetronomeUtil().getGain());
    binding.sliderSettingsGain.addOnChangeListener(this);

    binding.switchSettingsAlwaysVibrate.setOnCheckedChangeListener(null);
    binding.switchSettingsAlwaysVibrate.setChecked(getMetronomeUtil().isAlwaysVibrate());
    binding.switchSettingsAlwaysVibrate.jumpDrawablesToCurrentState();
    binding.switchSettingsAlwaysVibrate.setOnCheckedChangeListener(this);
    binding.linearSettingsAlwaysVibrate.setVisibility(
        activity.getHapticUtil().hasVibrator() ? View.VISIBLE : View.GONE
    );

    binding.switchSettingsElapsed.setOnCheckedChangeListener(null);
    binding.switchSettingsElapsed.setChecked(getMetronomeUtil().getShowElapsed());
    binding.switchSettingsElapsed.jumpDrawablesToCurrentState();
    binding.switchSettingsElapsed.setOnCheckedChangeListener(this);

    binding.switchSettingsResetTimer.setOnCheckedChangeListener(null);
    binding.switchSettingsResetTimer.setChecked(getMetronomeUtil().getResetTimerOnStop());
    binding.switchSettingsResetTimer.jumpDrawablesToCurrentState();
    binding.switchSettingsResetTimer.setOnCheckedChangeListener(this);

    binding.switchSettingsFlashScreen.setOnCheckedChangeListener(null);
    binding.switchSettingsFlashScreen.setChecked(getMetronomeUtil().getFlashScreen());
    binding.switchSettingsFlashScreen.jumpDrawablesToCurrentState();
    binding.switchSettingsFlashScreen.setOnCheckedChangeListener(this);

    binding.switchSettingsKeepAwake.setOnCheckedChangeListener(null);
    binding.switchSettingsKeepAwake.setChecked(getMetronomeUtil().getKeepAwake());
    binding.switchSettingsKeepAwake.jumpDrawablesToCurrentState();
    binding.switchSettingsKeepAwake.setOnCheckedChangeListener(this);

    MetronomeService service = activity.getMetronomeService();
    boolean permNotification = service != null && service.getPermNotification();
    binding.switchSettingsPermNotification.setOnCheckedChangeListener(null);
    binding.switchSettingsPermNotification.setChecked(permNotification);
    binding.switchSettingsPermNotification.jumpDrawablesToCurrentState();
    binding.switchSettingsPermNotification.setOnCheckedChangeListener(this);
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_settings_language && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsLanguage);
      activity.navigate(SettingsFragmentDirections.actionSettingsToLanguagesDialog());
    } else if (id == R.id.linear_settings_haptic) {
      binding.switchSettingsHaptic.toggle();
    } else if (id == R.id.linear_settings_reduce_animations) {
      binding.switchSettingsReduceAnimations.toggle();
    } else if (id == R.id.linear_settings_reset && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      dialogUtilReset.show();
    } else if (id == R.id.linear_settings_sound && getViewUtil().isClickEnabled(id)) {
      ViewUtil.startIcon(binding.imageSettingsSound);
      performHapticClick();
      dialogUtilSound.show();
    } else if (id == R.id.linear_settings_ignore_focus) {
      binding.switchSettingsIgnoreFocus.toggle();
    } else if (id == R.id.linear_settings_hide_sub_controls) {
      binding.switchSettingsHideSubControls.toggle();
    } else if (id == R.id.linear_settings_active_beat) {
      binding.switchSettingsActiveBeat.toggle();
    } else if (id == R.id.linear_settings_perm_notification) {
      binding.switchSettingsPermNotification.toggle();
    } else if (id == R.id.linear_settings_always_vibrate) {
      binding.switchSettingsAlwaysVibrate.toggle();
    } else if (id == R.id.linear_settings_elapsed) {
      binding.switchSettingsElapsed.toggle();
    } else if (id == R.id.linear_settings_reset_timer) {
      binding.switchSettingsResetTimer.toggle();
    } else if (id == R.id.linear_settings_big_time_text) {
      binding.switchSettingsBigTimeText.toggle();
    } else if (id == R.id.linear_settings_flash_screen) {
      binding.switchSettingsFlashScreen.toggle();
    } else if (id == R.id.linear_settings_keep_awake) {
      binding.switchSettingsKeepAwake.toggle();
    } else if (id == R.id.linear_settings_big_logo) {
      binding.switchSettingsBigLogo.toggle();
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.switch_settings_haptic) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsHaptic);
      getSharedPrefs().edit().putBoolean(PREF.HAPTIC, isChecked).apply();
      activity.getHapticUtil().setEnabled(isChecked);
    } else if (id == R.id.switch_settings_reduce_animations) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsReduceAnimations);
      getSharedPrefs().edit().putBoolean(PREF.REDUCE_ANIM, isChecked).apply();
    } else if (id == R.id.switch_settings_ignore_focus) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsIgnoreFocus);
      getMetronomeUtil().setIgnoreFocus(isChecked);
    } else if (id == R.id.switch_settings_hide_sub_controls) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsHideSubControls);
      getSharedPrefs().edit().putBoolean(PREF.HIDE_SUB_CONTROLS, isChecked).apply();
    } else if (id == R.id.switch_settings_active_beat) {
      performHapticClick();
      getSharedPrefs().edit().putBoolean(PREF.ACTIVE_BEAT, isChecked).apply();
    } else if (id == R.id.switch_settings_perm_notification) {
      performHapticClick();
      MetronomeService service = activity.getMetronomeService();
      if (service != null) {
        try {
          boolean permNotification = service.setPermNotification(isChecked);
          binding.switchSettingsPermNotification.setChecked(permNotification);
        } catch (IllegalStateException e) {
          binding.switchSettingsPermNotification.setChecked(false);
          activity.requestNotificationPermission(false);
        }
      } else {
        activity.showSnackbar(R.string.msg_connection_lost);
      }
    } else if (id == R.id.switch_settings_always_vibrate) {
      ViewUtil.startIcon(binding.imageSettingsAlwaysVibrate);
      getMetronomeUtil().setAlwaysVibrate(isChecked);
      performHapticClick();
    } else if (id == R.id.switch_settings_elapsed) {
      ViewUtil.startIcon(binding.imageSettingsElapsed);
      performHapticClick();
      getMetronomeUtil().setShowElapsed(isChecked);
    } else if (id == R.id.switch_settings_reset_timer) {
      ViewUtil.startIcon(binding.imageSettingsResetTimer);
      performHapticClick();
      getMetronomeUtil().setResetTimerOnStop(isChecked);
    } else if (id == R.id.switch_settings_big_time_text) {
      ViewUtil.startIcon(binding.imageSettingsBigTimeText);
      performHapticClick();
      getSharedPrefs().edit().putBoolean(PREF.BIG_TIME_TEXT, isChecked).apply();
    } else if (id == R.id.switch_settings_flash_screen) {
      ViewUtil.startIcon(binding.imageSettingsFlashScreen);
      performHapticClick();
      getMetronomeUtil().setFlashScreen(isChecked);
    } else if (id == R.id.switch_settings_keep_awake) {
      ViewUtil.startIcon(binding.imageSettingsKeepAwake);
      performHapticClick();
      getMetronomeUtil().setKeepAwake(isChecked);
      UiUtil.keepScreenAwake(activity, getMetronomeUtil().isPlaying() && isChecked);
    } else if (id == R.id.switch_settings_big_logo) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsBigLogo);
      getSharedPrefs().edit().putBoolean(PREF.BIG_LOGO, isChecked).apply();
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_settings_latency) {
      getMetronomeUtil().setLatency((long) value);
      //ViewUtil.startIcon(binding.imageSettingsLatency);
    } else if (id == R.id.slider_settings_gain) {
      getMetronomeUtil().setGain((int) value);
      ViewUtil.startIcon(binding.imageSettingsGain);
      performHapticSegmentTick(slider, false);
    }
  }

  private void setUpThemeSelection() {
    boolean hasDynamic = DynamicColors.isDynamicColorAvailable();
    ViewGroup container = binding.linearSettingsThemeContainer;
    for (int i = hasDynamic ? -1 : 0; i < 4; i++) {
      String name;
      int resId;
      switch (i) {
        case -1:
          name = THEME.DYNAMIC;
          resId = -1;
          break;
        case 0:
          name = THEME.RED;
          resId = getContrastThemeResId(
              R.style.Theme_Tack_Red,
              R.style.ThemeOverlay_Tack_Red_MediumContrast,
              R.style.ThemeOverlay_Tack_Red_HighContrast
          );
          break;
        case 2:
          name = THEME.GREEN;
          resId = getContrastThemeResId(
              R.style.Theme_Tack_Green,
              R.style.ThemeOverlay_Tack_Green_MediumContrast,
              R.style.ThemeOverlay_Tack_Green_HighContrast
          );
          break;
        case 3:
          name = THEME.BLUE;
          resId = getContrastThemeResId(
              R.style.Theme_Tack_Blue,
              R.style.ThemeOverlay_Tack_Blue_MediumContrast,
              R.style.ThemeOverlay_Tack_Blue_HighContrast
          );
          break;
        default:
          name = THEME.YELLOW;
          resId = getContrastThemeResId(
              R.style.Theme_Tack_Yellow,
              R.style.ThemeOverlay_Tack_Yellow_MediumContrast,
              R.style.ThemeOverlay_Tack_Yellow_HighContrast
          );
          break;
      }

      ThemeSelectionCardView card = new ThemeSelectionCardView(activity);
      card.setNestedContext(
          i == -1 && VERSION.SDK_INT >= VERSION_CODES.S
              ? DynamicColors.wrapContextIfAvailable(activity)
              : new ContextThemeWrapper(activity, resId)
      );
      card.setOnClickListener(v -> {
        if (!card.isChecked()) {
          card.startCheckedIcon();
          ViewUtil.startIcon(binding.imageSettingsTheme);
          performHapticClick();
          ViewUtil.uncheckAllChildren(container);
          card.setChecked(true);
          getSharedPrefs().edit().putString(PREF.THEME, name).apply();
          activity.restartToApply(
              100, getInstanceState(), true, false
          );
        }
      });

      String selected = getSharedPrefs().getString(PREF.THEME, DEF.THEME);
      boolean isSelected;
      if (selected.isEmpty()) {
        isSelected = hasDynamic ? name.equals(THEME.DYNAMIC) : name.equals(THEME.YELLOW);
      } else {
        isSelected = selected.equals(name);
      }
      card.setChecked(isSelected);
      container.addView(card);

      if (hasDynamic && i == -1) {
        MaterialDivider divider = new MaterialDivider(activity);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            UiUtil.dpToPx(activity, 1), UiUtil.dpToPx(activity, 40)
        );
        int marginLeft, marginRight;
        if (UiUtil.isLayoutRtl(activity)) {
          marginLeft = UiUtil.dpToPx(activity, 8);
          marginRight = UiUtil.dpToPx(activity, 4);
        } else {
          marginLeft = UiUtil.dpToPx(activity, 4);
          marginRight = UiUtil.dpToPx(activity, 8);
        }
        layoutParams.setMargins(marginLeft, 0, marginRight, 0);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        divider.setLayoutParams(layoutParams);
        container.addView(divider);
      }
    }

    Bundle bundleInstanceState = activity.getIntent().getBundleExtra(EXTRA.INSTANCE_STATE);
    if (bundleInstanceState != null) {
      binding.scrollHorizSettingsTheme.scrollTo(
          bundleInstanceState.getInt(EXTRA.SCROLL_POSITION, 0),
          0
      );
    }
  }

  private int getContrastThemeResId(
      @StyleRes int resIdStandard,
      @StyleRes int resIdMedium,
      @StyleRes int resIdHigh
  ) {
    switch (getSharedPrefs().getString(PREF.UI_CONTRAST, DEF.UI_CONTRAST)) {
      case CONTRAST.MEDIUM:
        return resIdMedium;
      case CONTRAST.HIGH:
        return resIdHigh;
      default:
        return resIdStandard;
    }
  }

  private Bundle getInstanceState() {
    Bundle bundle = new Bundle();
    if (binding != null) {
      bundle.putInt(EXTRA.SCROLL_POSITION, binding.scrollHorizSettingsTheme.getScrollX());
    }
    return bundle;
  }
}