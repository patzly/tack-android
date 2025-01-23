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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.LocalTextStyle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import xyz.zedler.patrick.tack.presentation.theme.TackTheme

@Composable
fun WrapContentCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  backgroundPainter: Painter = ColorPainter(MaterialTheme.colorScheme.background),
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  border: BorderStroke? = null,
  contentPadding: PaddingValues = CardDefaults.ContentPadding,
  shape: Shape = MaterialTheme.shapes.large,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  enabled: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  BasicCard(
    onClick = onClick,
    modifier = modifier,
    border = border,
    containerPainter = backgroundPainter,
    enabled = enabled,
    contentPadding = contentPadding,
    shape = shape,
    interactionSource = interactionSource,
    ripple = LocalIndication.current
  ) {
    CompositionLocalProvider(
      LocalContentColor provides contentColor,
      LocalTextStyle provides MaterialTheme.typography.displayMedium,
    ) {
      content()
    }
  }
}

@Preview
@Composable
fun WrapContentCardPreview() {
  TackTheme {
    WrapContentCard(onClick = {}) {
      Column {
        Text("Preview")
      }
    }
  }
}

@Composable
private fun BasicCard(
  onClick: () -> Unit,
  modifier: Modifier,
  border: BorderStroke?,
  containerPainter: Painter,
  enabled: Boolean,
  contentPadding: PaddingValues,
  shape: Shape,
  interactionSource: MutableInteractionSource,
  ripple: Indication,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .width(IntrinsicSize.Min)
      .height(IntrinsicSize.Min)
      .clip(shape = shape)
      .paint(
        painter = containerPainter,
        contentScale = ContentScale.Crop
      )
      .clickable(
        enabled = enabled,
        onClick = onClick,
        indication = ripple,
        interactionSource = interactionSource,
      )
      .then(
        border?.let { Modifier.border(border = border, shape = shape) } ?: Modifier
      )
      .padding(contentPadding),
    content = content
  )
}