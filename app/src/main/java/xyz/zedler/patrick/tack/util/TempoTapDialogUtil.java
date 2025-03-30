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

package xyz.zedler.patrick.tack.util;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.LinkedList;
import java.util.Queue;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.PartialDialogTempoTapBinding;
import xyz.zedler.patrick.tack.fragment.MainFragment;

public class TempoTapDialogUtil {

  private static final String TAG = TempoTapDialogUtil.class.getSimpleName();
  private static final int MAX_TAPS = 20;
  private static final double TEMPO_FACTOR = 0.5;
  private static final int INTERVAL_FACTOR = 3;

  private final Queue<Long> intervals = new LinkedList<>();
  private final MainActivity activity;
  private final MainFragment fragment;
  private final PartialDialogTempoTapBinding binding;
  private final DialogUtil dialogUtil;
  private long previous;

  @SuppressLint("ClickableViewAccessibility")
  public TempoTapDialogUtil(MainActivity activity, MainFragment fragment) {
    this.activity = activity;
    this.fragment = fragment;

    binding = PartialDialogTempoTapBinding.inflate(activity.getLayoutInflater());
    dialogUtil = new DialogUtil(activity, "tempo_tap");

    binding.textSwitcherTempoTapTempoTerm.setFactory(() -> {
      TextView textView = new TextView(activity);
      textView.setGravity(Gravity.CENTER_HORIZONTAL);
      textView.setTextSize(
          TypedValue.COMPLEX_UNIT_PX,
          activity.getResources().getDimension(R.dimen.label_text_size)
      );
      textView.setTextColor(ResUtil.getColor(activity, R.attr.colorOnTertiaryContainer));
      return textView;
    });

    binding.getRoot().setOnTouchListener((v, event) -> {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        binding.cloverTempoTap.setTapped(true);
        boolean enoughData = tap();
        if (enoughData) {
          setTempo(getMetronomeUtil().getTempo(), getTempo());
        }
        activity.performHapticHeavyClick();
        return true;
      } else if (event.getAction() == MotionEvent.ACTION_UP
          || event.getAction() == MotionEvent.ACTION_CANCEL) {
        binding.cloverTempoTap.setTapped(false);
      }
      return false;
    });

    dialogUtil.createDialog(builder -> {
      builder.setTitle(R.string.action_tempo_tap);
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

  public void update() {
    int tempo = getMetronomeUtil().getTempo();
    setTempo(tempo, tempo);
    binding.textSwitcherTempoTapTempoTerm.setCurrentText(fragment.getTempoTerm(tempo));
    binding.cloverTempoTap.setReduceAnimations(fragment.isReduceAnimations());
  }

  public boolean tap() {
    boolean enoughData = false;
    long current = System.currentTimeMillis();
    if (previous > 0) {
      enoughData = true;
      long interval = current - previous;
      if (!intervals.isEmpty() && shouldReset(interval)) {
        intervals.clear();
        enoughData = false;
      } else if (intervals.size() >= MAX_TAPS) {
        intervals.poll();
      }
      intervals.offer(interval);
    }
    previous = current;
    return enoughData;
  }

  private void setTempo(int tempoOld, int tempoNew) {
    if (binding == null || fragment == null || !fragment.isAdded()) {
      return;
    }
    fragment.setTempo(tempoNew);
    binding.textTempoTapTempo.setText(String.valueOf(tempoNew));
    String termNew = fragment.getTempoTerm(tempoNew);
    if (!termNew.equals(fragment.getTempoTerm(tempoOld))) {
      boolean isFaster = tempoNew > tempoOld;
      binding.textSwitcherTempoTapTempoTerm.setInAnimation(
          activity, isFaster ? R.anim.tempo_term_open_enter : R.anim.tempo_term_close_enter
      );
      binding.textSwitcherTempoTapTempoTerm.setOutAnimation(
          activity, isFaster ? R.anim.tempo_term_open_exit : R.anim.tempo_term_close_exit
      );
      binding.textSwitcherTempoTapTempoTerm.setText(termNew);
    }
  }

  public int getTempo() {
    return getTempo(getAverage());
  }

  private int getTempo(long interval) {
    if (interval > 0) {
      return (int) (60000 / interval);
    } else {
      return 0;
    }
  }

  private long getAverage() {
    long sum = 0;
    for (long interval : intervals) {
      sum += interval;
    }
    if (!intervals.isEmpty()) {
      return sum / intervals.size();
    } else {
      return 0;
    }
  }

  private boolean shouldReset(long interval) {
    return getTempo(interval) >= getTempo() * (1 + TEMPO_FACTOR)
        || getTempo(interval) <= getTempo() * (1 - TEMPO_FACTOR)
        || interval > getAverage() * INTERVAL_FACTOR;
  }

  private MetronomeUtil getMetronomeUtil() {
    return activity.getMetronomeUtil();
  }
}