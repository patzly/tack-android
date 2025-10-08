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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogTextBinding;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.ResUtil;

public class TextDialogUtil {

  private static final String TAG = TextDialogUtil.class.getSimpleName();

  private final PartialDialogTextBinding binding;
  private final DialogUtil dialogUtil;

  public TextDialogUtil(
      MainActivity activity,
      @StringRes int title,
      @RawRes int file,
      String[] highlights,
      @StringRes int link
  ) {
    binding = PartialDialogTextBinding.inflate(activity.getLayoutInflater());

    dialogUtil = new DialogUtil(activity, "text_" + title);
    dialogUtil.createDialog(builder -> {
      builder.setTitle(title);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> activity.performHapticClick()
      );
      if (link != 0) {
        builder.setNeutralButton(
            R.string.action_learn_more, (dialog, which) -> {
              activity.performHapticClick();
              activity.startActivity(
                  new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getString(link)))
              );
            }
        );
      }
    });

    binding.formattedText.setIsDialog(true);
    if (highlights != null) {
      binding.formattedText.setText(ResUtil.getRawText(activity, file), highlights);
    } else {
      binding.formattedText.setText(ResUtil.getRawText(activity, file));
    }
  }

  public TextDialogUtil(
      MainActivity activity,
      @StringRes int title,
      @RawRes int file,
      String[] highlights
  ) {
    this(activity, title, file, highlights, 0);
  }

  public TextDialogUtil(
      MainActivity activity,
      @StringRes int title,
      @RawRes int file,
      @StringRes int link
  ) {
    this(activity, title, file, null, link);
  }

  public TextDialogUtil(MainActivity activity, @StringRes int title, @RawRes int file) {
    this(activity, title, file, null, 0);
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

  private void update() {
    binding.scrollText.scrollTo(0, 0);
  }
}
