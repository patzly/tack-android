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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.databinding.RowLanguageBinding;
import xyz.zedler.patrick.tack.databinding.RowSongChipBinding;
import xyz.zedler.patrick.tack.model.Language;
import xyz.zedler.patrick.tack.recyclerview.adapter.LanguageAdapter.LanguageViewHolder;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipAdapter.SongChipViewHolder;
import xyz.zedler.patrick.tack.util.LocaleUtil;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class LanguageAdapter extends Adapter<ViewHolder> {

  private static final String TAG = LanguageAdapter.class.getSimpleName();

  private final List<Language> languages;
  private final String selectedCode;
  private final LanguageAdapterListener listener;
  private final HashMap<String, Language> languageHashMap;

  public LanguageAdapter(
      List<Language> languages, String selectedCode, LanguageAdapterListener listener
  ) {
    this.languages = languages;
    this.selectedCode = selectedCode;
    this.listener = listener;
    this.languageHashMap = new HashMap<>();
    for (Language language : languages) {
      languageHashMap.put(language.getCode(), language);
    }
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    RowLanguageBinding binding = RowLanguageBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false
    );
    return new LanguageViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    LanguageViewHolder languageHolder = (LanguageViewHolder) holder;
    int adapterPosition = holder.getBindingAdapterPosition();
    if (adapterPosition == 0) {
      languageHolder.binding.textLanguageName.setText(R.string.settings_language_system);
      languageHolder.binding.textLanguageTranslators.setText(
          R.string.settings_language_system_description
      );

      setSelected(languageHolder, selectedCode == null);
      languageHolder.binding.linearLanguageContainer.setOnClickListener(
          view -> listener.onItemRowClicked(null)
      );
      return;
    }

    Language language = languages.get(adapterPosition - 1);
    languageHolder.binding.textLanguageName.setText(language.getName());
    languageHolder.binding.textLanguageTranslators.setText(language.getTranslators());

    boolean isSelected = language.getCode().equals(selectedCode);
    if (selectedCode != null && !isSelected && !languageHashMap.containsKey(selectedCode)) {
      String lang = LocaleUtil.getLangFromLanguageCode(selectedCode);
      if (languageHashMap.containsKey(lang)) {
        isSelected = language.getCode().equals(lang);
      }
    }
    setSelected(languageHolder, isSelected);
    languageHolder.binding.linearLanguageContainer.setOnClickListener(
        view -> listener.onItemRowClicked(language)
    );
  }

  @Override
  public int getItemCount() {
    return languages.size() + 1;
  }

  private void setSelected(LanguageViewHolder holder, boolean selected) {
    Context context = holder.binding.getRoot().getContext();
    int colorSelected = ResUtil.getColor(context, R.attr.colorOnSecondaryContainer);
    holder.binding.imageLanguageSelected.setColorFilter(colorSelected);
    holder.binding.imageLanguageSelected.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    if (selected) {
      holder.binding.linearLanguageContainer.setBackground(ViewUtil.getBgListItemSelected(context));
    } else {
      holder.binding.linearLanguageContainer.setBackground(
          ViewUtil.getRippleBgListItemSurface(context)
      );
    }
    holder.binding.textLanguageName.setTextColor(
        selected ? colorSelected : ResUtil.getColor(context, R.attr.colorOnSurface)
    );
    holder.binding.textLanguageTranslators.setTextColor(
        selected ? colorSelected : ResUtil.getColor(context, R.attr.colorOnSurfaceVariant)
    );
    holder.binding.linearLanguageContainer.setOnClickListener(
        view -> listener.onItemRowClicked(null)
    );
  }

  public static class LanguageViewHolder extends ViewHolder {

    private final RowLanguageBinding binding;

    public LanguageViewHolder(RowLanguageBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public interface LanguageAdapterListener {

    void onItemRowClicked(@Nullable Language language);
  }
}
