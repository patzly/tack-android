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
 * Copyright (c) 2020-2026 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.behavior

import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout

class ScrollBehavior {

  // distance gets divided to prevent cutoff of edge effect
  private val pufferDivider = 2
  private var currentState = STATE_SCROLLED_UP

  // distance before top scroll when overScroll is turned off
  private var pufferSize = 0
  private var isTopScroll = false
  private var liftMode = LIFT_ON_SCROLL
  private var appBarLayout: AppBarLayout? = null
  private var scrollView: ViewGroup? = null
  private var onScrollChangedListener: OnScrollChangedListener? = null

  @JvmOverloads
  fun setUpScroll(
    appBarLayout: AppBarLayout,
    scrollView: ViewGroup?,
    liftMode: Int,
    keepScrollPosition: Boolean = false
  ) {
    this.appBarLayout = appBarLayout
    this.scrollView = scrollView
    this.liftMode = liftMode

    currentState = STATE_SCROLLED_UP

    if (scrollView != null) {
      measureScrollView()
    } else {
      // set lifted directly
      setLiftOnScroll(liftMode)
    }

    when (scrollView) {
      is NestedScrollView -> {
        if (!keepScrollPosition) {
          scrollView.postDelayed({ scrollView.scrollY = 0 }, 100)
        }
        scrollView.setOnScrollChangeListener(getOnScrollChangeListener())
      }

      is RecyclerView -> {
        if (!keepScrollPosition) {
          scrollView.postDelayed({ scrollView.scrollToPosition(0) }, 1)
        }
        scrollView.addOnScrollListener(getOnScrollListener())
      }
    }
  }

  private fun getOnScrollChangeListener(): NestedScrollView.OnScrollChangeListener {
    return NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
      if (!isTopScroll && scrollY == 0) { // TOP
        onTopScroll()
      } else {
        if (scrollY < oldScrollY) { // UP
          if (currentState != STATE_SCROLLED_UP) {
            onScrollUp()
          }
          if (liftMode == LIFT_ON_SCROLL && scrollY < pufferSize) {
            v.postDelayed({
              if (scrollY > 0) {
                updateOverScrollMode(false)
              }
            }, 1)
          }
        } else if (scrollY > oldScrollY) {
          if (currentState != STATE_SCROLLED_DOWN) { // DOWN
            onScrollDown()
          }
        }
      }
    }
  }

  fun setOnScrollChangedListener(listener: OnScrollChangedListener?) {
    this.onScrollChangedListener = listener
  }

  private fun getOnScrollListener(): RecyclerView.OnScrollListener {
    return object : RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val scrollAbsoluteY = recyclerView.computeVerticalScrollOffset()
        if (!isTopScroll && scrollAbsoluteY == 0) { // TOP
          onTopScroll()
        } else {
          if (dy < 0) { // UP
            if (currentState != STATE_SCROLLED_UP) {
              onScrollUp()
            }
            if (liftMode == LIFT_ON_SCROLL && dy < pufferSize) {
              recyclerView.postDelayed({
                if (scrollAbsoluteY > 0) {
                  updateOverScrollMode(false)
                }
              }, 1)
            }
          } else if (dy > 0) {
            if (currentState != STATE_SCROLLED_DOWN) { // DOWN
              onScrollDown()
            }
          }
        }
      }
    }
  }

  private fun onTopScroll() {
    isTopScroll = true
    if (liftMode == LIFT_ON_SCROLL) {
      appBarLayout?.isLifted = false
    }
    onScrollChangedListener?.onTopScroll()
    if (DEBUG) {
      Log.i(TAG, "onTopScroll: liftMode = $liftMode")
    }
  }

  private fun onScrollUp() {
    currentState = STATE_SCROLLED_UP
    if (liftMode != NEVER_LIFTED) {
      appBarLayout?.isLifted = true
    }
    onScrollChangedListener?.onScrollUp()
    if (DEBUG) {
      Log.i(TAG, "onScrollUp: UP")
    }
  }

  private fun onScrollDown() {
    // second top scroll is unrealistic before down scroll
    isTopScroll = false
    currentState = STATE_SCROLLED_DOWN
    if (scrollView != null) {
      if (liftMode != NEVER_LIFTED) {
        appBarLayout?.isLifted = true
        updateOverScrollMode(true)
      }
    } else if (DEBUG) {
      Log.e(TAG, "onScrollDown: scrollView is null")
    }
    onScrollChangedListener?.onScrollDown()
    if (DEBUG) {
      Log.i(TAG, "onScrollDown: DOWN")
    }
  }

  fun setLiftOnScroll(liftMode: Int) {
    this.liftMode = liftMode
    // We'll make this manually
    appBarLayout?.isLiftOnScroll = false
    appBarLayout?.setLiftable(true)
    val scroll = scrollView
    if (scroll != null) {
      when (liftMode) {
        LIFT_ON_SCROLL -> {
          if (scroll.scrollY == 0) {
            appBarLayout?.isLifted = false
            updateOverScrollMode(false)
          } else {
            appBarLayout?.isLifted = true
          }
        }

        ALWAYS_LIFTED -> {
          appBarLayout?.isLifted = true
          updateOverScrollMode(true)
        }

        NEVER_LIFTED -> {
          appBarLayout?.isLifted = false
          updateOverScrollMode(false)
        }
      }
    } else {
      appBarLayout?.isLifted = (liftMode == ALWAYS_LIFTED)
    }
    if (DEBUG) {
      Log.i(TAG, "setLiftOnScroll($liftMode)")
    }
  }

  private fun measureScrollView() {
    val scroll = scrollView ?: return
    scroll.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          setLiftOnScroll(liftMode)
          if (scroll is NestedScrollView) {
            val scrollViewHeight = scroll.measuredHeight
            val child = scroll.getChildAt(0)
            if (child != null) {
              val scrollContentHeight = child.height
              pufferSize = (scrollContentHeight - scrollViewHeight) / pufferDivider
            } else if (DEBUG) {
              Log.e(TAG, "measureScrollView: no child")
            }
          }
          // Kill ViewTreeObserver
          if (scroll.viewTreeObserver.isAlive) {
            scroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
          }
        }
      })
  }

  private fun updateOverScrollMode(enabled: Boolean) {
    val scroll = scrollView ?: return
    if (Build.VERSION.SDK_INT >= 31) {
      // Stretch effect is always nice
      scroll.overScrollMode = View.OVER_SCROLL_ALWAYS
    } else {
      scroll.overScrollMode = if (enabled) {
        View.OVER_SCROLL_IF_CONTENT_SCROLLS
      } else {
        View.OVER_SCROLL_NEVER
      }
    }
  }

  interface OnScrollChangedListener {
    fun onScrollUp()
    fun onScrollDown()
    fun onTopScroll()
  }

  companion object {
    private val TAG = ScrollBehavior::class.java.simpleName
    private const val DEBUG = false

    const val LIFT_ON_SCROLL = 0
    const val ALWAYS_LIFTED = 1
    const val NEVER_LIFTED = 2

    private const val STATE_SCROLLED_DOWN = 1
    private const val STATE_SCROLLED_UP = 2
  }
}
