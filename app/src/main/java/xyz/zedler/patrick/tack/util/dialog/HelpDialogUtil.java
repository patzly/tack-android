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
import android.view.View;
import android.view.View.OnClickListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.AutoTransition;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogHelpBinding;
import xyz.zedler.patrick.tack.util.DialogUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class HelpDialogUtil implements OnClickListener {

  private static final String TAG = HelpDialogUtil.class.getSimpleName();

  private final PartialDialogHelpBinding binding;
  private final DialogUtil dialogUtil;
  private final MainActivity activity;

  public HelpDialogUtil(MainActivity activity) {
    this.activity = activity;
    binding = PartialDialogHelpBinding.inflate(activity.getLayoutInflater());

    dialogUtil = new DialogUtil(activity, "help");
    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.title_help);
      builder.setView(binding.getRoot());
      builder.setPositiveButton(
          R.string.action_close, (dialog, which) -> activity.performHapticClick()
      );
    });

    ViewUtil.setOnClickListeners(
        this,
        binding.linearHelpQuestion1,
        binding.linearHelpQuestion2,
        binding.linearHelpQuestion3,
        binding.linearHelpQuestion4,
        binding.linearHelpQuestion5,
        binding.linearHelpQuestion6,
        binding.linearHelpQuestion7,
        binding.linearHelpQuestion8,
        binding.linearHelpQuestion9,
        binding.buttonHelpTranslate
    );
  }

  @Override
  public void onClick(View v) {
    activity.performHapticClick();

    int id = v.getId();
    if (id == R.id.linear_help_question1) {
      toggleAnswerVisibility(binding.textHelpAnswer1);
    } else if (id == R.id.linear_help_question2) {
      toggleAnswerVisibility(binding.textHelpAnswer2);
    } else if (id == R.id.linear_help_question3) {
      toggleAnswerVisibility(binding.textHelpAnswer3);
    } else if (id == R.id.linear_help_question4) {
      toggleAnswerVisibility(binding.textHelpAnswer4);
    } else if (id == R.id.linear_help_question5) {
      toggleAnswerVisibility(binding.textHelpAnswer5);
    } else if (id == R.id.linear_help_question6) {
      toggleAnswerVisibility(binding.textHelpAnswer6);
    } else if (id == R.id.linear_help_question7) {
      toggleAnswerVisibility(binding.textHelpAnswer7);
    } else if (id == R.id.linear_help_question8) {
      toggleAnswerVisibility(binding.textHelpAnswer8);
    } else if (id == R.id.linear_help_question9) {
      toggleAnswerVisibility(binding.textHelpAnswer9);
      binding.buttonHelpTranslate.setVisibility(
          binding.buttonHelpTranslate.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
      );
    } else if (id == R.id.button_help_translate) {
      activity.startActivity(
          new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getString(R.string.app_translate)))
      );
    }
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
    binding.scrollHelp.scrollTo(0, 0);

    binding.textHelpAnswer1.setVisibility(View.GONE);
    binding.textHelpAnswer2.setVisibility(View.GONE);
    binding.textHelpAnswer3.setVisibility(View.GONE);
    binding.textHelpAnswer4.setVisibility(View.GONE);
    binding.textHelpAnswer5.setVisibility(View.GONE);
    binding.textHelpAnswer6.setVisibility(View.GONE);
    binding.textHelpAnswer7.setVisibility(View.GONE);
    binding.textHelpAnswer8.setVisibility(View.GONE);
    binding.textHelpAnswer9.setVisibility(View.GONE);
    binding.buttonHelpTranslate.setVisibility(View.GONE);
  }

  private void toggleAnswerVisibility(View answerView) {
    Transition transition = new AutoTransition();
    transition.setDuration(Constants.ANIM_DURATION_SHORT);
    TransitionManager.beginDelayedTransition(binding.linearHelpContainer, transition);
    answerView.setVisibility(
        answerView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
    );
  }
}
