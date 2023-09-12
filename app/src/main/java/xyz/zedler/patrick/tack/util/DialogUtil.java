package xyz.zedler.patrick.tack.util;

import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;

public class DialogUtil {

  private static final String IS_SHOWING = "is_showing_dialog_";

  private final MainActivity activity;
  private final String tag;
  private AlertDialog dialog;
  private Bundle savedInstanceState;

  public DialogUtil(@NonNull MainActivity activity, @NonNull String tag) {
    this.activity = activity;
    this.tag = tag;
  }

  public void createActionRaw(@StringRes int titleResId, @RawRes int msgResId,
      @StringRes int actionResId, @NonNull Runnable task
  ) {
    createAction(
        activity.getString(titleResId),
        ResUtil.getRawText(activity, msgResId),
        activity.getString(actionResId),
        task
    );
  }

  public void createAction(@StringRes int titleResId, @StringRes int msgResId,
      @StringRes int actionResId, @NonNull Runnable task
  ) {
    createAction(
        activity.getString(titleResId),
        activity.getString(msgResId),
        activity.getString(actionResId),
        task
    );
  }

  public void createAction(
      String title, String msg, @StringRes int actionResId, @NonNull Runnable task
  ) {
    createAction(
        title,
        msg,
        activity.getString(actionResId),
        task
    );
  }

  public void createAction(String title, String msg, String action, @NonNull Runnable task) {
    dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Tack_AlertDialog)
        .setTitle(title)
        .setMessage(msg)
        .setPositiveButton(action, (dialog, which) -> {
          activity.performHapticClick();
          task.run();
        })
        .setNegativeButton(R.string.action_cancel, (dialog, which) -> activity.performHapticClick())
        .setOnCancelListener(dialog -> activity.performHapticTick())
        .create();
  }

  public void createActions(
      @StringRes int titleResId, @StringRes int msgResId,
      @StringRes int actionPositiveResId, @NonNull Runnable taskPositive,
      @StringRes int actionNegativeResId, @NonNull Runnable taskNegative
  ) {
    createActions(
        activity.getString(titleResId),
        activity.getString(msgResId),
        activity.getString(actionPositiveResId),
        taskPositive,
        activity.getString(actionNegativeResId),
        taskNegative
    );
  }

  public void createActions(
      String title, String msg,
      String actionPositive, @NonNull Runnable taskPositive,
      String actionNegative, @NonNull Runnable taskNegative
  ) {
    dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Tack_AlertDialog)
        .setTitle(title)
        .setMessage(msg)
        .setPositiveButton(actionPositive, (dialog, which) -> {
          activity.performHapticClick();
          taskPositive.run();
        })
        .setNegativeButton(actionNegative, (dialog, which) -> {
          activity.performHapticClick();
          taskNegative.run();
        })
        .setOnCancelListener(dialog -> activity.performHapticTick())
        .create();
  }

  public void createClose(@StringRes int titleResId, @StringRes int msgResId) {
    createClose(activity.getString(titleResId), activity.getString(msgResId));
  }

  public void createCloseRaw(@StringRes int titleResId, @RawRes int msgResId) {
    createClose(activity.getString(titleResId), ResUtil.getRawText(activity, msgResId));
  }

  public void createClose(String title, String msg) {
    dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Tack_AlertDialog)
        .setTitle(title)
        .setMessage(msg)
        .setPositiveButton(R.string.action_close, (dialog, which) -> activity.performHapticClick())
        .setOnCancelListener(dialog -> activity.performHapticTick())
        .create();
  }

  public void createCloseCustom(@StringRes int titleResId, @NonNull View view) {
    dialog = new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_Tack_AlertDialog)
        .setTitle(titleResId)
        .setView(view)
        .setPositiveButton(R.string.action_close, (dialog, which) -> activity.performHapticClick())
        .setOnCancelListener(dialog -> activity.performHapticTick())
        .create();
  }

  public void createCaution(@StringRes int titleResId, @StringRes int msgResId,
      @StringRes int actionResId, Runnable action) {
    dialog = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Tack_AlertDialog_Caution
    )
        .setTitle(titleResId)
        .setMessage(msgResId)
        .setPositiveButton(actionResId, (dialog, which) -> {
          activity.performHapticHeavyClick();
          action.run();
        })
        .setNegativeButton(R.string.action_cancel, (dialog, which) -> activity.performHapticClick())
        .setOnCancelListener(dialog -> activity.performHapticTick())
        .create();
  }

  public void createMultiChoice(@StringRes int titleResId, @NonNull String[] choices,
      @NonNull boolean[] initial, @NonNull OnMultiChoiceClickListener listener) {
    dialog = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Tack_AlertDialog
    )
        .setTitle(titleResId)
        .setMultiChoiceItems(choices, initial, listener)
        .setPositiveButton(R.string.action_close, (dialog, which) -> activity.performHapticClick())
        .setOnCancelListener(dialog -> activity.performHapticTick())
        .create();
  }

  public void createSingleChoice(@StringRes int titleResId, @NonNull String[] choices,
      int initial, @NonNull OnClickListener listener) {
    dialog = new MaterialAlertDialogBuilder(
        activity, R.style.ThemeOverlay_Tack_AlertDialog
    )
        .setTitle(titleResId)
        .setSingleChoiceItems(choices, initial, listener)
        .setPositiveButton(R.string.action_close, (dialog, which) -> activity.performHapticClick())
        .setOnCancelListener(dialog -> activity.performHapticTick())
        .create();
  }

  public void show() {
    if (dialog != null && !dialog.isShowing()) {
      dialog.show();
    }
  }

  public boolean showIfWasShown(@Nullable Bundle state) {
    if (state != null && state.getBoolean(IS_SHOWING + tag)) {
      new Handler(Looper.getMainLooper()).postDelayed(this::show, 10);
      return true;
    } else {
      return false;
    }
  }

  public void showIfWasShown() {
    if (savedInstanceState != null && savedInstanceState.getBoolean(IS_SHOWING + tag)) {
      new Handler(Looper.getMainLooper()).postDelayed(this::show, 10);
    }
  }

  public void setSavedInstanceState(@Nullable Bundle state) {
    savedInstanceState = state;
  }

  public void saveState(@NonNull Bundle outState) {
    outState.putBoolean(IS_SHOWING + tag, dialog != null && dialog.isShowing());
  }

  /**
   * Call in onDestroy, else it will throw an exception
   */
  public void dismiss() {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
    }
  }

  public void setCancelable(boolean cancelable) {
    if (dialog != null) {
      dialog.setCancelable(cancelable);
      dialog.setCanceledOnTouchOutside(cancelable);
    }
  }
}
