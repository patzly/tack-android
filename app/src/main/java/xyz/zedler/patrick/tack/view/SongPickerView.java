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

package xyz.zedler.patrick.tack.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.graphics.drawable.ScaleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import com.google.android.material.radiobutton.MaterialRadioButton;
import java.util.Collections;
import java.util.List;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.ViewSongPickerBinding;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipsAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipsAdapter.OnSongClickListener;
import xyz.zedler.patrick.tack.recyclerview.decoration.SongChipItemDecoration;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class SongPickerView extends FrameLayout {

  private static final String TAG = SongPickerView.class.getSimpleName();

  private final ViewSongPickerBinding binding;
  private final boolean isRtl;
  private SongPickerListener listener;
  private MainActivity activity;
  private List<SongWithParts> songs;
  private int songsOrder;
  private String currentSong;
  private Drawable gradientLeft, gradientRight;
  private ValueAnimator animator;

  public SongPickerView(@NonNull Context context, AttributeSet attributeSet) {
    super(context, attributeSet);

    binding = ViewSongPickerBinding.inflate(
        LayoutInflater.from(context), this, true
    );
    isRtl = UiUtil.isLayoutRtl(context);
    songs = Collections.emptyList();
  }

  public void setListener(SongPickerListener listener) {
    this.listener = listener;
  }

  public void init(@NonNull MainActivity activity) {
    this.activity = activity;
    songsOrder = activity.getSharedPrefs().getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER);
    currentSong = activity.getMetronomeUtil().getCurrentSong();

    initRecycler();
    initChip();
    setCurrentSong(currentSong, false);
  }

  public void setSongs(List<SongWithParts> songs) {
    this.songs = songs;
    SongChipsAdapter adapter = (SongChipsAdapter) binding.recyclerSongPicker.getAdapter();
    if (adapter == null) {
      throw new IllegalStateException("init() has to be called before any other method");
    }

    if (songsOrder == SONGS_ORDER.NAME_ASC || songsOrder == SONGS_ORDER.NAME_DESC) {
      Collections.sort(
          songs, (o1, o2) -> o1.getSong().getName().compareTo(o2.getSong().getName())
      );
    } else if (songsOrder == SONGS_ORDER.LAST_PLAYED_ASC
        || songsOrder == SONGS_ORDER.LAST_PLAYED_DESC) {
      Collections.sort(
          songs,
          (s1, s2) -> Long.compare(s2.getSong().getLastPlayed(), s1.getSong().getLastPlayed())
      );
    }
    if (songsOrder == SONGS_ORDER.NAME_DESC || songsOrder == SONGS_ORDER.LAST_PLAYED_DESC) {
      Collections.reverse(songs);
    }
    adapter.setSongs(songs);

    maybeCenterSongChips(-1);
  }

  private void initRecycler() {
    // Adapter
    OnSongClickListener onSongClickListener = (chip, song) -> {
      String currentSong = song.getSong().getName();
      if (listener != null) {
        listener.onCurrentSongChanged(currentSong);
      }
      setCurrentSong(currentSong, true);
    };
    SongChipsAdapter adapter = new SongChipsAdapter(
        onSongClickListener, currentSong == null
    );
    binding.recyclerSongPicker.setAdapter(adapter);
    // Layout manager
    LinearLayoutManager layoutManager = new LinearLayoutManager(
        activity, LinearLayoutManager.HORIZONTAL, false
    );
    binding.recyclerSongPicker.setLayoutManager(layoutManager);

    maybeCenterSongChips(-1);
  }

  @SuppressLint("RtlHardcoded")
  private void initChip() {
    binding.textSongPickerChip.setText(currentSong);
    binding.frameSongPickerChipClose.setOnClickListener(v -> {
      if (listener != null) {
        listener.onCurrentSongChanged(null);
      }
      setCurrentSong(null, true);
    });
    binding.frameSongPickerChipTouchTarget.setOnClickListener(v -> {
      ViewUtil.startIcon(binding.imageSongPickerChipIcon);
      if (listener != null) {
        listener.onCurrentSongClicked();
      }
    });

    int colorSurface = ResUtil.getColor(activity, R.attr.colorSurface);
    gradientLeft = new GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        new int[]{Color.TRANSPARENT, colorSurface, colorSurface, colorSurface}
    );
    gradientLeft = new ScaleDrawable(gradientLeft, Gravity.RIGHT, 1, 0);
    gradientRight = new GradientDrawable(
        Orientation.RIGHT_LEFT,
        new int[]{Color.TRANSPARENT, colorSurface, colorSurface, colorSurface}
    );
    gradientRight = new ScaleDrawable(gradientRight, Gravity.LEFT, 1, 0);
    binding.viewSongPickerGradientStart.setBackground(isRtl ? gradientRight : gradientLeft);
    binding.viewSongPickerGradientEnd.setBackground(isRtl ? gradientLeft : gradientRight);
  }

  private void setCurrentSong(@Nullable String currentSong, boolean animated) {
    if (animator != null) {
      animator.pause();
      animator.cancel();
      animator.removeAllUpdateListeners();
      animator.removeAllListeners();
      animator = null;
    }
    ViewGroup.LayoutParams closeIconParams = binding.imageSongPickerChipClose.getLayoutParams();
    if (animated) {
      setRecyclerClicksEnabled(false);
      if (currentSong != null) {
        binding.textSongPickerChip.setText(currentSong);
        binding.frameSongPickerChipContainer.setTranslationX(0);
        binding.frameSongPickerChipContainer.setVisibility(View.VISIBLE);
        binding.frameSongPickerChipContainer.setAlpha(1);
        closeIconParams.width = 0;
        binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
      } else {
        binding.recyclerSongPicker.setAlpha(0);
        binding.recyclerSongPicker.setVisibility(View.VISIBLE);
      }
      int position = getPositionOfSong(currentSong != null ? currentSong : this.currentSong);
      LayoutManager layoutManager = binding.recyclerSongPicker.getLayoutManager();
      if (position >= 0 && layoutManager != null) {
        if (currentSong == null) {
          // Scroll to current song chip
          layoutManager.scrollToPosition(position);
        }
        // Delay to ensure scrolling is finished
        binding.recyclerSongPicker.post(() -> {
          View targetChip = layoutManager.findViewByPosition(position);
          if (targetChip != null) {
            int endLeft = targetChip.getLeft();
            int startLeft = binding.frameSongPickerChipTouchTarget.getLeft();
            // Compensate half of close icon width
            startLeft += currentSong == null ? UiUtil.dpToPx(activity, 9) : 0;
            int diff = endLeft - startLeft;
            int closeIconWidth = UiUtil.dpToPx(activity, 18);

            if (currentSong != null) {
              animator = ValueAnimator.ofFloat(0, 1);
              animator.setInterpolator(new OvershootInterpolator());
            } else {
              animator = ValueAnimator.ofFloat(1, 0);
              animator.setInterpolator(new FastOutSlowInInterpolator());
            }
            animator.addUpdateListener(animation -> {
              float fraction = (float) animation.getAnimatedValue();
              binding.frameSongPickerChipContainer.setTranslationX((1 - fraction) * diff);
              closeIconParams.width = (int) Math.min(closeIconWidth * fraction, closeIconWidth);
              binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
              binding.imageSongPickerChipClose.setAlpha(fraction);
              int colorTertiaryContainer = ResUtil.getColor(activity, R.attr.colorTertiaryContainer);
              int colorOnTertiaryContainer = ResUtil.getColor(
                  activity, R.attr.colorOnTertiaryContainer
              );
              int colorPrimary = ResUtil.getColor(activity, R.attr.colorPrimary);
              int colorSurface = ResUtil.getColor(activity, R.attr.colorSurface);
              int colorOnSurface = ResUtil.getColor(activity, R.attr.colorOnSurface);
              int colorOutline = ResUtil.getColor(activity, R.attr.colorOutline);

              int colorBg = ColorUtils.blendARGB(colorSurface, colorTertiaryContainer, fraction);
              int colorText = ColorUtils.blendARGB(
                  colorOnSurface, colorOnTertiaryContainer, fraction
              );
              int colorIcon = ColorUtils.blendARGB(colorPrimary, colorOnTertiaryContainer, fraction);
              int colorStroke = ColorUtils.blendARGB(
                  colorOutline, colorOnTertiaryContainer, fraction
              );
              binding.cardSongPickerChip.setCardBackgroundColor(colorBg);
              binding.cardSongPickerChip.setStrokeColor(colorStroke);
              binding.textSongPickerChip.setTextColor(colorText);
              binding.imageSongPickerChipIcon.setColorFilter(colorIcon);
              binding.imageSongPickerChipClose.setColorFilter(colorIcon);

              gradientLeft.setLevel((int) (10000 * fraction));
              gradientRight.setLevel((int) (10000 * fraction));

              binding.recyclerSongPicker.setAlpha(1 - fraction);
            });
            animator.addListener(new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                if (currentSong == null) {
                  binding.frameSongPickerChipContainer.setVisibility(INVISIBLE);
                  binding.frameSongPickerChipContainer.setAlpha(1);
                }
                setRecyclerClicksEnabled(currentSong == null);
              }
            });
            animator.setDuration(Constants.ANIM_DURATION_LONG);
            animator.start();
          }
        });
      }
    } else {
      binding.recyclerSongPicker.setAlpha(1);
      binding.recyclerSongPicker.setVisibility(currentSong != null ? INVISIBLE : VISIBLE);
      binding.frameSongPickerChipContainer.setVisibility(currentSong == null ? INVISIBLE : VISIBLE);
      binding.textSongPickerChip.setText(currentSong);
      closeIconParams.width = UiUtil.dpToPx(activity, 18);
      binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
      binding.imageSongPickerChipClose.setAlpha(1f);
    }
    this.currentSong = currentSong;
  }

  private int getPositionOfSong(String songName) {
    int position = -1;
    if (songName != null) {
      for (int i = 0; i < songs.size(); i++) {
        if (songs.get(i).getSong().getName().equals(songName)) {
          position = i;
          break;
        }
      }
    }
    return position;
  }

  private void maybeCenterSongChips(int scrollToPosition) {
    int outerPadding = UiUtil.dpToPx(getContext(), 16);
    int innerPadding = UiUtil.dpToPx(getContext(), 4);
    SongChipItemDecoration decoration = new SongChipItemDecoration(
        outerPadding, innerPadding, isRtl
    );
    if (binding.recyclerSongPicker.getItemDecorationCount() > 0) {
      binding.recyclerSongPicker.removeItemDecorationAt(0);
    }
    binding.recyclerSongPicker.addItemDecoration(decoration);
    binding.recyclerSongPicker.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            RecyclerView recyclerView = binding.recyclerSongPicker;
            if (recyclerView.getAdapter() != null && recyclerView.getChildCount() > 0) {
              int totalWidth = 0;
              for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                totalWidth += child.getWidth();
              }
              if (recyclerView.getChildCount() > 0) {
                totalWidth += innerPadding * 2 * (recyclerView.getChildCount() - 1);
                totalWidth += outerPadding * 2;
              }
              int containerWidth = recyclerView.getWidth();
              boolean shouldCenter = totalWidth < containerWidth;
              if (shouldCenter) {
                int padding = (containerWidth - totalWidth) / 2;
                recyclerView.setPadding(
                    isRtl ? 0 : padding, 0,
                    isRtl ? padding : 0, 0
                );
                recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
              } else {
                recyclerView.setPadding(0, 0, 0, 0);
                recyclerView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
                if (scrollToPosition >= 0
                    && binding.recyclerSongPicker.getLayoutManager() != null) {
                  // Scroll to active song chip
                  binding.recyclerSongPicker.getLayoutManager().scrollToPosition(scrollToPosition);
                }
              }
            }
            recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  private void setRecyclerClicksEnabled(boolean enabled) {
    SongChipsAdapter adapter = (SongChipsAdapter) binding.recyclerSongPicker.getAdapter();
    if (adapter != null) {
      adapter.setClickable(enabled);
    }
  }

  public interface SongPickerListener {
    void onCurrentSongChanged(@Nullable String currentSong);
    void onCurrentSongClicked();
  }
}
