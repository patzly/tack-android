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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.AppSettings
import xyz.zedler.patrick.tack.core.model.MetronomeState
import xyz.zedler.patrick.tack.ui.component.core.AnimatedIcon
import xyz.zedler.patrick.tack.ui.theme.LocalTackDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainControls(
  settings: AppSettings,
  metronomeState: MetronomeState,
  onItemClick: () -> Unit,
  onOptionsClick: () -> Unit,
  onPlayStopClick: () -> Unit,
  onBeatModeClick: () -> Unit,
  modifier: Modifier
) {
  val dimens = LocalTackDimens.current

  val interactionSources = remember { List(3) { MutableInteractionSource() } }

  ButtonGroup(
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
          colors = IconButtonDefaults.filledTonalIconButtonColors(),
          shapes = IconButtonDefaults.shapes()
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_rounded_more_vert),
            contentDescription = contentDescription
          )
        }
      }
    },
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.padding(bottom = dimens.mainControlsPaddingBottom)
  ) {
    customItem(
      buttonGroupContent = {
        val contentPadding = ButtonDefaults.TextButtonContentPadding
        val layoutDirection = LocalLayoutDirection.current

        var optionsIconTrigger by remember { mutableStateOf(false) }

        FilledTonalIconButton(
          onClick = {
            onItemClick()
            onOptionsClick()
            optionsIconTrigger = !optionsIconTrigger
          },
          shape = IconButtonDefaults.largeRoundShape,
          interactionSource = interactionSources[0],
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(
              IconButtonDefaults.largeContainerSize(
                IconButtonDefaults.IconButtonWidthOption.Narrow
              )
            )
            .animateWidth(
              interactionSource = interactionSources[0],
              compressionLimit = contentPadding.calculateStartPadding(layoutDirection)
            ),
        ) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              TooltipAnchorPosition.Above
            ),
            tooltip = {
              PlainTooltip {
                Text(stringResource(R.string.title_options))
              }
            },
            state = rememberTooltipState(),
            modifier = Modifier.fillMaxSize()
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              AnimatedIcon(
                resId = R.drawable.ic_rounded_tune_anim,
                trigger = optionsIconTrigger,
                animated = !settings.reduceAnim,
                description = stringResource(R.string.title_options),
                modifier = Modifier.size(IconButtonDefaults.largeIconSize)
              )
            }
          }
        }
      },
      menuContent = {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.title_options)) },
          onClick = onOptionsClick
        )
      }
    )

    customItem(
      buttonGroupContent = {
        val contentPadding = ButtonDefaults.TextButtonContentPadding
        val layoutDirection = LocalLayoutDirection.current

        IconToggleButton(
          checked = false,
          onCheckedChange = {},
          shapes = IconButtonDefaults.toggleableShapes(),
          colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            checkedContainerColor = MaterialTheme.colorScheme.tertiary,
            checkedContentColor = MaterialTheme.colorScheme.onTertiary,
          ),
          interactionSource = interactionSources[1],
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .size(
                IconButtonDefaults.largeContainerSize(
                  IconButtonDefaults.IconButtonWidthOption.Wide
                )
              )
              .animateWidth(
                interactionSource = interactionSources[1],
                compressionLimit = contentPadding.calculateStartPadding(layoutDirection)
              ),
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_rounded_play_arrow),
            contentDescription = null,
            modifier = Modifier.size(IconButtonDefaults.largeIconSize)
          )
        }
      },
      menuContent = {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.title_options)) },
          onClick = onOptionsClick
        )
      }
    )

    customItem(
      buttonGroupContent = {
        val contentPadding = ButtonDefaults.TextButtonContentPadding
        val layoutDirection = LocalLayoutDirection.current

        FilledTonalIconButton(
          onClick = onOptionsClick,
          shape = IconButtonDefaults.largeRoundShape,
          interactionSource = interactionSources[2],
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .size(
                IconButtonDefaults.largeContainerSize(
                  IconButtonDefaults.IconButtonWidthOption.Narrow
                )
              )
              .animateWidth(
                interactionSource = interactionSources[2],
                compressionLimit = contentPadding.calculateStartPadding(layoutDirection)
              ),
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_rounded_more_vert),
            contentDescription = null,
            modifier = Modifier.size(IconButtonDefaults.largeIconSize)
          )
        }
      },
      menuContent = {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.title_options)) },
          onClick = onOptionsClick
        )
      }
    )
  }
}
