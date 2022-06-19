package xyz.zedler.patrick.tack.activity;

import android.os.Bundle;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.ActivityChangelogWearBinding;
import xyz.zedler.patrick.tack.util.ResUtil;

public class ChangelogActivity extends FragmentActivity {

  private ActivityChangelogWearBinding binding;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTheme(R.style.Theme_Tack_Lime);

    binding = ActivityChangelogWearBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    binding.textChangelog.setText(
        ResUtil.getBulletList(
            this,
            "- ",
            ResUtil.getRawText(this, R.raw.changelog),
            getResources().getStringArray(R.array.changelog_highlights)
        ),
        TextView.BufferType.SPANNABLE
    );
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
  }
}
