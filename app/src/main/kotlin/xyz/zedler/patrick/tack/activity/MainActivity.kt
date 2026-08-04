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
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.BuildConfig
import xyz.zedler.patrick.tack.Constants.ACTION
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.EXTRA
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.NavMainDirections
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.databinding.ActivityMainBinding
import xyz.zedler.patrick.tack.fragment.BaseFragment
import xyz.zedler.patrick.tack.fragment.MainFragment
import xyz.zedler.patrick.tack.metronome.MetronomeEngine
import xyz.zedler.patrick.tack.service.MetronomeService
import xyz.zedler.patrick.tack.service.MetronomeService.MetronomeBinder
import xyz.zedler.patrick.tack.util.HapticUtil
import xyz.zedler.patrick.tack.util.NotificationUtil
import xyz.zedler.patrick.tack.util.PrefsUtil
import xyz.zedler.patrick.tack.util.applyColorHarmonization
import xyz.zedler.patrick.tack.util.isUnlocked as isUnlockedUtil
import xyz.zedler.patrick.tack.util.setTheme
import xyz.zedler.patrick.tack.util.dialog.FeedbackDialogUtil
import xyz.zedler.patrick.tack.util.dialog.HelpDialogUtil
import xyz.zedler.patrick.tack.util.dialog.TextDialogUtil
import xyz.zedler.patrick.tack.util.dialog.UnlockDialogUtil
import xyz.zedler.patrick.tack.viewmodel.SongViewModel
import androidx.core.content.edit
import kotlin.time.Duration.Companion.milliseconds

open class MainActivity : AppCompatActivity(), ServiceConnection {

  private var _binding: ActivityMainBinding? = null
  private val binding get() = _binding!!

  private var navController: NavController? = null
  private var navHost: NavHostFragment? = null

  lateinit var sharedPrefs: SharedPreferences
    private set

  lateinit var hapticUtil: HapticUtil
    private set

  private val metronomeIntent by lazy {
      Intent(this, MetronomeService::class.java)
  }
  private var metronomeService: MetronomeService? = null

  lateinit var songViewModel: SongViewModel
    private set

  private var textDialogUtilChangelog: TextDialogUtil? = null
  private var helpDialogUtil: HelpDialogUtil? = null
  private var feedbackDialogUtil: FeedbackDialogUtil? = null
  private var unlockDialogUtil: UnlockDialogUtil? = null

  private var runAsSuperClass = false
  private var bound = false
  private var stopServiceWithActivity = true
  private var startMetronomeAfterPermission = false
  private var restartJob: kotlinx.coroutines.Job? = null

  private val requestPermissionLauncher: ActivityResultLauncher<String> = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      sharedPrefs.edit { putBoolean(PREF.PERMISSION_DENIED, true) }
      val snackbar = getSnackbar(R.string.msg_notification_permission_denied, 5000)
      snackbar.setAction(R.string.action_retry) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
      showSnackbar(snackbar)
    } else if (startMetronomeAfterPermission) {
      metronomeEngine?.start()
    }
    sharedPrefs.edit { putBoolean(PREF.PERMISSION_DENIED, !isGranted) }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    runAsSuperClass = savedInstanceState?.getBoolean(
        EXTRA.RUN_AS_SUPER_CLASS, false
    ) ?: false

    if (runAsSuperClass) {
      super.onCreate(savedInstanceState)
      return
    }

    sharedPrefs = PrefsUtil(this).checkForMigrations().sharedPrefs

    // DARK MODE
    val modeNight = sharedPrefs.getInt(PREF.UI_MODE, DEF.UI_MODE)
    var uiMode = resources.configuration.uiMode
    when (modeNight) {
      AppCompatDelegate.MODE_NIGHT_NO -> uiMode = Configuration.UI_MODE_NIGHT_NO
      AppCompatDelegate.MODE_NIGHT_YES -> uiMode = Configuration.UI_MODE_NIGHT_YES
    }
    AppCompatDelegate.setDefaultNightMode(modeNight)

    // APPLY CONFIG TO RESOURCES
    // base
    val resBase = baseContext.resources
    val configBase = resBase.configuration
    configBase.uiMode = uiMode
    resBase.updateConfiguration(configBase, resBase.displayMetrics)
    // app
    val resApp = applicationContext.resources
    val configApp = resApp.configuration
    // Don't set uiMode here, won't let FOLLOW_SYSTEM apply correctly
    resApp.updateConfiguration(configApp, resources.displayMetrics)

    setTheme(sharedPrefs)
    applyColorHarmonization()

    val bundleInstanceState = intent.getBundleExtra(EXTRA.INSTANCE_STATE)
    super.onCreate(bundleInstanceState ?: savedInstanceState)

    _binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    hapticUtil = HapticUtil(this)
    hapticUtil.intensity = sharedPrefs.getString(
        PREF.VIBRATION_INTENSITY, DEF.VIBRATION_INTENSITY
    ) ?: DEF.VIBRATION_INTENSITY

    songViewModel = ViewModelProvider(this).get(SongViewModel::class.java)

    navHost =
      supportFragmentManager.findFragmentById(R.id.fragment_main_nav_host) as NavHostFragment?
    navController = navHost?.navController

    intent?.action?.let { action ->
      if (action == ACTION.SHOW_SONGS) {
        navController?.navigate(NavMainDirections.actionGlobalSongsFragment())
        // empty intent so orientation change does not show the song list again
        intent = Intent()
      }
    }

    stopServiceWithActivity = true

    textDialogUtilChangelog = TextDialogUtil(
      this,
      R.string.about_changelog,
      R.raw.changelog,
      arrayOf("New:", "Improved:", "Fixed:")
    ).apply { showIfWasShown(savedInstanceState) }

    helpDialogUtil = HelpDialogUtil(this).apply { showIfWasShown(savedInstanceState) }

    feedbackDialogUtil = FeedbackDialogUtil(this) { unlockDialogUtil?.show() }.apply {
      showIfWasShown(savedInstanceState)
    }

    unlockDialogUtil = UnlockDialogUtil(this).apply { showIfWasShown(savedInstanceState) }

    if (savedInstanceState == null && bundleInstanceState == null) {
      lifecycleScope.launch {
        if (Build.VERSION.SDK_INT >= 31) delay(950.milliseconds)
        showInitialDialogs()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    if (!runAsSuperClass) {
      _binding = null
      textDialogUtilChangelog?.dismiss()
      feedbackDialogUtil?.dismiss()
      helpDialogUtil?.dismiss()
      unlockDialogUtil?.dismiss()

      if (isFinishing) {
        // metronome should be stopped when app is removed from recent apps
        // stopServiceWithActivity is false when it's e.g. only a theme change
        if (stopServiceWithActivity) {
          stopService(metronomeIntent)
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()

    if (!runAsSuperClass) {
      try {
        startService(metronomeIntent)
        // cannot use startForegroundService
        // would cause crash as notification is only displayed when app has notification permission
        bindService(metronomeIntent, this, Context.BIND_IMPORTANT)
      } catch (e: Exception) {
        Log.e(TAG, "onStart: could not bind metronome service", e)
      }
    }
  }

  override fun onStop() {
    super.onStop()

    if (!runAsSuperClass && bound) {
      unbindService(this)
      bound = false
    }
  }

  override fun onResume() {
    super.onResume()

    if (!runAsSuperClass) {
      hapticUtil.setEnabled(
        sharedPrefs.getBoolean(
          PREF.HAPTIC, HapticUtil.areSystemHapticsTurnedOn(this)
        )
      )
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

    textDialogUtilChangelog?.saveState(outState)
    helpDialogUtil?.saveState(outState)
    feedbackDialogUtil?.saveState(outState)
    unlockDialogUtil?.saveState(outState)
  }

  override fun attachBaseContext(base: Context) {
    if (runAsSuperClass) {
      super.attachBaseContext(base)
    } else {
      val sharedPrefs = PrefsUtil(base).checkForMigrations().sharedPrefs
      // Night mode
      val modeNight = sharedPrefs.getInt(PREF.UI_MODE, DEF.UI_MODE)
      var uiMode = base.resources.configuration.uiMode
      when (modeNight) {
        AppCompatDelegate.MODE_NIGHT_NO -> uiMode = Configuration.UI_MODE_NIGHT_NO
        AppCompatDelegate.MODE_NIGHT_YES -> uiMode = Configuration.UI_MODE_NIGHT_YES
      }
      AppCompatDelegate.setDefaultNightMode(modeNight)
      // Apply config to resources
      val resources = base.resources
      val config = resources.configuration
      config.uiMode = uiMode
      resources.updateConfiguration(config, resources.displayMetrics)
      super.attachBaseContext(
          base.createConfigurationContext(config)
      )
    }
  }

  override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
    val binder = iBinder as MetronomeBinder
    metronomeService = binder.getService()
    bound = true
    currentFragment?.updateMetronomeControls(false)

    // warm up Oboe stream to avoid delay when starting the next time
    metronomeService?.metronomeEngine?.warmUpAudio()
  }

  override fun onServiceDisconnected(componentName: ComponentName) {
    bound = false
  }

  override fun onBindingDied(name: ComponentName) {
    bound = false
    unbindService(this)
    try {
      bindService(metronomeIntent, this, Context.BIND_AUTO_CREATE)
    } catch (e: IllegalStateException) {
      Log.e(TAG, "onBindingDied: cannot start MetronomeService because app is in background")
    }
  }

  fun getMetronomeService(): MetronomeService? = metronomeService

  val metronomeEngine: MetronomeEngine?
    get() = if (bound) metronomeService?.metronomeEngine else null

  val currentFragment: BaseFragment?
    get() = navHost?.childFragmentManager?.fragments?.getOrNull(0) as? BaseFragment

  fun requestNotificationPermission(startMetronome: Boolean) {
    startMetronomeAfterPermission = startMetronome
    val hasPermission = NotificationUtil.hasPermission(this)
    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      try {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } catch (e: IllegalStateException) {
        Log.e(TAG, "requestNotificationPermission: ", e)
      }
    }
  }

  fun showSnackbar(@StringRes resId: Int) {
    _binding?.let {
      showSnackbar(Snackbar.make(it.coordinatorMain, resId, Snackbar.LENGTH_LONG))
    }
  }

  fun showSnackbar(snackbar: Snackbar) {
    val current = currentFragment
    if (current is MainFragment) {
      current.showSnackbar(snackbar)
    } else {
      snackbar.show()
    }
  }

  fun getSnackbar(@StringRes resId: Int, duration: Int): Snackbar {
    return Snackbar.make(binding.coordinatorMain, getString(resId), duration)
  }

  fun navigate(directions: NavDirections) {
    val controller = navController
    if (controller == null) {
      Log.e(TAG, "navigate: controller is null")
      return
    }
    try {
      controller.navigate(directions)
    } catch (e: IllegalArgumentException) {
      Log.e(TAG, "navigate: $directions", e)
    }
  }

  fun navigateUp() {
    navController?.navigateUp() ?: Log.e(TAG, "navigateUp: controller is null")
  }

  fun restartToApply(
    delay: Long, bundle: Bundle, restoreState: Boolean, stopService: Boolean
  ) {
    restartJob?.cancel()
    restartJob = lifecycleScope.launch {
      delay(delay.milliseconds)
      if (isFinishing) return@launch
      if (restoreState) {
        try {
          onSaveInstanceState(bundle)
        } catch (e: Exception) {
          Log.e(TAG, "restartToApply: failed to save state", e)
        }
      }
      stopServiceWithActivity = stopService
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        finish()
      }
      val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
        if (restoreState) {
          putExtra(EXTRA.INSTANCE_STATE, bundle)
        }
      }
      startActivity(intent)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        finish()
      }
      overridePendingTransition(
          R.anim.fade_in_restart, R.anim.fade_out_restart
      )
    }
  }

  private fun showInitialDialogs() {
    // Changelog
    val versionNew = BuildConfig.VERSION_CODE
    val versionOld = sharedPrefs.getInt(PREF.LAST_VERSION, 0)
    if (versionOld == 0) {
      sharedPrefs.edit { putInt(PREF.LAST_VERSION, versionNew) }
    } else if (versionOld != versionNew) {
      sharedPrefs.edit().putInt(PREF.LAST_VERSION, versionNew).apply()
      showChangelogDialog()
    }

    // Feedback
    val feedbackCount = sharedPrefs.getInt(PREF.FEEDBACK_POP_UP_COUNT, 1)
    if (feedbackCount > 0) {
      if (feedbackCount < 5) {
        sharedPrefs.edit { putInt(PREF.FEEDBACK_POP_UP_COUNT, feedbackCount + 1) }
      } else {
        showFeedbackDialog()
      }
    }
  }

  fun showFeedbackDialog() {
    feedbackDialogUtil?.show()
  }

  fun showChangelogDialog() {
    textDialogUtilChangelog?.show()
  }

  fun showHelpDialog() {
    helpDialogUtil?.show()
  }

  fun showUnlockDialog() {
    unlockDialogUtil?.show()
  }

  fun isUnlocked(): Boolean {
    val checkUnlockKey = sharedPrefs.getBoolean(PREF.CHECK_UNLOCK_KEY, true)
    return if (checkUnlockKey) {
      // also checks if Play Store is installed
      isUnlockedUtil(this)
    } else {
      true
    }
  }

  fun performHapticClick() {
    if (areHapticsAllowed()) hapticUtil.click()
  }

  fun performHapticHeavyClick() {
    if (areHapticsAllowed()) hapticUtil.heavyClick()
  }

  fun performHapticReject(view: View) {
    if (areHapticsAllowed()) hapticUtil.hapticReject(view)
  }

  fun performHapticTick() {
    if (areHapticsAllowed()) hapticUtil.tick()
  }

  fun performHapticSegmentTick(view: View, frequent: Boolean) {
    if (areHapticsAllowed()) hapticUtil.hapticSegmentTick(view, frequent)
  }

  private fun areHapticsAllowed(): Boolean {
    return metronomeEngine?.areHapticEffectsPossible(false) ?: false
  }

  companion object {
    private val TAG = MainActivity::class.java.simpleName
  }
}
