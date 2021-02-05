package xyz.zedler.patrick.tack.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetWearBinding;

public class WearBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

	private final static String TAG = "WearBottomSheet";

	private FragmentBottomsheetWearBinding binding;

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
		binding = FragmentBottomsheetWearBinding.inflate(
				inflater, container, false
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
