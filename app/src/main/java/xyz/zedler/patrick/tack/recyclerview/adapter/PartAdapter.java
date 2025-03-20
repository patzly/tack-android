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
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.util.List;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.databinding.RowPartBinding;
import xyz.zedler.patrick.tack.util.ResUtil;

public class PartAdapter extends ListAdapter<Part, ViewHolder> {

  private final static String TAG = SongChipAdapter.class.getSimpleName();

  private final static String PAYLOAD_MENU = "menu";

  private final OnPartMenuItemClickListener listener;

  public PartAdapter(@NonNull OnPartMenuItemClickListener listener) {
    super(new PartDiffCallback());
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    RowPartBinding binding = RowPartBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false
    );
    return new PartViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    int adapterPosition = holder.getBindingAdapterPosition();
    Part part = getItem(adapterPosition);
    PartViewHolder partHolder = (PartViewHolder) holder;
    Context context = partHolder.binding.getRoot().getContext();

    // name
    String partName = part.getName();
    if (partName == null || partName.trim().isEmpty()) {
      partName = context.getString(R.string.label_part_unnamed, adapterPosition + 1);
    }
    partHolder.binding.textPartName.setText(partName);

    partHolder.binding.toolbarPart.setOnMenuItemClickListener(item -> {
      listener.onMenuItemClick(part, item);
      return true;
    });
    ResUtil.tintMenuIcons(context, partHolder.binding.toolbarPart.getMenu());
    updateMenuItems(partHolder);

    // duration
    int timerDuration = part.getTimerDuration();
    String timerUnit = part.getTimerUnit();
    int durationResId;
    switch (timerUnit) {
      case UNIT.SECONDS:
        durationResId = R.plurals.options_timer_description_seconds;
        break;
      case UNIT.MINUTES:
        durationResId = R.plurals.options_timer_description_minutes;
        break;
      default:
        durationResId = R.plurals.options_timer_description_bars;
        break;
    }
    if (timerDuration > 0) {
      partHolder.binding.textPartDuration.setText(
          context.getResources().getQuantityString(durationResId, timerDuration, timerDuration)
      );
    } else {
      partHolder.binding.textPartDuration.setText(R.string.label_part_no_duration);
    }

    // tempo
    int tempo = part.getTempo();
    partHolder.binding.textPartTempo.setText(context.getString(R.string.label_bpm_value, tempo));

    // count in
    boolean isCountInActive = part.getCountIn() > 0;
    if (isCountInActive) {
      int countIn = part.getCountIn();
      partHolder.binding.textPartCountIn.setText(
          context.getResources().getQuantityString(
              R.plurals.options_count_in_description, countIn, countIn
          )
      );
    }
    partHolder.binding.linearPartCountIn.setVisibility(isCountInActive ? View.VISIBLE : View.GONE);

    // incremental
    int incrementalAmount = part.getIncrementalAmount();
    boolean incrementalIncrease = part.isIncrementalIncrease();
    boolean isIncrementalActive = part.getIncrementalAmount() > 0;
    if (isIncrementalActive) {
      partHolder.binding.textPartIncrementalAmount.setText(context.getString(
          incrementalIncrease
              ? R.string.options_incremental_amount_increase
              : R.string.options_incremental_amount_decrease,
          incrementalAmount
      ));

      int incrementalInterval = part.getIncrementalInterval();
      String incrementalUnit = part.getIncrementalUnit();
      int intervalResId;
      switch (incrementalUnit) {
        case UNIT.SECONDS:
          intervalResId = R.plurals.options_incremental_interval_seconds;
          break;
        case UNIT.MINUTES:
          intervalResId = R.plurals.options_incremental_interval_minutes;
          break;
        default:
          intervalResId = R.plurals.options_incremental_interval_bars;
          break;
      }
      partHolder.binding.textPartIncrementalInterval.setText(
          context.getResources().getQuantityString(
              intervalResId, incrementalInterval, incrementalInterval
          )
      );

      int incrementalLimit = part.getIncrementalLimit();
      if (incrementalLimit > 0) {
        partHolder.binding.textPartIncrementalLimit.setText(
            context.getResources().getString(
                incrementalIncrease
                    ? R.string.options_incremental_max
                    : R.string.options_incremental_min,
                incrementalLimit
            )
        );
      } else {
        partHolder.binding.textPartIncrementalLimit.setText(
            incrementalIncrease
                ? R.string.options_incremental_no_max
                : R.string.options_incremental_no_min
        );
      }
    }
    partHolder.binding.linearPartIncremental.setVisibility(
        isIncrementalActive ? View.VISIBLE : View.GONE
    );

    // muted
    int mutePlay = part.getMutePlay();
    int muteMute = part.getMuteMute();
    String muteUnit = part.getMuteUnit();
    boolean muteRandom = part.isMuteRandom();
    boolean isMuteActive = part.getMutePlay() > 0;
    int resIdPlay, resIdMute;
    if (muteUnit.equals(UNIT.SECONDS)) {
      resIdPlay = R.plurals.options_mute_play_seconds;
      resIdMute = R.plurals.options_mute_mute_seconds;
    } else {
      resIdPlay = R.plurals.options_mute_play_bars;
      resIdMute = R.plurals.options_mute_mute_bars;
    }
    if (isMuteActive) {
      partHolder.binding.textPartMutePlay.setText(
          context.getResources().getQuantityString(resIdPlay, mutePlay, mutePlay)
      );
      partHolder.binding.textPartMuteMute.setText(
          context.getResources().getQuantityString(resIdMute, muteMute, muteMute)
      );
      partHolder.binding.imagePartMuteRandom.setVisibility(muteRandom ? View.VISIBLE : View.GONE);
      partHolder.binding.textPartMuteRandom.setVisibility(muteRandom ? View.VISIBLE : View.GONE);
    }
    partHolder.binding.linearPartMute.setVisibility(isMuteActive ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onBindViewHolder(
      @NonNull ViewHolder holder, int position, @NonNull List<Object> payloads
  ) {
    PartViewHolder partHolder = (PartViewHolder) holder;
    if (payloads.contains(PAYLOAD_MENU)) {
      updateMenuItems(partHolder);
    } else {
      super.onBindViewHolder(holder, position, payloads);
    }
  }

  private void updateMenuItems(PartViewHolder holder) {
    int adapterPosition = holder.getBindingAdapterPosition();
    float alphaDisabled = 0.32f;
    MenuItem itemUp = holder.binding.toolbarPart.getMenu().findItem(R.id.action_move_up);
    boolean isUpEnabled = getItemCount() > 1 && adapterPosition > 0;
    itemUp.setEnabled(isUpEnabled);
    if (itemUp.getIcon() != null) {
      itemUp.getIcon().mutate().setAlpha(isUpEnabled ? 255 : (int) (alphaDisabled * 255));
    }
    MenuItem itemDown = holder.binding.toolbarPart.getMenu().findItem(R.id.action_move_down);
    boolean isDownEnabled = getItemCount() > 1 && adapterPosition < getItemCount() - 1;
    itemDown.setEnabled(isDownEnabled);
    if (itemDown.getIcon() != null) {
      itemDown.getIcon().mutate().setAlpha(isDownEnabled ? 255 : (int) (alphaDisabled * 255));
    }
    MenuItem itemDelete = holder.binding.toolbarPart.getMenu().findItem(R.id.action_delete);
    itemDelete.setEnabled(getItemCount() > 1);
  }

  public void notifyMenusChanged() {
    new Handler(Looper.getMainLooper()).postDelayed(
        () -> notifyItemRangeChanged(0, getItemCount(), PAYLOAD_MENU), 50
    );
  }

  public static class PartViewHolder extends ViewHolder {

    private final RowPartBinding binding;

    public PartViewHolder(RowPartBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public interface OnPartMenuItemClickListener {
    void onMenuItemClick(@NonNull Part part, @NonNull MenuItem item);
  }
}

class PartDiffCallback extends DiffUtil.ItemCallback<Part> {

  @Override
  public boolean areItemsTheSame(@NonNull Part oldItem, @NonNull Part newItem) {
    return oldItem.getId().equals(newItem.getId());
  }

  @Override
  public boolean areContentsTheSame(@NonNull Part oldItem, @NonNull Part newItem) {
    return oldItem.equals(newItem);
  }
}