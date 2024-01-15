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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.fragment.dialog;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetFeedbackBinding;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class FeedbackBottomSheetDialogFragment extends BaseBottomSheetDialogFragment
    implements OnClickListener {

  private static final String TAG = "FeedbackBottomSheet";

  private FragmentBottomsheetFeedbackBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
    binding = FragmentBottomsheetFeedbackBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    activity = (MainActivity) requireActivity();

    binding.linearFeedbackRate.setBackground(ViewUtil.getRippleBgListItemSurface(activity));
    binding.linearFeedbackIssue.setBackground(ViewUtil.getRippleBgListItemSurface(activity));
    binding.linearFeedbackEmail.setBackground(ViewUtil.getRippleBgListItemSurface(activity));
    binding.linearFeedbackRecommend.setBackground(ViewUtil.getRippleBgListItemSurface(activity));

    ViewUtil.setOnClickListeners(
        this,
        binding.linearFeedbackRate,
        binding.linearFeedbackIssue,
        binding.linearFeedbackEmail,
        binding.linearFeedbackRecommend
    );
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);

    if (getSharedPrefs().getInt(PREF.FEEDBACK_POP_UP_COUNT, 1) != 0) {
      getSharedPrefs().edit().putInt(PREF.FEEDBACK_POP_UP_COUNT, 0).apply();
    }
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.linear_feedback_rate && getViewUtil().isClickEnabled(id)) {
      ViewUtil.startIcon(binding.imageFeedbackRate);
      performHapticClick();
      Uri uri = Uri.parse(
          "market://details?id=" + activity.getApplicationContext().getPackageName()
      );
      Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
      goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
          Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
          Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
          Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        try {
          startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
              "http://play.google.com/store/apps/details?id="
                  + activity.getApplicationContext().getPackageName()
          )));
        }
        dismiss();
      }, 400);
    } else if (id == R.id.linear_feedback_issue && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      String issues = getString(R.string.app_github) + "/issues";
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(issues)));
    } else if (id == R.id.linear_feedback_email && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      Intent intent = new Intent(Intent.ACTION_SENDTO);
      intent.setData(
          Uri.parse(
              "mailto:"
                  + getString(R.string.app_mail)
                  + "?subject=" + Uri.encode("Feedback@Tack")
          )
      );
      startActivity(Intent.createChooser(intent, getString(R.string.action_send_feedback)));
      dismiss();
    } else if (id == R.id.linear_feedback_recommend && getViewUtil().isClickEnabled(id)) {
      performHapticClick();
      String text = getString(R.string.msg_recommend, getString(R.string.app_vending_app));
      ResUtil.share(activity, text);
      dismiss();
    }
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.linearFeedbackContainer.setLayoutParams(params);
  }
}
