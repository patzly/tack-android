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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import java.util.ArrayList;
import java.util.List;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.databinding.RowPartBinding;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.util.ViewUtil.OnMenuInflatedListener;

public class PartAdapter extends Adapter<PartAdapter.PartViewHolder> {

  private final static String TAG = SongChipAdapter.class.getSimpleName();

  private final static String PAYLOAD_ROLE = "role";

  private final List<Part> parts = new ArrayList<>();
  private final OnPartItemClickListener listener;

  public PartAdapter(@NonNull OnPartItemClickListener listener) {
    this.listener = listener;
  }

  @NonNull
  @Override
  public PartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    RowPartBinding binding = RowPartBinding.inflate(
        LayoutInflater.from(parent.getContext()), parent, false
    );
    return new PartViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull PartViewHolder holder, int position) {
    int adapterPosition = holder.getBindingAdapterPosition();
    Part part = getItem(adapterPosition);
    Context context = holder.binding.getRoot().getContext();

    updateItemBackground(holder, position);
    updateMoveButtons(holder, position);

    // number
    String partNumber = context.getString(
        R.string.label_part_unnamed, adapterPosition + 1
    );
    holder.binding.textPartNumber.setText(partNumber);

    holder.binding.buttonPartEdit.setOnClickListener(
        v -> listener.onEditClick(part)
    );
    holder.binding.buttonPartMoveUp.setOnClickListener(
        v -> listener.onMoveUpClick(part)
    );
    holder.binding.buttonPartMoveDown.setOnClickListener(
        v -> listener.onMoveDownClick(part)
    );
    holder.binding.buttonPartMenu.setOnClickListener(v -> {
      listener.onMoreClick(part);
      PopupMenu.OnMenuItemClickListener itemClickListener = item -> {
        int id = item.getItemId();
        if (id == R.id.action_rename) {
          listener.onRenameClick(part);
        } else if (id == R.id.action_duplicate) {
          listener.onDuplicateClick(part);
        } else if (id == R.id.action_delete) {
          listener.onDeleteClick(part);
        }
        return true;
      };
      OnMenuInflatedListener menuInflatedListener = menu -> {
        MenuItem itemDelete = menu.findItem(R.id.action_delete);
        itemDelete.setEnabled(getItemCount() > 1);
      };
      ViewUtil.showMenu(v, R.menu.menu_part, itemClickListener, menuInflatedListener);
    });

    ViewUtil.setTooltipText(holder.binding.buttonPartEdit, R.string.action_edit);
    ViewUtil.setTooltipText(holder.binding.buttonPartMoveUp, R.string.action_move_up);
    ViewUtil.setTooltipText(holder.binding.buttonPartMoveDown, R.string.action_move_down);
    ViewUtil.setTooltipText(holder.binding.buttonPartMenu, R.string.action_more);

    // name
    String partName = part.getName();
    holder.binding.textPartName.setText(partName);
    holder.binding.linearPartName.setVisibility(partName != null ? View.VISIBLE : View.GONE);

    // tempo
    int tempo = part.getTempo();
    holder.binding.textPartTempo.setText(context.getString(R.string.label_bpm_value, tempo));

    // beats
    String[] beats = part.getBeats().split(",");
    holder.binding.beatsPartBeats.setBeats(beats);

    // subdivisions
    String[] subdivisions = part.getSubdivisions().split(",");
    holder.binding.beatsPartSubdivisions.setBeats(subdivisions);
    holder.binding.linearPartSubdivisions.setVisibility(
        subdivisions.length > 1 ? View.VISIBLE : View.GONE
    );

    // count in
    boolean isCountInActive = part.getCountIn() > 0;
    if (isCountInActive) {
      int countIn = part.getCountIn();
      holder.binding.textPartCountIn.setText(
          context.getResources().getQuantityString(
              R.plurals.options_count_in_description, countIn, countIn
          )
      );
    }
    holder.binding.linearPartCountIn.setVisibility(isCountInActive ? View.VISIBLE : View.GONE);

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
      holder.binding.textPartDuration.setText(
          context.getResources().getQuantityString(durationResId, timerDuration, timerDuration)
      );
    }
    holder.binding.linearPartDuration.setVisibility(
        timerDuration > 0 ? View.VISIBLE : View.GONE
    );

    // incremental
    int incrementalAmount = part.getIncrementalAmount();
    boolean incrementalIncrease = part.isIncrementalIncrease();
    boolean isIncrementalActive = part.getIncrementalAmount() > 0;
    if (isIncrementalActive) {
      holder.binding.textPartIncrementalAmount.setText(context.getString(
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
      holder.binding.textPartIncrementalInterval.setText(
          context.getResources().getQuantityString(
              intervalResId, incrementalInterval, incrementalInterval
          )
      );

      int incrementalLimit = part.getIncrementalLimit();
      if (incrementalLimit > 0) {
        holder.binding.textPartIncrementalLimit.setText(
            context.getResources().getString(
                incrementalIncrease
                    ? R.string.options_incremental_max
                    : R.string.options_incremental_min,
                incrementalLimit
            )
        );
      } else {
        holder.binding.textPartIncrementalLimit.setText(
            incrementalIncrease
                ? R.string.options_incremental_no_max
                : R.string.options_incremental_no_min
        );
      }
    }
    holder.binding.linearPartIncremental.setVisibility(
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
      holder.binding.textPartMutePlay.setText(
          context.getResources().getQuantityString(resIdPlay, mutePlay, mutePlay)
      );
      holder.binding.textPartMuteMute.setText(
          context.getResources().getQuantityString(resIdMute, muteMute, muteMute)
      );
      holder.binding.imagePartMuteRandom.setVisibility(muteRandom ? View.VISIBLE : View.GONE);
      holder.binding.textPartMuteRandom.setVisibility(muteRandom ? View.VISIBLE : View.GONE);
    }
    holder.binding.linearPartMute.setVisibility(isMuteActive ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onBindViewHolder(
      @NonNull PartViewHolder holder, int position, @NonNull List<Object> payloads
  ) {
    if (payloads.contains(PAYLOAD_ROLE)) {
      updateItemBackground(holder, position);
      updateMoveButtons(holder, position);
    } else {
      super.onBindViewHolder(holder, position, payloads);
    }
  }

  @Override
  public int getItemCount() {
    return parts.size();
  }

  public Part getItem(int position) {
    return parts.get(position);
  }

  public void setParts(List<Part> newParts) {
    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
      @Override
      public int getOldListSize() {
        return parts.size();
      }

      @Override
      public int getNewListSize() {
        return newParts.size();
      }

      @Override
      public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Part oldPart = parts.get(oldItemPosition);
        Part newPart = newParts.get(newItemPosition);
        return oldPart.getId().equals(newPart.getId());
      }

      @Override
      public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Part oldPart = parts.get(oldItemPosition);
        Part newPart = newParts.get(newItemPosition);

        if (!oldPart.equals(newPart)) {
          return false;
        }

        int oldRole = getItemRole(oldItemPosition, parts.size());
        int newRole = getItemRole(newItemPosition, newParts.size());
        return oldRole == newRole;
      }

      @Override
      public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Part oldPart = parts.get(oldItemPosition);
        Part newPart = newParts.get(newItemPosition);

        int oldRole = getItemRole(oldItemPosition, parts.size());
        int newRole = getItemRole(newItemPosition, newParts.size());
        if (oldPart.equals(newPart) && oldRole != newRole) {
          return PAYLOAD_ROLE;
        }

        return null;
      }

      private int getItemRole(int position, int size) {
        if (size == 1) return -1;
        if (position == 0) return 0;
        if (position == size - 1) return 2;
        return 1;
      }
    });
    parts.clear();
    parts.addAll(newParts);
    diffResult.dispatchUpdatesTo(this);
  }

  private void updateItemBackground(PartViewHolder holder, int position) {
    if (getItemCount() == 1) {
      holder.binding.linearPartContainer.setBackgroundResource(
          R.drawable.ripple_list_item_bg_segmented_single
      );
    } else if (position == 0) {
      holder.binding.linearPartContainer.setBackgroundResource(
          R.drawable.ripple_list_item_bg_segmented_first
      );
    } else if (position == getItemCount() - 1) {
      holder.binding.linearPartContainer.setBackgroundResource(
          R.drawable.ripple_list_item_bg_segmented_last
      );
    } else {
      holder.binding.linearPartContainer.setBackgroundResource(
          R.drawable.ripple_list_item_bg_segmented_middle
      );
    }
  }

  private void updateMoveButtons(PartViewHolder holder, int position) {
    holder.binding.buttonPartMoveUp.setEnabled(getItemCount() > 1 && position > 0);
    holder.binding.buttonPartMoveDown.setEnabled(
        getItemCount() > 1 && position < getItemCount() - 1
    );
  }

  public static class PartViewHolder extends ViewHolder {
    private final RowPartBinding binding;

    public PartViewHolder(RowPartBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }

  public interface OnPartItemClickListener {
    void onEditClick(@NonNull Part part);
    void onMoveUpClick(@NonNull Part part);
    void onMoveDownClick(@NonNull Part part);
    void onMoreClick(@NonNull Part part);
    void onRenameClick(@NonNull Part part);
    void onDuplicateClick(@NonNull Part part);
    void onDeleteClick(@NonNull Part part);
  }
}