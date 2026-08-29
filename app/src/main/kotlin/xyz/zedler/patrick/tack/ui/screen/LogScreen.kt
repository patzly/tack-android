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

package xyz.zedler.patrick.tack.ui.screen

import android.content.ClipData
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.component.core.AnimatedIcon
import xyz.zedler.patrick.tack.ui.component.core.InsetLazyColumn
import xyz.zedler.patrick.tack.ui.component.core.insetItem
import xyz.zedler.patrick.tack.ui.dialog.FeedbackDialog
import xyz.zedler.patrick.tack.ui.dialog.UnlockDialog
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic
import xyz.zedler.patrick.tack.viewmodel.MainViewModel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

@Composable
fun LogScreen(viewModel: MainViewModel) {
  val haptic = LocalHaptic.current
  val settings by viewModel.settings.collectAsState()
  val unlockState by viewModel.unlockState.collectAsState()

  var logText by remember { mutableStateOf("") }
  var reloadTrigger by remember { mutableStateOf(false) }

  var showFeedbackDialog by remember { mutableStateOf(false) }
  var showUnlockDialog by remember { mutableStateOf(false) }

  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()

  suspend fun loadLogcat() {
    val log = withContext(Dispatchers.IO) {
      val logBuilder = StringBuilder()
      try {
        val process = Runtime.getRuntime().exec("logcat -d *:E -t 300")
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
          var line: String?
          while (reader.readLine().also { line = it } != null) {
            logBuilder.append(line).append('\n')
          }
        }
        process.destroy()
      } catch (_: IOException) {
      }
      logBuilder.toString()
    }
    logText = log
  }

  LaunchedEffect(reloadTrigger) {
    loadLogcat()
  }

  if (showFeedbackDialog) {
    FeedbackDialog(
      checkUnlockKey = settings.checkUnlockKey,
      isKeyInstalled = unlockState.isKeyInstalled,
      isPlayStoreInstalled = unlockState.isPlayStoreInstalled,
      onDismissRequest = { showFeedbackDialog = false },
      onSupport = { showUnlockDialog = true }
    )
  }

  if (showUnlockDialog) {
    UnlockDialog(onDismissRequest = { showUnlockDialog = false })
  }

  LogContent(
    logText = logText,
    reduceAnim = settings.reduceAnim,
    reloadTrigger = reloadTrigger,
    onBackClick = {
      haptic.click()
      viewModel.popBackstack()
    },
    onReloadClick = {
      haptic.click()
      reloadTrigger = !reloadTrigger
    },
    onCopyClick = {
      haptic.click()
      scope.launch {
        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("logcat", logText)))
      }
    },
    onFeedbackClick = {
      haptic.click()
      showFeedbackDialog = true
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogContent(
  logText: String = "java.lang.IllegalStateException",
  reduceAnim: Boolean = false,
  reloadTrigger: Boolean = false,
  onBackClick: () -> Unit = {},
  onReloadClick: () -> Unit = {},
  onCopyClick: () -> Unit = {},
  onFeedbackClick: () -> Unit = {}
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      LargeTopAppBar(
        title = {
          Text(
            stringResource(R.string.title_logcat),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        navigationIcon = {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              TooltipAnchorPosition.Below
            ),
            tooltip = {
              PlainTooltip {
                Text(stringResource(R.string.action_back))
              }
            },
            state = rememberTooltipState(),
          ) {
            FilledIconButton(
              onClick = onBackClick,
              colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
              ),
              shapes = IconButtonDefaults.shapes()
            ) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_arrow_back),
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        },
        actions = {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              TooltipAnchorPosition.Below
            ),
            tooltip = {
              PlainTooltip {
                Text(stringResource(R.string.action_reload))
              }
            },
            state = rememberTooltipState(),
          ) {
            FilledIconButton(
              onClick = onReloadClick,
              colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
              ),
              shapes = IconButtonDefaults.shapes()
            ) {
              AnimatedIcon(
                resId = R.drawable.ic_rounded_refresh_anim,
                trigger = reloadTrigger,
                animated = !reduceAnim,
                description = stringResource(R.string.action_reload),
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        scrollBehavior = scrollBehavior,
      )
    },
    containerColor = MaterialTheme.colorScheme.surfaceContainer
  ) { padding ->
    InsetLazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(padding),
      contentPadding = PaddingValues(
        top = padding.calculateTopPadding() + 16.dp,
        bottom = padding.calculateBottomPadding() + 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
      insetItem {
        Surface(
          color = MaterialTheme.colorScheme.surfaceBright,
          shape = ListItemDefaults.segmentedShapes(index = 0, count = 2).shape
        ) {
          Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = stringResource(R.string.msg_report_crash),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val interactionSources = remember { List(2) { MutableInteractionSource() } }

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
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.align(Alignment.End)
            ) {
              customItem(
                buttonGroupContent = {
                  val contentPadding = ButtonDefaults.TextButtonContentPadding
                  val layoutDirection = LocalLayoutDirection.current

                  FilledTonalButton(
                    onClick = onCopyClick,
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(0.dp),
                    interactionSource = interactionSources[0],
                    modifier =
                      Modifier.animateWidth(
                        interactionSource = interactionSources[0],
                        compressionLimit = contentPadding.calculateStartPadding(layoutDirection)
                      ),
                  ) {
                    Text(
                      text = stringResource(R.string.action_copy_to_clipboard),
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Visible,
                      modifier = Modifier
                        .wrapContentWidth(unbounded = true)
                        .padding(contentPadding),
                    )
                  }
                },
                menuContent = {
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_copy_to_clipboard)) },
                    onClick = onCopyClick
                  )
                }
              )

              customItem(
                buttonGroupContent = {
                  val contentPadding = ButtonDefaults.TextButtonContentPadding
                  val layoutDirection = LocalLayoutDirection.current

                  Button(
                    onClick = onFeedbackClick,
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(0.dp),
                    interactionSource = interactionSources[1],
                    modifier =
                      Modifier.animateWidth(
                        interactionSource = interactionSources[1],
                        compressionLimit = contentPadding.calculateEndPadding(layoutDirection)
                      ),
                  ) {
                    Text(
                      text = stringResource(R.string.action_send_feedback),
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Visible,
                      modifier = Modifier
                        .wrapContentWidth(unbounded = true)
                        .padding(contentPadding)
                    )
                  }
                },
                menuContent = {
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_copy_to_clipboard)) },
                    onClick = onFeedbackClick
                  )
                }
              )
            }
          }
        }
      }

      insetItem {
        Surface(
          color = MaterialTheme.colorScheme.surfaceBright,
          shape = ListItemDefaults.segmentedShapes(index = 1, count = 2).shape
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(16.dp)
          ) {
            Text(
              text = logText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun LogScreenPreview() {
  TackTheme {
    LogContent()
  }
}
