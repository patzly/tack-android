package xyz.zedler.patrick.tack;

import android.os.Bundle;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import xyz.zedler.patrick.tack.databinding.ActivityChangelogBinding;
import xyz.zedler.patrick.tack.util.BulletUtil;
import xyz.zedler.patrick.tack.util.ResUtil;

public class ChangelogActivity extends FragmentActivity {

    private ActivityChangelogBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChangelogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.textChangelog.setText(
                BulletUtil.makeBulletList(
                        this,
                        6,
                        2,
                        "- ",
                        ResUtil.readFromFile(this, "changelog"),
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
