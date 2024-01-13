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

package xyz.zedler.patrick.tack.activity;

import android.os.Bundle;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.ActivityChangelogWearBinding;
import xyz.zedler.patrick.tack.util.ResUtil;

public class ChangelogActivity extends FragmentActivity {

  private ActivityChangelogWearBinding binding;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTheme(R.style.Theme_Tack_Lime);

    binding = ActivityChangelogWearBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    binding.textChangelog.setText(
        ResUtil.getBulletList(
            this,
            "- ",
            ResUtil.getRawText(this, R.raw.changelog),
            getResources().getStringArray(R.array.changelog_highlights)
        ),
        TextView.BufferType.SPANNABLE
    );
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
  }
}
