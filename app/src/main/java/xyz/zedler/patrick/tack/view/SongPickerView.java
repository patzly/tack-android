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
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import com.google.android.material.motion.MotionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.DEF;
import xyz.zedler.patrick.tack.Constants.PREF;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.activity.MainActivity;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;
import xyz.zedler.patrick.tack.databinding.ViewSongPickerBinding;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipAdapter;
import xyz.zedler.patrick.tack.recyclerview.adapter.SongChipAdapter.OnSongClickListener;
import xyz.zedler.patrick.tack.recyclerview.decoration.SongChipItemDecoration;
import xyz.zedler.patrick.tack.recyclerview.layoutmanager.WrapperLinearLayoutManager;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.SortUtil;
import xyz.zedler.patrick.tack.util.UiUtil;
import xyz.zedler.patrick.tack.util.ViewUtil;
import xyz.zedler.patrick.tack.util.ViewUtil.OnMenuInflatedListener;
import xyz.zedler.patrick.tack.util.WidgetUtil;

public class SongPickerView extends FrameLayout {

  private static final String TAG = SongPickerView.class.getSimpleName();

  private final ViewSongPickerBinding binding;
  private final Context context;
  private final boolean isRtl;
  private final int heightCollapsed, heightExpanded, heightExpandedMargin;
  private final int paddingStartCollapsed, paddingStartExpanded;
  private final int minHeightCollapsed, minHeightExpanded;
  private final int colorBgCollapsed, colorBgExpanded;
  private final int colorFgCollapsed, colorFgExpanded;
  private final ViewUtil viewUtil;
  private MainActivity activity;
  private SongPickerListener listener;
  private List<SongWithParts> songsWithParts;
  private int sortOrder, currentPartIndex, widthMax, widthMin;
  private String currentSongId;
  private Drawable gradientLeft, gradientRight;
  private ValueAnimator animator;
  private SpringAnimation springAnimationExpand;
  private float expandFraction;
  private boolean isInitialized, expanded;

  public SongPickerView(@NonNull Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    this.context = context;

    viewUtil = new ViewUtil();

    binding = ViewSongPickerBinding.inflate(
        LayoutInflater.from(context), this, true
    );
    isRtl = UiUtil.isLayoutRtl(context);
    songsWithParts = Collections.emptyList();

    paddingStartCollapsed = UiUtil.dpToPx(context, 24 + 24 + 8);
    paddingStartExpanded = UiUtil.dpToPx(context, 8 + 48 + 4);
    minHeightCollapsed = UiUtil.dpToPx(context, 56);
    minHeightExpanded = UiUtil.dpToPx(context, 48 + 8 * 2);

    heightCollapsed = minHeightCollapsed;
    heightExpanded = UiUtil.dpToPx(context, 48 * 3 + 8 * 2);
    heightExpandedMargin = UiUtil.dpToPx(context, 32);

    colorBgCollapsed = ResUtil.getColor(context, R.attr.colorSecondaryContainer);
    colorBgExpanded = ResUtil.getColor(context, R.attr.colorSurfaceContainer);

    colorFgCollapsed = ResUtil.getColor(context, R.attr.colorOnSecondaryContainer);
    colorFgExpanded = ResUtil.getColor(context, R.attr.colorOnSurface);
  }

  public void setActivity(MainActivity activity) {
    this.activity = activity;
  }

  public void setListener(SongPickerListener listener) {
    this.listener = listener;
  }

  public void init(
      int songsOrder, @NonNull String currentSongId, int currentPartIndex, List<SongWithParts> songs
  ) {
    if (isInitialized) {
      return;
    }
    isInitialized = true;

    this.sortOrder = songsOrder;
    this.currentSongId = currentSongId;
    this.currentPartIndex = currentPartIndex;
    // To display current song title in current chip at start
    this.songsWithParts = new ArrayList<>(songs);

    initPickerSize();
    initRecycler();
    initChip();
    setCurrentSong(currentSongId, false);
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public void setSongs(List<SongWithParts> songs) {
    this.songsWithParts = new ArrayList<>(songs);
    // TODO: placeholder text if empty
    SongChipAdapter adapter = (SongChipAdapter) binding.recyclerSongPicker.getAdapter();
    if (adapter == null) {
      throw new IllegalStateException("init() has to be called before any other method");
    }

    SortUtil.sortSongsWithParts(songsWithParts, sortOrder);
    adapter.submitList(songsWithParts);

    maybeCenterSongChips();

    // maybe name of current song changed
    String songName = getSongNameFromId(currentSongId);
    String chipText = binding.textSongPickerChip.getText().toString();
    if (songName != null && !songName.equals(chipText)) {
      binding.textSongPickerChip.setText(songName);
    }
    // maybe name of current part changed
    setPartIndex(currentPartIndex);
  }

  public void setPartIndex(int partIndex) {
    String partName = getPartNameFromIndex(partIndex);
    String partLabel = context.getString(R.string.label_part_unnamed, partIndex + 1);
    if (partName != null) {
      partLabel = context.getString(
          R.string.label_part_current, partIndex + 1, partName
      );
    }
    binding.textSongPickerPart.setText(partLabel);
  }

  @SuppressLint("PrivateResource")
  public void setExpanded(boolean expanded, boolean animated) {
    this.expanded = expanded;
    if (springAnimationExpand != null) {
      springAnimationExpand.cancel();
    }
    boolean recyclerVisible =
        expanded && currentSongId.equals(Constants.SONG_ID_DEFAULT);
    if (animated) {
      setRecyclerClicksEnabled(false);
      if (recyclerVisible) {
        binding.buttonSongPickerCollapse.setVisibility(VISIBLE);
        binding.buttonGroupSongPickerTools.setVisibility(VISIBLE);
        binding.recyclerSongPicker.setVisibility(VISIBLE);
        binding.buttonSongPickerAddSong.setVisibility(VISIBLE);
      } else if (!expanded) {
        binding.imageSongPickerIcon.setVisibility(VISIBLE);
      }
      if (springAnimationExpand == null) {
        springAnimationExpand =
            new SpringAnimation(this, EXPAND_FRACTION)
                .setSpring(
                    MotionUtils.resolveThemeSpringForce(
                        getContext(),
                        R.attr.motionSpringDefaultSpatial,
                        R.style.Motion_Material3_Spring_Standard_Default_Spatial)
                )
                //.setSpring(new SpringForce().setStiffness(30f).setDampingRatio(0.9f))
                .setMinimumVisibleChange(0.01f)
                .addEndListener(
                    (animation, canceled, value, velocity) -> {
                      if (!canceled) {
                        setAnimationEndState();
                      }
                    });
      }
      springAnimationExpand.animateToFinalPosition(expanded ? 1 : 0);
    } else {
      setExpandFraction(expanded ? 1 : 0);
      setAnimationEndState();
    }
  }

  private void setAnimationEndState() {
    boolean recyclerVisible = expanded && currentSongId.equals(Constants.SONG_ID_DEFAULT);
    binding.frameSongPickerTop.setContentDescription(
        context.getString(expanded ? R.string.action_collapse : R.string.action_expand)
    );
    binding.buttonSongPickerCollapse.setVisibility(recyclerVisible ? VISIBLE : GONE);
    binding.imageSongPickerIcon.setVisibility(!expanded ? VISIBLE : GONE);
    binding.buttonGroupSongPickerTools.setVisibility(recyclerVisible ? VISIBLE : GONE);
    binding.recyclerSongPicker.setVisibility(recyclerVisible ? VISIBLE : GONE);
    setRecyclerClicksEnabled(recyclerVisible);
    binding.buttonSongPickerAddSong.setVisibility(recyclerVisible ? VISIBLE : GONE);
  }

  public void setParentWidth(int width) {
    widthMax = width - UiUtil.dpToPx(context, 16 + 16); // add horizontal margin
  }

  private void initPickerSize() {
    binding.buttonSongPickerCollapse.setOnClickListener(v -> {
      if (listener != null) {
        listener.onExpandCollapseClicked(false);
      }
      setExpanded(false, true);
    });
    ViewUtil.setTooltipText(binding.buttonSongPickerCollapse, R.string.action_collapse);
    ViewCompat.setAccessibilityDelegate(
        binding.frameSongPickerTop,
        new AccessibilityDelegateCompat() {
          @Override
          public void onInitializeAccessibilityNodeInfo(
              @NonNull View host,
              @NonNull AccessibilityNodeInfoCompat info
          ) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(Button.class.getName());
          }
        });
    binding.frameSongPickerTop.setOnClickListener(v -> {
      if (listener != null) {
        listener.onExpandCollapseClicked(expanded);
      }
      setExpanded(!this.expanded, true);
    });
    binding.frameSongPickerTop.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            widthMin = binding.frameSongPickerTop.getWidth();
            boolean isDefaultSong = currentSongId.equals(Constants.SONG_ID_DEFAULT);
            setExpanded(!isDefaultSong, false);
            if (binding.frameSongPickerTop.getViewTreeObserver().isAlive()) {
              binding.frameSongPickerTop.getViewTreeObserver().removeOnGlobalLayoutListener(
                  this
              );
            }
          }
        });
    binding.buttonSongPickerOpen.setOnClickListener(v -> {
      if (listener != null) {
        listener.onOpenSongsClicked();
      }
    });
    ViewUtil.setTooltipText(binding.buttonSongPickerOpen, R.string.action_show_list);
    binding.buttonSongPickerMenu.setOnClickListener(v -> {
      if (listener != null) {
        listener.onMenuOrMenuItemClicked();
      }
      PopupMenu.OnMenuItemClickListener itemClickListener = item -> {
        int id = item.getItemId();
        if (viewUtil.isClickDisabled(id)) {
          return false;
        }
        if (listener != null) {
          listener.onMenuOrMenuItemClicked();
        }
        if (id == R.id.action_sort_name
            || id == R.id.action_sort_last_played
            || id == R.id.action_sort_most_played) {
          if (item.isChecked()) {
            return false;
          }
          if (id == R.id.action_sort_name) {
            sortOrder = SONGS_ORDER.NAME_ASC;
          } else if (id == R.id.action_sort_last_played) {
            sortOrder = SONGS_ORDER.LAST_PLAYED_ASC;
          } else {
            sortOrder = SONGS_ORDER.MOST_PLAYED_ASC;
          }
          item.setChecked(true);
          setSongs(songsWithParts);
          activity.getSharedPrefs().edit().putInt(PREF.SONGS_ORDER, sortOrder).apply();
          activity.getMetronomeUtil().updateSongsOrder(sortOrder);
          if (!songsWithParts.isEmpty()) {
            // only update widget if sort order is important
            WidgetUtil.sendSongsWidgetUpdate(context);
          }
        } else if (id == R.id.action_backup) {
          if (listener != null) {
            listener.onBackupClicked();
          }
        }
        return true;
      };
      OnMenuInflatedListener menuInflatedListener = menu -> {
        sortOrder = activity.getSharedPrefs().getInt(PREF.SONGS_ORDER, DEF.SONGS_ORDER);
        int itemId = R.id.action_sort_name;
        if (sortOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
          itemId = R.id.action_sort_last_played;
        } else if (sortOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
          itemId = R.id.action_sort_most_played;
        }
        MenuItem itemSort = menu.findItem(itemId);
        if (itemSort != null) {
          itemSort.setChecked(true);
        }
        MenuItem itemBackup = menu.findItem(R.id.action_backup);
        if (itemBackup != null) {
          itemBackup.setEnabled(!songsWithParts.isEmpty());
        }
      };
      ViewUtil.showMenu(v, R.menu.menu_song_picker, itemClickListener, menuInflatedListener);
    });
    ViewUtil.setTooltipText(binding.buttonSongPickerMenu, R.string.action_more);
    binding.buttonSongPickerAddSong.setOnClickListener(v -> {
      if (listener != null) {
        listener.onAddSongClicked();
      }
    });
  }

  private void initRecycler() {
    // Adapter
    OnSongClickListener onSongClickListener = new OnSongClickListener() {
      @Override
      public void onSongClick(@NonNull SongWithParts song) {
        String currentSongId = song.getSong().getId();
        if (listener != null) {
          listener.onCurrentSongChanged(currentSongId);
        }
        setCurrentSong(currentSongId, true);
      }

      @Override
      public void onSongLongClick(@NonNull SongWithParts song) {
        if (listener != null) {
          listener.onSongLongClicked(song.getSong().getId());
        }
      }
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
    binding.frameSongPickerChipTouchTarget.setOnLongClickListener(v -> {
      if (listener != null) {
        listener.onCurrentSongLongClicked();
      }
      return true;
    });
    binding.cardSongPickerChip.setOnClickListener(
        v -> binding.frameSongPickerChipTouchTarget.callOnClick()
    );
    binding.cardSongPickerChip.setOnLongClickListener(v -> {
      if (listener != null) {
        listener.onCurrentSongLongClicked();
      }
      return true;
    });

    int colorSurface = ResUtil.getColor(context, R.attr.colorSurfaceContainer);
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
      binding.frameSongPickerChipTouchTarget.setBackgroundColor(
          ResUtil.getColor(context, R.attr.colorSurfaceContainer)
      );
      binding.cardSongPickerChip.setClickable(false);
      binding.viewSongPickerGradientStart.setVisibility(VISIBLE);
      binding.viewSongPickerGradientEnd.setVisibility(VISIBLE);
      if (!isDefaultSong) {
        binding.textSongPickerChip.setText(getSongNameFromId(currentSongId));
        binding.constraintSongPickerChipContainer.setTranslationX(0);
        binding.constraintSongPickerChipContainer.setVisibility(View.INVISIBLE);
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

            int colorTertiaryContainer = ResUtil.getColor(
                context, R.attr.colorTertiaryContainer
            );
            int colorOnTertiaryContainer = ResUtil.getColor(
                context, R.attr.colorOnTertiaryContainer
            );
            int colorPrimary = ResUtil.getColor(context, R.attr.colorPrimary);
            int colorSurface = ResUtil.getColor(context, R.attr.colorSurfaceBright);
            int colorOnSurface = ResUtil.getColor(context, R.attr.colorOnSurface);
            int colorOutlineVariant = ResUtil.getColor(context, R.attr.colorOutlineVariant);

            if (!isDefaultSong) {
              animator = ValueAnimator.ofFloat(0, 1);
              animator.setInterpolator(new OvershootInterpolator());
            } else {
              animator = ValueAnimator.ofFloat(1, 0);
              animator.setInterpolator(new FastOutSlowInInterpolator());
            }
            animator.addUpdateListener(animation -> {
              float fraction = (float) animation.getAnimatedValue();
              binding.constraintSongPickerChipContainer.setTranslationX((1 - fraction) * diff);
              if (binding.constraintSongPickerChipContainer.getVisibility() == View.INVISIBLE) {
                binding.constraintSongPickerChipContainer.setVisibility(View.VISIBLE);
              }
              closeIconParams.width = (int) Math.min(closeIconWidth * fraction, closeIconWidth);
              binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
              binding.imageSongPickerChipClose.setAlpha(fraction);

              float colorFraction = Math.max(0, Math.min(1, fraction));
              int colorBg = ColorUtils.blendARGB(
                  colorSurface, colorTertiaryContainer, colorFraction
              );
              int colorText = ColorUtils.blendARGB(
                  colorOnSurface, colorOnTertiaryContainer, colorFraction
              );
              int colorIcon = ColorUtils.blendARGB(
                  colorPrimary, colorOnTertiaryContainer, colorFraction
              );
              int colorStroke = ColorUtils.blendARGB(
                  colorOutlineVariant, Color.TRANSPARENT, colorFraction
              );
              binding.cardSongPickerChip.setCardBackgroundColor(colorBg);
              binding.cardSongPickerChip.setStrokeColor(colorStroke);
              binding.textSongPickerChip.setTextColor(colorText);
              binding.imageSongPickerChipIcon.setColorFilter(colorIcon);
              binding.imageSongPickerChipClose.setColorFilter(colorIcon);

              gradientLeft.setLevel((int) (10000 * fraction));
              gradientRight.setLevel((int) (10000 * fraction));

              binding.recyclerSongPicker.setAlpha(1 - colorFraction);

              binding.textSongPickerPart.setAlpha(colorFraction);
            });
            animator.addListener(new AnimatorListenerAdapter() {
              @Override
              public void onAnimationEnd(Animator animation) {
                if (isDefaultSong) {
                  binding.constraintSongPickerChipContainer.setVisibility(INVISIBLE);
                } else {
                  binding.viewSongPickerGradientStart.setVisibility(INVISIBLE);
                  binding.viewSongPickerGradientEnd.setVisibility(INVISIBLE);
                  binding.frameSongPickerChipTouchTarget.setBackground(null);
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
      boolean recyclerVisible = expanded && isDefaultSong;
      binding.recyclerSongPicker.setAlpha(recyclerVisible ? 1 : 0);
      binding.recyclerSongPicker.setVisibility(recyclerVisible ? VISIBLE : INVISIBLE);
      binding.constraintSongPickerChipContainer.setVisibility(isDefaultSong ? INVISIBLE : VISIBLE);
      binding.viewSongPickerGradientStart.setVisibility(INVISIBLE);
      binding.viewSongPickerGradientEnd.setVisibility(INVISIBLE);
      binding.frameSongPickerChipTouchTarget.setBackground(null);
      binding.frameSongPickerChipClose.setClickable(!isDefaultSong);
      binding.textSongPickerChip.setText(getSongNameFromId(currentSongId));
      closeIconParams.width = UiUtil.dpToPx(context, 18);
      binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
      binding.imageSongPickerChipClose.setAlpha(1f);
      binding.textSongPickerPart.setAlpha(isDefaultSong ? 0 : 1);
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

  private String getPartNameFromIndex(int partIndex) {
    for (SongWithParts songWithParts : songsWithParts) {
      if (songWithParts.getSong().getId().equals(currentSongId)) {
        for (Part part : songWithParts.getParts()) {
          if (part.getPartIndex() == partIndex) {
            return part.getName();
          }
        }
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

  public float getExpandFraction() {
    return expandFraction;
  }

  private void setExpandFraction(float fraction) {
    expandFraction = fraction;
    binding.cardSongPickerContainer.setCardBackgroundColor(
        ColorUtils.blendARGB(colorBgCollapsed, colorBgExpanded, fraction)
    );
    binding.textSongPickerTop.setTextColor(
        ColorUtils.blendARGB(colorFgCollapsed, colorFgExpanded, fraction)
    );
    int paddingStart =
        (int) (paddingStartCollapsed + (paddingStartExpanded - paddingStartCollapsed) * fraction);
    binding.frameSongPickerTop.setPadding(
        isRtl ? binding.frameSongPickerTop.getPaddingLeft() : paddingStart,
        binding.frameSongPickerTop.getPaddingTop(),
        isRtl ? paddingStart : binding.frameSongPickerTop.getPaddingRight(),
        binding.frameSongPickerTop.getPaddingBottom()
    );
    binding.frameSongPickerTop.setMinimumHeight(
        (int) (minHeightCollapsed + (minHeightExpanded - minHeightCollapsed) * fraction)
    );
    binding.buttonSongPickerCollapse.setAlpha(fraction);
    binding.imageSongPickerIcon.setAlpha(1 - fraction);
    binding.buttonGroupSongPickerTools.setAlpha(fraction);
    binding.recyclerSongPicker.setAlpha(fraction);
    binding.buttonSongPickerAddSong.setAlpha(fraction);
    ViewGroup.LayoutParams lp = getLayoutParams();
    lp.width = (int) (widthMin + (widthMax - widthMin) * fraction);
    lp.height = (int) (heightCollapsed + (heightExpanded - heightCollapsed) * fraction);
    setLayoutParams(lp);
    if (listener != null) {
      listener.onHeightChanged();
    }
  }

  public int getHeightExpanded() {
    return heightExpanded + heightExpandedMargin;
  }

  public int getHeightCollapsed() {
    return heightCollapsed;
  }

  public interface SongPickerListener {
    void onCurrentSongChanged(@NonNull String currentSongId);
    void onCurrentSongClicked();
    void onCurrentSongLongClicked();
    void onSongLongClicked(@NonNull String songId);
    void onExpandCollapseClicked(boolean expand);
    void onOpenSongsClicked();
    void onMenuOrMenuItemClicked();
    void onBackupClicked();
    void onAddSongClicked();
    void onHeightChanged();
  }

  private static final FloatPropertyCompat<SongPickerView> EXPAND_FRACTION =
      new FloatPropertyCompat<>("expandFraction") {
        @Override
        public float getValue(SongPickerView delegate) {
          return delegate.getExpandFraction();
        }

        @Override
        public void setValue(SongPickerView delegate, float value) {
          delegate.setExpandFraction(value);
        }
      };
}
