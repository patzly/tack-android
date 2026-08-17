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

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@SuppressLint("PrivateResource")
@Composable
fun ScrollableAlertDialog(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  horizontalBasePadding: Dp = 32.dp,
  verticalBasePadding: Dp = 8.dp,
  properties: DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false
  ),
  content: @Composable () -> Unit
) {
  Dialog(onDismissRequest = onDismissRequest, properties = properties) {
    val dialogPaneDescription = stringResource(androidx.compose.material3.R.string.m3c_dialog)

    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    val horizontalPadding = maxOf(
      safePadding.calculateStartPadding(layoutDirection),
      safePadding.calculateEndPadding(layoutDirection)
    ) + horizontalBasePadding

    Box(
      modifier = Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismissRequest
        )
        .padding(
          start = horizontalPadding,
          end = horizontalPadding,
          top = safePadding.calculateTopPadding() + verticalBasePadding,
          bottom = safePadding.calculateBottomPadding() + verticalBasePadding
        ),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = modifier
          .sizeIn(
            minWidth = 280.dp,
            maxWidth = 560.dp,
            maxHeight = 700.dp
          )
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { /* Consume clicks to prevent closing the dialog */ }
          )
          .semantics { this.paneTitle = dialogPaneDescription },
        propagateMinConstraints = true
      ) {
        content()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollableAlertDialogContent(
  title: @Composable (() -> Unit)?,
  confirmButton: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  icon: @Composable (() -> Unit)? = null,
  subtitle: @Composable (() -> Unit)? = null,
  dismissButton: @Composable (() -> Unit)? = null,
  extraButton: @Composable (() -> Unit)? = null,
  scrollState: androidx.compose.foundation.ScrollState? = rememberScrollState(),
  isScrollableControlledByContent: Boolean = false,
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
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(vertical = 24.dp)) {
      icon?.let {
        CompositionLocalProvider(LocalContentColor provides iconContentColor) {
          Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
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
                top = if (icon == null) 0.dp else 16.dp,
                bottom = if (subtitle == null) 16.dp else 4.dp,
              )
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

      subtitle?.let {
        ProvideContentColorTextStyle(
          contentColor = textContentColor,
          textStyle = MaterialTheme.typography.bodyMedium
        ) {
          Box(
            Modifier
              .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 16.dp)
              .align(
                if (icon == null) {
                  Alignment.Start
                } else {
                  Alignment.CenterHorizontally
                }
              )
          ) {
            subtitle()
          }
        }
      }

      val isScrollable by remember(isScrollableControlledByContent) {
        derivedStateOf {
          if (isScrollableControlledByContent) true
          else scrollState?.let { it.maxValue > 0 } ?: false
        }
      }

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
              .then(
                if (!isScrollableControlledByContent && scrollState != null) {
                  Modifier.verticalScroll(scrollState)
                } else {
                  Modifier
                }
              )
          ) {
            Box(
              modifier = Modifier
                .padding(
                  vertical = if (isScrollable && !isScrollableControlledByContent) 16.dp else 0.dp,
                  horizontal = 24.dp
                )
            ) {
              content.invoke()
            }
          }
        }
      }

      if (isScrollable) {
        HorizontalDivider()
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 24.dp, start = 24.dp, end = 24.dp)
      ) {
        AlertDialogCombinedButtons(
          confirmButton = confirmButton,
          dismissButton = dismissButton,
          extraButton = extraButton,
          buttonContentColor = buttonContentColor
        )
      }
    }
  }
}

@Composable
private fun AlertDialogCombinedButtons(
  confirmButton: @Composable () -> Unit,
  dismissButton: @Composable (() -> Unit)?,
  extraButton: @Composable (() -> Unit)?,
  buttonContentColor: Color
) {
  ProvideContentColorTextStyle(
    contentColor = buttonContentColor,
    textStyle = MaterialTheme.typography.labelLarge,
    content = {
      val buttonPaddingFromMICS =
        LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } -
            ButtonDefaults.MinHeight
      val p = (8.dp - buttonPaddingFromMICS).coerceAtLeast(0.dp)

      Layout(
        content = {
          Box(Modifier.layoutId("extra")) { extraButton?.invoke() }
          Box(Modifier.layoutId("dismiss")) { dismissButton?.invoke() }
          Box(Modifier.layoutId("confirm")) { confirmButton() }
        }
      ) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val extraPlaceable = measurables.find { it.layoutId == "extra" }
          ?.measure(looseConstraints)
        val dismissPlaceable = measurables.find { it.layoutId == "dismiss" }
          ?.measure(looseConstraints)
        val confirmPlaceable = measurables.find { it.layoutId == "confirm" }
          ?.measure(looseConstraints)

        val hasExtra = extraPlaceable != null && extraPlaceable.width > 0
        val hasDismiss = dismissPlaceable != null && dismissPlaceable.width > 0

        val wExtra = if (hasExtra) extraPlaceable.width else 0
        val wDismiss = if (hasDismiss) dismissPlaceable.width else 0
        val wConfirm = confirmPlaceable!!.width

        val hExtra = if (hasExtra) extraPlaceable.height else 0
        val hDismiss = if (hasDismiss) dismissPlaceable.height else 0
        val hConfirm = confirmPlaceable.height

        val pPx = p.roundToPx()
        val thresholdPx = 8.dp.roundToPx()

        val groupWidth = wConfirm + (if (hasDismiss) pPx + wDismiss else 0)
        val gap = constraints.maxWidth - wExtra - groupWidth

        val isStacked = (hasExtra && gap < thresholdPx) || groupWidth > constraints.maxWidth

        if (isStacked) {
          val height = (if (hasExtra) hExtra + pPx else 0) +
              hConfirm + (if (hasDismiss) hDismiss + pPx else 0)
          layout(constraints.maxWidth, height) {
            var y = 0
            if (hasExtra) {
              extraPlaceable.placeRelative(constraints.maxWidth - wExtra, y)
              y += hExtra + pPx
            }
            confirmPlaceable.placeRelative(constraints.maxWidth - wConfirm, y)
            y += hConfirm + pPx
            if (hasDismiss) {
              dismissPlaceable.placeRelative(constraints.maxWidth - wDismiss, y)
            }
          }
        } else {
          val height = maxOf(hExtra, hConfirm, hDismiss)
          layout(constraints.maxWidth, height) {
            if (hasExtra) {
              extraPlaceable.placeRelative(0, (height - hExtra) / 2)
            }
            confirmPlaceable.placeRelative(
              constraints.maxWidth - wConfirm,
              (height - hConfirm) / 2
            )
            if (hasDismiss) {
              dismissPlaceable.placeRelative(
                constraints.maxWidth - wConfirm - pPx - wDismiss,
                (height - hDismiss) / 2
              )
            }
          }
        }
      }
    }
  )
}

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
