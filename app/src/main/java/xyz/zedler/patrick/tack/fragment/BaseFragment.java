package xyz.zedler.patrick.tack.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View.OnClickListener;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.service.MetronomeService;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class BaseFragment extends Fragment {

  private static final String TAG = BaseFragment.class.getSimpleName();

  private MainActivity activity;
  private ViewUtil viewUtil;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    activity = (MainActivity) requireActivity();
    viewUtil = new ViewUtil();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    viewUtil.cleanUp();
  }

  public MetronomeService getMetronomeService() {
    return activity.getMetronomeService();
  }

  public boolean isBound() {
    return activity.isBound();
  }

  public SharedPreferences getSharedPrefs() {
    return activity.getSharedPrefs();
  }

  public ViewUtil getViewUtil() {
    return viewUtil;
  }

  public void navigateUp() {
    activity.navigateUp();
  }

  public void performHapticClick() {
    activity.performHapticClick();
  }

  public void performHapticTick() {
    activity.performHapticTick();
  }

  public void performHapticHeavyClick() {
    activity.performHapticHeavyClick();
  }

  public OnClickListener getNavigationOnClickListener() {
    return v -> {
      if (viewUtil.isClickEnabled(v.getId())) {
        performHapticClick();
        navigateUp();
      }
    };
  }
}