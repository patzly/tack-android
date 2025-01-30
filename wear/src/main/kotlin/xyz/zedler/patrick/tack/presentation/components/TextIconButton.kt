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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.isSmallScreen

@Composable
fun TextIconButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  reduceAnim: Boolean = false,
  interactionSource: MutableInteractionSource? = null
) {
  FilledTonalIconButton(
    onClick = onClick,
    shapes = if (reduceAnim) {
      IconButtonDefaults.shapes()
    } else {
      IconButtonDefaults.animatedShapes(
        pressedShape = MaterialTheme.shapes.small
      )
    },
    colors = IconButtonDefaults.filledTonalIconButtonColors(
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ),
    interactionSource = interactionSource,
    modifier = modifier.touchTargetAwareSize(
      if (isSmallScreen()) 42.dp else IconButtonDefaults.SmallButtonSize
    )
  ) {
    Text(
      modifier = Modifier.wrapContentSize(Alignment.Center),
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.titleLarge,
      text = label
    )
  }
}

@Preview
@Composable
fun TextIconButtonPreview() {
  TackTheme {
    TextIconButton(
      label = "+1",
      onClick = {},
    )
  }
}