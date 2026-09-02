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

package xyz.zedler.patrick.tack.ui.component.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.Tick
import kotlin.math.PI
import kotlin.math.cos

private val AccelerateDecelerate = Easing { fraction ->
  (cos((fraction + 1) * PI) / 2.0 + 0.5).toFloat()
}

@Composable
fun AnimatedLogo(
  tempo: Int,
  tickEvent: Flow<Tick>,
  modifier: Modifier = Modifier
) {
  val rotationAnim = remember { Animatable(-30f) }

  LaunchedEffect(tickEvent) {
    var isLeft = true

    tickEvent.collect { tick ->
      if (tick.subdivision != 1 || tick.isPoly) return@collect

      val interval = if (tempo > 0) 60000L / tempo else 1000L
      val target = if (isLeft) 30f else -30f
      isLeft = !isLeft

      launch {
        rotationAnim.animateTo(
          targetValue = target,
          animationSpec = tween(
            durationMillis = interval.toInt(),
            easing = AccelerateDecelerate
          )
        )
      }
    }
  }

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_logo_bg),
      contentDescription = null,
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
    )

    Image(
      painter = painterResource(id = R.drawable.ic_logo_pointer),
      contentDescription = null,
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
      modifier = Modifier
        .matchParentSize()
        .graphicsLayer {
          transformOrigin = TransformOrigin(0.5f, 0.7916f)
          rotationZ = rotationAnim.value
        }
    )

    Image(
      painter = painterResource(id = R.drawable.ic_logo_fg_fill),
      contentDescription = null,
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryContainer),
      modifier = Modifier.matchParentSize()
    )

    Image(
      painter = painterResource(id = R.drawable.ic_logo_fg_outline),
      contentDescription = null,
      colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
      modifier = Modifier.matchParentSize()
    )
  }
}
