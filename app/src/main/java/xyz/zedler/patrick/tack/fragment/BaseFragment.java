package xyz.zedler.patrick.tack.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View.OnClickListener;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class BaseFragment extends Fragment {

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

  public SharedPreferences getSharedPrefs() {
    return activity.getSharedPrefs();
  }

  public ViewUtil getViewUtil() {
    return viewUtil;
  }

  public void navigate(NavDirections directions) {
    activity.navigate(directions);
  }

  public void navigateToFragment(NavDirections directions) {
    activity.navigateToFragment(directions);
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

  public Toolbar.OnMenuItemClickListener getOnMenuItemClickListener() {
    return item -> {
      int id = item.getItemId();
      if (viewUtil.isClickDisabled(id)) {
        return false;
      }
      performHapticClick();

      if (id == R.id.action_feedback) {
        activity.showFeedbackBottomSheet();
      } else if (id == R.id.action_recommend) {
        ResUtil.share(activity, R.string.msg_recommend);
      }
      return true;
    };
  }
}