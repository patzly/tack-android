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


package xyz.zedler.patrick.tack.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Window
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import xyz.zedler.patrick.tack.R
import java.util.LinkedList

class ViewUtil @JvmOverloads constructor(private val idle: Long = 500) {

  private val timestamps = LinkedList<Timestamp>()

  private class Timestamp(val id: Int, var time: Long)

  fun isClickDisabled(id: Int): Boolean {
    timestamps.find { it.id == id }?.let {
      return if (SystemClock.elapsedRealtime() - it.time < idle) {
        true
      } else {
        it.time = SystemClock.elapsedRealtime()
        false
      }
    }
    timestamps.add(Timestamp(id, SystemClock.elapsedRealtime()))
    return false
  }

  fun isClickEnabled(id: Int): Boolean = !isClickDisabled(id)

  fun cleanUp() {
    timestamps.removeAll { SystemClock.elapsedRealtime() - it.time > idle }
  }
}

// Static utilities converted to top-level functions

fun Window.requestFocusAndShowKeyboard(view: View) {
  WindowCompat.getInsetsController(this, view).show(Type.ime())
  view.requestFocus()
}

fun setOnClickListeners(listener: View.OnClickListener, vararg views: View?) {
  views.forEach { it?.setOnClickListener(listener) }
}

fun setOnCheckedChangeListeners(
  listener: CompoundButton.OnCheckedChangeListener,
  vararg compoundButtons: CompoundButton
) {
  compoundButtons.forEach { it.setOnCheckedChangeListener(listener) }
}

fun setChecked(checked: Boolean, vararg cardViews: MaterialCardView?) {
  cardViews.forEach { it?.isChecked = checked }
}

fun uncheckAllChildren(vararg viewGroups: ViewGroup) {
  viewGroups.forEach { viewGroup ->
    for (i in 0 until viewGroup.childCount) {
      val child = viewGroup.getChildAt(i)
      if (child is MaterialCardView) {
        child.isChecked = false
      }
    }
  }
}

fun AppCompatActivity.showBottomSheet(sheet: BottomSheetDialogFragment) {
  sheet.show(supportFragmentManager, sheet.toString())
}

fun View.addOnGlobalLayoutListener(listener: OnGlobalLayoutListener) {
  viewTreeObserver.addOnGlobalLayoutListener(listener)
}

fun View.removeOnGlobalLayoutListener(victim: OnGlobalLayoutListener) {
  viewTreeObserver.removeOnGlobalLayoutListener(victim)
}

fun ImageView.startIcon() {
  (drawable as? Animatable)?.start()
    ?: Log.v("ViewUtil", "icon animation requires AnimVectorDrawable")
}

fun Drawable.start() {
  (this as? Animatable)?.start()
    ?: Log.v("ViewUtil", "icon animation requires AnimVectorDrawable")
}

fun ImageView.resetAnimatedIcon() {
  val animatable = drawable as? Animatable
  animatable?.stop()
  setImageDrawable(null)
  setImageDrawable(animatable as? Drawable)
}

fun MaterialButton.resetAnimatedIcon() {
  val animatable = icon as? Animatable
  animatable?.stop()
  icon = null
  icon = animatable as? Drawable
}

fun Context.getRippleBgListItemSurface(): Drawable {
  val radii = FloatArray(8) { dpToPx(16f).toFloat() }
  val shape = ShapeDrawable(
    RoundRectShape(radii, null, null)
  ).apply {
    paint.color = getAttrColor(R.attr.colorSurfaceContainerLow)
  }
  val layers = LayerDrawable(arrayOf(shape)).apply {
    setLayerInset(0, dpToPx(8f), dpToPx(2f), dpToPx(8f), dpToPx(2f))
  }
  return RippleDrawable(
    ColorStateList.valueOf(getColorHighlight()), null, layers
  )
}

@JvmOverloads
fun Context.getBgListItemSelected(
  @AttrRes color: Int = R.attr.colorSecondaryContainer,
  paddingStart: Float = 8f,
  paddingEnd: Float = 8f
): Drawable {
  val isRtl = isLayoutRtl()
  val radii = FloatArray(8) { dpToPx(16f).toFloat() }
  val shape = ShapeDrawable(
    RoundRectShape(radii, null, null)
  ).apply {
    paint.color = getAttrColor(color)
  }
  return LayerDrawable(arrayOf(shape)).apply {
    setLayerInset(
      0,
      dpToPx(if (isRtl) paddingEnd else paddingStart),
      dpToPx(2f),
      dpToPx(if (isRtl) paddingStart else paddingEnd),
      dpToPx(2f)
    )
  }
}

fun setEnabled(enabled: Boolean, vararg views: View) {
  views.forEach { it.isEnabled = enabled }
}

fun setEnabledAlpha(enabled: Boolean, animated: Boolean, vararg views: View) {
  views.forEach {
    it.isEnabled = enabled
    if (animated) {
      it.animate().alpha(if (enabled) 1f else 0.5f).setDuration(200).start()
    } else {
      it.alpha = if (enabled) 1f else 0.5f
    }
  }
}

fun View.setTooltipText(@StringRes resId: Int) {
  ViewCompat.setTooltipText(this, context.getString(resId))
}

fun View.setTooltipTextAndContentDescription(text: String) {
  ViewCompat.setTooltipText(this, text)
  contentDescription = text
}

fun View.setBackgroundSegmented(position: Int, itemCount: Int, isSelected: Boolean = false) {
  val resId = when {
    itemCount <= 1 -> if (isSelected) {
      R.drawable.ripple_list_item_bg_secondary_segmented_single
    } else {
      R.drawable.ripple_list_item_bg_segmented_single
    }

    position == 0 -> if (isSelected) {
      R.drawable.ripple_list_item_bg_secondary_segmented_first
    } else {
      R.drawable.ripple_list_item_bg_segmented_first
    }

    position == itemCount - 1 -> if (isSelected) {
      R.drawable.ripple_list_item_bg_secondary_segmented_last
    } else {
      R.drawable.ripple_list_item_bg_segmented_last
    }

    else -> if (isSelected) {
      R.drawable.ripple_list_item_bg_secondary_segmented_middle
    } else {
      R.drawable.ripple_list_item_bg_segmented_middle
    }
  }
  setBackgroundResource(resId)
}

@JvmOverloads
fun HorizontalScrollView.centerScrollContentIfNotFullWidth(additionalContentWidth: Int = 0) {
  if (isLaidOut) {
    centerScrollContentIfPossible(additionalContentWidth)
  } else {
    viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        centerScrollContentIfPossible(additionalContentWidth)
        if (viewTreeObserver.isAlive) {
          viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
      }
    })
  }
}

private fun HorizontalScrollView.centerScrollContentIfPossible(additionalContentWidth: Int) {
  if (isEmpty()) return
  val content = getChildAt(0)
  val contentWidth = content.width + additionalContentWidth
  (content.layoutParams as FrameLayout.LayoutParams).gravity =
    if (contentWidth >= width) Gravity.START else Gravity.CENTER_HORIZONTAL
  content.requestLayout()
}

@JvmOverloads
fun View.showMenu(
  @MenuRes menuRes: Int,
  listener: PopupMenu.OnMenuItemClickListener,
  gravity: Int = Gravity.END
) {
  PopupMenu(context, this).apply {
    menuInflater.inflate(menuRes, menu)
    setOnMenuItemClickListener(listener)
    setGravity(gravity)
    show()
  }
}

fun View.showMenu(
  @MenuRes menuRes: Int,
  onItemClickListener: PopupMenu.OnMenuItemClickListener,
  onInflatedListener: OnMenuInflatedListener?
) {
  PopupMenu(context, this).apply {
    menuInflater.inflate(menuRes, menu)
    onInflatedListener?.onMenuInflated(menu)
    setOnMenuItemClickListener(onItemClickListener)
    setGravity(Gravity.END)
    show()
  }
}

fun interface OnMenuInflatedListener {
  fun onMenuInflated(menu: Menu)
}

fun Slider.configureSafely(
  valueFrom: Int,
  valueTo: Int,
  stepSize: Int,
  value: Int
) {
  require(valueTo > valueFrom) { "valueTo must be > valueFrom" }
  require(stepSize >= 0) { "stepSize must be >= 0" }

  this.stepSize = 0f

  var safeTo = valueTo
  if (stepSize > 0) {
    val range = valueTo - valueFrom
    val steps = range / stepSize
    safeTo = valueFrom + steps * stepSize
  }

  this.valueFrom = valueFrom.toFloat()
  this.valueTo = safeTo.toFloat()

  var valueFinal = value.coerceIn(valueFrom, safeTo)

  if (stepSize > 0) {
    val offset = valueFinal - valueFrom
    val tick = Math.round(offset.toFloat() / stepSize)
    valueFinal = (valueFrom + tick * stepSize).coerceIn(valueFrom, safeTo)
  }

  this.value = valueFinal.toFloat()
  this.stepSize = stepSize.toFloat()
}

fun HorizontalScrollView.scrollToViewMinimal(targetView: View?) {
  post {
    targetView?.let {
      val targetLeft = it.left
      val targetRight = it.right
      val visibleLeft = scrollX + paddingLeft
      val visibleRight = scrollX + width - paddingRight

      if (targetLeft >= visibleLeft && targetRight <= visibleRight) return@let

      if (targetLeft < visibleLeft) {
        smoothScrollTo(targetLeft - paddingLeft, 0)
      } else {
        smoothScrollTo(targetRight - (width - paddingRight), 0)
      }
    }
  }
}
