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

@file:JvmName("ResUtil")

package xyz.zedler.patrick.tack.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import androidx.core.view.get
import androidx.core.view.size
import androidx.annotation.AttrRes
import androidx.annotation.DimenRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import xyz.zedler.patrick.tack.R

fun Context.getRawText(@RawRes resId: Int): String {
  return try {
    resources.openRawResource(resId).bufferedReader().use { it.readText() }.trimEnd()
  } catch (e: Exception) {
    Log.e("ResUtil", "getRawText", e)
    ""
  }
}

fun Context.share(@StringRes resId: Int) {
  share(getString(resId))
}

fun Context.share(text: String) {
  val intent = Intent(Intent.ACTION_SEND).apply {
    putExtra(Intent.EXTRA_TEXT, text)
    type = "text/plain"
  }
  startActivity(Intent.createChooser(intent, null))
}

fun Context.getAttrColor(@AttrRes resId: Int): Int {
  return MaterialColors.getColor(this, resId, Color.BLACK)
}

fun Context.getAttrColor(@AttrRes resId: Int, alpha: Float): Int {
  return ColorUtils.setAlphaComponent(getAttrColor(resId), (alpha * 255).toInt())
}

fun Context.getSysColor(@AttrRes resId: Int): Int {
  val typedValue = TypedValue()
  theme.resolveAttribute(resId, typedValue, true)
  return typedValue.data
}

fun Context.getColorHighlight(): Int {
  return getAttrColor(R.attr.colorSecondary, 0.09f)
}

fun Menu.tintIcons(context: Context) {
  for (i in 0 until size) {
    get(i).icon?.tint(context)
  }
}

fun Drawable.tint(context: Context) {
  setTint(context.getAttrColor(R.attr.colorOnSurfaceVariant))
}

fun Context.getDimension(@DimenRes resId: Int): Int {
  return resources.getDimension(resId).toInt()
}
