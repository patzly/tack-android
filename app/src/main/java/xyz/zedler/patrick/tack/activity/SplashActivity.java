package xyz.zedler.patrick.tack.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.util.ViewUtil;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    new SystemBarBehavior(this).setUp();

    if (Build.VERSION.SDK_INT >= 31) {
      startSettingsActivity(false);
    } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
      LayerDrawable splashContent = (LayerDrawable) ResourcesCompat.getDrawable(
          getResources(), R.drawable.splash_content, null
      );
      getWindow().setBackgroundDrawable(splashContent);
      try {
        assert splashContent != null;
        ViewUtil.startIcon(splashContent.findDrawableByLayerId(R.id.splash_logo));
        new Handler(Looper.getMainLooper()).postDelayed(
            () -> startSettingsActivity(true), 1000
        );
      } catch (Exception e) {
        startSettingsActivity(true);
      }
    } else {
      startSettingsActivity(true);
    }
  }

  private void startSettingsActivity(boolean fadeOut) {
    startActivity(new Intent(this, MainActivity.class));
    overridePendingTransition(0, fadeOut ? R.anim.fade_out : 0);
    finish();
  }
}
