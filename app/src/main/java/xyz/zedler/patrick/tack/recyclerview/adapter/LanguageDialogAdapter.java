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
 * Copyright (c) 2020-2025 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.recyclerview.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.util.List;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.RowDialogRadioBinding;
import xyz.zedler.patrick.tack.model.Language;
import xyz.zedler.patrick.tack.util.LocaleUtil;

public class LanguageDialogAdapter extends Adapter<LanguageDialogAdapter.LanguageViewHolder> {

  private final static String TAG = SongChipAdapter.class.getSimpleName();

  private final static String PAYLOAD_RADIO = "radio";

  private final List<Language> languages;
  private final OnLanguageChangedListener listener;
  private int languageIndex, languageIndexPrev;
  private String selectedCode;

  public LanguageDialogAdapter(List<Language> languages, OnLanguageChangedListener listener) {
    this.languages = languages;
    this.listener = listener;
  }

  @NonNull
  @Override
  public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    RowDialogRadioBinding binding = RowDialogRadioBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false
    );
    return new LanguageViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
    int adapterPosition = holder.getBindingAdapterPosition();
    if (adapterPosition == 0) {
      holder.binding.textRowDialogName.setText(R.string.settings_language_system);
      holder.binding.textRowDialogDescription.setText(
          R.string.settings_language_system_description
      );

      holder.binding.radioRowDialog.setChecked(languageIndex == 0);
      holder.binding.radioRowDialog.jumpDrawablesToCurrentState();

      holder.binding.linearRowDialog.setOnClickListener(
          view -> listener.onLanguageChanged(null, true)
      );
      return;
    }

    Language language = languages.get(adapterPosition - 1);
    holder.binding.textRowDialogName.setText(language.getName());
    holder.binding.textRowDialogDescription.setText(language.getTranslators());

    holder.binding.radioRowDialog.setChecked(adapterPosition == languageIndex);
    holder.binding.radioRowDialog.jumpDrawablesToCurrentState();

    holder.binding.linearRowDialog.setOnClickListener(
        view -> listener.onLanguageChanged(language.getCode(), true)
    );
  }

  @Override
  public void onBindViewHolder(
      @NonNull LanguageViewHolder holder, int position, @NonNull List<Object> payloads
  ) {
    if (payloads.contains(PAYLOAD_RADIO)) {
      int adapterPosition = holder.getBindingAdapterPosition();

      holder.binding.radioRowDialog.setChecked(adapterPosition == languageIndexPrev);
      holder.binding.radioRowDialog.jumpDrawablesToCurrentState();
      holder.binding.radioRowDialog.post(
          () -> holder.binding.radioRowDialog.setChecked(adapterPosition == languageIndex)
      );
    } else {
      onBindViewHolder(holder, position);
    }
  }

  @Override
  public int getItemCount() {
    return languages.size() + 1;
  }

  public void setLanguageCode(String selectedCode) {
    setLanguageCode(selectedCode, false);
  }

  private void setLanguageCode(String selectedCode, boolean fromUser) {
    if (selectedCode != null && selectedCode.equals(this.selectedCode)) {
      return;
    }
    languageIndexPrev = getIndexForCode(this.selectedCode);
    languageIndex = getIndexForCode(selectedCode);
    this.selectedCode = selectedCode;
    if (fromUser) {
      notifyItemChanged(languageIndexPrev, PAYLOAD_RADIO);
      notifyItemChanged(languageIndex, PAYLOAD_RADIO);
    } else {
      notifyItemChanged(languageIndexPrev);
      notifyItemChanged(languageIndex);
    }
    listener.onLanguageChanged(selectedCode, fromUser);
  }

  private int getIndexForCode(String languageCode) {
    if (languageCode == null) {
      return 0;
    }
    for (int i = 0; i < languages.size(); i++) {
      Language language = languages.get(i);
      if (language.getCode().equals(languageCode)) {
        return i + 1;
      }
    }
    // try to match only the language without region
    String lang = LocaleUtil.getLangFromLanguageCode(languageCode);
    for (int i = 0; i < languages.size(); i++) {
      Language language = languages.get(i);
      if (language.getCode().equals(lang)) {
        return i + 1;
      }
    }
    return 0;
  }

  public static class LanguageViewHolder extends ViewHolder {

    private final RowDialogRadioBinding binding;

    public LanguageViewHolder(RowDialogRadioBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public interface OnLanguageChangedListener {
    void onLanguageChanged(String languageCode, boolean fromUser);
  }
}