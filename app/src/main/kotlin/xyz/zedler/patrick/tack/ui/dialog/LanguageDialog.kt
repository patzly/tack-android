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

package xyz.zedler.patrick.tack.ui.dialog

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.core.model.Language
import xyz.zedler.patrick.tack.ui.component.ScrollableAlertDialog
import xyz.zedler.patrick.tack.ui.component.ScrollableAlertDialogContent
import xyz.zedler.patrick.tack.ui.theme.TackTheme
import xyz.zedler.patrick.tack.ui.util.LocalHaptic
import xyz.zedler.patrick.tack.util.LocaleUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(
  currentLanguageCode: String?,
  onLanguageSelected: (String?) -> Unit,
  onDismissRequest: () -> Unit
) {
  val context = LocalContext.current
  val haptic = LocalHaptic.current

  val languages = remember { LocaleUtil.getLanguages(context) }

  val appTranslate = stringResource(R.string.app_translate)

  ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
    LanguageDialogContent(
      languages = languages,
      currentLanguageCode = currentLanguageCode,
      onLanguageSelected = {
        haptic.click()
        onLanguageSelected(it)
        onDismissRequest()
      },
      onMoreClick = {
        haptic.click()
        context.startActivity(Intent(Intent.ACTION_VIEW, appTranslate.toUri()))
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
private fun LanguageDialogContent(
  languages: List<Language> = emptyList(),
  currentLanguageCode: String? = null,
  onLanguageSelected: (String?) -> Unit = {},
  onMoreClick: () -> Unit = {},
  onCloseClick: () -> Unit = {}
) {
  ScrollableAlertDialogContent(
    title = {
      Text(stringResource(R.string.settings_language))
    },
    subtitle = {
      Text(stringResource(R.string.settings_language_info))
    },
    confirmButton = {
      TextButton(
        onClick = onCloseClick,
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_close))
      }
    },
    extraButton = {
      TextButton(
        onClick = onMoreClick,
        shapes = ButtonDefaults.shapes()
      ) {
        Text(stringResource(R.string.action_learn_more))
      }
    },
    isScrollableControlledByContent = true
  ) {
    val colors = ListItemDefaults.segmentedColors(
      containerColor = MaterialTheme.colorScheme.surfaceBright,
      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
      selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
      val itemCount = languages.size + 1

      // Follow System item
      item {
        SegmentedListItem(
          onClick = {
            onLanguageSelected(null)
          },
          shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
          selected = currentLanguageCode == null,
          colors = colors,
          verticalAlignment = Alignment.CenterVertically,
          leadingContent = {
            RadioButton(
              selected = currentLanguageCode == null,
              onClick = null
            )
          },
          content = {
            Text(stringResource(R.string.settings_language_system))
          },
          supportingContent = {
            Text(stringResource(R.string.settings_language_system_description))
          }
        )
      }

      // Language items
      itemsIndexed(languages) { index, language ->
        SegmentedListItem(
          onClick = {
            onLanguageSelected(language.code)
          },
          shapes = ListItemDefaults.segmentedShapes(index = index + 1, count = itemCount),
          selected = currentLanguageCode == language.code,
          colors = colors,
          verticalAlignment = Alignment.CenterVertically,
          leadingContent = {
            RadioButton(
              selected = currentLanguageCode == language.code,
              onClick = null
            )
          },
          content = { Text(language.name) },
          supportingContent = { Text(language.translators) }
        )
      }
    }
  }
}

@Preview
@Composable
fun LanguageDialogPreview() {
  TackTheme {
    LanguageDialogContent(
      languages = listOf(
        Language(code = "de", translators = "Patrick Zedler", name = "Deutsch"),
        Language(code = "en", translators = "Patrick Zedler", name = "English")
      )
    )
  }
}
