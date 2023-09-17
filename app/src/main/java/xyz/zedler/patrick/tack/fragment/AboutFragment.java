package xyz.zedler.patrick.tack.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.FragmentAboutBinding;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class AboutFragment extends BaseFragment implements OnClickListener {

  private FragmentAboutBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    binding = FragmentAboutBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarAbout);
    systemBarBehavior.setScroll(binding.scrollAbout, binding.constraintAbout);
    systemBarBehavior.setUp();

    new ScrollBehavior().setUpScroll(binding.appBarAbout, binding.scrollAbout, true);

    binding.toolbarAbout.setNavigationOnClickListener(getNavigationOnClickListener());
    binding.toolbarAbout.setOnMenuItemClickListener(item -> {
      int id = item.getItemId();
      if (getViewUtil().isClickDisabled(id)) {
        return false;
      }
      performHapticClick();
      if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_recommend) {
        ResUtil.share(activity, R.string.msg_recommend);
      }
      return true;
    });

    ViewUtil.setOnClickListeners(
        this,
        binding.linearAboutChangelog,
        binding.linearAboutDeveloper,
        binding.linearAboutVending,
        binding.linearAboutGithub,
        binding.linearAboutTranslation,
        binding.linearAboutPrivacy,
        binding.linearAboutLicenseJost,
        binding.linearAboutLicenseMaterialComponents,
        binding.linearAboutLicenseMaterialIcons
    );
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (getViewUtil().isClickDisabled(id)) {
      return;
    } else {
      performHapticClick();
    }

    if (id == R.id.linear_about_changelog) {
      ViewUtil.startIcon(binding.imageAboutChangelog);
      activity.showChangelogBottomSheet();
    } else if (id == R.id.linear_about_developer) {
      ViewUtil.startIcon(binding.imageAboutDeveloper);
      new Handler(Looper.getMainLooper()).postDelayed(
          () -> startActivity(
              new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_website)))
          ), 300
      );
    } else if (id == R.id.linear_about_vending) {
      ViewUtil.startIcon(binding.imageAboutVending);
      new Handler(Looper.getMainLooper()).postDelayed(
          () -> startActivity(
              new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_vending)))
          ), 300
      );
    } else if (id == R.id.linear_about_github) {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_github))));
    } else if (id == R.id.linear_about_translation) {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_translate))));
    } else if (id == R.id.linear_about_privacy) {
      ViewUtil.startIcon(binding.imageAboutPrivacy);
      new Handler(Looper.getMainLooper()).postDelayed(
          () -> startActivity(
              new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_privacy)))
          ), 300
      );
    } else if (id == R.id.linear_about_license_jost) {
      ViewUtil.startIcon(binding.imageAboutLicenseJost);
      activity.showTextBottomSheet(
          R.raw.license_ofl, R.string.license_jost, R.string.license_jost_link
      );
    } else if (id == R.id.linear_about_license_material_components) {
      ViewUtil.startIcon(binding.imageAboutLicenseMaterialComponents);
      activity.showTextBottomSheet(
          R.raw.license_apache,
          R.string.license_material_components,
          R.string.license_material_components_link
      );
    } else if (id == R.id.linear_about_license_material_icons) {
      ViewUtil.startIcon(binding.imageAboutLicenseMaterialIcons);
      activity.showTextBottomSheet(
          R.raw.license_apache,
          R.string.license_material_icons,
          R.string.license_material_icons_link
      );
    }
  }
}