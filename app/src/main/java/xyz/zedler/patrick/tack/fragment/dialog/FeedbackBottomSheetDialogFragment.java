package xyz.zedler.patrick.tack.fragment.dialog;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.annotation.NonNull;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SETTINGS;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.FragmentBottomsheetFeedbackBinding;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.HapticUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class FeedbackBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

  private final static String TAG = "FeedbackBottomSheet";

  private FragmentBottomsheetFeedbackBinding binding;
  private SharedPreferences sharedPrefs;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    binding = FragmentBottomsheetFeedbackBinding.inflate(
        inflater, container, false
    );

    Activity activity = getActivity();
    assert activity != null;

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);

    HapticUtil hapticUtil = new HapticUtil(activity);
    hapticUtil.setEnabled(sharedPrefs.getBoolean(SETTINGS.HAPTIC_FEEDBACK, DEF.HAPTIC_FEEDBACK));

    binding.linearFeedbackRate.setOnClickListener(v -> {
      ViewUtil.startIcon(binding.imageFeedbackRate);
      hapticUtil.click();
      Uri uri = Uri.parse(
          "market://details?id=" + activity.getApplicationContext().getPackageName()
      );
      Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
      goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
          Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
          Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
          Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        try {
          startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
          startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
              "http://play.google.com/store/apps/details?id="
                  + activity.getApplicationContext().getPackageName()
          )));
        }
        dismiss();
      }, 400);
    });

    binding.linearFeedbackEmail.setOnClickListener(v -> {
      hapticUtil.click();
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

    binding.linearFeedbackShare.setOnClickListener(v -> {
      hapticUtil.click();
      ResUtil.share(activity, R.string.msg_share);
      dismiss();
    });

    return binding.getRoot();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog) {
    super.onDismiss(dialog);

    if (sharedPrefs.getInt(PREF.FEEDBACK_POP_UP_COUNT, 1) != 0) {
      sharedPrefs.edit().putInt(PREF.FEEDBACK_POP_UP_COUNT, 0).apply();
    }
  }

  @Override
  public void applyBottomInset(int bottom) {
    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, 0, bottom);
    binding.linearFeedbackContainer.setLayoutParams(params);
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}
