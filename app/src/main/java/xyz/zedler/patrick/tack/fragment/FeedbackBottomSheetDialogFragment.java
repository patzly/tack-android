package xyz.zedler.patrick.tack.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.IconUtil;

public class FeedbackBottomSheetDialogFragment extends CustomBottomSheetDialogFragment {

	private final static String TAG = "FeedbackBottomSheet";

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
		View view = inflater.inflate(
				R.layout.fragment_bottomsheet_feedback,
				container,
				false
		);

		Activity activity = getActivity();
		assert activity != null;

		view.findViewById(R.id.linear_feedback_rate).setOnClickListener(v -> {
			IconUtil.start(view, R.id.image_feedback_rate);
			Uri uri = Uri.parse(
					"market://details?id=" + activity.getApplicationContext().getPackageName()
			);
			Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
			goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
					Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
					Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
					Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			new Handler().postDelayed(() -> {
				try {
					startActivity(goToMarket);
				} catch (ActivityNotFoundException e) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
							"http://play.google.com/store/apps/details?id="
									+ activity.getApplicationContext().getPackageName()
					)));
				}
				dismiss();
			}, 300);
		});

		view.findViewById(R.id.linear_feedback_email).setOnClickListener(v -> {
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(
					Uri.parse(
							"mailto:"
									+ getString(R.string.app_mail)
									+ "?subject=" + Uri.encode("Feedback@Tack")
					)
			);
			startActivity(Intent.createChooser(intent, getString(R.string.action_send_feedback)));
			dismiss();
		});

		return view;
	}

	@NonNull
	@Override
	public String toString() {
		return TAG;
	}
}
