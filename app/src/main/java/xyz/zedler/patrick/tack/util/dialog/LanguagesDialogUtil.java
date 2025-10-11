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

package xyz.zedler.patrick.tack.util.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogLanguagesTitleBinding;
import xyz.zedler.patrick.tack.databinding.PartialDialogRecyclerBinding;
import xyz.zedler.patrick.tack.recyclerview.adapter.LanguageDialogAdapter;
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.LocaleUtil;

public class LanguagesDialogUtil {

  private static final String TAG = LanguagesDialogUtil.class.getSimpleName();

  private final PartialDialogLanguagesTitleBinding titleBinding;
  private final PartialDialogRecyclerBinding binding;
  private final DialogUtil dialogUtil;
  private final LanguageDialogAdapter adapter;

  public LanguagesDialogUtil(MainActivity activity) {
    titleBinding = PartialDialogLanguagesTitleBinding.inflate(activity.getLayoutInflater());

    binding = PartialDialogRecyclerBinding.inflate(activity.getLayoutInflater());
    dialogUtil = new DialogUtil(activity, "languages");

    binding.recyclerDialog.setLayoutManager(new WrapperLinearLayoutManager(activity));
    adapter = new LanguageDialogAdapter(
        LocaleUtil.getLanguages(activity),
        (languageCode, fromUser) -> {
          LocaleListCompat previous = AppCompatDelegate.getApplicationLocales();
          LocaleListCompat selected = LocaleListCompat.forLanguageTags(languageCode);
          if (!previous.equals(selected)) {
            if (fromUser) {
              activity.performHapticClick();
              setLanguageCode(languageCode);
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
              dismiss();
              AppCompatDelegate.setApplicationLocales(selected);
            }, 300);
          }
        }
    );
    binding.recyclerDialog.setAdapter(adapter);

    dialogUtil.createDialog(builder -> {
      builder.setCustomTitle(titleBinding.getRoot());
      builder.setView(binding.getRoot());
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> activity.performHapticClick()
      );
      builder.setNeutralButton(
          R.string.action_learn_more,
          (dialog, which) -> {
            activity.performHapticClick();
            activity.startActivity(
                new Intent(
                    Intent.ACTION_VIEW, Uri.parse(activity.getString(R.string.app_translate))
                )
            );
          });
    });
  }

  public void show() {
    update();
    dialogUtil.show();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    update();
    dialogUtil.showIfWasShown(state);
  }

  public void dismiss() {
    dialogUtil.dismiss();
  }

  public void saveState(@NonNull Bundle outState) {
    if (dialogUtil != null) {
      dialogUtil.saveState(outState);
    }
  }

  public void update() {
    if (binding == null || titleBinding == null) {
      return;
    }
    adapter.setLanguageCode(LocaleUtil.getLanguageCode(AppCompatDelegate.getApplicationLocales()));
    maybeShowDividers();
  }

  private void setLanguageCode(String languageCode) {
    adapter.setLanguageCode(languageCode);
  }

  private void maybeShowDividers() {
    binding.recyclerDialog.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.recyclerDialog.canScrollVertically(-1)
                || binding.recyclerDialog.canScrollVertically(1);
            binding.dividerDialogTop.setVisibility(isScrollable ? View.VISIBLE : View.GONE);
            binding.dividerDialogBottom.setVisibility(isScrollable ? View.VISIBLE : View.GONE);
            binding.recyclerDialog.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }
}