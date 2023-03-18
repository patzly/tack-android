package xyz.zedler.patrick.tack.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.color.DynamicColors;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.THEME;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.util.PrefsUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends MainActivity {

  @Override
  public void onCreate(Bundle bundle) {
    if (Build.VERSION.SDK_INT >= 31) {
      super.onCreate(bundle);

      getSplashScreen().setOnExitAnimationListener(view -> {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(view, "alpha", 0),
            ObjectAnimator.ofFloat(view.getIconView(), "alpha", 0)
        );
        set.setDuration(400);
        set.setStartDelay(600);
        set.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(@NonNull Animator animation, boolean isReverse) {
            view.remove();
          }
        });
        set.start();
      });
    } else {
      SharedPreferences sharedPrefs = new PrefsUtil(this)
          .checkForMigrations()
          .getSharedPrefs();

      // DARK MODE

      int modeNight = sharedPrefs.getInt(PREF.MODE, DEF.MODE);
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
      // Apply config to resources
      Resources resBase = getBaseContext().getResources();
      Configuration configBase = resBase.getConfiguration();
      configBase.uiMode = uiMode;
      resBase.updateConfiguration(configBase, resBase.getDisplayMetrics());

      // THEME

      switch (sharedPrefs.getString(PREF.THEME, DEF.THEME)) {
        case THEME.RED:
          setTheme(R.style.Theme_Tack_Red);
          break;
        case THEME.YELLOW:
          setTheme(R.style.Theme_Tack_Yellow);
          break;
        case THEME.LIME:
          setTheme(R.style.Theme_Tack_Lime);
          break;
        case THEME.GREEN:
          setTheme(R.style.Theme_Tack_Green);
          break;
        case THEME.TURQUOISE:
          setTheme(R.style.Theme_Tack_Turquoise);
          break;
        case THEME.TEAL:
          setTheme(R.style.Theme_Tack_Teal);
          break;
        case THEME.BLUE:
          setTheme(R.style.Theme_Tack_Blue);
          break;
        case THEME.PURPLE:
          setTheme(R.style.Theme_Tack_Purple);
          break;
        default:
          if (DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivityIfAvailable(this);
          } else {
            setTheme(R.style.Theme_Tack_Yellow);
          }
          break;
      }

      if (bundle == null) {
        bundle = new Bundle();
      }
      bundle.putBoolean(EXTRA.RUN_AS_SUPER_CLASS, true);
      super.onCreate(bundle);

      new SystemBarBehavior(this).setUp();

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        LayerDrawable splashContent = (LayerDrawable) ResourcesCompat.getDrawable(
            getResources(), R.drawable.splash_content, getTheme()
        );
        getWindow().getDecorView().setBackground(splashContent);
        try {
          assert splashContent != null;
          ViewUtil.startIcon(splashContent.findDrawableByLayerId(R.id.splash_logo));
          new Handler(Looper.getMainLooper()).postDelayed(this::startNewMainActivity, 900);
        } catch (Exception e) {
          startNewMainActivity();
        }
      } else {
        startNewMainActivity();
      }
    }
  }

  @Override
  protected void attachBaseContext(Context base) {
    if (Build.VERSION.SDK_INT >= 31) {
      super.attachBaseContext(base);
      return;
    }
    SharedPreferences sharedPrefs = new PrefsUtil(base).checkForMigrations().getSharedPrefs();
    // Night mode
    int modeNight = sharedPrefs.getInt(PREF.MODE, DEF.MODE);
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

  private void startNewMainActivity() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    startActivity(intent);
    overridePendingTransition(0, R.anim.fade_out);
    finish();
  }
}
