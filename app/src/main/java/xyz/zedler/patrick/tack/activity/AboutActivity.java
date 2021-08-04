package xyz.zedler.patrick.tack.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.EXTRA;
import xyz.zedler.patrick.tack.Constants.SETTINGS;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.ActivityAboutAppBinding;
import xyz.zedler.patrick.tack.fragment.ChangelogBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.fragment.TextBottomSheetDialogFragment;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {

  private ActivityAboutAppBinding binding;
  private SharedPreferences sharedPrefs;
  private ViewUtil viewUtil;
  private HapticUtil hapticUtil;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityAboutAppBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    viewUtil = new ViewUtil();
    hapticUtil = new HapticUtil(this);

    binding.frameAboutClose.setOnClickListener(v -> {
      if (viewUtil.isClickEnabled()) {
        hapticUtil.click();
        finish();
      }
    });

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(this);
    systemBarBehavior.setAppBar(binding.appBarAbout);
    systemBarBehavior.setScroll(binding.scrollAbout, binding.linearAboutContainer);
    systemBarBehavior.setUp();

    new ScrollBehavior(this).setUpScroll(
        binding.appBarAbout, binding.scrollAbout, true
    );

    ViewUtil.setOnClickListeners(
        this,
        binding.linearChangelog,
        binding.linearDeveloper,
        binding.linearLicenseEdwin,
        binding.linearLicenseJost,
        binding.linearLicenseMaterialComponents,
        binding.linearLicenseMaterialIcons,
        binding.linearLicenseMetronome
    );
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  protected void onResume() {
    super.onResume();
    hapticUtil.setEnabled(sharedPrefs.getBoolean(SETTINGS.HAPTIC_FEEDBACK, DEF.HAPTIC_FEEDBACK));
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_changelog && viewUtil.isClickEnabled()) {
      ViewUtil.startIcon(binding.imageChangelog);
      hapticUtil.click();
      BottomSheetDialogFragment fragment = new ChangelogBottomSheetDialogFragment();
      fragment.show(getSupportFragmentManager(), fragment.toString());
    } else if (id == R.id.linear_developer && viewUtil.isClickEnabled()) {
      ViewUtil.startIcon(binding.imageDeveloper);
      hapticUtil.click();
      new Handler(Looper.getMainLooper()).postDelayed(() -> startActivity(
          new Intent(
              Intent.ACTION_VIEW,
              Uri.parse("http://play.google.com/store/apps/dev?id=" +
                  "8122479227040208191"
              )
          )), 300
      );
    } else if (id == R.id.linear_license_edwin && viewUtil.isClickEnabled()) {
      ViewUtil.startIcon(binding.imageLicenseEdwin);
      hapticUtil.click();
      showTextBottomSheet(
          R.raw.ofl,
          R.string.license_edwin,
          R.string.license_edwin_link
      );
    } else if (id == R.id.linear_license_jost && viewUtil.isClickEnabled()) {
      ViewUtil.startIcon(binding.imageLicenseJost);
      hapticUtil.click();
      showTextBottomSheet(
          R.raw.ofl,
          R.string.license_jost,
          R.string.license_jost_link
      );
    } else if (id == R.id.linear_license_material_components && viewUtil.isClickEnabled()) {
      ViewUtil.startIcon(binding.imageLicenseMaterialComponents);
      hapticUtil.click();
      showTextBottomSheet(
          R.raw.apache,
          R.string.license_material_components,
          R.string.license_material_components_link
      );
    } else if (id == R.id.linear_license_material_icons && viewUtil.isClickEnabled()) {
      ViewUtil.startIcon(binding.imageLicenseMaterialIcons);
      hapticUtil.click();
      showTextBottomSheet(
          R.raw.apache,
          R.string.license_material_icons,
          R.string.license_material_icons_link
      );
    } else if (id == R.id.linear_license_metronome && viewUtil.isClickEnabled()) {
      ViewUtil.startIcon(binding.imageLicenseMetronome);
      hapticUtil.click();
      showTextBottomSheet(
          R.raw.apache,
          R.string.license_metronome,
          R.string.license_metronome_link
      );
    }
  }

  private void showTextBottomSheet(@RawRes int file, @StringRes int title, @StringRes int link) {
    Bundle bundle = new Bundle();
    bundle.putString(EXTRA.TITLE, getString(title));
    bundle.putInt(EXTRA.FILE, file);
    if (link != -1) {
      bundle.putString(EXTRA.LINK, getString(link));
    }
    ViewUtil.showBottomSheet(this, new TextBottomSheetDialogFragment(), bundle);
  }
}
