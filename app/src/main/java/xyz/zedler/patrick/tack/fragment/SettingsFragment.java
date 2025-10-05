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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.divider.MaterialDivider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import xyz.zedler.patrick.tack.Constants.CONTRAST;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.FLASH_SCREEN;
import xyz.zedler.patrick.tack.Constants.KEEP_AWAKE;
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
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.util.ShortcutUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.util.dialog.BackupDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.GainDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.LanguagesDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.LatencyDialogUtil;
import xyz.zedler.patrick.tack.view.ThemeSelectionCardView;

public class SettingsFragment extends BaseFragment
    implements OnClickListener, OnCheckedChangeListener, OnButtonCheckedListener {

  private static final String TAG = SettingsFragment.class.getSimpleName();

  private FragmentSettingsBinding binding;
  private MainActivity activity;
  private Bundle savedState;
  private DialogUtil dialogUtilReset, dialogUtilSound;
  private LanguagesDialogUtil languagesDialogUtil;
  private GainDialogUtil gainDialogUtil;
  private LatencyDialogUtil latencyDialogUtil;
  private BackupDialogUtil backupDialogUtil;

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
    languagesDialogUtil.dismiss();
    gainDialogUtil.dismiss();
    latencyDialogUtil.dismiss();
    backupDialogUtil.dismiss();
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
        binding.appBarSettings, binding.scrollSettings, ScrollBehavior.LIFT_ON_SCROLL
    );

    binding.buttonSettingsBack.setOnClickListener(getNavigationOnClickListener());
    binding.buttonSettingsMenu.setOnClickListener(v -> {
      performHapticClick();
      ViewUtil.showMenu(v, R.menu.menu_settings, item -> {
        int id = item.getItemId();
        if (getViewUtil().isClickDisabled(id)) {
          return false;
        }
        performHapticClick();
        if (id == R.id.action_feedback) {
          activity.showFeedback();
        } else if (id == R.id.action_about) {
          activity.navigate(SettingsFragmentDirections.actionSettingsToAbout());
        } else if (id == R.id.action_help) {
          activity.showHelp();
        } else if (id == R.id.action_log) {
          activity.navigate(SettingsFragmentDirections.actionSettingsToLog());
        }
        return true;
      });
    });
    ViewUtil.setTooltipText(binding.buttonSettingsBack, R.string.action_back);
    ViewUtil.setTooltipText(binding.buttonSettingsMenu, R.string.action_more);

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
    binding.toggleSettingsTheme.addOnButtonCheckedListener(
        (group, checkedId, isChecked) -> {
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
          activity.restartToApply(200, getInstanceState(), true, false);
        });

    int idContrast;
    switch (getSharedPrefs().getString(PREF.UI_CONTRAST, DEF.UI_CONTRAST)) {
      case CONTRAST.MEDIUM:
        idContrast = R.id.button_settings_contrast_medium;
        break;
      case CONTRAST.HIGH:
        idContrast = R.id.button_settings_contrast_high;
        break;
      default:
        idContrast = R.id.button_settings_contrast_standard;
        break;
    }
    binding.toggleSettingsContrast.check(idContrast);
    binding.toggleSettingsContrast.addOnButtonCheckedListener(
        (group, checkedId, isChecked) -> {
          if (!isChecked) {
            return;
          }
          String pref;
          if (checkedId == R.id.button_settings_contrast_medium) {
            pref = CONTRAST.MEDIUM;
          } else if (checkedId == R.id.button_settings_contrast_high) {
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
    binding.toggleSettingsContrast.setEnabled(!isDynamic);
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
        if (getMetronomeEngine() != null) {
          getMetronomeEngine().stop();
        }
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

    languagesDialogUtil = new LanguagesDialogUtil(activity);
    languagesDialogUtil.showIfWasShown(savedInstanceState);

    gainDialogUtil = new GainDialogUtil(activity, this);
    gainDialogUtil.showIfWasShown(savedInstanceState);

    latencyDialogUtil = new LatencyDialogUtil(activity, this);
    latencyDialogUtil.showIfWasShown(savedInstanceState);

    backupDialogUtil = new BackupDialogUtil(activity, this);
    backupDialogUtil.showIfWasShown(savedInstanceState);

    updateMetronomeControls(true);

    ViewUtil.setOnClickListeners(
        this,
        binding.linearSettingsLanguage,
        binding.linearSettingsHaptic,
        binding.linearSettingsReduceAnimations,
        binding.linearSettingsBackup,
        binding.linearSettingsReset,
        binding.linearSettingsSound,
        binding.linearSettingsLatency,
        binding.linearSettingsIgnoreFocus,
        binding.linearSettingsGain,
        binding.linearSettingsActiveBeat,
        binding.linearSettingsPermNotification,
        binding.linearSettingsElapsed,
        binding.linearSettingsResetTimer,
        binding.linearSettingsBigTimeText,
        binding.linearSettingsBigLogo
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.switchSettingsHaptic,
        binding.switchSettingsReduceAnimations,
        binding.switchSettingsIgnoreFocus,
        binding.switchSettingsActiveBeat,
        binding.switchSettingsPermNotification,
        binding.switchSettingsElapsed,
        binding.switchSettingsResetTimer,
        binding.switchSettingsBigTimeText,
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
    if (languagesDialogUtil != null) {
      languagesDialogUtil.saveState(outState);
    }
    if (gainDialogUtil != null) {
      gainDialogUtil.saveState(outState);
    }
    if (latencyDialogUtil != null) {
      latencyDialogUtil.saveState(outState);
    }
    if (backupDialogUtil != null) {
      backupDialogUtil.saveState(outState);
    }
  }

  @Override
  public void updateMetronomeControls(boolean init) {
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    if (binding == null || metronomeEngine == null) {
      return;
    }
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
    int initItem = sounds.indexOf(metronomeEngine.getSound());
    if (initItem == -1) {
      initItem = 0;
      getSharedPrefs().edit().remove(PREF.SOUND).apply();
    }
    int initItemFinal = initItem;
    binding.textSettingsSound.setText(items[initItemFinal]);
    dialogUtilSound.createDialog(builder -> {
      builder.setTitle(R.string.settings_sound);
      builder.setSingleChoiceItems(
          items, initItemFinal, (dialog, which) -> {
            performHapticClick();
            if (getMetronomeEngine() != null) {
              getMetronomeEngine().setSound(sounds.get(which));
            }
            binding.textSettingsSound.setText(items[which]);
          }
      );
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> performHapticClick()
      );
    });
    dialogUtilSound.showIfWasShown(savedState);

    gainDialogUtil.showIfWasShown(savedState);
    latencyDialogUtil.showIfWasShown(savedState);

    binding.switchSettingsIgnoreFocus.setOnCheckedChangeListener(null);
    binding.switchSettingsIgnoreFocus.setChecked(metronomeEngine.getIgnoreAudioFocus());
    binding.switchSettingsIgnoreFocus.jumpDrawablesToCurrentState();
    binding.switchSettingsIgnoreFocus.setOnCheckedChangeListener(this);

    updateGainDescription(metronomeEngine.getGain());
    updateLatencyDescription(metronomeEngine.getLatency());

    binding.switchSettingsElapsed.setOnCheckedChangeListener(null);
    binding.switchSettingsElapsed.setChecked(metronomeEngine.getShowElapsed());
    binding.switchSettingsElapsed.jumpDrawablesToCurrentState();
    binding.switchSettingsElapsed.setOnCheckedChangeListener(this);

    binding.switchSettingsResetTimer.setOnCheckedChangeListener(null);
    binding.switchSettingsResetTimer.setChecked(metronomeEngine.getResetTimerOnStop());
    binding.switchSettingsResetTimer.jumpDrawablesToCurrentState();
    binding.switchSettingsResetTimer.setOnCheckedChangeListener(this);

    binding.toggleSettingsFlashScreen.removeOnButtonCheckedListener(this);
    int idFlashScreen;
    switch (getSharedPrefs().getString(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN)) {
      case FLASH_SCREEN.SUBTLE:
        idFlashScreen = R.id.button_settings_flash_screen_subtle;
        break;
      case FLASH_SCREEN.STRONG:
        idFlashScreen = R.id.button_settings_flash_screen_strong;
        break;
      default:
        idFlashScreen = R.id.button_settings_flash_screen_off;
        break;
    }
    binding.toggleSettingsFlashScreen.check(idFlashScreen);
    binding.toggleSettingsFlashScreen.jumpDrawablesToCurrentState();
    binding.toggleSettingsFlashScreen.addOnButtonCheckedListener(this);

    binding.toggleSettingsKeepAwake.removeOnButtonCheckedListener(this);
    int idKeepAwake;
    switch (getSharedPrefs().getString(PREF.KEEP_AWAKE, DEF.KEEP_AWAKE)) {
      case KEEP_AWAKE.WHILE_PLAYING:
        idKeepAwake = R.id.button_settings_keep_awake_while_playing;
        break;
      case KEEP_AWAKE.NEVER:
        idKeepAwake = R.id.button_settings_keep_awake_never;
        break;
      default:
        idKeepAwake = R.id.button_settings_keep_awake_always;
        break;
    }
    binding.toggleSettingsKeepAwake.check(idKeepAwake);
    binding.toggleSettingsKeepAwake.jumpDrawablesToCurrentState();
    binding.toggleSettingsKeepAwake.addOnButtonCheckedListener(this);

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
      languagesDialogUtil.show();
    } else if (id == R.id.linear_settings_haptic) {
      binding.switchSettingsHaptic.toggle();
    } else if (id == R.id.linear_settings_reduce_animations) {
      binding.switchSettingsReduceAnimations.toggle();
    } else if (id == R.id.linear_settings_backup && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      backupDialogUtil.show();
    } else if (id == R.id.linear_settings_reset && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      dialogUtilReset.show();
    } else if (id == R.id.linear_settings_sound && getViewUtil().isClickEnabled(id)) {
      ViewUtil.startIcon(binding.imageSettingsSound);
      performHapticClick();
      dialogUtilSound.show();
    } else if (id == R.id.linear_settings_latency && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      latencyDialogUtil.show();
    } else if (id == R.id.linear_settings_ignore_focus) {
      binding.switchSettingsIgnoreFocus.toggle();
    } else if (id == R.id.linear_settings_gain && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      gainDialogUtil.show();
    } else if (id == R.id.linear_settings_active_beat) {
      binding.switchSettingsActiveBeat.toggle();
    } else if (id == R.id.linear_settings_perm_notification) {
      binding.switchSettingsPermNotification.toggle();
    } else if (id == R.id.linear_settings_elapsed) {
      binding.switchSettingsElapsed.toggle();
    } else if (id == R.id.linear_settings_reset_timer) {
      binding.switchSettingsResetTimer.toggle();
    } else if (id == R.id.linear_settings_big_time_text) {
      binding.switchSettingsBigTimeText.toggle();
    } else if (id == R.id.linear_settings_big_logo) {
      binding.switchSettingsBigLogo.toggle();
    }
  }

  @Override
  public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    if (metronomeEngine == null) {
      return;
    }
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
      metronomeEngine.setIgnoreFocus(isChecked);
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
      }
    } else if (id == R.id.switch_settings_elapsed) {
      ViewUtil.startIcon(binding.imageSettingsElapsed);
      performHapticClick();
      metronomeEngine.setShowElapsed(isChecked);
    } else if (id == R.id.switch_settings_reset_timer) {
      ViewUtil.startIcon(binding.imageSettingsResetTimer);
      performHapticClick();
      metronomeEngine.setResetTimerOnStop(isChecked);
    } else if (id == R.id.switch_settings_big_time_text) {
      performHapticClick();
      getSharedPrefs().edit().putBoolean(PREF.BIG_TIME_TEXT, isChecked).apply();
    } else if (id == R.id.switch_settings_big_logo) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsBigLogo);
      getSharedPrefs().edit().putBoolean(PREF.BIG_LOGO, isChecked).apply();
    }
  }

  @Override
  public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
    MetronomeEngine metronomeEngine = activity.getMetronomeEngine();
    if (!isChecked || metronomeEngine == null) {
      return;
    }
    int id = group.getId();
    if (id == R.id.toggle_settings_flash_screen) {
      String flashScreen;
      if (checkedId == R.id.button_settings_flash_screen_subtle) {
        flashScreen = FLASH_SCREEN.SUBTLE;
      } else if (checkedId == R.id.button_settings_flash_screen_strong) {
        flashScreen = FLASH_SCREEN.STRONG;
      } else {
        flashScreen = FLASH_SCREEN.OFF;
      }
      metronomeEngine.setFlashScreen(flashScreen);
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsFlashScreen);
    } else if (id == R.id.toggle_settings_keep_awake) {
      String keepAwake;
      if (checkedId == R.id.button_settings_keep_awake_while_playing) {
        keepAwake = KEEP_AWAKE.WHILE_PLAYING;
      } else if (checkedId == R.id.button_settings_keep_awake_never) {
        keepAwake = KEEP_AWAKE.NEVER;
      } else {
        keepAwake = KEEP_AWAKE.ALWAYS;
      }
      metronomeEngine.setKeepAwake(keepAwake);
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsKeepAwake);
      boolean keepAwakeNow = keepAwake.equals(KEEP_AWAKE.ALWAYS)
          || (keepAwake.equals(KEEP_AWAKE.WHILE_PLAYING) && metronomeEngine.isPlaying());
      UiUtil.keepScreenAwake(activity, keepAwakeNow);
    }
  }

  public void updateGainDescription(int gain) {
    if (binding != null) {
      binding.textSettingsGain.setText(
          activity.getString(R.string.label_db_signed, gain > 0 ? "+" + gain : String.valueOf(gain))
      );
    }
  }

  public void updateLatencyDescription(long latency) {
    if (binding != null) {
      binding.textSettingsLatency.setText(
          activity.getString(R.string.label_ms, String.valueOf(latency))
      );
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