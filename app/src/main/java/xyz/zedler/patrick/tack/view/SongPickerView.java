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
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.ViewSongPickerBinding;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipAdapter.OnSongClickListener;
import xyz.zedler.patrick.tack.recyclerview.decoration.SongChipItemDecoration;
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;

public class SongPickerView extends FrameLayout {

  private static final String TAG = SongPickerView.class.getSimpleName();

  private final ViewSongPickerBinding binding;
  private final boolean isRtl;
  private final Context context;
  private SongPickerListener listener;
  private List<SongWithParts> songsWithParts;
  private int songsOrder;
  private String currentSongId;
  private Drawable gradientLeft, gradientRight;
  private ValueAnimator animator;
  private boolean isInitialized;

  public SongPickerView(@NonNull Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    this.context = context;

    binding = ViewSongPickerBinding.inflate(
        LayoutInflater.from(context), this, true
    );
    isRtl = UiUtil.isLayoutRtl(context);
    songsWithParts = Collections.emptyList();
  }

  public void setListener(SongPickerListener listener) {
    this.listener = listener;
  }

  public void init(int songsOrder, @NonNull String currentSongId, List<SongWithParts> songs) {
    if (isInitialized) {
      return;
    }
    isInitialized = true;

    this.songsOrder = songsOrder;
    this.currentSongId = currentSongId;
    // To display current song title in current chip at start
    this.songsWithParts = new ArrayList<>(songs);

    initRecycler();
    initChip();
    setCurrentSong(currentSongId, false);
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public void setSongs(List<SongWithParts> songs) {
    this.songsWithParts = new ArrayList<>(songs);
    SongChipAdapter adapter = (SongChipAdapter) binding.recyclerSongPicker.getAdapter();
    if (adapter == null) {
      throw new IllegalStateException("init() has to be called before any other method");
    }

    if (songsOrder == SONGS_ORDER.NAME_ASC) {
      if (VERSION.SDK_INT >= VERSION_CODES.N) {
        Comparator<String> comparator = Comparator.nullsLast(
            Comparator.comparing(String::toLowerCase, Comparator.naturalOrder())
        );
        Collections.sort(
            songsWithParts,
            Comparator.comparing(
                o -> o.getSong().getName(),
                comparator
            )
        );
      } else {
        Collections.sort(songsWithParts, (o1, o2) -> {
          String name1 = (o1.getSong() != null) ? o1.getSong().getName() : null;
          String name2 = (o2.getSong() != null) ? o2.getSong().getName() : null;
          // Nulls last handling
          if (name1 == null && name2 == null) return 0;
          if (name1 == null) return 1;
          if (name2 == null) return -1;
          return name1.compareToIgnoreCase(name2);
        });
      }
    } else if (songsOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      Collections.sort(
          songsWithParts,
          (s1, s2) -> Long.compare(
              s2.getSong().getLastPlayed(), s1.getSong().getLastPlayed()
          )
      );
    } else if (songsOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
      Collections.sort(
          songsWithParts,
          (s1, s2) -> Integer.compare(
              s2.getSong().getPlayCount(), s1.getSong().getPlayCount()
          )
      );
    }
    adapter.submitList(songsWithParts);

    maybeCenterSongChips();
  }

  private void initRecycler() {
    // Adapter
    OnSongClickListener onSongClickListener = (chip, song) -> {
      String currentSongId = song.getSong().getId();
      if (listener != null) {
        listener.onCurrentSongChanged(currentSongId);
      }
      setCurrentSong(currentSongId, true);
    };
    SongChipAdapter adapter = new SongChipAdapter(
        onSongClickListener, currentSongId.equals(Constants.SONG_ID_DEFAULT)
    );
    binding.recyclerSongPicker.setAdapter(adapter);
    // Layout manager
    LinearLayoutManager layoutManager = new WrapperLinearLayoutManager(
        context, LinearLayoutManager.HORIZONTAL, false
    );
    binding.recyclerSongPicker.setLayoutManager(layoutManager);
    boolean isPortrait = UiUtil.isOrientationPortrait(context);
    boolean isLandTablet = UiUtil.isLandTablet(context);
    binding.recyclerSongPicker.setHorizontalFadingEdgeEnabled(!isPortrait && !isLandTablet);

    maybeCenterSongChips();
  }

  @SuppressLint("RtlHardcoded")
  private void initChip() {
    binding.textSongPickerChip.setText(getSongNameFromId(currentSongId));
    binding.frameSongPickerChipClose.setOnClickListener(v -> {
      if (listener != null) {
        listener.onCurrentSongChanged(Constants.SONG_ID_DEFAULT);
      }
      setCurrentSong(Constants.SONG_ID_DEFAULT, true);
    });
    binding.imageSongPickerChipClose.setOnClickListener(
        v -> binding.frameSongPickerChipClose.callOnClick()
    );
    ViewCompat.setAccessibilityDelegate(
        binding.imageSongPickerChipClose, new AccessibilityDelegateCompat() {
          @Override
          public void onInitializeAccessibilityNodeInfo(
              @NonNull View host,
              @NonNull AccessibilityNodeInfoCompat info
          ) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(Button.class.getName());
          }
        });
    // TODO: improve accessibility
    binding.frameSongPickerChipTouchTarget.setOnClickListener(v -> {
      ViewUtil.startIcon(binding.imageSongPickerChipIcon);
      if (listener != null) {
        listener.onCurrentSongClicked();
      }
    });
    binding.cardSongPickerChip.setOnClickListener(
        v -> binding.frameSongPickerChipTouchTarget.callOnClick()
    );

    int colorSurface = ResUtil.getColor(context, R.attr.colorSurface);
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

  private void setCurrentSong(@NonNull String currentSongId, boolean animated) {
    boolean isDefaultSong = currentSongId.equals(Constants.SONG_ID_DEFAULT);
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
      binding.frameSongPickerChipClose.setClickable(false);
      binding.frameSongPickerChipTouchTarget.setClickable(false);
      binding.cardSongPickerChip.setClickable(false);
      if (!isDefaultSong) {
        binding.textSongPickerChip.setText(getSongNameFromId(currentSongId));
        binding.frameSongPickerChipContainer.setTranslationX(0);
        binding.frameSongPickerChipContainer.setVisibility(View.INVISIBLE);
        closeIconParams.width = 0;
        binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
      } else {
        binding.recyclerSongPicker.setAlpha(0);
        binding.recyclerSongPicker.setVisibility(View.VISIBLE);
      }
      int position = getPositionOfSong(!isDefaultSong ? currentSongId : this.currentSongId);
      LayoutManager layoutManager = binding.recyclerSongPicker.getLayoutManager();
      if (position >= 0 && layoutManager != null) {
        if (isDefaultSong) {
          // Scroll to current song chip
          layoutManager.scrollToPosition(position);
          maybeCenterSongChips();
        }
        // Delay to ensure scrolling is finished
        binding.recyclerSongPicker.post(() -> {
          View targetChip = layoutManager.findViewByPosition(position);
          if (targetChip != null) {
            int endLeft = targetChip.getLeft();
            int startLeft = binding.frameSongPickerChipTouchTarget.getLeft();
            // Compensate half of close icon width
            startLeft += isDefaultSong ? UiUtil.dpToPx(context, 9) : 0;
            int diff = endLeft - startLeft;
            int closeIconWidth = UiUtil.dpToPx(context, 18);

            if (!isDefaultSong) {
              animator = ValueAnimator.ofFloat(0, 1);
              animator.setInterpolator(new OvershootInterpolator());
            } else {
              animator = ValueAnimator.ofFloat(1, 0);
              animator.setInterpolator(new FastOutSlowInInterpolator());
            }
            animator.addUpdateListener(animation -> {
              float fraction = (float) animation.getAnimatedValue();
              binding.frameSongPickerChipContainer.setTranslationX((1 - fraction) * diff);
              if (binding.frameSongPickerChipContainer.getVisibility() == View.INVISIBLE) {
                binding.frameSongPickerChipContainer.setVisibility(View.VISIBLE);
              }
              closeIconParams.width = (int) Math.min(closeIconWidth * fraction, closeIconWidth);
              binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
              binding.imageSongPickerChipClose.setAlpha(fraction);
              int colorTertiaryContainer = ResUtil.getColor(
                  context, R.attr.colorTertiaryContainer
              );
              int colorOnTertiaryContainer = ResUtil.getColor(
                  context, R.attr.colorOnTertiaryContainer
              );
              int colorPrimary = ResUtil.getColor(context, R.attr.colorPrimary);
              int colorSurface = ResUtil.getColor(context, R.attr.colorSurface);
              int colorOnSurface = ResUtil.getColor(context, R.attr.colorOnSurface);
              int colorOutline = ResUtil.getColor(context, R.attr.colorOutline);

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
                if (isDefaultSong) {
                  binding.frameSongPickerChipContainer.setVisibility(INVISIBLE);
                }
                binding.frameSongPickerChipClose.setClickable(!isDefaultSong);
                binding.frameSongPickerChipTouchTarget.setClickable(!isDefaultSong);
                binding.cardSongPickerChip.setClickable(!isDefaultSong);
                setRecyclerClicksEnabled(isDefaultSong);
              }
            });
            animator.setDuration(Constants.ANIM_DURATION_LONG);
            animator.start();
          }
        });
      }
    } else {
      binding.recyclerSongPicker.setAlpha(1);
      binding.recyclerSongPicker.setVisibility(!isDefaultSong ? INVISIBLE : VISIBLE);
      binding.frameSongPickerChipContainer.setVisibility(isDefaultSong ? INVISIBLE : VISIBLE);
      binding.frameSongPickerChipClose.setClickable(!isDefaultSong);
      binding.textSongPickerChip.setText(getSongNameFromId(currentSongId));
      closeIconParams.width = UiUtil.dpToPx(context, 18);
      binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
      binding.imageSongPickerChipClose.setAlpha(1f);
      setRecyclerClicksEnabled(isDefaultSong);
    }
    this.currentSongId = currentSongId;
  }

  private int getPositionOfSong(@NonNull String songNameId) {
    int position = -1;
    for (int i = 0; i < songsWithParts.size(); i++) {
      if (songsWithParts.get(i).getSong().getId().equals(songNameId)) {
        position = i;
        break;
      }
    }
    return position;
  }

  private void maybeCenterSongChips() {
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
            SongChipAdapter adapter = (SongChipAdapter) recyclerView.getAdapter();
            int itemCount = adapter != null ? adapter.getItemCount() : 0;
            if (adapter != null && itemCount > 0) {
              int totalWidth = 0;
              for (int i = 0; i < itemCount; i++) {
                View child = recyclerView.getChildAt(i);
                if (child != null) {
                  // adapter item count sometimes leads to index out of bounds of recyclerview
                  // but child count sometimes leads to wrong number of children which leads to
                  // an animation to the wrong place, so simply ignore the index out of bounds
                  totalWidth += child.getWidth();
                }
              }
              totalWidth += innerPadding * 2 * (itemCount - 1);
              totalWidth += outerPadding * 2;
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
              }
            }
            recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          }
        });
  }

  private String getSongNameFromId(@NonNull String songId) {
    for (SongWithParts songWithParts : songsWithParts) {
      if (songWithParts.getSong().getId().equals(songId)) {
        return songWithParts.getSong().getName();
      }
    }
    return null;
  }

  private void setRecyclerClicksEnabled(boolean enabled) {
    SongChipAdapter adapter = (SongChipAdapter) binding.recyclerSongPicker.getAdapter();
    if (adapter != null) {
      adapter.setClickable(enabled);
    }
  }

  public interface SongPickerListener {
    void onCurrentSongChanged(@NonNull String currentSongId);
    void onCurrentSongClicked();
  }
}
