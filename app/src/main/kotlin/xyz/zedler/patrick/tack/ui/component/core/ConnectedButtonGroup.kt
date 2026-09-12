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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEachIndexed
import xyz.zedler.patrick.tack.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ConnectedButtonGroup(
  options: List<T>,
  checked: T,
  onCheckedChange: (T) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  label: @Composable (T) -> String = { it.toString() }
) {
  if (options.isEmpty()) return

  ButtonGroup(
    expandedRatio = 0.0f,
    overflowIndicator = { menuState ->
      val contentDescription = stringResource(R.string.action_more)

      TooltipWrapper(text = contentDescription) {
        FilledIconButton(
          onClick = {
            if (menuState.isShowing) menuState.dismiss() else menuState.show()
          },
          enabled = enabled,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(IconButtonDefaults.smallContainerSize()),
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          shapes = IconButtonDefaults.shapes()
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_rounded_more_vert),
            contentDescription = contentDescription
          )
        }
      }
    },
    horizontalArrangement = Arrangement.spacedBy(
      ButtonGroupDefaults.ConnectedSpaceBetween
    ),
    modifier = modifier
  ) {
    options.fastForEachIndexed { index, option ->
      val isSelected = option == checked

      customItem(
        buttonGroupContent = {
          ToggleButton(
            checked = isSelected,
            onCheckedChange = {
              if (enabled && !isSelected) {
                onCheckedChange(option)
              }
            },
            enabled = enabled,
            shapes = when {
              options.size == 1 -> ToggleButtonDefaults.shapesFor(
                ToggleButtonSize.Small
              )
              index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
              index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
              else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
          ) {
            Text(
              text = label(option),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        },
        menuContent = { menuState ->
          SelectableDropdownMenuItem(
            text = { Text(label(option)) },
            selected = isSelected,
            enabled = enabled,
            onClick = {
              if (enabled) {
                if (!isSelected) {
                  onCheckedChange(option)
                }
                menuState.dismiss()
              }
            },
            shapes = MenuDefaults.itemShape(index, options.size)
          )
        }
      )
    }
  }
}
