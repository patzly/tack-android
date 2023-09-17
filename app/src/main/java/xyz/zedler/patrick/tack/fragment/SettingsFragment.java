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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;
import com.google.android.material.slider.Slider.OnSliderTouchListener;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
import xyz.zedler.patrick.tack.service.MetronomeService.MetronomeListener;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.LocaleUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.Tick;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.SelectionCardView;

public class SettingsFragment extends BaseFragment
    implements OnClickListener, OnCheckedChangeListener, OnChangeListener, MetronomeListener {

  private static final String TAG = SettingsFragment.class.getSimpleName();

  private FragmentSettingsBinding binding;
  private MainActivity activity;
  private DialogUtil dialogUtilReset, dialogUtilSound;
  private boolean flashScreen, wasPlaying, wasBeatModeVibrate, wasAlwaysVibrate;
  private int wasTempo, wasGain;
  String[] wasBeats, wasSubs;
  private Drawable itemBgFlash;

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
      } else if (id == R.id.action_recommend) {
        ResUtil.share(activity, R.string.msg_recommend);
      } else if (id == R.id.action_about) {
        activity.navigateToFragment(SettingsFragmentDirections.actionSettingsToAbout());
      }
      return true;
    });

    binding.textSettingsLanguage.setText(
        LocaleUtil.followsSystem()
            ? getString(R.string.settings_language_system)
            : LocaleUtil.getLocaleName()
    );

    setUpThemeSelection();

    int id;
    switch (getSharedPrefs().getInt(PREF.MODE, DEF.MODE)) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        id = R.id.button_other_theme_light;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
        id = R.id.button_other_theme_dark;
        break;
      default:
        id = R.id.button_other_theme_auto;
        break;
    }
    binding.toggleOtherTheme.check(id);
    binding.toggleOtherTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
      if (!isChecked) {
        return;
      }
      int pref;
      if (checkedId == R.id.button_other_theme_light) {
        pref = AppCompatDelegate.MODE_NIGHT_NO;
      } else if (checkedId == R.id.button_other_theme_dark) {
        pref = AppCompatDelegate.MODE_NIGHT_YES;
      } else {
        pref = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
      }
      getSharedPrefs().edit().putInt(PREF.MODE, pref).apply();
      performHapticClick();
      activity.restartToApply(0, getInstanceState(), true);
    });

    binding.partialOptionTransition.linearOptionTransition.setOnClickListener(
        v -> binding.partialOptionTransition.switchOptionTransition.setChecked(
            !binding.partialOptionTransition.switchOptionTransition.isChecked()
        )
    );
    binding.partialOptionTransition.switchOptionTransition.setChecked(
        getSharedPrefs().getBoolean(PREF.USE_SLIDING, DEF.USE_SLIDING)
    );
    binding.partialOptionTransition.switchOptionTransition.jumpDrawablesToCurrentState();

    binding.switchSettingsHaptic.setChecked(
        getSharedPrefs().getBoolean(PREF.HAPTIC, HapticUtil.areSystemHapticsTurnedOn(activity))
    );
    binding.switchSettingsHaptic.jumpDrawablesToCurrentState();
    binding.linearSettingsHaptic.setVisibility(
        activity.getHapticUtil().hasVibrator() ? View.VISIBLE : View.GONE
    );

    dialogUtilReset = new DialogUtil(activity, "reset");
    dialogUtilReset.createCaution(
        R.string.msg_reset,
        R.string.msg_reset_description,
        R.string.action_reset,
        () -> {
          if (isBound() && getMetronomeService().isPlaying()) {
            getMetronomeService().stop();
          }
          getSharedPrefs().edit().clear().apply();
          activity.restartToApply(100, getInstanceState(), false);
        });
    dialogUtilReset.showIfWasShown(savedInstanceState);

    dialogUtilSound = new DialogUtil(activity, "sound");
    Map<String, String> labels = new LinkedHashMap<>();
    labels.put(SOUND.WOOD, getString(R.string.settings_sound_wood));
    labels.put(SOUND.SINE, getString(R.string.settings_sound_sine));
    labels.put(SOUND.CLICK, getString(R.string.settings_sound_click));
    labels.put(SOUND.DING, getString(R.string.settings_sound_ding));
    labels.put(SOUND.BEEP, getString(R.string.settings_sound_beep));
    ArrayList<String> sounds = new ArrayList<>(labels.keySet());
    String[] items = labels.values().toArray(new String[]{});
    int init = sounds.indexOf(getSharedPrefs().getString(PREF.SOUND, DEF.SOUND));
    binding.textSettingsSound.setText(items[init]);
    dialogUtilSound.createSingleChoice(
        R.string.settings_sound, items, init, (dialog, which) -> {
          performHapticClick();
          if (isBoundOrShowWarning()) {
            getMetronomeService().setSound(sounds.get(which));
            binding.textSettingsSound.setText(items[which]);
          }
        });
    dialogUtilSound.showIfWasShown(savedInstanceState);

    binding.sliderSettingsLatency.setValue(getSharedPrefs().getLong(PREF.LATENCY, DEF.LATENCY));
    binding.sliderSettingsLatency.addOnChangeListener(this);
    binding.sliderSettingsLatency.addOnSliderTouchListener(new OnSliderTouchListener() {
      @Override
      public void onStartTrackingTouch(@NonNull Slider slider) {
        flashScreen = true;
        if (isBound()) {
          wasPlaying = getMetronomeService().isPlaying();
          wasTempo = getMetronomeService().getTempo();
          wasBeats = getMetronomeService().getBeats();
          wasSubs = getMetronomeService().getSubdivisions();
          wasBeatModeVibrate = getMetronomeService().isBeatModeVibrate();
          wasAlwaysVibrate = getMetronomeService().isAlwaysVibrate();
          wasGain = getMetronomeService().getGain();
          // Turn all visuals and audio on and start playing if not already started
          getMetronomeService().setTempo(80);
          getMetronomeService().setBeats(DEF.BEATS.split(","));
          getMetronomeService().setSubdivisions(DEF.SUBDIVISIONS.split(","));
          getMetronomeService().setBeatModeVibrate(false);
          getMetronomeService().setAlwaysVibrate(true);
          getMetronomeService().setGain(0);
          getMetronomeService().setMetronomeListener(SettingsFragment.this);
          if (!getMetronomeService().isPlaying()) {
            getMetronomeService().start();
          }
        }
      }

      @Override
      public void onStopTrackingTouch(@NonNull Slider slider) {
        flashScreen = false;
        if (isBound()) {
          if (!wasPlaying) {
            getMetronomeService().stop();
          }
          getMetronomeService().setTempo(wasTempo);
          getMetronomeService().setBeats(wasBeats);
          getMetronomeService().setSubdivisions(wasSubs);
          getMetronomeService().setBeatModeVibrate(wasBeatModeVibrate);
          getMetronomeService().setAlwaysVibrate(wasAlwaysVibrate);
          getMetronomeService().setGain(wasGain);
          getMetronomeService().setMetronomeListener(null);
        }
      }
    });
    binding.sliderSettingsLatency.setLabelFormatter(
        value -> getString(
            R.string.label_ms, String.format(activity.getLocale(), "%.0f", value)
        )
    );
    itemBgFlash = ViewUtil.getBgListItemSelected(activity, R.attr.colorPrimaryContainer);

    binding.sliderSettingsGain.setValue(getSharedPrefs().getInt(PREF.GAIN, DEF.GAIN));
    binding.sliderSettingsGain.addOnChangeListener(this);
    binding.sliderSettingsGain.setLabelFormatter(
        value -> getString(R.string.label_db, (int) value)
    );

    binding.switchSettingsAlwaysVibrate.setChecked(
        getSharedPrefs().getBoolean(PREF.ALWAYS_VIBRATE, DEF.ALWAYS_VIBRATE)
    );
    binding.switchSettingsAlwaysVibrate.jumpDrawablesToCurrentState();

    binding.switchSettingsFlashScreen.setChecked(
        getSharedPrefs().getBoolean(PREF.FLASH_SCREEN, DEF.FLASH_SCREEN)
    );
    binding.switchSettingsFlashScreen.jumpDrawablesToCurrentState();

    binding.switchSettingsKeepAwake.setChecked(
        getSharedPrefs().getBoolean(PREF.KEEP_AWAKE, DEF.KEEP_AWAKE)
    );
    binding.switchSettingsKeepAwake.jumpDrawablesToCurrentState();

    ViewUtil.setOnClickListeners(
        this,
        binding.linearSettingsLanguage,
        binding.linearSettingsHaptic,
        binding.linearSettingsReset,
        binding.linearSettingsSound,
        binding.linearSettingsAlwaysVibrate,
        binding.linearSettingsFlashScreen,
        binding.linearSettingsKeepAwake
    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.partialOptionTransition.switchOptionTransition,
        binding.switchSettingsHaptic,
        binding.switchSettingsAlwaysVibrate,
        binding.switchSettingsFlashScreen,
        binding.switchSettingsKeepAwake
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

  @Override
  public void onMetronomeStart() {}

  @Override
  public void onMetronomeStop() {}

  @Override
  public void onMetronomePreTick(Tick tick) {}

  @Override
  public void onMetronomeTick(Tick tick) {
    if (!isBound()) {
      return;
    }
    activity.runOnUiThread(() -> {
      if (binding == null) {
        return;
      }
      if (flashScreen) {
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
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_settings_language && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsLanguage);
      activity.navigate(SettingsFragmentDirections.actionSettingsToLanguagesDialog());
    } else if (id == R.id.linear_settings_haptic) {
      binding.switchSettingsHaptic.toggle();
    } else if (id == R.id.linear_settings_reset && getViewUtil().isClickEnabled(id)) {
      ViewUtil.startIcon(binding.imageSettingsReset);
      performHapticClick();
      dialogUtilReset.show();
    } else if (id == R.id.linear_settings_sound && getViewUtil().isClickEnabled(id)) {
      ViewUtil.startIcon(binding.imageSettingsSound);
      performHapticClick();
      dialogUtilSound.show();
    } else if (id == R.id.linear_settings_always_vibrate) {
      binding.switchSettingsAlwaysVibrate.toggle();
    } else if (id == R.id.linear_settings_flash_screen) {
      binding.switchSettingsFlashScreen.toggle();
    } else if (id == R.id.linear_settings_keep_awake) {
      binding.switchSettingsKeepAwake.toggle();
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();
    if (id == R.id.switch_option_transition) {
      performHapticClick();
      ViewUtil.startIcon(binding.partialOptionTransition.imageOptionTransition);
      getSharedPrefs().edit().putBoolean(PREF.USE_SLIDING, isChecked).apply();
    } else if (id == R.id.switch_settings_haptic) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsHaptic);
      getSharedPrefs().edit().putBoolean(PREF.HAPTIC, isChecked).apply();
      activity.getHapticUtil().setEnabled(isChecked);
    } else if (id == R.id.switch_settings_always_vibrate) {
      ViewUtil.startIcon(binding.imageSettingsAlwaysVibrate);
      if (isBoundOrShowWarning()) {
        getMetronomeService().setAlwaysVibrate(isChecked);
        performHapticClick();
      } else {
        performHapticClick();
        binding.switchSettingsAlwaysVibrate.setOnCheckedChangeListener(null);
        binding.switchSettingsAlwaysVibrate.setChecked(!isChecked);
        binding.switchSettingsAlwaysVibrate.setOnCheckedChangeListener(this);
      }
    } else if (id == R.id.switch_settings_flash_screen) {
      performHapticClick();
      //ViewUtil.startIcon(binding.imageSettingsFlashScreen);
      getSharedPrefs().edit().putBoolean(PREF.FLASH_SCREEN, isChecked).apply();
    } else if (id == R.id.switch_settings_keep_awake) {
      performHapticClick();
      ViewUtil.startIcon(binding.imageSettingsKeepAwake);
      getSharedPrefs().edit().putBoolean(PREF.KEEP_AWAKE, isChecked).apply();
    }
  }

  @Override
  public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
    if (!fromUser) {
      return;
    }
    int id = slider.getId();
    if (id == R.id.slider_settings_latency) {
      if (isBoundOrShowWarning()) {
        getMetronomeService().setLatency((long) value);
      }
      //ViewUtil.startIcon(binding.imageSettingsLatency);
      performHapticTick();
    } else if (id == R.id.slider_settings_gain) {
      if (isBoundOrShowWarning()) {
        getMetronomeService().setGain((int) value);
      }
      //ViewUtil.startIcon(binding.imageSettingsLatency);
      performHapticTick();
    }
  }

  private void setUpThemeSelection() {
    boolean hasDynamic = DynamicColors.isDynamicColorAvailable();
    ViewGroup container = binding.linearOtherThemeContainer;
    for (int i = hasDynamic ? -1 : 0; i < 8; i++) {
      String name;
      int resId;
      if (i == -1) {
        name = THEME.DYNAMIC;
        resId = -1;
      } else if (i == 0) {
        name = THEME.RED;
        resId = R.style.Theme_Tack_Red;
      } else if (i == 1) {
        name = THEME.YELLOW;
        resId = R.style.Theme_Tack_Yellow;
      } else if (i == 2) {
        name = THEME.LIME;
        resId = R.style.Theme_Tack_Lime;
      } else if (i == 3) {
        name = THEME.GREEN;
        resId = R.style.Theme_Tack_Green;
      } else if (i == 4) {
        name = THEME.TURQUOISE;
        resId = R.style.Theme_Tack_Turquoise;
      } else if (i == 5) {
        name = THEME.TEAL;
        resId = R.style.Theme_Tack_Teal;
      } else if (i == 6) {
        name = THEME.BLUE;
        resId = R.style.Theme_Tack_Blue;
      } else if (i == 7) {
        name = THEME.PURPLE;
        resId = R.style.Theme_Tack_Purple;
      } else {
        name = THEME.YELLOW;
        resId = R.style.Theme_Tack_Yellow;
      }

      SelectionCardView card = new SelectionCardView(activity);
      card.setEnsureContrast(false);
      int color = VERSION.SDK_INT >= VERSION_CODES.S && i == -1
          ? ContextCompat.getColor(
              activity,
              UiUtil.isDarkModeActive(activity)
                  ? android.R.color.system_accent1_700
                  : android.R.color.system_accent1_100
      ) : ResUtil.getColorAttr(
          new ContextThemeWrapper(activity, resId), R.attr.colorPrimaryContainer
      );
      card.setCardBackgroundColor(color);
      card.setOnClickListener(v -> {
        if (!card.isChecked()) {
          card.startCheckedIcon();
          ViewUtil.startIcon(binding.imageSettingsTheme);
          performHapticClick();
          ViewUtil.uncheckAllChildren(container);
          card.setChecked(true);
          getSharedPrefs().edit().putString(PREF.THEME, name).apply();
          activity.restartToApply(
              100, getInstanceState(), true
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
      binding.scrollHorizOtherTheme.scrollTo(
          bundleInstanceState.getInt(EXTRA.SCROLL_POSITION, 0),
          0
      );
    }
  }

  private boolean isBoundOrShowWarning() {
    boolean isBound = isBound();
    if (!isBound) {
      activity.showSnackbar(
          activity.getSnackbar(R.string.msg_connection_lost, Snackbar.LENGTH_SHORT)
      );
    }
    return isBound;
  }

  private Bundle getInstanceState() {
    Bundle bundle = new Bundle();
    if (binding != null) {
      bundle.putInt(EXTRA.SCROLL_POSITION, binding.scrollHorizOtherTheme.getScrollX());
    }
    return bundle;
  }
}