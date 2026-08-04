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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import xyz.zedler.patrick.tack.BuildConfig
import xyz.zedler.patrick.tack.Constants.PREF
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.behavior.ScrollBehavior
import xyz.zedler.patrick.tack.behavior.SystemBarBehavior
import xyz.zedler.patrick.tack.databinding.FragmentAboutBinding
import xyz.zedler.patrick.tack.util.isKeyInstalled
import xyz.zedler.patrick.tack.util.isPlayStoreInstalled
import xyz.zedler.patrick.tack.util.share
import xyz.zedler.patrick.tack.util.dialog.TextDialogUtil
import xyz.zedler.patrick.tack.util.setTooltipText
import xyz.zedler.patrick.tack.util.showMenu
import xyz.zedler.patrick.tack.util.setOnClickListeners
import xyz.zedler.patrick.tack.util.startIcon
import androidx.core.net.toUri
import androidx.core.content.edit

class AboutFragment : BaseFragment(), View.OnClickListener {

  private var _binding: FragmentAboutBinding? = null
  private val binding get() = _binding!!

  private lateinit var textDialogUtilMdc: TextDialogUtil
  private lateinit var textDialogUtilMds: TextDialogUtil
  private lateinit var textDialogUtilGoogleSansFlex: TextDialogUtil
  private var longClickCount = 0

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentAboutBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    if (::textDialogUtilMdc.isInitialized) textDialogUtilMdc.dismiss()
    if (::textDialogUtilMds.isInitialized) textDialogUtilMds.dismiss()
    if (::textDialogUtilGoogleSansFlex.isInitialized) textDialogUtilGoogleSansFlex.dismiss()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val systemBarBehavior = SystemBarBehavior(activity)
    systemBarBehavior.setAppBar(binding.appBarAbout)
    systemBarBehavior.setScroll(
      binding.scrollAbout, binding.linearAboutContainer
    )
    systemBarBehavior.setUp()

    ScrollBehavior().setUpScroll(
      binding.appBarAbout,
      binding.scrollAbout,
      ScrollBehavior.LIFT_ON_SCROLL
    )

    binding.buttonAboutBack.setOnClickListener(getNavigationOnClickListener())
    binding.buttonAboutMenu.setOnClickListener { v ->
      performHapticClick()
      v.showMenu(R.menu.menu_about, PopupMenu.OnMenuItemClickListener { item ->
        val id = item.itemId
        if (getViewUtil().isClickDisabled(id)) {
          return@OnMenuItemClickListener false
        }
        performHapticClick()
        when (id) {
          R.id.action_feedback -> activity.showFeedbackDialog()
          R.id.action_help -> activity.showHelpDialog()
          R.id.action_recommend -> {
            val text = getString(
              R.string.msg_recommend,
              getString(R.string.app_vending_app)
            )
            activity.share(text)
          }
        }
        true
      })
    }
    binding.buttonAboutBack.setTooltipText(R.string.action_back)
    binding.buttonAboutMenu.setTooltipText(R.string.action_more)

    binding.textAboutVersion.text = BuildConfig.VERSION_NAME

    updateUnlockItem()

    textDialogUtilMdc = TextDialogUtil(
      activity,
      R.string.license_material_components,
      R.raw.license_apache,
      link = R.string.license_material_components_link
    )
    textDialogUtilMdc.showIfWasShown(savedInstanceState)

    textDialogUtilMds = TextDialogUtil(
      activity,
      R.string.license_material_icons,
      R.raw.license_apache,
      link = R.string.license_material_icons_link
    )
    textDialogUtilMds.showIfWasShown(savedInstanceState)

    textDialogUtilGoogleSansFlex = TextDialogUtil(
      activity,
      R.string.license_google_sans_flex,
      R.raw.license_ofl,
      link = R.string.license_google_sans_flex_link
    )
    textDialogUtilGoogleSansFlex.showIfWasShown(savedInstanceState)

    setOnClickListeners(
      this,
      binding.linearAboutDeveloper,
      binding.linearAboutChangelog,
      binding.linearAboutVending,
      binding.linearAboutKey,
      binding.linearAboutGithub,
      binding.linearAboutTranslation,
      binding.linearAboutPrivacy,
      binding.linearAboutLicenseMaterialComponents,
      binding.linearAboutLicenseMaterialIcons,
      binding.linearAboutLicenseGoogleSansFlex
    )
  }

  override fun onResume() {
    super.onResume()
    updateUnlockItem()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    if (::textDialogUtilMdc.isInitialized) {
      textDialogUtilMdc.saveState(outState)
    }
    if (::textDialogUtilMds.isInitialized) {
      textDialogUtilMds.saveState(outState)
    }
    if (::textDialogUtilGoogleSansFlex.isInitialized) {
      textDialogUtilGoogleSansFlex.saveState(outState)
    }
  }

  override fun onClick(v: View) {
    val id = v.id
    if (getViewUtil().isClickDisabled(id)) {
      return
    } else {
      performHapticClick()
    }

    when (id) {
      R.id.linear_about_developer -> {
        startActivity(
          Intent(Intent.ACTION_VIEW, getString(R.string.app_website).toUri())
        )
      }

      R.id.linear_about_changelog -> {
        binding.imageAboutChangelog.startIcon()
        activity.showChangelogDialog()
      }

      R.id.linear_about_vending -> {
        startActivity(
          Intent(
            Intent.ACTION_VIEW, getString(R.string.app_vending_dev).toUri()
          )
        )
      }

      R.id.linear_about_key -> {
        if (isKeyInstalled(activity)) {
          activity.startActivity(
            Intent(
              Intent.ACTION_VIEW, getString(R.string.app_vending_key).toUri()
            )
          )
        } else {
          activity.showUnlockDialog()
        }
      }

      R.id.linear_about_github -> {
        startActivity(
          Intent(Intent.ACTION_VIEW, getString(R.string.app_github).toUri())
        )
      }

      R.id.linear_about_translation -> {
        startActivity(
          Intent(Intent.ACTION_VIEW, getString(R.string.app_translate).toUri())
        )
      }

      R.id.linear_about_privacy -> {
        startActivity(
          Intent(Intent.ACTION_VIEW, getString(R.string.app_privacy).toUri())
        )
      }

      R.id.linear_about_license_google_sans_flex -> {
        binding.imageAboutLicenseGoogleSansFlex.startIcon()
        textDialogUtilGoogleSansFlex.show()
      }

      R.id.linear_about_license_material_components -> {
        binding.imageAboutLicenseMaterialComponents.startIcon()
        textDialogUtilMdc.show()
      }

      R.id.linear_about_license_material_icons -> {
        binding.imageAboutLicenseMaterialIcons.startIcon()
        textDialogUtilMds.show()
      }
    }
  }

  private fun updateUnlockItem() {
    val isPlayStoreInstalled = isPlayStoreInstalled(activity)
    binding.linearAboutKey.visibility = if (isPlayStoreInstalled) View.VISIBLE else View.GONE
    if (!isPlayStoreInstalled) {
      return
    }
    if (activity.isUnlocked()) {
      binding.linearAboutKey.setOnLongClickListener(null)
    } else {
      binding.linearAboutKey.setOnLongClickListener {
        longClickCount++
        if (longClickCount >= 10) {
          sharedPrefs.edit { putBoolean(PREF.CHECK_UNLOCK_KEY, false) }
          updateUnlockItem()
          binding.linearAboutKey.setOnLongClickListener(null)
        }
        true
      }
    }
    var resId = R.string.about_key_description_not_installed
    val checkUnlockKey = sharedPrefs.getBoolean(PREF.CHECK_UNLOCK_KEY, true)
    if (isKeyInstalled(activity)) {
      resId = R.string.about_key_description_installed
    } else if (!checkUnlockKey) {
      resId = R.string.about_key_description_ignored
    }
    binding.textAboutKeyDescription.setText(resId)
  }
}
