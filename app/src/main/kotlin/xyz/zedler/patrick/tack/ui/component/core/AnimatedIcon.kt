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

package xyz.zedler.patrick.tack.ui.component.core

import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun AnimatedIcon(
  @DrawableRes resId: Int,
  trigger: Boolean,
  modifier: Modifier = Modifier,
  animated: Boolean = true,
  contentDescription: String? = null,
  tint: Color = LocalContentColor.current
) {
  val image = AnimatedImageVector.animatedVectorResource(resId)

  val painter = if (animated) {
    rememberAnimatedVectorPainter(
      animatedImageVector = image,
      atEnd = trigger
    )
  } else {
    rememberVectorPainter(image.imageVector)
  }

  Icon(
    painter = painter,
    contentDescription = contentDescription,
    modifier = modifier,
    tint = tint
  )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun AnimatedIcon(
  @DrawableRes resId1: Int,
  @DrawableRes resId2: Int,
  trigger: Boolean,
  modifier: Modifier = Modifier,
  animated: Boolean = true,
  contentDescription: String? = null,
  tint: Color = LocalContentColor.current
) {
  val painter = if (animated) {
    val image1 = AnimatedImageVector.animatedVectorResource(resId1)
    val image2 = AnimatedImageVector.animatedVectorResource(resId2)

    val painterForward = rememberAnimatedVectorPainter(
      animatedImageVector = image1,
      atEnd = trigger
    )
    val painterBackward = rememberAnimatedVectorPainter(
      animatedImageVector = image2,
      atEnd = !trigger
    )

    if (trigger) painterForward else painterBackward
  } else {
    val staticResId = if (trigger) resId2 else resId1
    val staticImage = AnimatedImageVector.animatedVectorResource(staticResId)
    rememberVectorPainter(staticImage.imageVector)
  }

  Icon(
    painter = painter,
    contentDescription = contentDescription,
    modifier = modifier,
    tint = tint
  )
}
