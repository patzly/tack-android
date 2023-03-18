package xyz.zedler.patrick.tack.fragment.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetChangelogBinding;
import xyz.zedler.patrick.tack.util.ResUtil;

public class ChangelogBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

  private final static String TAG = "ChangelogBottomSheetDialog";

  private FragmentBottomsheetChangelogBinding binding;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    binding = FragmentBottomsheetChangelogBinding.inflate(
        inflater, container, false
    );

    Context context = getContext();
    assert context != null;

    /*binding.textChangelog.setText(
        ResUtil.getBulletList(
            getContext(),
            "- ",
            ResUtil.getRawText(getContext(), R.raw.changelog),
            getResources().getStringArray(R.array.changelog_highlights)
        ),
        TextView.BufferType.SPANNABLE
    );*/

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.textChangelog.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
