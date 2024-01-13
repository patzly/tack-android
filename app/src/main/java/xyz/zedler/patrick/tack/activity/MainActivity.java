package xyz.zedler.patrick.tack.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import xyz.zedler.patrick.tack.BuildConfig;
import xyz.zedler.patrick.tack.Constants.ACTION;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.NavMainDirections;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.ActivityMainBinding;
import xyz.zedler.patrick.tack.fragment.BaseFragment;
import xyz.zedler.patrick.tack.fragment.MainFragment;
import xyz.zedler.patrick.tack.fragment.SettingsFragment;
import xyz.zedler.patrick.tack.service.MetronomeService;
import xyz.zedler.patrick.tack.service.MetronomeService.MetronomeBinder;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.LocaleUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil;
import xyz.zedler.patrick.tack.util.MetronomeUtil.MetronomeListener;
import xyz.zedler.patrick.tack.util.NotificationUtil;
import xyz.zedler.patrick.tack.util.PrefsUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

  private static final String TAG = MainActivity.class.getSimpleName();

  private ActivityMainBinding binding;
  private NavController navController;
  private NavHostFragment navHost;
  private SharedPreferences sharedPrefs;
  private HapticUtil hapticUtil;
  private MetronomeUtil metronomeUtil;
  private Locale locale;
  private MetronomeService metronomeService;
  private boolean runAsSuperClass, bound;
  private ActivityResultLauncher<String> requestPermissionLauncher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    runAsSuperClass = savedInstanceState != null
        && savedInstanceState.getBoolean(EXTRA.RUN_AS_SUPER_CLASS, false);

    if (runAsSuperClass) {
      super.onCreate(savedInstanceState);
      return;
    }

    sharedPrefs = new PrefsUtil(this).checkForMigrations().getSharedPrefs();

    // DARK MODE

    int modeNight = sharedPrefs.getInt(PREF.UI_MODE, DEF.UI_MODE);
    int uiMode = getResources().getConfiguration().uiMode;
    switch (modeNight) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        uiMode = Configuration.UI_MODE_NIGHT_NO;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
        uiMode = Configuration.UI_MODE_NIGHT_YES;
        break;
    }
    AppCompatDelegate.setDefaultNightMode(modeNight);

    // APPLY CONFIG TO RESOURCES

    // base
    Resources resBase = getBaseContext().getResources();
    Configuration configBase = resBase.getConfiguration();
    configBase.uiMode = uiMode;
    resBase.updateConfiguration(configBase, resBase.getDisplayMetrics());
    // app
    Resources resApp = getApplicationContext().getResources();
    Configuration configApp = resApp.getConfiguration();
    // Don't set uiMode here, won't let FOLLOW_SYSTEM apply correctly
    resApp.updateConfiguration(configApp, getResources().getDisplayMetrics());

    UiUtil.setTheme(this, sharedPrefs);

    Bundle bundleInstanceState = getIntent().getBundleExtra(EXTRA.INSTANCE_STATE);
    super.onCreate(bundleInstanceState != null ? bundleInstanceState : savedInstanceState);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    hapticUtil = new HapticUtil(this);
    metronomeUtil = new MetronomeUtil(this, false);

    locale = LocaleUtil.getLocale();

    navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(
        R.id.fragment_main_nav_host
    );
    assert navHost != null;
    navController = navHost.getNavController();

    requestPermissionLauncher = registerForActivityResult(new RequestPermission(), isGranted -> {
      if (!isGranted && VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        Snackbar snackbar = getSnackbar(
            R.string.msg_notification_permission_denied, Snackbar.LENGTH_LONG
        );
        snackbar.setAction(
            R.string.action_retry,
            v -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        );
        showSnackbar(snackbar);
      }
    });

    updateMetronomeUtil();

    if (savedInstanceState == null && bundleInstanceState == null) {
      new Handler(Looper.getMainLooper()).postDelayed(
          this::showInitialBottomSheets, VERSION.SDK_INT >= 31 ? 950 : 0
      );
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (!runAsSuperClass) {
      binding = null;
      // metronome should be stopped when app is removed from recent apps
      if (getMetronomeUtil().isFromService()) {
        getMetronomeUtil().stop();
        getMetronomeUtil().destroy();
        sendBroadcast(new Intent(ACTION.STOP));
      }
    }
  }

  @Override
  public void onStart() {
    super.onStart();

    if (!runAsSuperClass) {
      try {
        Intent intent = new Intent(this, MetronomeService.class);
        startService(intent);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
      } catch (IllegalStateException e) {
        Log.e(TAG, "onStart: cannot start MetronomeService because app is in background");
      }
    }
  }

  @Override
  public void onStop() {
    super.onStop();

    if (!runAsSuperClass && bound) {
      unbindService(this);
      bound = false;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (!runAsSuperClass) {
      hapticUtil.setEnabled(
          sharedPrefs.getBoolean(PREF.HAPTIC, HapticUtil.areSystemHapticsTurnedOn(this))
      );
    }
  }

  @Override
  protected void attachBaseContext(Context base) {
    if (runAsSuperClass) {
      super.attachBaseContext(base);
    } else {
      SharedPreferences sharedPrefs = new PrefsUtil(base).checkForMigrations().getSharedPrefs();
      // Night mode
      int modeNight = sharedPrefs.getInt(PREF.UI_MODE, DEF.UI_MODE);
      int uiMode = base.getResources().getConfiguration().uiMode;
      switch (modeNight) {
        case AppCompatDelegate.MODE_NIGHT_NO:
          uiMode = Configuration.UI_MODE_NIGHT_NO;
          break;
        case AppCompatDelegate.MODE_NIGHT_YES:
          uiMode = Configuration.UI_MODE_NIGHT_YES;
          break;
      }
      AppCompatDelegate.setDefaultNightMode(modeNight);
      // Apply config to resources
      Resources resources = base.getResources();
      Configuration config = resources.getConfiguration();
      config.uiMode = uiMode;
      resources.updateConfiguration(config, resources.getDisplayMetrics());
      super.attachBaseContext(base.createConfigurationContext(config));
    }
  }

  @Override
  public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
    MetronomeBinder binder = (MetronomeBinder) iBinder;
    metronomeService = binder.getService();
    bound = metronomeService != null;
    updateMetronomeUtil();
    if (bound) {
      BaseFragment current = getCurrentFragment();
      if (current instanceof MainFragment) {
        ((MainFragment) current).updateMetronomeControls();
      } else if (current instanceof SettingsFragment) {
        ((SettingsFragment) current).updateMetronomeSettings();
      }
    }
  }

  @Override
  public void onServiceDisconnected(ComponentName componentName) {
    metronomeService = null;
    bound = false;
    updateMetronomeUtil();
  }

  public MetronomeUtil getMetronomeUtil() {
    if (bound) {
      return metronomeService.getMetronomeUtil();
    } else {
      return metronomeUtil;
    }
  }

  private void updateMetronomeUtil() {
    Set<MetronomeListener> listeners = new HashSet<>(metronomeUtil.getListeners());
    if (bound) {
      listeners.addAll(metronomeService.getMetronomeUtil().getListeners());
    }
    getMetronomeUtil().addListeners(listeners);
    getMetronomeUtil().setToPreferences();
  }

  public boolean hasNotificationPermission() {
    boolean hasPermission = NotificationUtil.hasPermission(this);
    if (!hasPermission && VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
      requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
    return hasPermission;
  }

  @Nullable
  public BaseFragment getCurrentFragment() {
    if (navHost.getHost() != null) {
      return (BaseFragment) navHost.getChildFragmentManager().getFragments().get(0);
    }
    return null;
  }

  public void showSnackbar(@StringRes int resId) {
    showSnackbar(Snackbar.make(binding.coordinatorMain, getString(resId), Snackbar.LENGTH_LONG));
  }

  public void showSnackbar(Snackbar snackbar) {
    BaseFragment current = getCurrentFragment();
    if (current instanceof MainFragment) {
      ((MainFragment) current).showSnackbar(snackbar);
    } else {
      snackbar.show();
    }
  }

  public Snackbar getSnackbar(@StringRes int resId, int duration) {
    return Snackbar.make(binding.coordinatorMain, getString(resId), duration);
  }

  public void navigate(NavDirections directions) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      navController.navigate(directions);
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: " + directions, e);
    }
  }

  public void navigateToFragment(NavDirections directions) {
    if (navController == null || directions == null) {
      Log.e(TAG, "navigate: controller or direction is null");
      return;
    }
    try {
      NavOptions.Builder builder = new NavOptions.Builder();
      if (UiUtil.areAnimationsEnabled(this)) {
        boolean useSliding = getSharedPrefs().getBoolean(PREF.USE_SLIDING, DEF.USE_SLIDING);
        builder.setEnterAnim(useSliding ? R.anim.open_enter_slide : R.animator.open_enter);
        builder.setExitAnim(useSliding ? R.anim.open_exit_slide : R.animator.open_exit);
        builder.setPopEnterAnim(useSliding ? R.anim.close_enter_slide : R.animator.close_enter);
        builder.setPopExitAnim(useSliding ? R.anim.close_exit_slide : R.animator.close_exit);
      } else {
        builder.setEnterAnim(-1).setExitAnim(-1).setPopEnterAnim(-1).setPopExitAnim(-1);
      }
      navController.navigate(directions, builder.build());
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "navigate: " + directions, e);
    }
  }

  public void navigateUp() {
    if (navController != null) {
      navController.navigateUp();
    } else {
      Log.e(TAG, "navigateUp: controller is null");
    }
  }

  public SharedPreferences getSharedPrefs() {
    return sharedPrefs;
  }

  public Locale getLocale() {
    return locale;
  }

  public void restartToApply(
      long delay, @NonNull Bundle bundle, boolean restoreState
  ) {
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      if (restoreState) {
        onSaveInstanceState(bundle);
      }
      if (VERSION.SDK_INT < VERSION_CODES.S) {
        finish();
      }
      Intent intent = new Intent(this, MainActivity.class);
      if (restoreState) {
        intent.putExtra(EXTRA.INSTANCE_STATE, bundle);
      }
      startActivity(intent);
      if (VERSION.SDK_INT >= VERSION_CODES.S) {
        finish();
      }
      if (UiUtil.areAnimationsEnabled(this)) {
        overridePendingTransition(R.anim.fade_in_restart, R.anim.fade_out_restart);
      } else {
        overridePendingTransition(0, 0);
      }
    }, delay);
  }

  private void showInitialBottomSheets() {
    // Changelog
    int versionNew = BuildConfig.VERSION_CODE;
    int versionOld = sharedPrefs.getInt(PREF.LAST_VERSION, 0);
    if (versionOld == 0) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
    } else if (versionOld != versionNew) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
      showChangelogBottomSheet();
    }

    // Feedback
    int feedbackCount = sharedPrefs.getInt(PREF.FEEDBACK_POP_UP_COUNT, 1);
    if (feedbackCount > 0) {
      if (feedbackCount < 5) {
        sharedPrefs.edit().putInt(PREF.FEEDBACK_POP_UP_COUNT, feedbackCount + 1).apply();
      } else {
        showFeedbackBottomSheet();
      }
    }
  }

  public void showTextBottomSheet(@RawRes int file, @StringRes int title, @StringRes int link) {
    NavMainDirections.ActionGlobalTextDialog action
        = NavMainDirections.actionGlobalTextDialog();
    action.setTitle(title);
    action.setFile(file);
    if (link != 0) {
      action.setLink(link);
    }
    navigate(action);
  }

  public void showFeedbackBottomSheet() {
    navigate(NavMainDirections.actionGlobalFeedbackDialog());
  }

  public void showChangelogBottomSheet() {
    NavMainDirections.ActionGlobalTextDialog action
        = NavMainDirections.actionGlobalTextDialog();
    action.setTitle(R.string.about_changelog);
    action.setFile(R.raw.changelog);
    action.setHighlights(new String[]{"New:", "Improved:", "Fixed:"});
    navigate(action);
  }

  public HapticUtil getHapticUtil() {
    return hapticUtil;
  }

  public void performHapticClick() {
    if (areHapticsAllowed()) {
      hapticUtil.click();
    }
  }

  public void performHapticHeavyClick() {
    if (areHapticsAllowed()) {
      hapticUtil.heavyClick();
    }
  }

  public void performHapticTick() {
    if (areHapticsAllowed()) {
      hapticUtil.tick();
    }
  }

  private boolean areHapticsAllowed() {
    return getMetronomeUtil().areHapticEffectsPossible();
  }
}
