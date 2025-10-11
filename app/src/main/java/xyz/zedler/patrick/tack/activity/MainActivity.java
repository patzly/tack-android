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
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.Snackbar;
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
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.service.MetronomeService;
import xyz.zedler.patrick.tack.service.MetronomeService.MetronomeBinder;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.NotificationUtil;
import xyz.zedler.patrick.tack.util.PrefsUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.UnlockUtil;
import xyz.zedler.patrick.tack.util.dialog.FeedbackDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.HelpDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.TextDialogUtil;
import xyz.zedler.patrick.tack.viewmodel.SongViewModel;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

  private static final String TAG = MainActivity.class.getSimpleName();

  private ActivityMainBinding binding;
  private NavController navController;
  private NavHostFragment navHost;
  private SharedPreferences sharedPrefs;
  private HapticUtil hapticUtil;
  private Intent metronomeIntent;
  private MetronomeService metronomeService;
  private SongViewModel songViewModel;
  private TextDialogUtil textDialogUtilChangelog;
  private HelpDialogUtil helpDialogUtil;
  private FeedbackDialogUtil feedbackDialogUtil;
  private boolean runAsSuperClass, bound, stopServiceWithActivity, startMetronomeAfterPermission;
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
    UiUtil.applyColorHarmonization(this);

    Bundle bundleInstanceState = getIntent().getBundleExtra(EXTRA.INSTANCE_STATE);
    super.onCreate(bundleInstanceState != null ? bundleInstanceState : savedInstanceState);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    hapticUtil = new HapticUtil(this);

    songViewModel = new ViewModelProvider(this).get(SongViewModel.class);

    navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(
        R.id.fragment_main_nav_host
    );
    assert navHost != null;
    navController = navHost.getNavController();

    Intent intent = getIntent();
    if (intent != null && intent.getAction() != null) {
      String action = intent.getAction();
      if (action.equals(ACTION.SHOW_SONGS)) {
        navController.navigate(NavMainDirections.actionGlobalSongsFragment());
        // empty intent so orientation change does not show the song list again
        setIntent(new Intent());
      }
    }

    requestPermissionLauncher = registerForActivityResult(
        new RequestPermission(), isGranted -> {
          if (!isGranted && VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            getSharedPrefs().edit().putBoolean(PREF.PERMISSION_DENIED, true).apply();
            Snackbar snackbar = getSnackbar(
                R.string.msg_notification_permission_denied, 5000
            );
            snackbar.setAction(
                R.string.action_retry,
                v -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            );
            showSnackbar(snackbar);
          } else if (startMetronomeAfterPermission && getMetronomeEngine() != null) {
            getMetronomeEngine().start();
          }
          getSharedPrefs().edit().putBoolean(PREF.PERMISSION_DENIED, !isGranted).apply();
        });

    metronomeIntent = new Intent(this, MetronomeService.class);
    stopServiceWithActivity = true;

    textDialogUtilChangelog = new TextDialogUtil(
        this,
        R.string.about_changelog,
        R.raw.changelog,
        new String[]{"New:", "Improved:", "Fixed:"}
    );
    textDialogUtilChangelog.showIfWasShown(savedInstanceState);

    helpDialogUtil = new HelpDialogUtil(this);
    helpDialogUtil.showIfWasShown(savedInstanceState);

    feedbackDialogUtil = new FeedbackDialogUtil(this);
    feedbackDialogUtil.showIfWasShown(savedInstanceState);

    if (savedInstanceState == null && bundleInstanceState == null) {
      new Handler(Looper.getMainLooper()).postDelayed(
          this::showInitialDialogs, VERSION.SDK_INT >= 31 ? 950 : 0
      );
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (!runAsSuperClass) {
      binding = null;
      textDialogUtilChangelog.dismiss();

      if (isFinishing()) {
        // metronome should be stopped when app is removed from recent apps
        // stopServiceWithActivity is false when it's e. g. only a theme change
        if (stopServiceWithActivity) {
          stopService(metronomeIntent);
        }
      }
    }
  }

  @Override
  public void onStart() {
    super.onStart();

    if (!runAsSuperClass) {
      try {
        startService(metronomeIntent);
        // cannot use startForegroundService
        // would cause crash as notification is only displayed when app has notification permission
        bindService(metronomeIntent, this, Context.BIND_IMPORTANT);
      } catch (Exception e) {
        Log.e(TAG, "onStart: could not bind metronome service", e);
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
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);

    if (textDialogUtilChangelog != null) {
      textDialogUtilChangelog.saveState(outState);
    }
    if (helpDialogUtil != null) {
      helpDialogUtil.saveState(outState);
    }
    if (feedbackDialogUtil != null) {
      feedbackDialogUtil.saveState(outState);
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
    bound = true;
    BaseFragment current = getCurrentFragment();
    if (current != null) {
      current.updateMetronomeControls(false);
    }
  }

  @Override
  public void onServiceDisconnected(ComponentName componentName) {
    bound = false;
  }

  @Override
  public void onBindingDied(ComponentName name) {
    bound = false;
    unbindService(this);
    try {
      bindService(metronomeIntent, this, Context.BIND_AUTO_CREATE);
    } catch (IllegalStateException e) {
      Log.e(TAG, "onBindingDied: cannot start MetronomeService because app is in background");
    }
  }

  @Nullable
  public MetronomeService getMetronomeService() {
    return metronomeService;
  }

  @Nullable
  public MetronomeEngine getMetronomeEngine() {
    if (bound) {
      return metronomeService.getMetronomeEngine();
    }
    return null;
  }

  public SongViewModel getSongViewModel() {
    return songViewModel;
  }

  public void requestNotificationPermission(boolean startMetronome) {
    startMetronomeAfterPermission = startMetronome;
    boolean hasPermission = NotificationUtil.hasPermission(this);
    if (!hasPermission && VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
      try {
        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
      } catch (IllegalStateException e) {
        Log.e(TAG, "requestNotificationPermission: ", e);
      }
    }
  }

  @Nullable
  public BaseFragment getCurrentFragment() {
    if (navHost.getHost() != null) {
      return (BaseFragment) navHost.getChildFragmentManager().getFragments().get(0);
    }
    return null;
  }

  public void showSnackbar(@StringRes int resId) {
    if (binding != null) {
      showSnackbar(Snackbar.make(binding.coordinatorMain, resId, Snackbar.LENGTH_LONG));
    }
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

  public void restartToApply(
      long delay, @NonNull Bundle bundle, boolean restoreState, boolean stopService
  ) {
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      if (restoreState) {
        onSaveInstanceState(bundle);
      }
      stopServiceWithActivity = stopService;
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
      overridePendingTransition(R.anim.fade_in_restart, R.anim.fade_out_restart);
    }, delay);
  }

  private void showInitialDialogs() {
    // Changelog
    int versionNew = BuildConfig.VERSION_CODE;
    int versionOld = sharedPrefs.getInt(PREF.LAST_VERSION, 0);
    if (versionOld == 0) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
    } else if (versionOld != versionNew) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply();
      showChangelog();
    }

    // Feedback
    int feedbackCount = sharedPrefs.getInt(PREF.FEEDBACK_POP_UP_COUNT, 1);
    if (feedbackCount > 0) {
      if (feedbackCount < 5) {
        sharedPrefs.edit().putInt(PREF.FEEDBACK_POP_UP_COUNT, feedbackCount + 1).apply();
      } else {
        showFeedback();
      }
    }
  }

  public void showFeedback() {
    feedbackDialogUtil.show();
  }

  public void showChangelog() {
    textDialogUtilChangelog.show();
  }

  public void showHelp() {
    helpDialogUtil.show();
  }

  public boolean isUnlocked() {
    boolean checkUnlockKey = sharedPrefs.getBoolean(PREF.CHECK_UNLOCK_KEY, true);
    if (checkUnlockKey) {
      // also checks if Play Store is installed
      return UnlockUtil.isUnlocked(this);
    } else {
      return true;
    }
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

  public void performHapticReject(View view) {
    if (areHapticsAllowed()) {
      hapticUtil.hapticReject(view);
    }
  }

  public void performHapticTick() {
    if (areHapticsAllowed()) {
      hapticUtil.tick();
    }
  }

  public void performHapticSegmentTick(View view, boolean frequent) {
    if (areHapticsAllowed()) {
      hapticUtil.hapticSegmentTick(view, frequent);
    }
  }

  private boolean areHapticsAllowed() {
    if (getMetronomeEngine() != null) {
      return getMetronomeEngine().areHapticEffectsPossible(false);
    }
    return false;
  }
}
