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

import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.util.LocaleUtil
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(
  onDismissRequest: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val languages = remember { LocaleUtil.getLanguages(context) }
  val currentLanguageCode = remember { LocaleUtil.getLanguageCode() }

  val listState = rememberLazyListState()

  val appTranslate = stringResource(R.string.app_translate)

  BasicAlertDialog(onDismissRequest = onDismissRequest) {
    Surface(
      shape = AlertDialogDefaults.shape,
      color = AlertDialogDefaults.containerColor,
      tonalElevation = AlertDialogDefaults.TonalElevation,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 600.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.headlineSmall
          )
          Text(
            text = stringResource(R.string.settings_language_info),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        HorizontalDivider()

        val colors = ListItemDefaults.colors(
          containerColor = MaterialTheme.colorScheme.surfaceBright
        )

        LazyColumn(
          state = listState,
          modifier = Modifier.weight(1f, fill = false),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
          val itemCount = languages.size + 1

          // Follow System item
          item {
            SegmentedListItem(
              onClick = {
                scope.launch {
                  AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                  onDismissRequest()
                }
              },
              shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
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
                scope.launch {
                  AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(language.code)
                  )
                  onDismissRequest()
                }
              },
              shapes = ListItemDefaults.segmentedShapes(index = index + 1, count = itemCount),
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

        HorizontalDivider()

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          TextButton(
            onClick = {
              context.startActivity(
                Intent(Intent.ACTION_VIEW, appTranslate.toUri())
              )
            }
          ) {
            Text(stringResource(R.string.action_learn_more))
          }
          TextButton(onClick = onDismissRequest) {
            Text(stringResource(R.string.action_close))
          }
        }
      }
    }
  }
}
