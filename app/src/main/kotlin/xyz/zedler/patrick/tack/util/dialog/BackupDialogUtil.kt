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

package xyz.zedler.patrick.tack.util.dialog

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import xyz.zedler.patrick.tack.Constants
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.database.relations.SongWithParts
import xyz.zedler.patrick.tack.databinding.PartialDialogBackupBinding
import xyz.zedler.patrick.tack.fragment.BaseFragment
import xyz.zedler.patrick.tack.util.*
import java.io.BufferedReader
import java.io.InputStreamReader

class BackupDialogUtil(
  private val activity: MainActivity,
  fragment: BaseFragment
) : View.OnClickListener {

  private val binding = PartialDialogBackupBinding.inflate(activity.layoutInflater)
  private val dialogUtil = DialogUtil(activity, "backup")
  private val gson = Gson()
  private val viewUtil = ViewUtil()
  private val launcherBackup: ActivityResultLauncher<String>
  private val launcherRestore: ActivityResultLauncher<Array<String>>

  init {
    dialogUtil.createDialog { builder ->
      builder.setTitle(R.string.settings_backup)
      builder.setView(binding.root)
      builder.setPositiveButton(R.string.action_close) { _, _ ->
        activity.performHapticClick()
      }
    }

    launcherBackup = fragment.registerForActivityResult(
      ActivityResultContracts.CreateDocument("application/json")
    ) { exportJsonToFile(it) }

    launcherRestore = fragment.registerForActivityResult(
      ActivityResultContracts.OpenDocument()
    ) { importJsonFromFile(it) }

    setOnClickListeners(
      this,
      binding.linearBackupBackup,
      binding.linearBackupRestore
    )

    updateDividerVisibility(activity.isOrientationPortrait().not())
  }

  fun show() {
    update()
    dialogUtil.show()
  }

  fun showIfWasShown(state: Bundle?) {
    update()
    dialogUtil.showIfWasShown(state)
  }

  fun dismiss() {
    dialogUtil.dismiss()
  }

  fun saveState(outState: Bundle) {
    dialogUtil.saveState(outState)
  }

  fun update() {
    binding.scrollBackup.scrollTo(0, 0)
    measureScrollView()
  }

  override fun onClick(v: View) {
    if (viewUtil.isClickDisabled(v.id)) return
    activity.performHapticClick()

    when (v.id) {
      R.id.linear_backup_backup -> launcherBackup.launch("song_library.json")
      R.id.linear_backup_restore -> launcherRestore.launch(arrayOf("application/json"))
    }
  }

  private fun exportJsonToFile(uri: Uri?) {
    if (uri == null) {
      showToast(R.string.msg_backup_directory_missing)
      return
    }
    activity.songViewModel.fetchAllSongsWithParts { songsWithParts ->
      val filteredSongs = songsWithParts.toMutableList().apply {
        removeAll { it.song.id == Constants.SONG_ID_DEFAULT }
      }
      try {
        activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
          val json = gson.toJson(filteredSongs)
          outputStream.write(json.toByteArray())
          outputStream.flush()
          showToast(R.string.msg_backup_success)
        }
      } catch (e: Exception) {
        showToast(R.string.msg_backup_error)
        Log.e(TAG, "exportJsonToFile: ", e)
      }
    }
  }

  private fun importJsonFromFile(uri: Uri?) {
    if (uri == null) {
      showToast(R.string.msg_restore_file_missing)
      return
    }
    try {
      activity.contentResolver.openInputStream(uri)?.use { inputStream ->
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
          val jsonString = reader.readText()
          val listType = object : TypeToken<List<SongWithParts>>() {}.type
          val songsWithParts: List<SongWithParts>? = gson.fromJson(jsonString, listType)
          if (songsWithParts != null) {
            // look for duplicates of existing song names
            val nameCountMap = mutableMapOf<String, Int>()
            val idNameMap = mutableMapOf<String, String>()
            // count existing song names
            activity.songViewModel.fetchAllSongsWithParts { existingSongs ->
              for (existingSong in existingSongs) {
                idNameMap[existingSong.song.id] = existingSong.song.name ?: ""
                val existingName = existingSong.song.name ?: continue
                if (existingName.isEmpty()) continue
                nameCountMap[existingName] = (nameCountMap[existingName] ?: 0) + 1
              }
              for (songWithParts in songsWithParts) {
                val songId = songWithParts.song.id
                if (idNameMap.containsKey(songId)) {
                  songWithParts.song.name = idNameMap[songId]
                  continue
                }
                val originalName = songWithParts.song.name ?: ""
                var newName = originalName
                var counter = nameCountMap[originalName] ?: 0
                if (counter > 0) {
                  do {
                    newName = activity.getString(
                      R.string.msg_restore_duplicate_name, originalName, counter
                    )
                    counter++
                  } while (nameCountMap.containsKey(newName))
                }
                songWithParts.song.name = newName
                nameCountMap[newName] = 1
              }
              activity.songViewModel.insertSongsWithParts(songsWithParts) {
                showToast(R.string.msg_restore_success)
                activity.metronomeEngine?.updateShortcuts()
                sendSongsWidgetUpdate(activity)
              }
            }
          } else {
            showToast(R.string.msg_restore_error)
          }
        }
      }
    } catch (e: Exception) {
      showToast(R.string.msg_restore_error)
      Log.e(TAG, "importJsonFromFile: ", e)
    }
  }

  private fun showToast(resId: Int) {
    activity.runOnUiThread {
      Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show()
    }
  }

  private fun measureScrollView() {
    binding.scrollBackup.onGlobalLayout {
      val isScrollable = binding.scrollBackup.canScrollVertically(-1) ||
          binding.scrollBackup.canScrollVertically(1)
      updateDividerVisibility(isScrollable)
    }
  }

  private fun updateDividerVisibility(visible: Boolean) {
    binding.scrollBackup.setDividerVisibility(
      visible,
      binding.dividerBackupTop,
      binding.dividerBackupBottom,
      binding.linearBackupContainer
    )
  }

  companion object {
    private val TAG = BackupDialogUtil::class.java.simpleName
  }
}
