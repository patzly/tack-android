package xyz.zedler.patrick.tack.fragment;

import android.annotation.SuppressLint;
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
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.THEME;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.FragmentSettingsAppBinding;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.view.SelectionCardView;

public class SettingsFragment extends BaseFragment
    implements OnClickListener, OnCheckedChangeListener {

  private static final String TAG = SettingsFragment.class.getSimpleName();

  private FragmentSettingsAppBinding binding;
  private MainActivity activity;
  private DialogUtil dialogUtilReset;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentSettingsAppBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
    dialogUtilReset.dismiss();
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
      if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_recommend) {
        ResUtil.share(activity, R.string.msg_recommend);
      } else if (id == R.id.action_about) {
        activity.navigateToFragment(SettingsFragmentDirections.actionSettingsToAbout());
      }
      performHapticClick();
      return true;
    });

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
          getSharedPrefs().edit().clear().apply();
          activity.restartToApply(100, getInstanceState(), false);
        });
    dialogUtilReset.showIfWasShown(savedInstanceState);

    /*ViewUtil.setOnClickListeners(
        this,

    );

    ViewUtil.setOnCheckedChangeListeners(
        this,
        binding.partialOptionGerman.switchOptionGerman,
        binding.partialOptionTransition.switchOptionTransition,
        binding.switchSettingsShowInfo,
        binding.switchSettingsShowStreak,
        binding.partialOptionLeftSteno.switchOptionLeftSteno,
        binding.switchSettingsHaptic,
        binding.switchSettingsReminder,
        binding.switchSettingsVoiceOutput,
        binding.switchSettingsVerticalGuides,
        binding.switchSettingsShowArrows,
        binding.switchSettingsUseTflite,
        binding.switchSettingsAutoEvaluation,
        binding.switchSettingsAskDotted
    );*/
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (dialogUtilReset != null) {
      dialogUtilReset.saveState(outState);
    }
  }

  @SuppressLint("ShowToast")
  @Override
  public void onClick(View v) {
    int id = v.getId();

  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    int id = buttonView.getId();

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

  private Bundle getInstanceState() {
    Bundle bundle = new Bundle();
    if (binding != null) {
      bundle.putInt(EXTRA.SCROLL_POSITION, binding.scrollHorizOtherTheme.getScrollX());
    }
    return bundle;
  }
}