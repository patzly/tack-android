package xyz.zedler.patrick.tack.util;

import android.graphics.drawable.Animatable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

public class ViewUtil {

  public static void setOnClickListeners(View.OnClickListener listener, View... views) {
    for (View view : views) {
      view.setOnClickListener(listener);
    }
  }

  public static void setOnCheckedChangedListeners(
      CompoundButton.OnCheckedChangeListener listener,
      CompoundButton... compoundButtons
  ) {
    for (CompoundButton compoundButton : compoundButtons) {
      compoundButton.setOnCheckedChangeListener(listener);
    }
  }

  public static void startAnimatedIcon(ImageView imageView) {
    try {
      ((Animatable) imageView.getDrawable()).start();
    } catch (ClassCastException ignored) {
    }
  }
}
