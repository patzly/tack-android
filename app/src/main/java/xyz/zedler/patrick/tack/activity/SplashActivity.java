package xyz.zedler.patrick.tack.activity;

import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;

public class SplashActivity extends AppCompatActivity {

  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    new SystemBarBehavior(this).setUp();

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      startMainActivity();
      return;
    }

    LayerDrawable splashContent = (LayerDrawable) ResourcesCompat.getDrawable(
        getResources(), R.drawable.splash_content, null
    );

    getWindow().setBackgroundDrawable(splashContent);

    try {
      assert splashContent != null;
      Drawable splashLogo = splashContent.findDrawableByLayerId(R.id.splash_logo);
      AnimatedVectorDrawable logo = (AnimatedVectorDrawable) splashLogo;
      logo.start();
      new Handler(Looper.getMainLooper()).postDelayed(
          this::startMainActivity, 1000
      );
    } catch (Exception e) {
      startMainActivity();
    }
  }

  private void startMainActivity() {
    startActivity(new Intent(this, MainActivity.class));
    overridePendingTransition(0, R.anim.fade_out);
    finish();
  }
}
