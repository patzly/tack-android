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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.transition.MaterialSharedAxis;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class BaseFragment extends Fragment {

  private static final String TAG = BaseFragment.class.getSimpleName();

  private MainActivity activity;
  private ViewUtil viewUtil;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    activity = (MainActivity) requireActivity();
    viewUtil = new ViewUtil();

    setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
    setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    viewUtil.cleanUp();
  }

  @Nullable
  public MetronomeEngine getMetronomeEngine() {
    return activity.getMetronomeEngine();
  }

  public void updateMetronomeControls(boolean init) {}

  public SharedPreferences getSharedPrefs() {
    return activity.getSharedPrefs();
  }

  public ViewUtil getViewUtil() {
    return viewUtil;
  }

  public void navigateUp() {
    activity.navigateUp();
  }

  public void performHapticClick() {
    activity.performHapticClick();
  }

  public void performHapticTick() {
    activity.performHapticTick();
  }

  public void performHapticSegmentTick(View view, boolean frequent) {
    activity.performHapticSegmentTick(view, frequent);
  }

  public void performHapticHeavyClick() {
    activity.performHapticHeavyClick();
  }

  public OnClickListener getNavigationOnClickListener() {
    return v -> {
      if (viewUtil.isClickEnabled(v.getId())) {
        performHapticClick();
        navigateUp();
      }
    };
  }
}