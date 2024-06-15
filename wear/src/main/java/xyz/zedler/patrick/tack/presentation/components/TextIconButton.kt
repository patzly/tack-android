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

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize

@Composable
fun TextIconButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  FilledTonalIconButton(
    onClick = onClick,
    colors = IconButtonDefaults.filledTonalIconButtonColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    modifier = modifier.touchTargetAwareSize(IconButtonDefaults.SmallButtonSize)
  ) {
    Text(
      modifier = Modifier.wrapContentSize(Alignment.Center),
      textAlign = TextAlign.Center,
      color = IconButtonDefaults.filledTonalIconButtonColors().contentColor,
      style = MaterialTheme.typography.titleLarge,
      text = label
    )
  }
}