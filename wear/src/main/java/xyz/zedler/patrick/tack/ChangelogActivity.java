package xyz.zedler.patrick.tack;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import java.util.Locale;

import xyz.zedler.patrick.tack.databinding.ActivityChangelogBinding;
import xyz.zedler.patrick.tack.util.ResUtil;

public class ChangelogActivity extends FragmentActivity
        implements AmbientModeSupport.AmbientCallbackProvider {

    private ActivityChangelogBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChangelogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String file = "changelog";
        String fileLocalized = file + "-" + Locale.getDefault().getLanguage();

        String localized = ResUtil.readFromFile(this, fileLocalized);
        binding.textChangelog.setText(
                localized != null
                        ? localized
                        : ResUtil.readFromFile(this, file)
        );

        AmbientModeSupport.attach(this);
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new AmbientModeSupport.AmbientCallback() {

            public void onEnterAmbient(Bundle ambientDetails) { }

            public void onExitAmbient(Bundle ambientDetails) { }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
