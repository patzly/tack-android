package xyz.zedler.patrick.tack.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetTextBinding;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class TextBottomSheetDialogFragment extends BottomSheetDialogFragment {

	private final static String TAG = "TextBottomSheetDialog";

	private FragmentBottomsheetTextBinding binding;

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
		binding = FragmentBottomsheetTextBinding.inflate(
				inflater, container, false
		);

		Context context = getContext();
		Bundle bundle = getArguments();
		assert context != null && bundle != null;

		binding.textTextTitle.setText(
				bundle.getString(Constants.EXTRA.TITLE)
		);

		String link = bundle.getString(Constants.EXTRA.LINK);
		if (link != null) {
			binding.frameTextOpenLink.setOnClickListener(v -> {
				ViewUtil.startAnimatedIcon(binding.imageTextOpenLink);
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

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
