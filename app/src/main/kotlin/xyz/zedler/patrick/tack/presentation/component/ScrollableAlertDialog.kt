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

package xyz.zedler.patrick.tack.presentation.component

import android.provider.Settings.Global.getString
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollableAlertDialogContent(
  icon: @Composable (() -> Unit)?,
  title: @Composable (() -> Unit)?,
  subtitle: @Composable (() -> Unit)?,
  confirmButton: @Composable () -> Unit,
  dismissButton: @Composable (() -> Unit)?,
  modifier: Modifier = Modifier,
  iconContentColor: Color = MaterialTheme.colorScheme.secondary,
  titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
  textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  buttonContentColor: Color = MaterialTheme.colorScheme.primary,
  content: @Composable (() -> Unit)?
) {
  Surface(
    shape = AlertDialogDefaults.shape,
    color = AlertDialogDefaults.containerColor,
    tonalElevation = AlertDialogDefaults.TonalElevation,
    modifier = modifier.heightIn(max = 600.dp)
  ) {
    Column(modifier = Modifier.padding(vertical = 24.dp)) {
      icon?.let {
        CompositionLocalProvider(LocalContentColor provides iconContentColor) {
          Box(
            Modifier
              .padding(top = 24.dp)
              .align(Alignment.CenterHorizontally)
          ) {
            icon()
          }
        }
      }

      title?.let {
        ProvideContentColorTextStyle(
          contentColor = titleContentColor,
          textStyle = MaterialTheme.typography.headlineSmall
        ) {
          Box(
            // Align the title to the center when an icon is present.
            Modifier
              .padding(
                start = 24.dp,
                end = 24.dp,
                top = if (icon == null) 24.dp else 16.dp,
                bottom = if (subtitle == null) 16.dp else 4.dp,
              ).align(
                if (icon == null) {
                  Alignment.Start
                } else {
                  Alignment.CenterHorizontally
                }
              )
          ) {
            title()
          }
        }
      }

      subtitle?.let {
        ProvideContentColorTextStyle(
          contentColor = textContentColor,
          textStyle = MaterialTheme.typography.bodyMedium
        ) {
          Box(
            Modifier
              .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 16.dp)
              .align(Alignment.Start)
          ) {
            subtitle()
          }
        }
      }

      val scrollState = rememberScrollState()
      val isScrollable by remember { derivedStateOf { scrollState.maxValue > 0 } }

      if (isScrollable) {
        HorizontalDivider()
      }

      content?.let {
        ProvideContentColorTextStyle(
          contentColor = textContentColor,
          textStyle = MaterialTheme.typography.bodyMedium
        ) {
          Box(
            modifier = Modifier
              .weight(1f, fill = false)
              .align(Alignment.Start)
              .verticalScroll(scrollState)
          ) {
            Box(
              modifier = Modifier
                .padding(
                  vertical = if (isScrollable) 16.dp else 0.dp,
                  horizontal = 24.dp
                )
            ) {
              content()
            }
          }
        }
      }

      if (isScrollable) {
        HorizontalDivider()
      }

      Box(
        modifier = Modifier
          .padding(top = 24.dp)
          .align(Alignment.End)
      ) {
        ProvideContentColorTextStyle(
          contentColor = buttonContentColor,
          textStyle = MaterialTheme.typography.labelLarge,
          content = {
            val buttonPaddingFromMICS =
              LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } -
                  ButtonDefaults.MinHeight
            AlertDialogFlowRow(
              mainAxisSpacing = ButtonsMainAxisSpacing,
              crossAxisSpacing = (8.dp - buttonPaddingFromMICS).coerceIn(0.dp, 8.dp)
            ) {
              confirmButton()
              dismissButton?.invoke()
            }
          },
        )
      }
    }
  }
}

@Composable
fun AlertDialogImpl(
  onDismissRequest: () -> Unit,
  confirmButton: @Composable () -> Unit,
  modifier: Modifier,
  dismissButton: @Composable (() -> Unit)?,
  icon: @Composable (() -> Unit)?,
  title: @Composable (() -> Unit)?,
  text: @Composable (() -> Unit)?,
  shape: Shape,
  containerColor: Color,
  iconContentColor: Color,
  titleContentColor: Color,
  textContentColor: Color,
  tonalElevation: Dp,
  properties: DialogProperties,
) {
  BasicAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    properties = properties,
  ) {
    AlertDialogContent(
      buttons = {
        val buttonPaddingFromMICS =
          LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } -
              ButtonDefaults.MinHeight
        AlertDialogFlowRow(
          mainAxisSpacing = ButtonsMainAxisSpacing,
          crossAxisSpacing =
            (ButtonsCrossAxisSpacing - buttonPaddingFromMICS).coerceIn(
              0.dp,
              ButtonsCrossAxisSpacing,
            ),
        ) {
          confirmButton()
          dismissButton?.invoke()
        }
      },
      icon = icon,
      title = title,
      text = text,
      shape = shape,
      containerColor = containerColor,
      tonalElevation = tonalElevation,
      // Note that a button content color is provided here from the dialog's token, but in
      // most cases, TextButtons should be used for dismiss and confirm buttons. TextButtons
      // will not consume this provided content color value, and will used their own defined
      // or default colors.
      buttonContentColor = MaterialTheme.colorScheme.primary,
      iconContentColor = iconContentColor,
      titleContentColor = titleContentColor,
      textContentColor = textContentColor,
    )
  }
}

@Composable
internal fun AlertDialogContent(
  buttons: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  icon: (@Composable () -> Unit)?,
  title: (@Composable () -> Unit)?,
  text: (@Composable () -> Unit)?,
  shape: Shape,
  containerColor: Color,
  tonalElevation: Dp,
  buttonContentColor: Color,
  iconContentColor: Color,
  titleContentColor: Color,
  textContentColor: Color,
) {
  Surface(
    modifier = modifier,
    shape = shape,
    color = containerColor,
    tonalElevation = tonalElevation,
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      icon?.let {
        CompositionLocalProvider(LocalContentColor provides iconContentColor) {
          Box(Modifier.padding(IconPadding).align(Alignment.CenterHorizontally)) {
            icon()
          }
        }
      }
      title?.let {
        ProvideContentColorTextStyle(
          contentColor = titleContentColor,
          textStyle = MaterialTheme.typography.headlineSmall,
        ) {
          Box(
            // Align the title to the center when an icon is present.
            Modifier.padding(TitlePadding)
              .align(
                if (icon == null) {
                  Alignment.Start
                } else {
                  Alignment.CenterHorizontally
                }
              )
          ) {
            title()
          }
        }
      }
      text?.let {
        ProvideContentColorTextStyle(
          contentColor = textContentColor,
          textStyle = MaterialTheme.typography.bodyMedium,
        ) {
          Box(
            Modifier.weight(weight = 1f, fill = false)
              .padding(24.dp)
              .align(Alignment.Start)
          ) {
            text()
          }
        }
      }
      Box(modifier = Modifier.align(Alignment.End)) {
        ProvideContentColorTextStyle(
          contentColor = buttonContentColor,
          textStyle = MaterialTheme.typography.labelLarge,
          content = buttons,
        )
      }
    }
  }
}

/**
 * [FlowRow] for dialog buttons. The confirm button is expected to be the first child of [content].
 */
@Composable
internal fun AlertDialogFlowRow(
  mainAxisSpacing: Dp,
  crossAxisSpacing: Dp,
  content: @Composable () -> Unit,
) {
  val originalLayoutDirection = LocalLayoutDirection.current
  // The confirm button comes BEFORE the dismiss button when stacked vertically,
  // but AFTER the dismiss button when stacked horizontally.
  CompositionLocalProvider(LocalLayoutDirection provides originalLayoutDirection.flip()) {
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
      verticalArrangement = Arrangement.spacedBy(crossAxisSpacing),
    ) {
      CompositionLocalProvider(
        LocalLayoutDirection provides originalLayoutDirection,
        content = content,
      )
    }
  }
}

private fun LayoutDirection.flip(): LayoutDirection =
  when (this) {
    LayoutDirection.Ltr -> LayoutDirection.Rtl
    LayoutDirection.Rtl -> LayoutDirection.Ltr
  }

internal val DialogMinWidth
  get() = 280.dp
internal val DialogMaxWidth
  get() = 560.dp

private val ButtonsMainAxisSpacing
  get() = 8.dp
private val ButtonsCrossAxisSpacing
  get() = 8.dp

private val IconPadding = PaddingValues(bottom = 16.dp)
private val TitlePadding = PaddingValues(bottom = 16.dp)

/**
 * A convenience method to provide values to both [LocalContentColor] and [LocalTextStyle] in one
 * call. This is less expensive than nesting calls to [CompositionLocalProvider].
 *
 * Text styles will be merged with the current value of [LocalTextStyle].
 */
@Composable
internal fun ProvideContentColorTextStyle(
  contentColor: Color,
  textStyle: TextStyle,
  content: @Composable () -> Unit,
) {
  val mergedStyle = LocalTextStyle.current.merge(textStyle)
  CompositionLocalProvider(
    LocalContentColor provides contentColor,
    LocalTextStyle provides mergedStyle,
    content = content,
  )
}