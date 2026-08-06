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

package xyz.zedler.patrick.tack.recyclerview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import xyz.zedler.patrick.tack.R
import xyz.zedler.patrick.tack.databinding.RowDialogRadioBinding
import xyz.zedler.patrick.tack.model.Language
import xyz.zedler.patrick.tack.util.getLangFromLanguageCode

class LanguageDialogAdapter(
  private val languages: List<Language>,
  private val listener: OnLanguageChangedListener
) : RecyclerView.Adapter<LanguageDialogAdapter.LanguageViewHolder>() {

  private var languageIndex = 0
  private var languageIndexPrev = 0
  private var selectedCode: String? = null

  companion object {
    private const val PAYLOAD_RADIO = "radio"
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
    val binding = RowDialogRadioBinding.inflate(
      LayoutInflater.from(parent.context), parent, false
    )
    return LanguageViewHolder(binding)
  }

  override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
    val adapterPosition = holder.bindingAdapterPosition
    if (adapterPosition == 0) {
      holder.binding.textRowDialogName.setText(R.string.settings_language_system)
      holder.binding.textRowDialogDescription.setText(
        R.string.settings_language_system_description
      )

      holder.binding.radioRowDialog.isChecked = languageIndex == 0
      holder.binding.radioRowDialog.jumpDrawablesToCurrentState()

      holder.binding.linearRowDialog.setOnClickListener {
        listener.onLanguageChanged(null, true)
      }
      return
    }

    val language = languages[adapterPosition - 1]
    holder.binding.textRowDialogName.text = language.name
    holder.binding.textRowDialogDescription.text = language.translators

    holder.binding.radioRowDialog.isChecked = adapterPosition == languageIndex
    holder.binding.radioRowDialog.jumpDrawablesToCurrentState()

    holder.binding.linearRowDialog.setOnClickListener {
      listener.onLanguageChanged(language.code, true)
    }
  }

  override fun onBindViewHolder(
    holder: LanguageViewHolder,
    position: Int,
    payloads: List<Any>
  ) {
    if (payloads.contains(PAYLOAD_RADIO)) {
      val adapterPosition = holder.bindingAdapterPosition

      holder.binding.radioRowDialog.isChecked = adapterPosition == languageIndexPrev
      holder.binding.radioRowDialog.jumpDrawablesToCurrentState()
      holder.binding.radioRowDialog.post {
        holder.binding.radioRowDialog.isChecked = adapterPosition == languageIndex
      }
    } else {
      super.onBindViewHolder(holder, position, payloads)
    }
  }

  override fun getItemCount(): Int = languages.size + 1

  fun setLanguageCode(selectedCode: String?, fromUser: Boolean = false) {
    if (selectedCode != null && selectedCode == this.selectedCode) {
      return
    }
    languageIndexPrev = getIndexForCode(this.selectedCode)
    languageIndex = getIndexForCode(selectedCode)
    this.selectedCode = selectedCode
    if (fromUser) {
      notifyItemChanged(languageIndexPrev, PAYLOAD_RADIO)
      notifyItemChanged(languageIndex, PAYLOAD_RADIO)
    } else {
      notifyItemChanged(languageIndexPrev)
      notifyItemChanged(languageIndex)
    }
    listener.onLanguageChanged(selectedCode, fromUser)
  }

  private fun getIndexForCode(languageCode: String?): Int {
    if (languageCode == null) return 0
    val index = languages.indexOfFirst { it.code == languageCode }
    if (index != -1) return index + 1

    // try to match only the language without region
    val lang = languageCode.getLangFromLanguageCode()
    val langIndex = languages.indexOfFirst { it.code == lang }
    return if (langIndex != -1) langIndex + 1 else 0
  }

  class LanguageViewHolder(val binding: RowDialogRadioBinding) :
    RecyclerView.ViewHolder(binding.root)

  fun interface OnLanguageChangedListener {
    fun onLanguageChanged(languageCode: String?, fromUser: Boolean)
  }
}
