package xyz.zedler.patrick.tack.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetTextBinding;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.VibratorUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class TextBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

  private final static String TAG = "TextBottomSheetDialog";

  private FragmentBottomsheetTextBinding binding;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    binding = FragmentBottomsheetTextBinding.inflate(
        inflater, container, false
    );

    Context context = getContext();
    Bundle bundle = getArguments();
    assert context != null && bundle != null;

    VibratorUtil vibratorUtil = new VibratorUtil(context);

    binding.textTextTitle.setText(
        bundle.getString(Constants.EXTRA.TITLE)
    );

    String link = bundle.getString(Constants.EXTRA.LINK);
    if (link != null) {
      binding.frameTextOpenLink.setOnClickListener(v -> {
        ViewUtil.startIcon(binding.imageTextOpenLink);
        vibratorUtil.click();
        new Handler(Looper.getMainLooper()).postDelayed(
            () -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))),
            500
        );
      });
    } else {
      binding.frameTextOpenLink.setVisibility(View.GONE);
    }

    binding.textText.setText(
        ResUtil.readFromFile(context, bundle.getString(Constants.EXTRA.FILE))
    );

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
    binding.textText.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
