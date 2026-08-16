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

package xyz.zedler.patrick.tack.presentation.dialog

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.presentation.component.ScrollableAlertDialogContent
import xyz.zedler.patrick.tack.presentation.theme.TackTheme
import xyz.zedler.patrick.tack.presentation.util.LocalHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
  checkUnlockKey: Boolean,
  isKeyInstalled: Boolean,
  isPlayStoreInstalled: Boolean,
  onDismissRequest: () -> Unit,
  onSupportClick: () -> Unit
) {
  val context = LocalContext.current
  val haptic = LocalHaptic.current

  val appMail = stringResource(R.string.app_mail)
  val appGithub = stringResource(R.string.app_github)
  val appVendingApp = stringResource(R.string.app_vending_app)
  val recommendText = stringResource(R.string.msg_recommend, appVendingApp)
  val actionSendFeedback = stringResource(R.string.action_send_feedback)

  BasicAlertDialog(onDismissRequest = onDismissRequest) {
    FeedbackDialogContent(
      isSupportVisible = checkUnlockKey && isPlayStoreInstalled && !isKeyInstalled,
      onRateClick = {
        haptic.click()
        val uri = "market://details?id=${context.packageName}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
          addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
          )
        }
        try {
          context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
          context.startActivity(
            Intent(
              Intent.ACTION_VIEW,
              "http://play.google.com/store/apps/details?id=${context.packageName}"
                .toUri()
            )
          )
        }
        onDismissRequest()
      },
      onSupportClick = {
        haptic.click()
        onSupportClick()
        onDismissRequest()
      },
      onRecommendClick = {
        haptic.click()
        val sendIntent = Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_TEXT, recommendText)
          type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
        onDismissRequest()
      },
      onEmailClick = {
        haptic.click()
        val intent = Intent(Intent.ACTION_SENDTO).apply {
          data = "mailto:$appMail?subject=${Uri.encode("Feedback@Tack")}".toUri()
        }
        context.startActivity(
          Intent.createChooser(intent, actionSendFeedback)
        )
        onDismissRequest()
      },
      onIssueClick = {
        haptic.click()
        val issues = "$appGithub/issues"
        context.startActivity(Intent(Intent.ACTION_VIEW, issues.toUri()))
        onDismissRequest()
      },
      onCloseClick = {
        haptic.click()
        onDismissRequest()
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackDialogContent(
  isSupportVisible: Boolean = true,
  onRateClick: () -> Unit = {},
  onSupportClick: () -> Unit = {},
  onRecommendClick: () -> Unit = {},
  onIssueClick: () -> Unit = {},
  onEmailClick: () -> Unit = {},
  onCloseClick: () -> Unit = {}
) {
  ScrollableAlertDialogContent(
    title = {
      Text(stringResource(R.string.title_feedback))
    },
    confirmButton = {
      TextButton(
        onClick = onCloseClick,
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_close))
      }
    }
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = stringResource(R.string.msg_feedback),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = stringResource(R.string.msg_feedback_contact),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        val itemCount = if (isSupportVisible) 5 else 4

        val colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceBright
        )

        SegmentedListItem(
          onClick = onRateClick,
          shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
          colors = colors,
          leadingContent = {
            Box(modifier = Modifier.padding(vertical = 10.dp)) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_star),
                contentDescription = null
              )
            }
          },
          content = { Text(stringResource(R.string.action_rate)) },
          supportingContent = { Text(stringResource(R.string.action_rate_description)) }
        )

        if (isSupportVisible) {
          SegmentedListItem(
            onClick = onSupportClick,
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            colors = colors,
            leadingContent = {
              Box(modifier = Modifier.padding(vertical = 10.dp)) {
                Icon(
                  painter = painterResource(R.drawable.ic_rounded_volunteer_activism),
                  contentDescription = null
                )
              }
            },
            content = { Text(stringResource(R.string.action_support)) },
            supportingContent = { Text(stringResource(R.string.action_support_description)) }
          )
        }

        SegmentedListItem(
          onClick = onRecommendClick,
          shapes = ListItemDefaults.segmentedShapes(
            index = if (isSupportVisible) 2 else 1,
            count = itemCount
          ),
          colors = colors,
          leadingContent = {
            Box(modifier = Modifier.padding(vertical = 10.dp)) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_group),
                contentDescription = null
              )
            }
          },
          content = { Text(stringResource(R.string.action_recommend)) },
          supportingContent = { Text(stringResource(R.string.action_recommend_description)) }
        )

        SegmentedListItem(
          onClick = onIssueClick,
          shapes = ListItemDefaults.segmentedShapes(
            index = if (isSupportVisible) 3 else 2,
            count = itemCount
          ),
          colors = colors,
          leadingContent = {
            Box(modifier = Modifier.padding(vertical = 10.dp)) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_bug_report),
                contentDescription = null
              )
            }
          },
          content = { Text(stringResource(R.string.action_issue)) },
          supportingContent = { Text(stringResource(R.string.action_issue_description)) }
        )

        SegmentedListItem(
          onClick = onEmailClick,
          shapes = ListItemDefaults.segmentedShapes(
            index = if (isSupportVisible) 4 else 3,
            count = itemCount
          ),
          colors = colors,
          leadingContent = {
            Box(modifier = Modifier.padding(vertical = 10.dp)) {
              Icon(
                painter = painterResource(R.drawable.ic_rounded_mail),
                contentDescription = null
              )
            }
          },
          content = { Text(stringResource(R.string.action_email)) },
          supportingContent = { Text(stringResource(R.string.action_email_description)) }
        )
      }
    }
  }
}

@Preview
@Composable
fun FeedbackDialogPreview() {
  TackTheme {
    FeedbackDialogContent()
  }
}
