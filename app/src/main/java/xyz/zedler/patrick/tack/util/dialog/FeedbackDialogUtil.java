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

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogFeedbackBinding;
import xyz.zedler.patrick.tack.util.DialogUtil;

public class FeedbackDialogUtil {

  private static final String TAG = FeedbackDialogUtil.class.getSimpleName();

  private final PartialDialogFeedbackBinding binding;
  private final DialogUtil dialogUtil;

  public FeedbackDialogUtil(MainActivity activity) {
    binding = PartialDialogFeedbackBinding.inflate(activity.getLayoutInflater());

    dialogUtil = new DialogUtil(activity, "feedback");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.title_feedback);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> activity.performHapticClick()
      );
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

  private void update() {
    binding.scrollFeedback.scrollTo(0, 0);
  }
}
