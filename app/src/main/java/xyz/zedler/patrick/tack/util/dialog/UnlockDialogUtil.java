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
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.UnlockUtil;

public class UnlockDialogUtil {

  private final DialogUtil dialogUtilUnlock;

  public UnlockDialogUtil(@NonNull MainActivity activity) {
    dialogUtilUnlock = new DialogUtil(activity, "unlock");
    dialogUtilUnlock.createDialog(builder -> {
      builder.setTitle(R.string.msg_unlock);
      builder.setMessage(R.string.msg_unlock_description);
      builder.setPositiveButton(
          R.string.action_open_play_store,
          (dialog, which) -> {
            activity.performHapticClick();
            UnlockUtil.openPlayStore(activity);
          });
      builder.setNegativeButton(
          R.string.action_cancel, (dialog, which) -> activity.performHapticClick()
      );
    });
  }

  public void show() {
    dialogUtilUnlock.show();
  }

  public void showIfWasShown(@Nullable Bundle state) {
    dialogUtilUnlock.showIfWasShown(state);
  }

  public void dismiss() {
    dialogUtilUnlock.dismiss();
  }

  public void saveState(@NonNull Bundle outState) {
    if (dialogUtilUnlock != null) {
      dialogUtilUnlock.saveState(outState);
    }
  }
}