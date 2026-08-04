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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import xyz.zedler.patrick.tack.Constants.DEF
import xyz.zedler.patrick.tack.Constants.EXTRA
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior
import xyz.zedler.patrick.tack.util.PrefsUtil
import xyz.zedler.patrick.tack.util.setTheme
import xyz.zedler.patrick.tack.util.start
import java.time.Instant
import java.time.temporal.ChronoUnit

@SuppressLint("CustomSplashScreen")
class SplashActivity : MainActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    val sharedPrefs = PrefsUtil(this)
      .checkForMigrations()
      .sharedPrefs

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
      super.onCreate(savedInstanceState)

      splashScreen.setOnExitAnimationListener { view ->
        val startTime = view.iconAnimationStart
        val animator = ObjectAnimator.ofFloat(
          view, "alpha", 0f
        )
        animator.duration = 250
        animator.startDelay = startTime?.let {
          (900 - it.until(Instant.now(), ChronoUnit.MILLIS))
            .coerceAtLeast(0)
        } ?: 0
        animator.addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
            view.remove()
          }
        })
        animator.start()
      }
    } else {
      // DARK MODE
      val modeNight = sharedPrefs.getInt(PREF.UI_MODE, DEF.UI_MODE)
      var uiMode = resources.configuration.uiMode
      when (modeNight) {
        AppCompatDelegate.MODE_NIGHT_NO -> uiMode = Configuration.UI_MODE_NIGHT_NO
        AppCompatDelegate.MODE_NIGHT_YES -> uiMode = Configuration.UI_MODE_NIGHT_YES
      }
      AppCompatDelegate.setDefaultNightMode(modeNight)
      // Apply config to resources
      val resBase = baseContext.resources
      val configBase = resBase.configuration
      configBase.uiMode = uiMode
      resBase.updateConfiguration(configBase, resBase.displayMetrics)

      // THEME
      setTheme(sharedPrefs)

      val finalBundle = savedInstanceState ?: Bundle()
      finalBundle.putBoolean(EXTRA.RUN_AS_SUPER_CLASS, true)
      super.onCreate(finalBundle)

      SystemBarBehavior(this).setUp()

      val splashContent = ResourcesCompat.getDrawable(
        resources, R.drawable.splash_content, theme
      ) as? LayerDrawable
      window.decorView.background = splashContent
      try {
        checkNotNull(splashContent)
        splashContent.findDrawableByLayerId(R.id.splash_logo).start()
        Handler(Looper.getMainLooper()).postDelayed(
          { startNewMainActivity() }, 600
        )
      } catch (e: Exception) {
        startNewMainActivity()
      }
    }
  }

  override fun attachBaseContext(base: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      super.attachBaseContext(base)
      return
    }
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
    super.attachBaseContext(base.createConfigurationContext(config))
  }

  private fun startNewMainActivity() {
    val intent = Intent(this, MainActivity::class.java)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    startActivity(intent)
    overridePendingTransition(0, R.anim.fade_out)
    finish()
  }
}
