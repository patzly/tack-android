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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import xyz.zedler.patrick.tack.Constants.TICK_TYPE
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.util.AnimatedVectorDrawable

@Composable
fun BeatIconButton(
  tickType: String,
  index: Int,
  enabled: Boolean = true,
  onClick: () -> Unit,
  animTrigger: Boolean
) {
  val shapes = listOf(
    R.drawable.ic_beat_star_anim,
    R.drawable.ic_beat_oval_anim,
    R.drawable.ic_beat_arrow_anim,
    R.drawable.ic_beat_clover_anim,
    R.drawable.ic_beat_pentagon_anim,
  )
  val sizeDefault = if (tickType != TICK_TYPE.MUTED) 24 else 12
  val sizeBeat = if (tickType != TICK_TYPE.MUTED) 32 else 24

  val animatedSize = remember { Animatable(sizeDefault.toFloat()) }
  val isFirstExecution = remember { mutableStateOf(true) }
  LaunchedEffect(animTrigger, tickType) {
    if (!isFirstExecution.value) {
      animatedSize.animateTo(
        targetValue = sizeBeat.toFloat(),
        animationSpec = tween(durationMillis = 25)
      )
      animatedSize.animateTo(
        targetValue = sizeDefault.toFloat(),
        animationSpec = tween(durationMillis = 375)
      )
    } else {
      isFirstExecution.value = false
    }
  }

  IconButton(
    enabled = enabled,
    onClick = onClick,
    modifier = Modifier.size(IconButtonDefaults.ExtraSmallButtonSize)
  ) {
    val targetColor = when (tickType) {
      TICK_TYPE.STRONG -> MaterialTheme.colorScheme.error
      TICK_TYPE.SUB -> MaterialTheme.colorScheme.onSurfaceVariant
      TICK_TYPE.MUTED -> MaterialTheme.colorScheme.outline
      else -> MaterialTheme.colorScheme.primary
    }
    val color by animateColorAsState(
      targetValue = targetColor,
      label = "beatColor",
      animationSpec = TweenSpec(durationMillis = 400)
    )
    AnimatedVectorDrawable(
      resId = shapes[index % shapes.size],
      description = stringResource(id = R.string.action_tempo_tap),
      color = color,
      trigger = if (tickType != TICK_TYPE.MUTED) animTrigger else false,
      modifier = Modifier.size(animatedSize.value.dp)
    )
  }
}