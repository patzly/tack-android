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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogFeedbackBinding;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class FeedbackDialogUtil implements OnClickListener {

  private static final String TAG = FeedbackDialogUtil.class.getSimpleName();

  private final PartialDialogFeedbackBinding binding;
  private final DialogUtil dialogUtil;
  private final ViewUtil viewUtil;
  private final MainActivity activity;

  public FeedbackDialogUtil(MainActivity activity) {
    this.activity = activity;

    binding = PartialDialogFeedbackBinding.inflate(activity.getLayoutInflater());

    dialogUtil = new DialogUtil(activity, "feedback");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.title_feedback);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> activity.performHapticClick()
      );
      builder.setOnDismissListener(dialog -> {
        if (activity.getSharedPrefs().getInt(PREF.FEEDBACK_POP_UP_COUNT, 1) != 0) {
          activity.getSharedPrefs().edit().putInt(PREF.FEEDBACK_POP_UP_COUNT, 0).apply();
        }
      });
    });

    viewUtil = new ViewUtil();

    ViewUtil.setOnClickListeners(
        this,
        binding.linearFeedbackRate,
        binding.linearFeedbackIssue,
        binding.linearFeedbackEmail,
        binding.linearFeedbackRecommend
    );

    setDividerVisibility(!UiUtil.isOrientationPortrait(activity));
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (viewUtil.isClickDisabled(id)) {
      return;
    }
    activity.performHapticClick();
    if (id == R.id.linear_feedback_rate) {
      Uri uri = Uri.parse(
          "market://details?id=" + activity.getApplicationContext().getPackageName()
      );
      Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
      goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
          Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
          Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
          Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
      try {
        activity.startActivity(goToMarket);
      } catch (ActivityNotFoundException e) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
            "http://play.google.com/store/apps/details?id="
                + activity.getApplicationContext().getPackageName()
        )));
      }
    } else if (id == R.id.linear_feedback_issue) {
      String issues = activity.getString(R.string.app_github) + "/issues";
      activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(issues)));
    } else if (id == R.id.linear_feedback_email) {
      Intent intent = new Intent(Intent.ACTION_SENDTO);
      intent.setData(
          Uri.parse(
              "mailto:"
                  + activity.getString(R.string.app_mail)
                  + "?subject=" + Uri.encode("Feedback@Tack")
          )
      );
      activity.startActivity(
          Intent.createChooser(intent, activity.getString(R.string.action_send_feedback))
      );
    } else if (id == R.id.linear_feedback_recommend) {
      String text = activity.getString(
          R.string.msg_recommend, activity.getString(R.string.app_vending_app)
      );
      ResUtil.share(activity, text);
    }
    dismiss();
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
    if (binding == null) {
      return;
    }
    binding.scrollFeedback.scrollTo(0, 0);
    measureScrollView();
  }

  private void measureScrollView() {
    binding.scrollFeedback.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            boolean isScrollable = binding.scrollFeedback.canScrollVertically(-1)
                || binding.scrollFeedback.canScrollVertically(1);
            setDividerVisibility(isScrollable);
            binding.scrollFeedback.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  private void setDividerVisibility(boolean visible) {
    binding.dividerFeedbackTop.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.dividerFeedbackBottom.setVisibility(visible ? View.VISIBLE : View.GONE);
    binding.linearFeedbackContainer.setPadding(
        binding.linearFeedbackContainer.getPaddingLeft(),
        visible ? UiUtil.dpToPx(activity, 16) : 0,
        binding.linearFeedbackContainer.getPaddingRight(),
        visible ? UiUtil.dpToPx(activity, 16) : 0
    );
  }
}
