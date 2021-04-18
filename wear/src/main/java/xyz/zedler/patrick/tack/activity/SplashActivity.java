package xyz.zedler.patrick.tack.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

public class SplashActivity extends FragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    startActivity(new Intent(this, MainActivity.class));
    finish();
  }
}