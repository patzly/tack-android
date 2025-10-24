/*
 * This file is part of Tack Android.
 *
 * Tack Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tack Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tack Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import androidx.annotation.AttrRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import xyz.zedler.patrick.tack.R;

public class ViewUtil {

  private static final String TAG = ViewUtil.class.getSimpleName();

  private final long idle;
  private final LinkedList<Timestamp> timestamps;

  private static class Timestamp {

    private final int id;
    private long time;

    public Timestamp(int id, long time) {
      this.id = id;
      this.time = time;
    }
  }

  // Prevent multiple clicks

  public ViewUtil(long minClickIdle) {
    idle = minClickIdle;
    timestamps = new LinkedList<>();
  }

  public ViewUtil() {
    idle = 500;
    timestamps = new LinkedList<>();
  }

  public boolean isClickDisabled(int id) {
    for (int i = 0; i < timestamps.size(); i++) {
      if (timestamps.get(i).id == id) {
        if (SystemClock.elapsedRealtime() - timestamps.get(i).time < idle) {
          return true;
        } else {
          timestamps.get(i).time = SystemClock.elapsedRealtime();
          return false;
        }
      }
    }
    timestamps.add(new Timestamp(id, SystemClock.elapsedRealtime()));
    return false;
  }

  public boolean isClickEnabled(int id) {
    return !isClickDisabled(id);
  }

  public void cleanUp() {
    for (Iterator<Timestamp> iterator = timestamps.iterator(); iterator.hasNext(); ) {
      Timestamp timestamp = iterator.next();
      if (SystemClock.elapsedRealtime() - timestamp.time > idle) {
        iterator.remove();
      }
    }
  }

  // Show keyboard for EditText

  public static void requestFocusAndShowKeyboard(@NonNull Window window, @NonNull View view) {
    WindowCompat.getInsetsController(window, view).show(Type.ime());
    view.requestFocus();
  }

  // ClickListeners & OnCheckedChangeListeners

  public static void setOnClickListeners(View.OnClickListener listener, View... views) {
    for (View view : views) {
      if (view != null) {
        view.setOnClickListener(listener);
      }
    }
  }

  public static void setOnCheckedChangeListeners(
      CompoundButton.OnCheckedChangeListener listener,
      CompoundButton... compoundButtons
  ) {
    for (CompoundButton view : compoundButtons) {
      view.setOnCheckedChangeListener(listener);
    }
  }

  public static void setChecked(boolean checked, MaterialCardView... cardViews) {
    for (MaterialCardView cardView : cardViews) {
      if (cardView != null) {
        cardView.setChecked(checked);
      }
    }
  }

  public static void uncheckAllChildren(ViewGroup... viewGroups) {
    for (ViewGroup viewGroup : viewGroups) {
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        View child = viewGroup.getChildAt(i);
        if (child instanceof MaterialCardView) {
          ((MaterialCardView) child).setChecked(false);
        }
      }
    }
  }

  // BottomSheets

  public static void showBottomSheet(AppCompatActivity activity, BottomSheetDialogFragment sheet) {
    sheet.show(activity.getSupportFragmentManager(), sheet.toString());
  }

  // OnGlobalLayoutListeners

  public static void addOnGlobalLayoutListener(
      @Nullable View view, @NonNull OnGlobalLayoutListener listener) {
    if (view != null) {
      view.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }
  }

  public static void removeOnGlobalLayoutListener(
      @Nullable View view, @NonNull OnGlobalLayoutListener victim) {
    if (view != null) {
      view.getViewTreeObserver().removeOnGlobalLayoutListener(victim);
    }
  }

  // Animated icons

  public static void startIcon(ImageView imageView) {
    if (imageView == null) {
      return;
    }
    startIcon(imageView.getDrawable());
  }

  public static void startIcon(Drawable drawable) {
    if (drawable == null) {
      return;
    }
    try {
      ((Animatable) drawable).start();
    } catch (ClassCastException e) {
      Log.v(TAG, "icon animation requires AnimVectorDrawable");
    }
  }

  public static void resetAnimatedIcon(ImageView imageView) {
    if (imageView == null) {
      return;
    }
    try {
      Animatable animatable = (Animatable) imageView.getDrawable();
      if (animatable != null) {
        animatable.stop();
      }
      imageView.setImageDrawable(null);
      imageView.setImageDrawable((Drawable) animatable);
    } catch (ClassCastException e) {
      Log.v(TAG, "resetting animated icon requires AnimVectorDrawable");
    }
  }

  public static void resetAnimatedIcon(MaterialButton button) {
    if (button == null) {
      return;
    }
    try {
      Animatable animatable = (Animatable) button.getIcon();
      if (animatable != null) {
        animatable.stop();
      }
      button.setIcon(null);
      button.setIcon((Drawable) animatable);
    } catch (ClassCastException e) {
      Log.v(TAG, "resetting animated icon requires AnimVectorDrawable");
    }
  }

  // Ripple background for surface list items

  public static Drawable getRippleBgListItemSurface(Context context) {
    float[] radii = new float[8];
    Arrays.fill(radii, UiUtil.dpToPx(context, 16));
    RoundRectShape rect = new RoundRectShape(radii, null, null);
    ShapeDrawable shape = new ShapeDrawable(rect);
    shape.getPaint().setColor(ResUtil.getColor(context, R.attr.colorSurfaceContainerLow));
    LayerDrawable layers = new LayerDrawable(new ShapeDrawable[]{shape});
    layers.setLayerInset(
        0,
        UiUtil.dpToPx(context, 8),
        UiUtil.dpToPx(context, 2),
        UiUtil.dpToPx(context, 8),
        UiUtil.dpToPx(context, 2)
    );
    return new RippleDrawable(
        ColorStateList.valueOf(ResUtil.getColorHighlight(context)), null, layers
    );
  }

  public static Drawable getBgListItemSelected(Context context) {
    return getBgListItemSelected(context, 8, 8);
  }

  public static Drawable getBgListItemSelected(
      Context context, float paddingStart, float paddingEnd
  ) {
    return getBgListItemSelected(context, R.attr.colorSecondaryContainer, paddingStart, paddingEnd);
  }

  public static Drawable getBgListItemSelected(Context context, @AttrRes int color) {
    return getBgListItemSelected(context, color, 8, 8);
  }

  public static Drawable getBgListItemSelected(
      Context context, @AttrRes int color, float paddingStart, float paddingEnd
  ) {
    boolean isRtl = UiUtil.isLayoutRtl(context);
    float[] radii = new float[8];
    Arrays.fill(radii, UiUtil.dpToPx(context, 16));
    RoundRectShape rect = new RoundRectShape(radii, null, null);
    ShapeDrawable shape = new ShapeDrawable(rect);
    shape.getPaint().setColor(ResUtil.getColor(context, color));
    LayerDrawable layers = new LayerDrawable(new ShapeDrawable[]{shape});
    layers.setLayerInset(
        0,
        UiUtil.dpToPx(context, isRtl ? paddingEnd : paddingStart),
        UiUtil.dpToPx(context, 2),
        UiUtil.dpToPx(context, isRtl ? paddingStart : paddingEnd),
        UiUtil.dpToPx(context, 2)
    );
    return layers;
  }

  public static void setEnabled(boolean enabled, View... views) {
    for (View view : views) {
      view.setEnabled(enabled);
    }
  }

  public static void setEnabledAlpha(boolean enabled, boolean animated, View... views) {
    for (View view : views) {
      view.setEnabled(enabled);
      if (animated) {
        view.animate().alpha(enabled ? 1 : 0.5f).setDuration(200).start();
      } else {
        view.setAlpha(enabled ? 1 : 0.5f);
      }
    }
  }

  public static void setTooltipText(@NonNull View view, @StringRes int resId) {
    ViewCompat.setTooltipText(view, view.getContext().getString(resId));
  }

  public static void setTooltipTextAndContentDescription(@NonNull View view, String text) {
    ViewCompat.setTooltipText(view, text);
    view.setContentDescription(text);
  }

  public static void centerScrollContentIfNotFullWidth(HorizontalScrollView scrollView) {
    centerScrollContentIfNotFullWidth(scrollView, 0);
  }

  public static void centerScrollContentIfNotFullWidth(
      HorizontalScrollView scrollView, int additionalContentWidth
  ) {
    if (scrollView.isLaidOut()) {
      centerScrollContentIfPossible(scrollView, additionalContentWidth);
    } else {
      scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
          new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              centerScrollContentIfPossible(scrollView, additionalContentWidth);
              if (scrollView.getViewTreeObserver().isAlive()) {
                scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
              }
            }
          });
    }
  }

  private static void centerScrollContentIfPossible(
      HorizontalScrollView scrollView, int additionalContentWidth
  ) {
    if (scrollView.getChildCount() == 0) {
      return;
    }
    View content = scrollView.getChildAt(0);
    int scrollWidth = scrollView.getWidth();
    //int tolerance = UiUtil.dpToPx(scrollView.getContext(), 16) * (canCenterEarlier ? -1 : 1);
    int contentWidth = content.getWidth() + additionalContentWidth;
    ((HorizontalScrollView.LayoutParams) content.getLayoutParams()).gravity =
        contentWidth >= scrollWidth
            ? Gravity.START
            : Gravity.CENTER_HORIZONTAL;
    content.requestLayout();
  }

  // PopupMenu

  public static void showMenu(
      View v, @MenuRes int menuRes, PopupMenu.OnMenuItemClickListener listener, int gravity
  ) {
    PopupMenu popup = new PopupMenu(v.getContext(), v);
    popup.getMenuInflater().inflate(menuRes, popup.getMenu());
    popup.setOnMenuItemClickListener(listener);
    popup.setGravity(gravity);
    popup.show();
  }

  public static void showMenu(
      View v, @MenuRes int menuRes, PopupMenu.OnMenuItemClickListener listener
  ) {
    showMenu(v, menuRes, listener, Gravity.END);
  }

  public static void showMenu(
      View v,
      @MenuRes int menuRes,
      PopupMenu.OnMenuItemClickListener onItemClickListener,
      OnMenuInflatedListener onInflatedListener
  ) {
    PopupMenu popup = new PopupMenu(v.getContext(), v);
    popup.getMenuInflater().inflate(menuRes, popup.getMenu());
    if (onInflatedListener != null) {
      onInflatedListener.onMenuInflated(popup.getMenu());
    }
    popup.setOnMenuItemClickListener(onItemClickListener);
    popup.setGravity(Gravity.END);
    popup.show();
  }

  public interface OnMenuInflatedListener {
    void onMenuInflated(@NonNull Menu menu);
  }
}