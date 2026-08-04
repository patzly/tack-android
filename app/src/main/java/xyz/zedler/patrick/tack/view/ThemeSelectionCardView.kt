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

package xyz.zedler.patrick.tack.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.util.getColorHighlight
import xyz.zedler.patrick.tack.util.getAttrColor
import xyz.zedler.patrick.tack.util.isLayoutRtl
import xyz.zedler.patrick.tack.util.dpToPx
import xyz.zedler.patrick.tack.util.start

class ThemeSelectionCardView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : MaterialCardView(context, attrs) {

  private val innerSize = context.dpToPx(48f)

  init {
    val outerRadius = context.dpToPx(16f)
    val outerPadding = context.dpToPx(16f)

    layoutParams = LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.WRAP_CONTENT,
      LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      if (context.isLayoutRtl()) {
        leftMargin = context.dpToPx(4f)
      } else {
        rightMargin = context.dpToPx(4f)
      }
    }

    setContentPadding(
      outerPadding, outerPadding, outerPadding, outerPadding
    )
    radius = outerRadius.toFloat()
    cardElevation = 0f
    setCardForegroundColor(null)
    super.setCardBackgroundColor(context.getAttrColor(R.attr.colorSurfaceContainer))
    rippleColor = ColorStateList.valueOf(context.getColorHighlight())
    strokeWidth = 0
    isCheckable = true
    setCheckedIconResource(R.drawable.shape_selection_check)
    checkedIconSize = innerSize
    checkedIconMargin = outerPadding
  }

  @Deprecated("Does nothing", ReplaceWith(""))
  override fun setCardBackgroundColor(color: Int) {
    // Ignored
  }

  fun setNestedContext(nestedContext: Context) {
    removeAllViews()
    val innerCard = MaterialCardView(nestedContext).apply {
      layoutParams = ViewGroup.LayoutParams(innerSize, innerSize)
      radius = innerSize / 2f
      strokeWidth = nestedContext.dpToPx(1f)
      strokeColor = nestedContext.getAttrColor(R.attr.colorOutline)
      setCardBackgroundColor(nestedContext.getAttrColor(R.attr.colorPrimaryContainer))
      isCheckable = false
    }
    addView(innerCard)
    checkedIconTint = ColorStateList.valueOf(
      nestedContext.getAttrColor(R.attr.colorOnPrimaryContainer)
    )
  }

  fun startCheckedIcon() {
    try {
      val layers = checkedIcon as? LayerDrawable
      layers?.findDrawableByLayerId(R.id.icon_selection_check)?.start()
    } catch (ignored: ClassCastException) {
      // For API 21 it will be a androidx.core.graphics.drawable.WrappedDrawableApi21
    }
  }
}
