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

package xyz.zedler.patrick.tack.behavior;

import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import com.google.android.material.appbar.AppBarLayout;

public class ScrollBehavior {

  private static final String TAG = ScrollBehavior.class.getSimpleName();
  private static final boolean DEBUG = false;

  public static final int LIFT_ON_SCROLL = 0;
  public static final int ALWAYS_LIFTED = 1;
  public static final int NEVER_LIFTED = 2;

  private static final int STATE_SCROLLED_DOWN = 1;
  private static final int STATE_SCROLLED_UP = 2;

  // distance gets divided to prevent cutoff of edge effect
  private final int pufferDivider = 2;
  private int currentState = STATE_SCROLLED_UP;
  // distance before top scroll when overScroll is turned off
  private int pufferSize = 0;
  private boolean isTopScroll = false;
  private int liftMode = LIFT_ON_SCROLL;
  private AppBarLayout appBarLayout;
  private ViewGroup scrollView;
  private OnScrollChangedListener onScrollChangedListener;

  public void setUpScroll(
      @NonNull AppBarLayout appBarLayout,
      @Nullable ViewGroup scrollView,
      int liftMode, boolean keepScrollPosition
  ) {
    this.appBarLayout = appBarLayout;
    this.scrollView = scrollView;
    this.liftMode = liftMode;

    currentState = STATE_SCROLLED_UP;

    if (scrollView != null) {
      measureScrollView();
    } else {
      // set lifted directly
      setLiftOnScroll(liftMode);
    }

    if (scrollView instanceof NestedScrollView) {
      NestedScrollView nested = (NestedScrollView) scrollView;
      if (!keepScrollPosition) {
        nested.postDelayed(() -> nested.setScrollY(0), 100);
      }
      nested.setOnScrollChangeListener(getOnScrollChangeListener());
    } else if (scrollView instanceof RecyclerView) {
      RecyclerView recycler = (RecyclerView) scrollView;
      if (!keepScrollPosition) {
        recycler.postDelayed(() -> recycler.scrollToPosition(0), 1);
      }
      recycler.addOnScrollListener(getOnScrollListener());
    }
  }

  public void setUpScroll(
      @NonNull AppBarLayout appBarLayout,
      @Nullable ViewGroup scrollView,
      int liftMode
  ) {
    setUpScroll(appBarLayout, scrollView, liftMode, false);
  }

  private NestedScrollView.OnScrollChangeListener getOnScrollChangeListener() {
    return (NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
      if (!isTopScroll && scrollY == 0) { // TOP
        onTopScroll();
      } else {
        if (scrollY < oldScrollY) { // UP
          if (currentState != STATE_SCROLLED_UP) {
            onScrollUp();
          }
          if (liftMode == LIFT_ON_SCROLL && scrollY < pufferSize) {
            v.postDelayed(() -> {
              if (scrollY > 0) {
                updateOverScrollMode(false);
              }
            }, 1);
          }
        } else if (scrollY > oldScrollY) {
          if (currentState != STATE_SCROLLED_DOWN) { // DOWN
            onScrollDown();
          }
        }
      }
    };
  }

  public void setOnScrollChangedListener(OnScrollChangedListener listener) {
    this.onScrollChangedListener = listener;
  }

  private OnScrollListener getOnScrollListener() {
    return new OnScrollListener() {
      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        int scrollAbsoluteY = recyclerView.computeVerticalScrollOffset();
        if (!isTopScroll && scrollAbsoluteY == 0) { // TOP
          onTopScroll();
        } else {
          if (dy < 0) { // UP
            if (currentState != STATE_SCROLLED_UP) {
              onScrollUp();
            }
            if (liftMode == LIFT_ON_SCROLL && dy < pufferSize) {
              recyclerView.postDelayed(() -> {
                if (scrollAbsoluteY > 0) {
                  updateOverScrollMode(false);
                }
              }, 1);
            }
          } else if (dy > 0) {
            if (currentState != STATE_SCROLLED_DOWN) { // DOWN
              onScrollDown();
            }
          }
        }
      }
    };
  }

  private void onTopScroll() {
    isTopScroll = true;
    if (liftMode == LIFT_ON_SCROLL) {
      appBarLayout.setLifted(false);
    }
    if (onScrollChangedListener != null) {
      onScrollChangedListener.onTopScroll();
    }
    if (DEBUG) {
      Log.i(TAG, "onTopScroll: liftMode = " + liftMode);
    }
  }

  private void onScrollUp() {
    currentState = STATE_SCROLLED_UP;
    if (liftMode != NEVER_LIFTED) {
      appBarLayout.setLifted(true);
    }
    if (onScrollChangedListener != null) {
      onScrollChangedListener.onScrollUp();
    }
    if (DEBUG) {
      Log.i(TAG, "onScrollUp: UP");
    }
  }

  private void onScrollDown() {
    // second top scroll is unrealistic before down scroll
    isTopScroll = false;
    currentState = STATE_SCROLLED_DOWN;
    if (scrollView != null) {
      if (liftMode != NEVER_LIFTED) {
        appBarLayout.setLifted(true);
        updateOverScrollMode(true);
      }
    } else if (DEBUG) {
      Log.e(TAG, "onScrollDown: scrollView is null");
    }
    if (onScrollChangedListener != null) {
      onScrollChangedListener.onScrollDown();
    }
    if (DEBUG) {
      Log.i(TAG, "onScrollDown: DOWN");
    }
  }

  public void setLiftOnScroll(int liftMode) {
    this.liftMode = liftMode;
    // We'll make this manually
    appBarLayout.setLiftOnScroll(false);
    appBarLayout.setLiftable(true);
    if (scrollView != null) {
      if (liftMode == LIFT_ON_SCROLL) {
        if (scrollView.getScrollY() == 0) {
          appBarLayout.setLifted(false);
          updateOverScrollMode(false);
        } else {
          appBarLayout.setLifted(true);
        }
      } else if (liftMode == ALWAYS_LIFTED) {
        appBarLayout.setLifted(true);
        updateOverScrollMode(true);
      } else if (liftMode == NEVER_LIFTED) {
        appBarLayout.setLifted(false);
        updateOverScrollMode(false);
      }
    } else {
      appBarLayout.setLifted(liftMode == ALWAYS_LIFTED);
    }
    if (DEBUG) {
      Log.i(TAG, "setLiftOnScroll(" + liftMode + ")");
    }
  }

  private void measureScrollView() {
    if (scrollView == null) {
      return;
    }
    scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            setLiftOnScroll(liftMode);
            if (scrollView instanceof NestedScrollView) {
              int scrollViewHeight = scrollView.getMeasuredHeight();
              if (scrollView.getChildAt(0) != null) {
                int scrollContentHeight = scrollView.getChildAt(0).getHeight();
                pufferSize = (scrollContentHeight - scrollViewHeight) / pufferDivider;
              } else if (DEBUG) {
                Log.e(TAG, "measureScrollView: no child");
              }
            }
            // Kill ViewTreeObserver
            if (scrollView.getViewTreeObserver().isAlive()) {
              scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
          }
        });
  }

  private void updateOverScrollMode(boolean enabled) {
    if (scrollView == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= 31) {
      // Stretch effect is always nice
      scrollView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
    } else {
      scrollView.setOverScrollMode(
          enabled ? View.OVER_SCROLL_IF_CONTENT_SCROLLS : View.OVER_SCROLL_NEVER
      );
    }
  }

  public interface OnScrollChangedListener {
    void onScrollUp();
    void onScrollDown();
    void onTopScroll();
  }
}
