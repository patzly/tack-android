/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior;
import xyz.zedler.patrick.tack.databinding.FragmentAboutBinding;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.dialog.TextDialogUtil;
import xyz.zedler.patrick.tack.util.dialog.UnlockDialogUtil;
import xyz.zedler.patrick.tack.util.UnlockUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class AboutFragment extends BaseFragment implements OnClickListener {

  private FragmentAboutBinding binding;
  private MainActivity activity;
  private TextDialogUtil textDialogUtilMdc, textDialogUtilMds, textDialogUtilNunito;
  private UnlockDialogUtil unlockDialogUtil;
  private int longClickCount = 0;

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
    textDialogUtilMdc.dismiss();
    textDialogUtilMds.dismiss();
    textDialogUtilNunito.dismiss();
    unlockDialogUtil.dismiss();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBarAbout);
    systemBarBehavior.setScroll(binding.scrollAbout, binding.linearAboutContainer);
    systemBarBehavior.setUp();

    new ScrollBehavior().setUpScroll(
        binding.appBarAbout, binding.scrollAbout, ScrollBehavior.LIFT_ON_SCROLL
    );

    binding.buttonAboutBack.setOnClickListener(getNavigationOnClickListener());
    binding.buttonAboutMenu.setOnClickListener(v -> {
      performHapticClick();
      ViewUtil.showMenu(v, R.menu.menu_about, item -> {
        int id = item.getItemId();
        if (getViewUtil().isClickDisabled(id)) {
          return false;
        }
        performHapticClick();
        if (id == R.id.action_feedback) {
          activity.showFeedback();
        } else if (id == R.id.action_help) {
          activity.showHelp();
        } else if (id == R.id.action_recommend) {
          String text = getString(R.string.msg_recommend, getString(R.string.app_vending_app));
          ResUtil.share(activity, text);
        }
        return true;
      });
    });
    ViewUtil.setTooltipText(binding.buttonAboutBack, R.string.action_back);
    ViewUtil.setTooltipText(binding.buttonAboutMenu, R.string.action_more);

    updateUnlockItem();

    unlockDialogUtil = new UnlockDialogUtil(activity);
    unlockDialogUtil.showIfWasShown(savedInstanceState);

    textDialogUtilMdc = new TextDialogUtil(
        activity,
        R.string.license_material_components,
        R.raw.license_apache,
        R.string.license_material_components_link
    );
    textDialogUtilMdc.showIfWasShown(savedInstanceState);

    textDialogUtilMds = new TextDialogUtil(
        activity,
        R.string.license_material_icons,
        R.raw.license_apache,
        R.string.license_material_icons_link
    );
    textDialogUtilMds.showIfWasShown(savedInstanceState);

    textDialogUtilNunito = new TextDialogUtil(
        activity,
        R.string.license_nunito,
        R.raw.license_ofl,
        R.string.license_nunito_link
    );
    textDialogUtilNunito.showIfWasShown(savedInstanceState);

    ViewUtil.setOnClickListeners(
        this,
        binding.linearAboutDeveloper,
        binding.linearAboutChangelog,
        binding.linearAboutVending,
        binding.linearAboutKey,
        binding.linearAboutGithub,
        binding.linearAboutTranslation,
        binding.linearAboutPrivacy,
        binding.linearAboutLicenseMaterialComponents,
        binding.linearAboutLicenseMaterialIcons,
        binding.linearAboutLicenseNunito
    );
  }

  @Override
  public void onResume() {
    super.onResume();
    updateUnlockItem();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (unlockDialogUtil != null) {
      unlockDialogUtil.saveState(outState);
    }
    if (textDialogUtilMdc != null) {
      textDialogUtilMdc.saveState(outState);
    }
    if (textDialogUtilMds != null) {
      textDialogUtilMds.saveState(outState);
    }
    if (textDialogUtilNunito != null) {
      textDialogUtilNunito.saveState(outState);
    }
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (getViewUtil().isClickDisabled(id)) {
      return;
    } else {
      performHapticClick();
    }

    if (id == R.id.linear_about_developer) {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_website))));
    } else if (id == R.id.linear_about_changelog) {
      ViewUtil.startIcon(binding.imageAboutChangelog);
      activity.showChangelog();
    } else if (id == R.id.linear_about_vending) {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_vending_dev))));
    } else if (id == R.id.linear_about_key) {
      if (UnlockUtil.isKeyInstalled(activity)) {
        UnlockUtil.openPlayStore(activity);
      } else {
        unlockDialogUtil.show();
      }
    } else if (id == R.id.linear_about_github) {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_github))));
    } else if (id == R.id.linear_about_translation) {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_translate))));
    } else if (id == R.id.linear_about_privacy) {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_privacy))));
    } else if (id == R.id.linear_about_license_nunito) {
      ViewUtil.startIcon(binding.imageAboutLicenseNunito);
      textDialogUtilNunito.show();
    } else if (id == R.id.linear_about_license_material_components) {
      ViewUtil.startIcon(binding.imageAboutLicenseMaterialComponents);
      textDialogUtilMdc.show();
    } else if (id == R.id.linear_about_license_material_icons) {
      ViewUtil.startIcon(binding.imageAboutLicenseMaterialIcons);
      textDialogUtilMds.show();
    }
  }

  private void updateUnlockItem() {
    boolean isPlayStoreInstalled = UnlockUtil.isPlayStoreInstalled(activity);
    binding.linearAboutKey.setVisibility(isPlayStoreInstalled ? View.VISIBLE : View.GONE);
    if (!isPlayStoreInstalled) {
      return;
    }
    if (activity.isUnlocked()) {
      binding.linearAboutKey.setOnLongClickListener(null);
    } else {
      binding.linearAboutKey.setOnLongClickListener(v -> {
        longClickCount++;
        if (longClickCount >= 10) {
          getSharedPrefs().edit().putBoolean(PREF.CHECK_UNLOCK_KEY, false).apply();
          updateUnlockItem();
          binding.linearAboutKey.setOnLongClickListener(null);
        }
        return true;
      });
    }
    int resId = R.string.about_key_description_not_installed;
    int textColor = ResUtil.getColor(activity, R.attr.colorOnSurfaceVariant);
    boolean checkUnlockKey = getSharedPrefs().getBoolean(PREF.CHECK_UNLOCK_KEY, true);
    if (UnlockUtil.isKeyInstalled(activity)) {
      resId = R.string.about_key_description_installed;
    } else if (!checkUnlockKey) {
      resId = R.string.about_key_description_ignored;
    }
    binding.textAboutKeyDescription.setText(resId);
    binding.textAboutKeyDescription.setTextColor(textColor);
  }
}