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

import android.os.Bundle
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.util.*

class UnlockDialogUtil(activity: MainActivity) {

    private val dialogUtilUnlock = DialogUtil(activity, "unlock").apply {
        createDialog { builder ->
            builder.setTitle(R.string.msg_unlock)
            builder.setMessage(R.string.msg_unlock_description)
            builder.setPositiveButton(R.string.action_open_play_store) { _, _ ->
                activity.performHapticClick()
                openPlayStore(activity)
            }
            builder.setNegativeButton(R.string.action_cancel) { _, _ ->
                activity.performHapticClick()
            }
        }
    }

    fun show() {
        dialogUtilUnlock.show()
    }

    fun showIfWasShown(state: Bundle?) {
        dialogUtilUnlock.showIfWasShown(state)
    }

    fun dismiss() {
        dialogUtilUnlock.dismiss()
    }

    fun saveState(outState: Bundle) {
        dialogUtilUnlock.saveState(outState)
    }
}
