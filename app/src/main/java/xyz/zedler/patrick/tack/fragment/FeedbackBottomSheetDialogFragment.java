package xyz.zedler.patrick.tack.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import xyz.zedler.patrick.tack.FeedbackActivity;
import xyz.zedler.patrick.tack.R;

public class FeedbackBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new BottomSheetDialog(requireContext(), R.style.Theme_Tack_BottomSheetDialog);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_bottomsheet_feedback, container, false);

		Activity activity = getActivity();
		assert activity != null;

		view.findViewById(R.id.button_feedback_rate).setOnClickListener(this);
		view.findViewById(R.id.button_feedback_send_feedback).setOnClickListener(this);

		return view;
	}

	@Override
	public void onClick(View v) {
		SharedPreferences.Editor sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
		switch(v.getId()) {
			case R.id.button_feedback_rate:
				sharedPrefs.putInt("feedback_pop_up", 0);
				Uri uri = Uri.parse("market://details?id=xyz.zedler.patrick.tack");
				Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
				goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
						Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
						Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
						Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
				try {
					startActivity(goToMarket);
				} catch (ActivityNotFoundException e) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
							"http://play.google.com/store/apps/details?id=xyz.zedler.patrick.tack"
					)));
				}
				dismiss();
				break;
			case R.id.button_feedback_send_feedback:
				sharedPrefs.putInt("feedback_pop_up", 0);
				startActivity(new Intent(getContext(), FeedbackActivity.class));
				dismiss();
				break;
		}
		sharedPrefs.apply();
	}
}
