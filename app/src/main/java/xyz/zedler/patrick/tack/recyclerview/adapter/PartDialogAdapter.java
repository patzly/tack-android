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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.util.List;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.RowDialogRadioBinding;

public class PartDialogAdapter extends Adapter<ViewHolder> {

  private final static String TAG = SongChipAdapter.class.getSimpleName();

  private final static String PAYLOAD_RADIO = "radio";

  private final OnPartChangedListener listener;
  private SongWithParts songWithParts;
  private int partIndex, partIndexPrev;

  public PartDialogAdapter(@NonNull OnPartChangedListener listener) {
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    RowDialogRadioBinding binding = RowDialogRadioBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false
    );
    return new PartDialogViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    int adapterPosition = holder.getBindingAdapterPosition();
    PartDialogViewHolder partHolder = (PartDialogViewHolder) holder;
    Context context = partHolder.binding.getRoot().getContext();

    Part part = songWithParts.getParts().get(adapterPosition);
    String name = part.getName();
    if (name == null) {
      name = context.getString(R.string.label_part_unnamed, adapterPosition + 1);
    }
    partHolder.binding.textRowDialogName.setText(name);
    partHolder.binding.textRowDialogDescription.setText(part.getTimerDurationString(context));

    partHolder.binding.radioRowDialog.setChecked(adapterPosition == partIndex);
    partHolder.binding.radioRowDialog.jumpDrawablesToCurrentState();

    partHolder.binding.linearRowDialog.setOnClickListener(
        v -> setPartIndex(adapterPosition, true)
    );
  }

  @Override
  public void onBindViewHolder(
      @NonNull ViewHolder holder, int position, @NonNull List<Object> payloads
  ) {
    if (payloads.contains(PAYLOAD_RADIO)) {
      int adapterPosition = holder.getBindingAdapterPosition();
      PartDialogViewHolder partHolder = (PartDialogViewHolder) holder;

      partHolder.binding.radioRowDialog.setChecked(adapterPosition == partIndexPrev);
      partHolder.binding.radioRowDialog.jumpDrawablesToCurrentState();
      partHolder.binding.radioRowDialog.post(
          () -> partHolder.binding.radioRowDialog.setChecked(adapterPosition == partIndex)
      );
    } else {
      onBindViewHolder(holder, position);
    }
  }

  @Override
  public int getItemCount() {
    return songWithParts != null ? songWithParts.getParts().size() : 0;
  }

  @SuppressLint("NotifyDataSetChanged")
  public void setSongWithParts(SongWithParts songWithParts) {
    if ((songWithParts == null && this.songWithParts == null)) {
      return;
    } else if (songWithParts != null) {
      String idNew = songWithParts.getSong().getId();
      if (this.songWithParts != null) {
        String idOld = this.songWithParts.getSong().getId();
        if (idNew.equals(idOld)
            && songWithParts.getParts().size() == this.songWithParts.getParts().size()) {
          return;
        }
      }
    }
    this.songWithParts = songWithParts;
    partIndex = 0;
    partIndexPrev = 0;
    notifyDataSetChanged();
  }

  public void setPartIndex(int partIndex) {
    setPartIndex(partIndex, false);
  }

  private void setPartIndex(int partIndex, boolean fromUser) {
    if (partIndex == this.partIndex) {
      return;
    }
    partIndexPrev = this.partIndex;
    this.partIndex = partIndex;
    if (fromUser) {
      notifyItemChanged(partIndexPrev, PAYLOAD_RADIO);
      notifyItemChanged(partIndex, PAYLOAD_RADIO);
    } else {
      notifyItemChanged(partIndexPrev);
      notifyItemChanged(partIndex);
    }
    listener.onPartChanged(partIndex, fromUser);
  }

  public static class PartDialogViewHolder extends ViewHolder {

    private final RowDialogRadioBinding binding;

    public PartDialogViewHolder(RowDialogRadioBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public interface OnPartChangedListener {
    void onPartChanged(int partIndex, boolean fromUser);
  }
}