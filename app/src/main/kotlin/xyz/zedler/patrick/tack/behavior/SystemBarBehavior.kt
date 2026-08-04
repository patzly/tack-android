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

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager.LayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import xyz.zedler.patrick.tack.util.SCRIM
import xyz.zedler.patrick.tack.util.dpToPx
import xyz.zedler.patrick.tack.util.getAttrColor
import xyz.zedler.patrick.tack.util.isDarkModeActive
import xyz.zedler.patrick.tack.util.isLandTablet
import xyz.zedler.patrick.tack.util.isNavigationModeGesture
import xyz.zedler.patrick.tack.util.isOrientationPortrait
import xyz.zedler.patrick.tack.util.layoutEdgeToEdge
import xyz.zedler.patrick.tack.util.setLightNavigationBar
import xyz.zedler.patrick.tack.util.setLightStatusBar
import androidx.core.graphics.toColorInt

class SystemBarBehavior(private val activity: Activity) {

  private val window: Window = activity.window

  var containerPaddingTop: Int = 0
    private set
  var containerPaddingBottom: Int = 0
    private set
  var containerPaddingLeft: Int = 0
    private set
  var containerPaddingRight: Int = 0
    private set

  var scrollContentPaddingBottom: Int = 0
    private set

  private var appBarLayout: AppBarLayout? = null
  private var container: ViewGroup? = null
  private var scrollView: NestedScrollView? = null
  private var scrollContent: ViewGroup? = null

  private var applyAppBarInsetOnContainer = true
  private var applyStatusBarInsetOnContainer = true
  private var applyCutoutInsetOnContainer = true

  private var isScrollable = false
  private var isMultiColumnLayout = false
  private var hasScrollView = false
  private var hasRecycler = false

  private var statusBarInset = 0
  private var navBarInset = 0
  private var cutoutInsetLeft = 0
  private var cutoutInsetRight = 0

  var additionalBottomInset: Int = 0
  var imeInset: Int = 0

  init {
    // GOING EDGE TO EDGE
    window.layoutEdgeToEdge()
    if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
      window.attributes.layoutInDisplayCutoutMode =
        LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    } else if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode =
        LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
  }

  fun setAppBar(appBarLayout: AppBarLayout?) {
    this.appBarLayout = appBarLayout
  }

  fun setContainer(container: ViewGroup) {
    this.container = container
    containerPaddingTop = container.paddingTop
    containerPaddingBottom = container.paddingBottom
    containerPaddingLeft = container.paddingLeft
    containerPaddingRight = container.paddingRight
  }

  fun setScroll(scrollView: NestedScrollView, scrollContent: ViewGroup) {
    this.scrollView = scrollView
    this.scrollContent = scrollContent
    scrollContentPaddingBottom = scrollContent.paddingBottom
    hasScrollView = true
    hasRecycler = false

    if (container == null) {
      setContainer(scrollView)
    }
  }

  fun setRecycler(recycler: RecyclerView) {
    this.scrollContent = recycler
    scrollContentPaddingBottom = recycler.paddingBottom
    hasRecycler = true
    hasScrollView = false

    if (container == null) {
      throw RuntimeException("Container has to be set before calling setRecycler()")
    }
  }

  fun setUp() {
    val root = window.decorView.findViewById<View>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
      var containerPaddingTopExtra = 0
      var containerPaddingBottomExtra = 0
      var containerPaddingLeftExtra = 0
      var containerPaddingRightExtra = 0

      // TOP INSET
      statusBarInset = insets.getInsets(Type.systemBars()).top
      val appBar = appBarLayout
      if (appBar != null) {
        // STATUS BAR INSET
        appBar.setPadding(0, statusBarInset, 0, appBar.paddingBottom)
        appBar.measure(
          View.MeasureSpec.UNSPECIFIED,
          View.MeasureSpec.UNSPECIFIED
        )
        // APP BAR INSET
        val containerView = container
        if (containerView != null && applyAppBarInsetOnContainer) {
          val params = containerView.layoutParams as ViewGroup.MarginLayoutParams
          params.topMargin = appBar.measuredHeight
          containerView.layoutParams = params
        } else if (containerView != null && applyStatusBarInsetOnContainer) {
          containerPaddingTopExtra += statusBarInset
        }
      } else if (container != null && applyStatusBarInsetOnContainer) {
        // STATUS BAR INSET
        // if no app bar exists, status bar inset is applied to container
        containerPaddingTopExtra += statusBarInset
      }

      // CUTOUT INSET
      if (container != null && applyCutoutInsetOnContainer) {
        cutoutInsetLeft = insets.getInsets(Type.displayCutout()).left
        cutoutInsetRight = insets.getInsets(Type.displayCutout()).right
        containerPaddingLeftExtra += cutoutInsetLeft
        containerPaddingRightExtra += cutoutInsetRight
      }

      // NAV BAR INSET
      val useBottomNavBarInset = activity.isOrientationPortrait() ||
          activity.isNavigationModeGesture() ||
          activity.isLandTablet()
      val containerView = container
      if (useBottomNavBarInset && containerView != null) {
        navBarInset = insets.getInsets(Type.systemBars()).bottom
        val content = scrollContent
        if (hasScrollView || hasRecycler) {
          content?.setPadding(
            content.paddingLeft,
            content.paddingTop,
            content.paddingRight,
            scrollContentPaddingBottom + additionalBottomInset +
                navBarInset.coerceAtLeast(imeInset)
          )
        } else {
          containerPaddingBottomExtra += additionalBottomInset +
              navBarInset.coerceAtLeast(imeInset)
        }
      } else if (containerView != null) {
        navBarInset = 0 // no bottom nav bar inset
        root.setPadding(
          insets.getInsets(Type.systemBars()).left,
          root.paddingTop,
          insets.getInsets(Type.systemBars()).right,
          root.paddingBottom
        )
        // Add additional bottom inset
        val content = scrollContent
        if (hasScrollView || hasRecycler) {
          content?.setPadding(
            content.paddingLeft,
            content.paddingTop,
            content.paddingRight,
            scrollContentPaddingBottom + additionalBottomInset + imeInset
          )
        } else {
          containerPaddingBottomExtra += additionalBottomInset + imeInset
        }
      }

      containerView?.setPadding(
        containerPaddingLeft + containerPaddingLeftExtra,
        containerPaddingTop + containerPaddingTopExtra,
        containerPaddingRight + containerPaddingRightExtra,
        containerPaddingBottom + containerPaddingBottomExtra
      )
      insets
    }

    if (hasScrollView) {
      // call viewThreeObserver, this updates the system bar appearance
      measureScrollView()
    } else {
      if (hasRecycler) {
        measureRecyclerView()
      }
      // call directly because there won't be any changes caused by scroll content
      updateSystemBars()
    }
  }

  fun refresh(measureScrollContent: Boolean) {
    var containerPaddingTopExtra = 0
    var containerPaddingBottomExtra = 0
    var containerPaddingLeftExtra = 0
    var containerPaddingRightExtra = 0

    // TOP INSET
    val appBar = appBarLayout
    if (appBar != null) {
      // STATUS BAR INSET
      appBar.setPadding(0, statusBarInset, 0, appBar.paddingBottom)
      appBar.measure(
        View.MeasureSpec.UNSPECIFIED,
        View.MeasureSpec.UNSPECIFIED
      )
      // APP BAR INSET
      val containerView = container
      if (containerView != null && applyAppBarInsetOnContainer) {
        val params = containerView.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = appBar.measuredHeight
        containerView.layoutParams = params
      } else if (containerView != null && applyStatusBarInsetOnContainer) {
        containerPaddingTopExtra += statusBarInset
      }
    } else if (container != null && applyStatusBarInsetOnContainer) {
      // STATUS BAR INSET
      // if no app bar exists, status bar inset is applied to container
      containerPaddingTopExtra += statusBarInset
    }

    // CUTOUT INSET
    if (container != null && applyCutoutInsetOnContainer) {
      containerPaddingLeftExtra += cutoutInsetLeft
      containerPaddingRightExtra += cutoutInsetRight
    }

    // NAV BAR INSET
    val useBottomInset = activity.isOrientationPortrait() ||
        activity.isNavigationModeGesture() ||
        activity.isLandTablet()
    val containerView = container
    if (useBottomInset && containerView != null) {
      val content = scrollContent
      if (hasScrollView || hasRecycler) {
        content?.setPadding(
          content.paddingLeft,
          content.paddingTop,
          content.paddingRight,
          scrollContentPaddingBottom + additionalBottomInset +
              navBarInset.coerceAtLeast(imeInset)
        )
      } else {
        containerPaddingBottomExtra += additionalBottomInset +
            navBarInset.coerceAtLeast(imeInset)
      }
    } else if (containerView != null) {
      // Add additional bottom inset
      val content = scrollContent
      if (hasScrollView || hasRecycler) {
        content?.setPadding(
          content.paddingLeft,
          content.paddingTop,
          content.paddingRight,
          scrollContentPaddingBottom + additionalBottomInset + imeInset
        )
      } else {
        containerPaddingBottomExtra += additionalBottomInset + imeInset
      }
    }

    containerView?.setPadding(
      containerPaddingLeft + containerPaddingLeftExtra,
      containerPaddingTop + containerPaddingTopExtra,
      containerPaddingRight + containerPaddingRightExtra,
      containerPaddingBottom + containerPaddingBottomExtra
    )

    if (hasScrollView && measureScrollContent) {
      // call viewThreeObserver, this updates the system bar appearance
      measureScrollView()
    } else if (measureScrollContent) {
      if (hasRecycler) {
        measureRecyclerView()
      }
      // call directly because there won't be any changes caused by scroll content
      updateSystemBars()
    }
  }

  private fun measureScrollView() {
    val scroll = scrollView ?: return
    scroll.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          var scrollViewWidth = scroll.width
          scrollViewWidth -= scroll.paddingLeft + scroll.paddingRight
          val content = scrollContent
          if (content != null) {
            val scrollContentWidth = content.width + activity.dpToPx(16f)
            if (applyCutoutInsetOnContainer &&
              !isMultiColumnLayout &&
              scrollContentWidth < scrollViewWidth
            ) {
              // cutout insets not needed, remove them
              scroll.setPadding(
                scroll.paddingLeft - cutoutInsetLeft,
                scroll.paddingTop,
                scroll.paddingRight - cutoutInsetRight,
                scroll.paddingBottom
              )
              // Re-measure scroll content, else padding could be lost
              content.requestLayout()
            }
            val scrollViewHeight = scroll.height
            val scrollContentHeight = content.height
            isScrollable = scrollViewHeight - scrollContentHeight < 0
            updateSystemBars()
          }
          // Kill ViewTreeObserver
          if (scroll.viewTreeObserver.isAlive) {
            scroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
          }
        }
      })
  }

  private fun measureRecyclerView() {
    val containerView = container ?: throw RuntimeException(
      "Container has to be set for RecyclerView"
    )
    val content = scrollContent ?: return
    content.viewTreeObserver.addOnGlobalLayoutListener(
      object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          var containerWidth = containerView.width
          containerWidth -= containerView.paddingLeft + containerView.paddingRight
          val scrollContentWidth = content.width + activity.dpToPx(16f)
          if (applyCutoutInsetOnContainer &&
            !isMultiColumnLayout &&
            scrollContentWidth < containerWidth
          ) {
            // cutout insets not needed, remove them
            containerView.setPadding(
              containerView.paddingLeft - cutoutInsetLeft,
              containerView.paddingTop,
              containerView.paddingRight - cutoutInsetRight,
              containerView.paddingBottom
            )
          }
          // Kill ViewTreeObserver
          if (content.viewTreeObserver.isAlive) {
            content.viewTreeObserver.removeOnGlobalLayoutListener(this)
          }
        }
      })
  }

  fun applyAppBarInsetOnContainer(apply: Boolean) {
    applyAppBarInsetOnContainer = apply
  }

  fun applyStatusBarInsetOnContainer(apply: Boolean) {
    applyStatusBarInsetOnContainer = apply
  }

  fun applyCutoutInsetOnContainer(apply: Boolean) {
    applyCutoutInsetOnContainer = apply
  }

  fun setMultiColumnLayout(multiColumnLayout: Boolean) {
    isMultiColumnLayout = multiColumnLayout
  }

  private fun updateSystemBars() {
    val isOrientationPortrait = activity.isOrientationPortrait()
    val isLandTablet = activity.isLandTablet()
    val isDarkModeActive = activity.isDarkModeActive()

    val colorScrim = activity.getAttrColor(
      xyz.zedler.patrick.tack.R.attr.colorSurface, 0.7f
    )

    when {
      Build.VERSION.SDK_INT >= VERSION_CODES.VANILLA_ICE_CREAM -> { // 35
        if (!isDarkModeActive) {
          window.decorView.setLightStatusBar(true)
          if (!activity.isNavigationModeGesture()) {
            window.decorView.setLightNavigationBar(true)
          }
        }
        window.isNavigationBarContrastEnforced = true
      }

      Build.VERSION.SDK_INT >= VERSION_CODES.Q -> { // 29
        window.statusBarColor = Color.TRANSPARENT
        if (!isDarkModeActive) {
          window.decorView.setLightStatusBar(true)
        }
        if (activity.isNavigationModeGesture()) {
          window.navigationBarColor = Color.TRANSPARENT
          window.isNavigationBarContrastEnforced = true
        } else {
          if (!isDarkModeActive) {
            window.decorView.setLightNavigationBar(true)
          }
          if (isOrientationPortrait || isLandTablet) {
            window.navigationBarColor = if (isScrollable) {
              colorScrim
            } else {
              "#01000000".toColorInt()
            }
          } else {
            window.navigationBarDividerColor = activity.getAttrColor(
              xyz.zedler.patrick.tack.R.attr.colorOutlineVariant
            )
            window.navigationBarColor = activity.getAttrColor(
              xyz.zedler.patrick.tack.R.attr.colorSurface
            )
          }
        }
      }

      Build.VERSION.SDK_INT == VERSION_CODES.P -> { // 28
        window.statusBarColor = Color.TRANSPARENT
        if (!isDarkModeActive) {
          window.decorView.setLightStatusBar(true)
          window.decorView.setLightNavigationBar(true)
        }
        if (isOrientationPortrait || isLandTablet) {
          window.navigationBarColor = if (isScrollable) colorScrim else Color.TRANSPARENT
        } else {
          window.navigationBarDividerColor = activity.getAttrColor(
            xyz.zedler.patrick.tack.R.attr.colorOutlineVariant
          )
          window.navigationBarColor = activity.getAttrColor(
            xyz.zedler.patrick.tack.R.attr.colorSurface
          )
        }
      }

      Build.VERSION.SDK_INT >= VERSION_CODES.O -> { // 26
        window.statusBarColor = Color.TRANSPARENT
        if (!isDarkModeActive) {
          window.decorView.setLightStatusBar(true)
        }
        if (isOrientationPortrait || isLandTablet) {
          window.navigationBarColor = if (isScrollable) colorScrim else Color.TRANSPARENT
          if (!isDarkModeActive) {
            window.decorView.setLightNavigationBar(true)
          }
        } else {
          window.navigationBarColor = if (isDarkModeActive) Color.BLACK else SCRIM
        }
      }

      else -> { // 23
        window.statusBarColor = Color.TRANSPARENT
        if (!isDarkModeActive) {
          window.decorView.setLightStatusBar(true)
        }
        if (isOrientationPortrait || isLandTablet) {
          window.navigationBarColor = if (isDarkModeActive) {
            if (isScrollable) colorScrim else Color.TRANSPARENT
          } else {
            SCRIM
          }
        } else {
          window.navigationBarColor = if (isDarkModeActive) colorScrim else SCRIM
        }
      }
    }
  }

  companion object {
    private val TAG = SystemBarBehavior::class.java.simpleName

    @JvmStatic
    @JvmOverloads
    fun applyBottomInset(view: View, additionalMargin: Int = 0) {
      val params = view.layoutParams as ViewGroup.MarginLayoutParams
      ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        params.bottomMargin =
          additionalMargin + insets.getInsets(Type.systemBars()).bottom
        v.layoutParams = params
        insets
      }
    }
  }
}
