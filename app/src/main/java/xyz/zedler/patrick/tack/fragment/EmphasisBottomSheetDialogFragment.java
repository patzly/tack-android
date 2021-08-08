package xyz.zedler.patrick.tack.fragment;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetEmphasisBinding;

public class EmphasisBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

  private final static String TAG = "EmphasisBottomSheet";

  private FragmentBottomsheetEmphasisBinding binding;
  private MainActivity activity;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    binding = FragmentBottomsheetEmphasisBinding.inflate(inflater, container, false);

    activity = (MainActivity) getActivity();
    assert activity != null;

    binding.sliderEmphasis.setValue(
        PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(
            PREF.EMPHASIS, DEF.EMPHASIS
        )
    );

    binding.sliderEmphasis.addOnChangeListener(
        (slider, value, fromUser) -> activity.setEmphasis(Math.round(value))
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
    binding.linearEmphasisContainer.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
