package xyz.zedler.patrick.tack.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetEmphasisBinding;

public class EmphasisBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

	private final static String TAG = "EmphasisBottomSheet";

	private FragmentBottomsheetEmphasisBinding binding;
	private MainActivity activity;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new BottomSheetDialog(requireContext(), R.style.Theme_Tack_BottomSheetDialog);
	}

	@Override
	public View onCreateView(
			@NonNull LayoutInflater inflater,
			ViewGroup container,
			Bundle savedInstanceState
	) {
		binding = FragmentBottomsheetEmphasisBinding.inflate(
				inflater, container, false
		);

		activity = (MainActivity) getActivity();
		assert activity != null;

		binding.sliderEmphasis.setValue(
				PreferenceManager.getDefaultSharedPreferences(
						getContext()
				).getInt("emphasis", 0)
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

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
