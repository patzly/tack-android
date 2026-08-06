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
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.text.HtmlCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.textview.MaterialTextView
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.util.getAttrColor
import xyz.zedler.patrick.tack.util.dpToPx
import androidx.core.net.toUri

class FormattedTextView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

  private var textColor = context.getAttrColor(R.attr.colorOnSurface)
  private var textColorVariant = context.getAttrColor(R.attr.colorOnSurfaceVariant)
  private var isDialog = false

  init {
    orientation = VERTICAL
    setPadding(0, context.dpToPx(16f), 0, 0)
  }

  fun setText(text: String, vararg highlights: String) {
    removeAllViews()

    val parts = text.split("\n\n")
    parts.forEachIndexed { i, p ->
      var part = p
      val partNext = if (i < parts.size - 1) parts[i + 1] else ""

      highlights.forEach { highlight ->
        part = part.replace(highlight, "<b>$highlight</b>")
      }
      part = part.replace("\n", "<br/>")

      when {
        part.startsWith("__") -> {
          addView(getMediumParagraph(part.substring(2)))
        }

        part.startsWith("#") -> {
          val keepDistance = !partNext.startsWith("=> ")
          val firstSpace = part.indexOf(' ')
          val prefixEnd = if (firstSpace != -1) firstSpace else part.length
          val prefix = part.substring(0, prefixEnd)
          val useTNum = prefix.endsWith("_")
          val h0 = if (useTNum) prefix.substring(0, prefix.length - 1) else prefix
          val headlineText = if (firstSpace != -1) part.substring(firstSpace + 1) else ""
          addView(getHeadline(h0.length, headlineText, keepDistance, useTNum))
        }

        part.startsWith("- ") -> {
          val bullets = part.trim().split("- ")
          bullets.filterIndexed { index, _ -> index > 0 }.forEachIndexed { index, bullet ->
            addView(getBullet(bullet, index == bullets.size - 2))
          }
        }

        part.startsWith("> ") || part.startsWith("=> ") -> {
          val link = part.substring(
            if (part.startsWith("> ")) 2 else 3
          ).trim().split(" ")
          addView(if (link.size == 1) getLink(link[0], link[0])
          else getLink(link[0], link[1]))
        }

        part.startsWith("? ") -> {
          addView(getMessage(part.substring(2), false))
        }

        part.startsWith("! ") -> {
          addView(getMessage(part.substring(2), true))
        }

        part.startsWith("---") -> {
          addView(getDivider())
        }

        else -> {
          val keepDistance = !partNext.startsWith("=> ")
              && !partNext.startsWith("__ ")
          if (isDialog) {
            addView(getMediumParagraph(part))
          } else {
            addView(getParagraph(part, keepDistance))
          }
        }
      }
    }
  }

  fun setTextColor(@ColorInt color: Int) {
    textColor = color
  }

  fun setIsDialog(isDialog: Boolean) {
    this.isDialog = isDialog
  }

  private fun getParagraph(text: String, keepDistance: Boolean): MaterialTextView {
    return MaterialTextView(
      ContextThemeWrapper(context, R.style.Widget_Tack_TextView_Paragraph)
    ).apply {
      layoutParams = getVerticalLayoutParams(16, if (keepDistance) 16 else 0)
      setTextColor(this@FormattedTextView.textColor)
      this.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
  }

  private fun getMediumParagraph(text: String): MaterialTextView {
    return MaterialTextView(
      ContextThemeWrapper(
        context, R.style.Widget_Tack_TextView_ListItem_Description
      )
    ).apply {
      layoutParams = getVerticalLayoutParams(16, 16)
      setTextColor(textColorVariant)
      this.text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
  }

  private fun getHeadline(
    h: Int,
    title: String,
    keepDistance: Boolean,
    useTNum: Boolean
  ): MaterialTextView {
    val textView = MaterialTextView(
      ContextThemeWrapper(context, R.style.Widget_Tack_TextView)
    ).apply {
      layoutParams = getVerticalLayoutParams(16, if (keepDistance) 16 else 0)
      this.text = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY)
      val resId = when (h) {
        1 -> R.style.TextAppearance_Tack_HeadlineLarge
        2 -> R.style.TextAppearance_Tack_HeadlineMedium
        3 -> R.style.TextAppearance_Tack_HeadlineSmall
        4 -> R.style.TextAppearance_Tack_TitleLarge
        else -> R.style.TextAppearance_Tack_TitleMedium
      }
      TextViewCompat.setTextAppearance(this, resId)
      setTextColor(this@FormattedTextView.textColor)
      fontFeatureSettings = if (useTNum) "tnum" else "normal"
    }
    return textView
  }

  private fun getLink(text: String, link: String): MaterialTextView {
    return MaterialTextView(
      ContextThemeWrapper(context, R.style.Widget_Tack_TextView_LabelLarge)
    ).apply {
      layoutParams = getVerticalLayoutParams(16, 16)
      setTextColor(context.getAttrColor(R.attr.colorPrimary))
      this.text = text
      setOnClickListener {
        context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
      }
    }
  }

  private fun getDivider(): View {
    return MaterialDivider(context).apply {
      layoutParams = LayoutParams(
        context.dpToPx(56f), LayoutParams.WRAP_CONTENT
      ).apply {
        setMargins(0, context.dpToPx(8f), 0, context.dpToPx(24f))
        gravity = Gravity.CENTER_HORIZONTAL
      }
    }
  }

  private fun getBullet(text: String, isLast: Boolean): LinearLayout {
    val bulletSize = context.dpToPx(4f)

    val viewBullet = View(context).apply {
      layoutParams = FrameLayout.LayoutParams(bulletSize, bulletSize).apply {
        rightMargin = context.dpToPx(6f)
        leftMargin = context.dpToPx(6f)
        gravity = Gravity.CENTER_VERTICAL
      }
      background = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setSize(bulletSize, bulletSize)
        setColor(this@FormattedTextView.textColor)
      }
    }

    val textViewHeight = MaterialTextView(
      ContextThemeWrapper(
        context,
        if (isDialog) R.style.Widget_Tack_TextView_BodyMedium
        else R.style.Widget_Tack_TextView
      )
    ).apply {
      layoutParams =
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
      this.text = "E"
      visibility = INVISIBLE
    }

    val frameLayout = FrameLayout(context).apply {
      layoutParams =
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
      addView(viewBullet)
      addView(textViewHeight)
    }

    val textView = MaterialTextView(
      ContextThemeWrapper(
        context,
        if (isDialog) R.style.Widget_Tack_TextView_BodyMedium
        else R.style.Widget_Tack_TextView
      )
    ).apply {
      layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
        weight = 1f
      }
      setTextColor(this@FormattedTextView.textColor)
      var cleanText = text
      if (cleanText.trim().endsWith("<br/>")) {
        cleanText = cleanText.trim().substring(0, cleanText.length - 5)
      }
      this.text = HtmlCompat.fromHtml(cleanText, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    return LinearLayout(context).apply {
      layoutParams = getVerticalLayoutParams(16, if (isLast) 16 else 8)
      addView(frameLayout)
      addView(textView)
    }
  }

  private fun getMessage(text: String, useErrorColors: Boolean): MaterialCardView {
    val colorSurface = context.getAttrColor(
      if (useErrorColors) R.attr.colorErrorContainer else R.attr.colorSurfaceContainerHighest
    )
    val colorOnSurface = context.getAttrColor(
      if (useErrorColors) R.attr.colorOnErrorContainer else R.attr.colorOnSurfaceVariant
    )
    val padding = context.dpToPx(16f)

    return MaterialCardView(context).apply {
      layoutParams = getVerticalLayoutParams(16, 16)
      setContentPadding(padding, padding, padding, padding)
      setCardBackgroundColor(colorSurface)
      strokeWidth = 0
      radius = padding.toFloat()

      addView(getParagraph(text, false).apply {
        layoutParams = getVerticalLayoutParams(0, 0)
        setTextColor(colorOnSurface)
      })
    }
  }

  private fun getVerticalLayoutParams(side: Int, bottom: Int): LayoutParams {
    val pxSide = context.dpToPx(side.toFloat())
    val pxBottom = context.dpToPx(bottom.toFloat())
    return LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
    ).apply {
      setMargins(pxSide, 0, pxSide, pxBottom)
    }
  }
}
