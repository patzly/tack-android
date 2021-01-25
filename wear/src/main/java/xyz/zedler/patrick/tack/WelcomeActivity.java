package xyz.zedler.patrick.tack;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.input.WearableButtons;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.fragment.app.FragmentActivity;

public class WelcomeActivity extends FragmentActivity {

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        if(WearableButtons.getButtonCount(this) >= 2
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        ) {
            ((ImageView) findViewById(R.id.image_feature_fsb1)).setImageDrawable(
                    WearableButtons.getButtonIcon(this, KeyEvent.KEYCODE_STEM_1)
            );
            ((ImageView) findViewById(R.id.image_feature_fsb2)).setImageDrawable(
                    WearableButtons.getButtonIcon(this, KeyEvent.KEYCODE_STEM_2)
            );
        } else {
            findViewById(R.id.linear_feature_fsb).setVisibility(View.GONE);
        }

        findViewById(R.id.frame_ok).setOnClickListener(v -> finish());
    }
}
