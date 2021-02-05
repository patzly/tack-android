package xyz.zedler.patrick.tack.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetChangelogBinding;
import xyz.zedler.patrick.tack.util.BulletUtil;
import xyz.zedler.patrick.tack.util.ResUtil;

public class ChangelogBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

	private final static String TAG = "ChangelogBottomSheetDialog";

	private FragmentBottomsheetChangelogBinding binding;

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
		binding = FragmentBottomsheetChangelogBinding.inflate(
				inflater, container, false
		);

		Context context = getContext();
		assert context != null;

		binding.textChangelog.setText(
				BulletUtil.makeBulletList(
						getContext(),
						6,
						2,
						"- ",
						ResUtil.readFromFile(getContext(), "changelog"),
						getResources().getStringArray(R.array.changelog_highlights)
				),
				TextView.BufferType.SPANNABLE
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
