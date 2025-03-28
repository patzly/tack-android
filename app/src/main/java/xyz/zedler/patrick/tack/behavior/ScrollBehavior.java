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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.appbar.AppBarLayout;

public class ScrollBehavior {

  private static final String TAG = ScrollBehavior.class.getSimpleName();
  private static final boolean DEBUG = false;

  private static final int STATE_SCROLLED_DOWN = 1;
  private static final int STATE_SCROLLED_UP = 2;
  // distance gets divided to prevent cutoff of edge effect
  private final int pufferDivider = 2;
  private int currentState = STATE_SCROLLED_UP;
  // distance before top scroll when overScroll is turned off
  private int pufferSize = 0;
  private boolean isTopScroll = false;
  private boolean liftOnScroll = true;
  private boolean killObserver = true;
  private boolean noOverScroll = false;
  private AppBarLayout appBarLayout;
  private NestedScrollView scrollView;

  public void setUpScroll(
      @NonNull AppBarLayout appBarLayout,
      NestedScrollView scrollView,
      boolean liftOnScroll,
      boolean noOverScroll,
      boolean killObserver
  ) {
    this.appBarLayout = appBarLayout;
    this.scrollView = scrollView;
    this.liftOnScroll = liftOnScroll;
    this.noOverScroll = noOverScroll;
    this.killObserver = killObserver;

    currentState = STATE_SCROLLED_UP;

    measureScrollView();
    setLiftOnScroll(liftOnScroll);

    if (scrollView == null) {
      return;
    }
    scrollView.setOnScrollChangeListener((NestedScrollView v,
        int scrollX,
        int scrollY,
        int oldScrollX,
        int oldScrollY) -> {
      if (!isTopScroll && scrollY == 0) { // TOP
        onTopScroll();
      } else {
        if (scrollY < oldScrollY) { // UP
          if (currentState != STATE_SCROLLED_UP) {
            onScrollUp();
          }
          if (liftOnScroll && scrollY < pufferSize) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
              if (scrollY > 0) {
                setOverScrollModeEnabled(false);
              }
            }, 1);
          }
        } else if (scrollY > oldScrollY) {
          if (currentState != STATE_SCROLLED_DOWN) { // DOWN
            onScrollDown();
          }
        }
      }
    });
  }

  public void setUpScroll(@NonNull AppBarLayout appBarLayout,
      NestedScrollView scrollView,
      boolean liftOnScroll) {
    setUpScroll(appBarLayout, scrollView, liftOnScroll, false, true);
  }

  private void onTopScroll() {
    isTopScroll = true;
    if (liftOnScroll) {
      appBarLayout.setLifted(false);
    }
    if (DEBUG) {
      Log.i(TAG, "onTopScroll: liftOnScroll = " + liftOnScroll);
    }
  }

  private void onScrollUp() {
    currentState = STATE_SCROLLED_UP;
    appBarLayout.setLifted(true);
    if (DEBUG) {
      Log.i(TAG, "onScrollUp: UP");
    }
  }

  private void onScrollDown() {
    // second top scroll is unrealistic before down scroll
    isTopScroll = false;
    currentState = STATE_SCROLLED_DOWN;
    if (scrollView != null) {
      appBarLayout.setLifted(true);
      setOverScrollModeEnabled(!noOverScroll);
    } else if (DEBUG) {
      Log.e(TAG, "onScrollDown: scrollView is null");
    }
    if (DEBUG) {
      Log.i(TAG, "onScrollDown: DOWN");
    }
  }

  /**
   * Sets the global boolean and changes the elevation manually if necessary. If scrollY of the
   * scrollView is 0, overScroll is turned off. Otherwise it's on if the view is scrollable.
   */
  public void setLiftOnScroll(boolean lift) {
    liftOnScroll = lift;
    // We'll make this manually
    appBarLayout.setLiftOnScroll(false);
    appBarLayout.setLiftable(true);
    if (scrollView != null) {
      if (lift) {
        if (scrollView.getScrollY() == 0) {
          appBarLayout.setLifted(false);
          setOverScrollModeEnabled(false);
        } else {
          appBarLayout.setLifted(true);
        }
      } else {
        appBarLayout.setLifted(true);
        setOverScrollModeEnabled(!noOverScroll);
      }
    } else {
      appBarLayout.setLifted(!lift);
    }
    if (DEBUG) {
      Log.i(TAG, "setLiftOnScroll(" + lift + ")");
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
            int scrollViewHeight = scrollView.getMeasuredHeight();
            if (scrollView.getChildAt(0) != null) {
              int scrollContentHeight = scrollView.getChildAt(0).getHeight();
              pufferSize = (scrollContentHeight - scrollViewHeight) / pufferDivider;
            } else if (DEBUG) {
              Log.e(TAG, "measureScrollView: no child");
            }
            // Kill ViewTreeObserver
            if (!killObserver) {
              return;
            }
            if (scrollView.getViewTreeObserver().isAlive()) {
              scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
          }
        });
  }

  private void setOverScrollModeEnabled(boolean enabled) {
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
}
