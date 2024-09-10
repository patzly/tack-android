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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.util.isSmallScreen

@Composable
fun DialogDismissButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  label: String? = stringResource(id = R.string.wear_action_cancel),
  icon: @Composable (BoxScope.() -> Unit) = {
    Icon(
      painter = painterResource(id = R.drawable.ic_rounded_close),
      contentDescription = label
    )
  }
) {
  val dismissSize = if (isSmallScreen()) 56.dp else 64.dp
  FilledTonalIconButton(
    onClick = onClick,
    modifier = modifier.size(dismissSize),
    shape = MaterialTheme.shapes.medium
  ) {
    icon()
  }
}

@Preview
@Composable
fun DialogDismissButtonPreview() {
  TackTheme {
    DialogDismissButton(onClick = {})
  }
}