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

package xyz.zedler.patrick.tack.fragment.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetTextBinding;
import xyz.zedler.patrick.tack.util.ResUtil;

public class TextBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

  private FragmentBottomsheetTextBinding binding;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
    binding = FragmentBottomsheetTextBinding.inflate(inflater, container, false);
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

    MainActivity activity = (MainActivity) requireActivity();

    TextBottomSheetDialogFragmentArgs args
        = TextBottomSheetDialogFragmentArgs.fromBundle(getArguments());

    binding.toolbarText.setTitle(getString(args.getTitle()));

    String link = args.getLink() != 0 ? getString(args.getLink()) : null;
    if (link != null) {
      binding.toolbarText.inflateMenu(R.menu.menu_link);
      ResUtil.tintMenuIcons(activity, binding.toolbarText.getMenu());
      binding.toolbarText.setOnMenuItemClickListener(item -> {
        int id = item.getItemId();
        if (id == R.id.action_open_link && getViewUtil().isClickEnabled(id)) {
          performHapticClick();
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
          return true;
        } else {
          return false;
        }
      });
    }
    binding.toolbarText.setTitleCentered(link == null);

    String[] highlights = args.getHighlights();
    if (highlights == null) {
      highlights = new String[]{};
    }
    binding.formattedText.setText(ResUtil.getRawText(activity, args.getFile()), highlights);
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.formattedText.setLayoutParams(params);
  }
}
