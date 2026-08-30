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
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import xyz.zedler.patrick.tack.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedButtonGroup(
  options: List<String>,
  labels: List<String>,
  checked: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onCheckedChange: (String) -> Unit
) {
  ButtonGroup(
    expandedRatio = 0.0f,
    overflowIndicator = { menuState ->
      val contentDescription = stringResource(R.string.action_more)

      TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
          TooltipAnchorPosition.Above
        ),
        tooltip = {
          PlainTooltip {
            Text(contentDescription)
          }
        },
        state = rememberTooltipState(),
      ) {
        FilledIconButton(
          onClick = {
            if (menuState.isShowing) {
              menuState.dismiss()
            } else {
              menuState.show()
            }
          },
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .size(IconButtonDefaults.smallContainerSize()),
          colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          shapes = IconButtonDefaults.shapes()
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_rounded_more_vert),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    },
    horizontalArrangement = Arrangement.spacedBy(
      ButtonGroupDefaults.ConnectedSpaceBetween
    ),
    modifier = modifier
  ) {
    options.forEachIndexed { index, option ->
      val isSelected = option == checked

      customItem(
        buttonGroupContent = {
          ToggleButton(
            checked = isSelected,
            onCheckedChange = { if (enabled) onCheckedChange(option) },
            enabled = enabled,
            shapes = when (index) {
              0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
              options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
              else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            },
            modifier = Modifier.semantics { role = Role.RadioButton },
          ) {
            Text(
              text = labels[index],
              maxLines = 1
            )
          }
        },
        menuContent = {
          CheckableDropdownMenuItem(
            text = { Text(labels[index]) },
            checked = isSelected,
            onCheckedChange = { if (enabled) onCheckedChange(option) },
            shapes = MenuDefaults.itemShape(0, 1)
          )
        }
      )
    }
  }
}