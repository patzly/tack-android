package xyz.zedler.patrick.tack.presentation.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.zedler.patrick.tack.BuildConfig
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.component.AnimatedIcon
import xyz.zedler.patrick.tack.presentation.dialog.FeedbackDialog
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.presentation.util.LocalHaptic
import xyz.zedler.patrick.tack.util.UnlockUtil
import xyz.zedler.patrick.tack.viewmodel.MainViewModel

@Composable
fun AboutScreen(viewModel: MainViewModel = viewModel()) {
  val context = LocalContext.current
  val haptic = LocalHaptic.current
  val reduceAnim by viewModel.reduceAnim.collectAsStateWithLifecycle()
  val checkUnlockKey by viewModel.checkUnlockKey.collectAsStateWithLifecycle()

  var showFeedbackDialog by rememberSaveable { mutableStateOf(false) }

  if (showFeedbackDialog) {
    FeedbackDialog(
      checkUnlockKey = checkUnlockKey,
      onDismissRequest = { showFeedbackDialog = false },
      onSupportClick = { /* TODO: Show unlock dialog */ }
    )
  }

  val appWebsite = stringResource(R.string.app_website)
  val appVendingDev = stringResource(R.string.app_vending_dev)
  val appVendingKey = stringResource(R.string.app_vending_key)
  val appGithub = stringResource(R.string.app_github)
  val appTranslate = stringResource(R.string.app_translate)
  val appPrivacy = stringResource(R.string.app_privacy)
  val appVendingApp = stringResource(R.string.app_vending_app)
  val recommendText = stringResource(R.string.msg_recommend, appVendingApp)

  AboutContent(
    reduceAnim = reduceAnim,
    versionName = BuildConfig.VERSION_NAME,
    isKeyInstalled = UnlockUtil.isKeyInstalled(context),
    isPlayStoreInstalled = UnlockUtil.isPlayStoreInstalled(context),
    checkUnlockKey = checkUnlockKey,
    onBack = {
      viewModel.popBackstack()
      haptic.click()
    },
    onDeveloperClick = {
      context.startActivity(Intent(Intent.ACTION_VIEW, appWebsite.toUri()))
    },
    onChangelogClick = { /* TODO: Implement actual dialog */ },
    onVendingClick = {
      context.startActivity(Intent(Intent.ACTION_VIEW, appVendingDev.toUri()))
    },
    onKeyClick = {
      if (UnlockUtil.isKeyInstalled(context)) {
        context.startActivity(
          Intent(Intent.ACTION_VIEW, appVendingKey.toUri())
        )
      } else {
        // TODO: Show unlock dialog
      }
    },
    onKeyLongClick = {},
    onGithubClick = {
      context.startActivity(Intent(Intent.ACTION_VIEW, appGithub.toUri()))
    },
    onTranslationClick = {
      context.startActivity(Intent(Intent.ACTION_VIEW, appTranslate.toUri()))
    },
    onPrivacyClick = {
      context.startActivity(Intent(Intent.ACTION_VIEW, appPrivacy.toUri()))
    },
    onLicenseClick = { /* TODO: Implement actual dialogs */ },
    onFeedbackClick = { showFeedbackDialog = true },
    onHelpClick = { /* TODO: Implement actual dialog */ },
    onRecommendClick = {
      val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, recommendText)
        type = "text/plain"
      }
      context.startActivity(Intent.createChooser(sendIntent, null))
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
  onBack: () -> Unit = {},
  onDeveloperClick: () -> Unit = {},
  onChangelogClick: () -> Unit = {},
  onVendingClick: () -> Unit = {},
  onKeyClick: () -> Unit = {},
  onKeyLongClick: () -> Unit = {},
  onGithubClick: () -> Unit = {},
  onTranslationClick: () -> Unit = {},
  onPrivacyClick: () -> Unit = {},
  onLicenseClick: (Int) -> Unit = {},
  onFeedbackClick: () -> Unit = {},
  onHelpClick: () -> Unit = {},
  onRecommendClick: () -> Unit = {}
) {
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
              onClick = onBack,
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

          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
              TooltipAnchorPosition.Below
            ),
            tooltip = {
              PlainTooltip {
                Text(stringResource(R.string.action_more))
              }
            },
            state = rememberTooltipState(),
          ) {
            FilledIconButton(
              onClick = { showMenu = true },
              modifier =
                Modifier
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
                onClick = {
                  showMenu = false
                  onHelpClick()
                },
                shape = MenuDefaults.itemShape(0, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_recommend)) },
                onClick = {
                  showMenu = false
                  onRecommendClick()
                },
                shape = MenuDefaults.itemShape(1, itemCount).shape
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_send_feedback)) },
                onClick = {
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
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .consumeWindowInsets(padding),
      contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = padding.calculateTopPadding() + 16.dp,
        bottom = padding.calculateBottomPadding() + 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 4
          val colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
          )

          var changelogIconTrigger by remember { mutableStateOf(false) }

          SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = colors,
            overlineContent = {
              Text(stringResource(R.string.about_version))
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_info),
                  contentDescription = null
                )
              }
            },
            content = { Text(versionName) },
          )

          SegmentedListItem(
            onClick = {
              onChangelogClick()
              changelogIconTrigger = !changelogIconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.about_changelog_description))
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
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
            onClick = onDeveloperClick,
            shapes = ListItemDefaults.segmentedShapes(index = 2, count = itemCount),
            colors = colors,
            overlineContent = {
              Text(stringResource(R.string.about_developer))
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_person),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.app_developer)) },
          )

          SegmentedListItem(
            onClick = onVendingClick,
            shapes = ListItemDefaults.segmentedShapes(index = 3, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.about_vending_description))
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
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
        item {
          Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            val itemCount = 1
            val colors = ListItemDefaults.colors(
              containerColor = MaterialTheme.colorScheme.surfaceBright
            )

            SegmentedListItem(
              onClick = onKeyClick,
              onLongClick = onKeyLongClick,
              shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
              colors = colors,
              supportingContent = {
                val keyDescription = when {
                  isKeyInstalled -> stringResource(R.string.about_key_description_installed)
                  !checkUnlockKey -> stringResource(R.string.about_key_description_ignored)
                  else -> stringResource(R.string.about_key_description_not_installed)
                }
                Text(keyDescription)
              },
              leadingContent = {
                Box(modifier = Modifier.padding(vertical = 10.dp)) {
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

      item {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 3
          val colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
          )

          SegmentedListItem(
            onClick = onGithubClick,
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.about_github_description))
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_code),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.about_github)) },
          )

          SegmentedListItem(
            onClick = {
              onTranslationClick()
            },
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.about_translation_description))
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_translate),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.about_translation)) },
          )

          SegmentedListItem(
            onClick = onPrivacyClick,
            shapes = ListItemDefaults.segmentedShapes(index = 2, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.about_privacy_description))
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
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

      item {
        Text(
          text = stringResource(R.string.title_licenses),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
          val itemCount = 2
          val colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
          )

          var copyright1IconTrigger by remember { mutableStateOf(false) }
          var copyright2IconTrigger by remember { mutableStateOf(false) }

          SegmentedListItem(
            onClick = {
              onLicenseClick(0)
              copyright1IconTrigger = !copyright1IconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.license_author_google))
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_copyright_anim,
                  trigger = copyright1IconTrigger,
                  animated = !reduceAnim
                )
              }
            },
            content = { Text(stringResource(R.string.license_google_sans_flex)) },
          )

          SegmentedListItem(
            onClick = {
              onLicenseClick(1)
              copyright2IconTrigger = !copyright2IconTrigger
            },
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = colors,
            supportingContent = {
              Text(stringResource(R.string.license_author_google))
            },
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                AnimatedIcon(
                  resId = R.drawable.ic_rounded_copyright_anim,
                  trigger = copyright2IconTrigger,
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

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
  TackTheme {
    AboutContent()
  }
}
