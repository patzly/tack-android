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
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.Tick
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.time.Duration.Companion.milliseconds

private val PendulumEasing = Easing { fraction ->
  (cos((fraction + 1) * PI) / 2.0 + 0.5).toFloat()
}

private class PendulumLogic {
  var currentTarget: Float = -30f
  var wasPlaying: Boolean = false
}

@Composable
fun AnimatedLogo(
  isPlaying: Boolean,
  tempo: Int,
  tickEvent: Flow<Tick>,
  modifier: Modifier = Modifier
) {
  val intervalMillis = if (tempo > 0) 60000L / tempo else 1000L
  val currentInterval by rememberUpdatedState(intervalMillis)

  val rotationAnim = remember { Animatable(-30f) }

  val logic = remember { PendulumLogic() }

  LaunchedEffect(isPlaying, tickEvent) {
    if (isPlaying) {
      logic.wasPlaying = true
      val startTime = System.currentTimeMillis()

      launch {
        delay((currentInterval / 2L).milliseconds)
        logic.currentTarget = if (rotationAnim.value > 0) -30f else 30f

        rotationAnim.animateTo(
          targetValue = logic.currentTarget,
          animationSpec = tween(currentInterval.toInt(), easing = PendulumEasing)
        )
      }

      tickEvent.collect {
        if (System.currentTimeMillis() - startTime < 100L) return@collect

        launch {
          delay((currentInterval / 2L).milliseconds)
          logic.currentTarget = if (rotationAnim.value > 0) -30f else 30f

          rotationAnim.animateTo(
            targetValue = logic.currentTarget,
            animationSpec = tween(currentInterval.toInt(), easing = PendulumEasing)
          )
        }
      }
    } else if (logic.wasPlaying) {
      val current = rotationAnim.value
      val totalDistance = 60f
      val remainingFraction = abs(current - logic.currentTarget) / totalDistance

      if (remainingFraction > 0.01f) {
        val duration =
          (currentInterval * remainingFraction).toInt().coerceAtLeast(10)
        rotationAnim.animateTo(
          targetValue = logic.currentTarget,
          animationSpec = tween(duration, easing = LinearOutSlowInEasing)
        )
      }
      logic.wasPlaying = false
    }
  }

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_logo_bg),
      contentDescription = null
    )

    Image(
      painter = painterResource(id = R.drawable.ic_logo_pointer),
      contentDescription = null,
      modifier = Modifier
        .matchParentSize()
        .graphicsLayer {
          transformOrigin = TransformOrigin(0.5f, 0.7916f)
          rotationZ = rotationAnim.value
        }
    )

    Image(
      painter = painterResource(id = R.drawable.ic_logo_fg),
      contentDescription = null,
      modifier = Modifier.matchParentSize()
    )
  }
}
