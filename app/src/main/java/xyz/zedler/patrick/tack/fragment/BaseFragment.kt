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

package xyz.zedler.patrick.tack.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import xyz.zedler.patrick.tack.activity.MainActivity
import xyz.zedler.patrick.tack.metronome.MetronomeEngine
import xyz.zedler.patrick.tack.util.ViewUtil

open class BaseFragment : Fragment() {

  val activity: MainActivity
    get() = requireActivity() as MainActivity

  private lateinit var viewUtil: ViewUtil

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewUtil = ViewUtil()

    enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
    reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (::viewUtil.isInitialized) {
      viewUtil.cleanUp()
    }
  }

  val metronomeEngine: MetronomeEngine?
    get() = activity.metronomeEngine

  open fun updateMetronomeControls(init: Boolean) {}

  val sharedPrefs: SharedPreferences
    get() = activity.sharedPrefs

  fun getViewUtil(): ViewUtil = viewUtil

  fun navigateUp() {
    activity.navigateUp()
  }

  fun performHapticClick() {
    activity.performHapticClick()
  }

  fun performHapticTick() {
    activity.performHapticTick()
  }

  fun performHapticSegmentTick(view: View, frequent: Boolean) {
    activity.performHapticSegmentTick(view, frequent)
  }

  fun performHapticHeavyClick() {
    activity.performHapticHeavyClick()
  }

  fun getNavigationOnClickListener(): View.OnClickListener {
    return View.OnClickListener { v ->
      if (viewUtil.isClickEnabled(v.id)) {
        performHapticClick()
        navigateUp()
      }
    }
  }

  companion object {
    private val TAG = BaseFragment::class.java.simpleName
  }
}
