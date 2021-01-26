package xyz.zedler.patrick.tack;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import java.util.Locale;

import xyz.zedler.patrick.tack.databinding.ActivityChangelogBinding;
import xyz.zedler.patrick.tack.util.ResUtil;

public class ChangelogActivity extends FragmentActivity {

    private ActivityChangelogBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChangelogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String file = "changelog";
        String fileLocalized = file + "-" + Locale.getDefault().getLanguage();

        String localized = ResUtil.readFromFile(this, fileLocalized);
        localized = localized != null ? localized : ResUtil.readFromFile(this, file);

        binding.textChangelog.setText(
                Html.fromHtml(getAsHtml(localized)), TextView.BufferType.SPANNABLE
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private String getAsHtml(String text) {
        text = text.replaceAll("\n", "<br/>");
        String textNew = getString(R.string.changelog_new);
        text = text.replaceAll(textNew, getBold(textNew));
        String textImproved = getString(R.string.changelog_improved);
        text = text.replaceAll(textImproved, getBold(textImproved));
        String textFixed = getString(R.string.changelog_fixed);
        text = text.replaceAll(textFixed, getBold(textFixed));
        return text;
    }

    private String getBold(String text) {
        return "<b>" + text + "</b>";
    }
}
