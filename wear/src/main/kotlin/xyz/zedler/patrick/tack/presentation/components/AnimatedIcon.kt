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

package xyz.zedler.patrick.tack.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Icon

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun AnimatedIcon(
  @DrawableRes resId: Int,
  description: String,
  trigger: Boolean,
  modifier: Modifier = Modifier,
  animated: Boolean = true
) {
  val image = AnimatedImageVector.animatedVectorResource(resId)
  val painterForward = rememberAnimatedVectorPainter(
    animatedImageVector = image,
    atEnd = if (animated) trigger else false
  )
  val painterBackward = rememberAnimatedVectorPainter(
    animatedImageVector = image,
    atEnd = !trigger
  )
  Icon(
    modifier = modifier,
    painter = if (trigger || !animated) painterForward else painterBackward,
    contentDescription = description
  )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun AnimatedIcon(
  @DrawableRes resId1: Int,
  @DrawableRes resId2: Int,
  description: String,
  trigger: Boolean,
  modifier: Modifier = Modifier,
  animated: Boolean = true
) {
  val image1 = AnimatedImageVector.animatedVectorResource(resId1)
  val image2 = AnimatedImageVector.animatedVectorResource(resId2)
  val painterForward = rememberAnimatedVectorPainter(
    animatedImageVector = image1,
    atEnd = if (animated) trigger else true
  )
  val painterBackward = rememberAnimatedVectorPainter(
    animatedImageVector = image2,
    atEnd = if (animated) !trigger else true
  )
  Icon(
    modifier = modifier,
    painter = if (trigger) painterForward else painterBackward,
    contentDescription = description
  )
}