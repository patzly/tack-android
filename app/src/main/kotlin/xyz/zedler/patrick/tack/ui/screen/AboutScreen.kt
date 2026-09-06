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

import android.content.Intent
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.zedler.patrick.tack.BuildConfig
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.ui.component.core.AnimatedIcon
import xyz.zedler.patrick.tack.ui.component.core.InsetLazyColumn
import xyz.zedler.patrick.tack.ui.component.core.TooltipWrapper
import xyz.zedler.patrick.tack.ui.component.core.insetItem
import xyz.zedler.patrick.tack.ui.dialog.FeedbackDialog
import xyz.zedler.patrick.tack.ui.dialog.HelpDialog
import xyz.zedler.patrick.tack.ui.dialog.TextDialog
import xyz.zedler.patrick.tack.ui.dialog.UnlockDialog
import xyz.zedler.patrick.tack.ui.theme.LocalDimens
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic
import xyz.zedler.patrick.tack.util.rememberRawText
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

enum class ActiveDialog(
  @StringRes val titleRes: Int? = null,
  @RawRes val textRes: Int? = null,
  @StringRes val linkRes: Int? = null
) {
  FEEDBACK,
  HELP,
  UNLOCK,
  CHANGELOG(
    titleRes = R.string.about_changelog,
    textRes = R.raw.changelog
  ),
  LICENSE_FONT(
    titleRes = R.string.license_google_sans_flex,
    textRes = R.raw.license_ofl,
    linkRes = R.string.license_google_sans_flex_link
  ),
  LICENSE_ICONS(
    titleRes = R.string.license_material_icons,
    textRes = R.raw.license_ofl,
    linkRes = R.string.license_material_icons_link
  );
}

@Composable
fun AboutScreen(viewModel: MainViewModel) {
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current

  val appVendingKey = stringResource(R.string.app_vending_key)
  val appVendingApp = stringResource(R.string.app_vending_app)
  val recommendText = stringResource(R.string.msg_recommend, appVendingApp)

  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val unlockState by viewModel.unlockState.collectAsStateWithLifecycle()

  var keyLongClickCount by rememberSaveable { mutableIntStateOf(0) }

  var activeDialog by rememberSaveable { mutableStateOf<ActiveDialog?>(null) }
  val onDismissRequest = { activeDialog = null }

  when (val dialog = activeDialog) {
    ActiveDialog.FEEDBACK -> {
      FeedbackDialog(
        checkUnlockKey = unlockState.checkUnlockKey,
        isKeyInstalled = unlockState.isKeyInstalled,
        isPlayStoreInstalled = unlockState.isPlayStoreInstalled,
        onDismissRequest = onDismissRequest,
        onSupport = {
          activeDialog = ActiveDialog.UNLOCK
        }
      )
    }

    ActiveDialog.HELP -> {
      HelpDialog(onDismissRequest = onDismissRequest)
    }

    ActiveDialog.UNLOCK -> {
      UnlockDialog(
        onOpen = {
          uriHandler.openUri(appVendingKey)
        },
        onDismissRequest = onDismissRequest
      )
    }

    else -> {
      if (dialog != null && dialog.titleRes != null && dialog.textRes != null) {
        TextDialog(
          title = stringResource(dialog.titleRes),
          text = rememberRawText(dialog.textRes),
          link = dialog.linkRes?.let { stringResource(it) },
          onDismissRequest = onDismissRequest
        )
      }
    }
  }

  AboutContent(
    reduceAnim = settings.reduceAnim,
    versionName = BuildConfig.VERSION_NAME,
    isKeyInstalled = unlockState.isKeyInstalled,
    isPlayStoreInstalled = unlockState.isPlayStoreInstalled,
    checkUnlockKey = unlockState.checkUnlockKey,
    onBackClick = {
      viewModel.popBackstack()
    },
    onHelpClick = {
      activeDialog = ActiveDialog.HELP
    },
    onRecommendClick = {
      val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, recommendText)
        type = "text/plain"
      }
      context.startActivity(Intent.createChooser(sendIntent, null))
    },
    onFeedbackClick = {
      activeDialog = ActiveDialog.FEEDBACK
    },
    onChangelogClick = {
      activeDialog = ActiveDialog.CHANGELOG
    },
    onKeyClick = {
      if (unlockState.isKeyInstalled) {
        uriHandler.openUri(appVendingKey)
      } else {
        activeDialog = ActiveDialog.UNLOCK
      }
    },
    onKeyLongClick = {
      if (!unlockState.isKeyInstalled && unlockState.checkUnlockKey) {
        keyLongClickCount++
        if (keyLongClickCount >= 10) {
          viewModel.updateCheckUnlockKey(false)
          viewModel.refreshUnlockState()
        }
      }
    },
    onLicenseFontClick = {
      activeDialog = ActiveDialog.LICENSE_FONT
    },
    onLicenseIconsClick = {
      activeDialog = ActiveDialog.LICENSE_ICONS
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutContent(
  reduceAnim: Boolean = false,
  versionName: String = "1.0.0",
  isKeyInstalled: Boolean = false,
  isPlayStoreInstalled: Boolean = true,
  checkUnlockKey: Boolean = true,
  onBackClick: () -> Unit = {},
  onHelpClick: () -> Unit = {},
  onRecommendClick: () -> Unit = {},
  onFeedbackClick: () -> Unit = {},
  onChangelogClick: () -> Unit = {},
  onKeyClick: () -> Unit = {},
  onKeyLongClick: () -> Unit = {},
  onLicenseFontClick: () -> Unit = {},
  onLicenseIconsClick: () -> Unit = {}
) {
  val haptic = LocalHaptic.current

  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      LargeTopAppBar(
        title = {
          Text(
            stringResource(R.string.title_about),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        navigationIcon = {
          TooltipWrapper(
            text = stringResource(R.string.action_back),
            positioning = TooltipAnchorPosition.Below
          ) {
            FilledIconButton(
              onClick = haptic.withClick(onBackClick),
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
          var showMenu by remember { mutableStateOf(false) }

          TooltipWrapper(
            text = stringResource(R.string.action_more),
            positioning = TooltipAnchorPosition.Below
          ) {
            FilledIconButton(
              onClick = haptic.withClick { showMenu = true },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(
                  IconButtonDefaults.smallContainerSize(
                    IconButtonDefaults.IconButtonWidthOption.Narrow
                  )
                ),
              colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
              ),
              shapes = IconButtonDefaults.shapes()
            ) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_more_vert),
                contentDescription = stringResource(R.string.action_more),
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          DropdownMenuPopup(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
              MenuAnchorPosition.Below,
              offset = DpOffset(x = (-8).dp, 0.dp)
            )
          ) {
            val groupCount = 1

            DropdownMenuGroup(
              shapes = MenuDefaults.groupShape(0, groupCount),
            ) {
              val itemCount = 3

              DropdownMenuItem(
                text = { Text(stringResource(R.string.title_help)) },
                onClick = haptic.withClick {
                  showMenu = false
                  onHelpClick()
                },
                shape = MenuDefaults.itemShape(0, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_recommend)) },
                onClick = haptic.withClick {
                  showMenu = false
                  onRecommendClick()
                },
                shape = MenuDefaults.itemShape(1, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_send_feedback)) },
                onClick = haptic.withClick {
                  showMenu = false
                  onFeedbackClick()
                },
                shape = MenuDefaults.itemShape(2, itemCount).shape
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
    val uriHandler = LocalUriHandler.current

    val appWebsite = stringResource(R.string.app_website)
    val appVendingDev = stringResource(R.string.app_vending_dev)
    val appGithub = stringResource(R.string.app_github)
    val appTranslate = stringResource(R.string.app_translate)
    val appPrivacy = stringResource(R.string.app_privacy)

    val segmentedColors = ListItemDefaults.segmentedColors(
      containerColor = MaterialTheme.colorScheme.surfaceBright
    )

    InsetLazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(padding),
      contentPadding = PaddingValues(
        top = padding.calculateTopPadding() + 16.dp,
        bottom = padding.calculateBottomPadding() + 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      insetItem {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 4

          var changelogIconTrigger by remember { mutableStateOf(false) }

          SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = segmentedColors,
            overlineContent = {
              Text(stringResource(R.string.about_version))
            },
            leadingContent = {
              ItemLeadingIcon {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_info),
                  contentDescription = null
                )
              }
            },
            content = { Text(versionName) },
          )

          SegmentedListItem(
            onClick = haptic.withClick {
              onChangelogClick()
              changelogIconTrigger = !changelogIconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = segmentedColors,
            supportingContent = {
              Text(stringResource(R.string.about_changelog_description))
            },
            leadingContent = {
              ItemLeadingIcon {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_history_anim,
                  trigger = changelogIconTrigger,
                  animated = !reduceAnim
                )
              }
            },
            content = { Text(stringResource(R.string.about_changelog)) },
          )

          SegmentedListItem(
            onClick = haptic.withClick { uriHandler.openUri(appWebsite) },
            shapes = ListItemDefaults.segmentedShapes(index = 2, count = itemCount),
            colors = segmentedColors,
            overlineContent = {
              Text(stringResource(R.string.about_developer))
            },
            leadingContent = {
              ItemLeadingIcon {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_person),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.app_developer)) },
          )

          SegmentedListItem(
            onClick = haptic.withClick { uriHandler.openUri(appVendingDev) },
            shapes = ListItemDefaults.segmentedShapes(index = 3, count = itemCount),
            colors = segmentedColors,
            supportingContent = {
              Text(stringResource(R.string.about_vending_description))
            },
            leadingContent = {
              ItemLeadingIcon {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_shop),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.about_vending)) },
          )
        }
      }

      if (isPlayStoreInstalled) {
        insetItem {
          Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            val itemCount = 1

            SegmentedListItem(
              onClick = haptic.withClick(onKeyClick),
              onLongClick = onKeyLongClick,
              shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
              colors = segmentedColors,
              supportingContent = {
                val keyDescription = when {
                  isKeyInstalled -> stringResource(R.string.about_key_description_installed)
                  !checkUnlockKey -> stringResource(R.string.about_key_description_ignored)
                  else -> stringResource(R.string.about_key_description_not_installed)
                }
                Text(keyDescription)
              },
              leadingContent = {
                ItemLeadingIcon {
                  Icon(
                    painter = painterResource(R.drawable.ic_rounded_key),
                    contentDescription = null
                  )
                }
              },
              content = { Text(stringResource(R.string.about_key)) },
            )
          }
        }
      }

      insetItem {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 3

          SegmentedListItem(
            onClick = haptic.withClick { uriHandler.openUri(appGithub) },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = segmentedColors,
            supportingContent = {
              Text(stringResource(R.string.about_github_description))
            },
            leadingContent = {
              ItemLeadingIcon {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_code),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.about_github)) },
          )

          SegmentedListItem(
            onClick = haptic.withClick { uriHandler.openUri(appTranslate) },
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = segmentedColors,
            supportingContent = {
              Text(stringResource(R.string.about_translation_description))
            },
            leadingContent = {
              ItemLeadingIcon {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_translate),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.about_translation)) },
          )

          SegmentedListItem(
            onClick = haptic.withClick { uriHandler.openUri(appPrivacy) },
            shapes = ListItemDefaults.segmentedShapes(index = 2, count = itemCount),
            colors = segmentedColors,
            supportingContent = {
              Text(stringResource(R.string.about_privacy_description))
            },
            leadingContent = {
              ItemLeadingIcon {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_policy),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.about_privacy)) },
          )
        }
      }

      insetItem {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          Text(
            text = stringResource(R.string.title_licenses),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          val itemCount = 2

          var copyrightFontIconTrigger by remember { mutableStateOf(false) }
          var copyrightIconsIconTrigger by remember { mutableStateOf(false) }

          SegmentedListItem(
            onClick = haptic.withClick {
              onLicenseFontClick()
              copyrightFontIconTrigger = !copyrightFontIconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = segmentedColors,
            supportingContent = {
              Text(stringResource(R.string.license_author_google))
            },
            leadingContent = {
              ItemLeadingIcon {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_copyright_anim,
                  trigger = copyrightFontIconTrigger,
                  animated = !reduceAnim
                )
              }
            },
            content = { Text(stringResource(R.string.license_google_sans_flex)) },
          )

          SegmentedListItem(
            onClick = haptic.withClick {
              onLicenseIconsClick()
              copyrightIconsIconTrigger = !copyrightIconsIconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = segmentedColors,
            supportingContent = {
              Text(stringResource(R.string.license_author_google))
            },
            leadingContent = {
              ItemLeadingIcon {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_copyright_anim,
                  trigger = copyrightIconsIconTrigger,
                  animated = !reduceAnim
                )
              }
            },
            content = { Text(stringResource(R.string.license_material_icons)) },
          )
        }
      }
    }
  }
}

@Composable
private fun ItemLeadingIcon(content: @Composable () -> Unit) {
  val dimens = LocalDimens.current
  Box(
    modifier = Modifier.padding(
      vertical = dimens.segmentedListItemLeadingContentPaddingVertical
    )
  ) {
    content()
  }
}

@Preview(name = "Key not installed", showBackground = true)
@Composable
fun AboutScreenPreview() {
  TackTheme {
    AboutContent()
  }
}

@Preview(name = "Key installed", showBackground = true)
@Composable
fun AboutScreenUnlockedPreview() {
  TackTheme {
    AboutContent(isKeyInstalled = true)
  }
}

@Preview(name = "No Play Store", showBackground = true)
@Composable
fun AboutScreenNoPlayStorePreview() {
  TackTheme {
    AboutContent(isPlayStoreInstalled = false)
  }
}
