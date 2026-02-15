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

package xyz.zedler.patrick.tack.presentation.components

import android.graphics.Matrix
import android.graphics.Path
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.wear.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TempoTap(
  isTouched: Boolean,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.primary
) {
  val morph = remember {
    val shapeStart = MaterialShapes.VerySunny
    val shapeEnd = MaterialShapes.Sunny
    Morph(shapeStart, shapeEnd)
  }

  val animationProgress = remember { Animatable(0f) }
  LaunchedEffect(isTouched) {
    if (isTouched) {
      animationProgress.animateTo(
        targetValue = 1f,
        animationSpec = spring(
          dampingRatio = 0.9f,
          stiffness = 6000f
        )
      )
    } else {
      animationProgress.animateTo(
        targetValue = 0f,
        animationSpec = spring(
          dampingRatio = 0.4f,
          stiffness = 1400f
        )
      )
    }
  }

  val androidPath = remember { Path() }
  val matrix = remember { Matrix() }
  val strokeWidthPx = with(LocalDensity.current) { 2.dp.toPx() }

  Canvas(modifier = modifier) {
    androidPath.rewind()
    morph.toPath(progress = animationProgress.value, path = androidPath)

    matrix.reset()
    matrix.setScale(size.width, size.height)

    androidPath.transform(matrix)

    drawPath(
      path = androidPath.asComposePath(),
      color = color,
      style = Stroke(width = strokeWidthPx)
    )
  }
}