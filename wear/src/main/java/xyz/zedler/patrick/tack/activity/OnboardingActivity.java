package xyz.zedler.patrick.tack.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.wearable.input.WearableButtons;
import android.view.KeyEvent;
import android.view.View;
import androidx.fragment.app.FragmentActivity;
import xyz.zedler.patrick.tack.databinding.ActivityOnboardingWearBinding;

public class OnboardingActivity extends FragmentActivity {

  private ActivityOnboardingWearBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityOnboardingWearBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    if (WearableButtons.getButtonCount(this) >= 2
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    ) {
      binding.imageFeatureFsb1.setImageDrawable(
          WearableButtons.getButtonIcon(this, KeyEvent.KEYCODE_STEM_1)
      );
      binding.imageFeatureFsb2.setImageDrawable(
          WearableButtons.getButtonIcon(this, KeyEvent.KEYCODE_STEM_2)
      );
    } else {
      binding.linearFeatureFsb.setVisibility(View.GONE);
    }

    binding.frameOk.setOnClickListener(v -> finish());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
  }
}
