package xyz.zedler.patrick.tack.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.tack.MainActivity;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetEmphasisBinding;

public class EmphasisBottomSheetDialogFragment extends CustomBottomSheetDialogFragment {

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

		binding.sliderEmphasis.setValue(
				PreferenceManager.getDefaultSharedPreferences(
						getContext()
				).getInt("emphasis", 0)
		);

		return binding.getRoot();
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		if(activity!= null) activity.setEmphasis(Math.round(binding.sliderEmphasis.getValue()));
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
