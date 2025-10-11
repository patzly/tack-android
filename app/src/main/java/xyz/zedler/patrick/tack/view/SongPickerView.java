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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
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
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import com.google.android.material.motion.MotionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.R;
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

  private static final boolean TEST_ANIMATIONS = false;

  private final ViewSongPickerBinding binding;
  private final Context context;
  private final boolean isRtl;
  private final int heightCollapsed, heightExpanded, heightExpandedMargin;
  private final int colorBgCollapsed, colorBgExpanded;
  private final int chipCloseIconWidth;
  private final int colorPrimary, colorTertiaryContainer, colorOnTertiaryContainer;
  private final int colorSurfaceBright, colorSurfaceContainer;
  private final int colorOnSurface, colorOnSurfaceVariant;
  private final ViewUtil viewUtil;
  private SongPickerListener listener;
  private List<SongWithParts> songsWithParts;
  private int sortOrder, partIndex, widthMax, widthMin, chipTargetTranslationX;
  private String currentSongId;
  private Drawable gradientLeft, gradientRight;
  private SpringAnimation springAnimationExpand;
  private SpringAnimation springAnimationDeselectSpatial, springAnimationDeselectEffects;
  private SpringAnimation springAnimationSelectSpatial, springAnimationSelectEffects;
  private float expandFraction, selectSpatialFraction, selectEffectsFraction;
  private boolean isInitialized, isExpanded;

  public SongPickerView(@NonNull Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    this.context = context;

    viewUtil = new ViewUtil();

    binding = ViewSongPickerBinding.inflate(LayoutInflater.from(context), this);
    isRtl = UiUtil.isLayoutRtl(context);
    songsWithParts = Collections.emptyList();

    heightCollapsed = UiUtil.dpToPx(context, 56);
    heightExpanded = UiUtil.dpToPx(context, 48 * 3 + 8 * 2);
    heightExpandedMargin = UiUtil.dpToPx(context, 32);

    colorBgCollapsed = ResUtil.getColor(context, R.attr.colorSecondaryContainer);
    colorBgExpanded = ResUtil.getColor(context, R.attr.colorSurfaceContainer);

    chipCloseIconWidth = UiUtil.dpToPx(context, 18);
    colorTertiaryContainer = ResUtil.getColor(context, R.attr.colorTertiaryContainer);
    colorOnTertiaryContainer = ResUtil.getColor(context, R.attr.colorOnTertiaryContainer);
    colorPrimary = ResUtil.getColor(context, R.attr.colorPrimary);
    colorSurfaceBright = ResUtil.getColor(context, R.attr.colorSurfaceBright);
    colorSurfaceContainer = ResUtil.getColor(context, R.attr.colorSurfaceContainer);
    colorOnSurface = ResUtil.getColor(context, R.attr.colorOnSurface);
    colorOnSurfaceVariant = ResUtil.getColor(context, R.attr.colorOnSurfaceVariant);
  }

  public void setListener(SongPickerListener listener) {
    this.listener = listener;
  }

  public void init(
      @NonNull String currentSongId,
      int currentPartIndex,
      List<SongWithParts> songs,
      int sortOrder,
      boolean expanded
  ) {
    if (isInitialized) {
      return;
    }
    isInitialized = true;

    this.sortOrder = sortOrder;
    this.currentSongId = currentSongId;
    // To display current song title in current chip at start
    this.songsWithParts = new ArrayList<>(songs);

    initPickerSize(expanded, currentSongId);
    initRecycler();
    initChip();
    setCurrentSong(currentSongId, false);
    setPartIndex(currentPartIndex);
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public void setSongs(List<SongWithParts> songs) {
    this.songsWithParts = new ArrayList<>(songs);

    binding.textSongPickerEmpty.setVisibility(songsWithParts.isEmpty() ? VISIBLE : GONE);

    sortSongs();

    if (currentSongId.equals(Constants.SONG_ID_DEFAULT)) {
      return;
    }
    // maybe name of current song changed
    String songName = getSongNameFromId(currentSongId);
    String chipText = binding.textSongPickerChip.getText().toString();
    if (songName != null && !songName.equals(chipText)) {
      binding.textSongPickerChip.setText(songName);
    }
    // maybe name of current part changed
    String partName = getPartNameFromIndex(partIndex);
    String partLabel = context.getString(R.string.label_part_unnamed, partIndex + 1);
    if (partName != null) {
      partLabel = context.getString(
          R.string.label_part_current, partIndex + 1, partName
      );
    }
    binding.buttonSongPickerPart.setText(partLabel);
  }

  private void sortSongs() {
    SortUtil.sortSongsWithParts(songsWithParts, sortOrder);
    SongChipAdapter adapter = (SongChipAdapter) binding.recyclerSongPicker.getAdapter();
    if (adapter != null) {
      adapter.submitList(songsWithParts, this::maybeCenterSongChips);
    } else {
      throw new IllegalStateException("init() has to be called before any other method");
    }
  }

  public void setPartIndex(int partIndex) {
    this.partIndex = partIndex;
    String partName = getPartNameFromIndex(partIndex);
    String partLabel = context.getString(R.string.label_part_unnamed, partIndex + 1);
    if (partName != null) {
      partLabel = context.getString(
          R.string.label_part_current, partIndex + 1, partName
      );
    }
    binding.buttonSongPickerPart.setText(partLabel);
    int partCount = getPartCount();
    binding.buttonSongPickerPartPrevious.setEnabled(partCount > 0 && partIndex > 0);
    binding.buttonSongPickerPartNext.setEnabled(partCount > 0 && partIndex < partCount - 1);
  }

  public void setParentWidth(int width) {
    boolean isPortrait = UiUtil.isOrientationPortrait(context);
    boolean isLandTablet = UiUtil.isLandTablet(context);
    if (isPortrait || isLandTablet) {
      width = Math.min(width, ResUtil.getDimension(context, R.dimen.max_content_width));
    }
    widthMax = width - UiUtil.dpToPx(context, 16 + 16); // add horizontal margin
  }

  private void initPickerSize(boolean expanded, String currentSongId) {
    binding.buttonSongPickerExpand.setOnClickListener(v -> {
      if (isExpanded) {
        return;
      }
      if (listener != null) {
        listener.onExpandCollapseClicked(true);
      }
      setExpanded(true, true);
    });
    binding.buttonSongPickerCollapse.setOnClickListener(v -> {
      if (!isExpanded) {
        return;
      }
      if (listener != null) {
        listener.onExpandCollapseClicked(false);
      }
      setExpanded(false, true);
    });
    ViewUtil.setTooltipText(binding.buttonSongPickerCollapse, R.string.action_collapse);
    binding.frameSongPickerTop.setOnClickListener(
        v -> binding.buttonSongPickerCollapse.performClick()
    );
    binding.buttonSongPickerExpand.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            widthMin = binding.buttonSongPickerExpand.getWidth();

            setExpanded(expanded, false);
            if (expanded) {
              // Redo song select animation stuff
              setCurrentSong(currentSongId, false);
            }

            if (binding.buttonSongPickerExpand.getViewTreeObserver().isAlive()) {
              binding.buttonSongPickerExpand.getViewTreeObserver().removeOnGlobalLayoutListener(
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
    ViewUtil.setTooltipText(binding.buttonSongPickerOpen, R.string.action_show_songs_list);
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

          sortSongs();

          if (listener != null) {
            listener.onSortOrderChanged(sortOrder);
          }
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
        context, onSongClickListener, currentSongId.equals(Constants.SONG_ID_DEFAULT)
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
    binding.buttonSongPickerPart.setOnClickListener(v -> {
      if (listener != null) {
        listener.onCurrentPartClicked();
      }
    });
    ViewUtil.setTooltipText(binding.buttonSongPickerPart, R.string.action_show_parts);
    binding.buttonSongPickerPartPrevious.setOnClickListener(v -> {
      if (listener != null) {
        listener.onPreviousPartClicked();
      }
    });
    ViewUtil.setTooltipText(binding.buttonSongPickerPartPrevious, R.string.action_prev_part);
    binding.buttonSongPickerPartNext.setOnClickListener(v -> {
      if (listener != null) {
        listener.onNextPartClicked();
      }
    });
    ViewUtil.setTooltipText(binding.buttonSongPickerPartNext, R.string.action_next_part);

    gradientLeft = new GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        new int[]{
            Color.TRANSPARENT, colorSurfaceContainer, colorSurfaceContainer, colorSurfaceContainer
        }
    );
    gradientLeft = new ScaleDrawable(gradientLeft, Gravity.RIGHT, 1, 0);
    gradientRight = new GradientDrawable(
        Orientation.RIGHT_LEFT,
        new int[]{
            Color.TRANSPARENT, colorSurfaceContainer, colorSurfaceContainer, colorSurfaceContainer
        }
    );
    gradientRight = new ScaleDrawable(gradientRight, Gravity.LEFT, 1, 0);
    binding.viewSongPickerGradientStart.setBackground(isRtl ? gradientRight : gradientLeft);
    binding.viewSongPickerGradientEnd.setBackground(isRtl ? gradientLeft : gradientRight);
  }

  @SuppressLint("PrivateResource")
  public void setExpanded(boolean expanded, boolean animated) {
    this.isExpanded = expanded;
    if (listener != null) {
      listener.onExpandChanged(expanded);
    }

    if (springAnimationExpand != null) {
      springAnimationExpand.cancel();
    }
    if (animated) {
      if (springAnimationExpand == null) {
        springAnimationExpand =
            new SpringAnimation(this, EXPAND_FRACTION)
                .setSpring(
                    MotionUtils.resolveThemeSpringForce(
                        getContext(),
                        R.attr.motionSpringDefaultSpatial,
                        R.style.Motion_Material3_Spring_Standard_Default_Spatial)
                )
                .setMinimumVisibleChange(0.01f)
                .addEndListener(
                    (animation, canceled, value, velocity) -> {
                      if (!canceled) {
                        setExpandAnimationEndState();
                      }
                    });
      }
      if (TEST_ANIMATIONS) {
        springAnimationExpand.setSpring(new SpringForce().setStiffness(30f).setDampingRatio(0.9f));
      }
      setExpandAnimationStartState();
      springAnimationExpand.animateToFinalPosition(expanded ? 1 : 0);
    } else {
      setExpandAnimationStartState();
      setExpandFraction(expanded ? 1 : 0);
      setExpandAnimationEndState();
    }
  }

  private void setExpandAnimationStartState() {
    binding.buttonSongPickerExpand.setClickable(!isExpanded);
    binding.buttonSongPickerCollapse.setClickable(isExpanded);
    binding.frameSongPickerTop.setClickable(isExpanded);
    binding.buttonSongPickerOpen.setClickable(isExpanded);
    binding.buttonSongPickerMenu.setClickable(isExpanded);
    binding.buttonSongPickerAddSong.setClickable(isExpanded);

    binding.buttonSongPickerExpand.setVisibility(VISIBLE);
    binding.buttonSongPickerCollapse.setVisibility(VISIBLE);
    binding.buttonGroupSongPickerTools.setVisibility(VISIBLE);
    binding.recyclerSongPicker.setVisibility(VISIBLE);
    setRecyclerClicksEnabled(false);
    binding.buttonSongPickerAddSong.setVisibility(VISIBLE);
  }

  private void setExpandAnimationEndState() {
    binding.buttonSongPickerExpand.setVisibility(isExpanded ? INVISIBLE : VISIBLE);
    binding.buttonSongPickerCollapse.setVisibility(isExpanded ? VISIBLE : GONE);
    binding.buttonGroupSongPickerTools.setVisibility(isExpanded ? VISIBLE : GONE);
    binding.recyclerSongPicker.setVisibility(isExpanded ? VISIBLE : INVISIBLE);
    setRecyclerClicksEnabled(isExpanded);
    if (isExpanded) {
      maybeCenterSongChips();
    }
    binding.buttonSongPickerAddSong.setVisibility(isExpanded ? VISIBLE : GONE);
  }

  private void setExpandFraction(float fraction) {
    expandFraction = fraction;

    binding.buttonSongPickerExpand.setAlpha(1 - fraction);
    binding.cardSongPickerContainer.setCardBackgroundColor(
        ColorUtils.blendARGB(colorBgCollapsed, colorBgExpanded, fraction)
    );
    binding.buttonSongPickerCollapse.setAlpha(fraction);
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

  public float getExpandFraction() {
    return expandFraction;
  }

  public int getHeightExpanded() {
    return heightExpanded + heightExpandedMargin;
  }

  @SuppressLint("PrivateResource")
  private void setCurrentSong(@NonNull String currentSongId, boolean animated) {
    boolean isDefaultSong = currentSongId.equals(Constants.SONG_ID_DEFAULT);
    int position = getPositionOfSong(isDefaultSong ? this.currentSongId : currentSongId);
    this.currentSongId = currentSongId;

    if (springAnimationSelectSpatial != null) {
      springAnimationSelectSpatial.cancel();
    }
    if (springAnimationSelectEffects != null) {
      springAnimationSelectEffects.cancel();
    }
    if (springAnimationDeselectSpatial != null) {
      springAnimationDeselectSpatial.cancel();
    }
    if (springAnimationDeselectEffects != null) {
      springAnimationDeselectEffects.cancel();
    }
    if (animated) {
      if (springAnimationSelectSpatial == null) {
        springAnimationSelectSpatial =
            new SpringAnimation(this, SELECT_SPATIAL_FRACTION)
                .setSpring(new SpringForce().setStiffness(300f).setDampingRatio(0.6f))
                .setMinimumVisibleChange(0.01f)
                .addEndListener(
                    (animation, canceled, value, velocity) -> {
                      if (!canceled) {
                        setSelectAnimationEndState();
                      }
                    });
        if (TEST_ANIMATIONS) {
          springAnimationSelectSpatial.setSpring(
              new SpringForce().setStiffness(20f).setDampingRatio(0.3f)
          );
        }
      }
      if (springAnimationSelectEffects == null) {
        springAnimationSelectEffects =
            new SpringAnimation(this, SELECT_EFFECTS_FRACTION)
                .setSpring(new SpringForce().setStiffness(300f).setDampingRatio(1f))
                .setMinimumVisibleChange(0.01f);
        if (TEST_ANIMATIONS) {
          springAnimationSelectEffects.setSpring(
              new SpringForce().setStiffness(40f).setDampingRatio(1f)
          );
        }
      }
      if (springAnimationDeselectSpatial == null) {
        springAnimationDeselectSpatial =
            new SpringAnimation(this, DESELECT_SPATIAL_FRACTION)
                .setSpring(
                    MotionUtils.resolveThemeSpringForce(
                        getContext(),
                        R.attr.motionSpringSlowSpatial,
                        R.style.Motion_Material3_Spring_Standard_Slow_Spatial)
                )
                .setMinimumVisibleChange(0.01f)
                .addEndListener(
                    (animation, canceled, value, velocity) -> {
                      if (!canceled) {
                        setSelectAnimationEndState();
                      }
                    });
        if (TEST_ANIMATIONS) {
          springAnimationDeselectSpatial.setSpring(
              new SpringForce().setStiffness(30f).setDampingRatio(0.9f)
          );
        }
      }
      if (springAnimationDeselectEffects == null) {
        springAnimationDeselectEffects =
            new SpringAnimation(this, DESELECT_EFFECTS_FRACTION)
                .setSpring(
                    MotionUtils.resolveThemeSpringForce(
                        getContext(),
                        R.attr.motionSpringSlowEffects,
                        R.style.Motion_Material3_Spring_Standard_Slow_Effects)
                )
                .setMinimumVisibleChange(0.01f);
        if (TEST_ANIMATIONS) {
          springAnimationDeselectEffects.setSpring(
              new SpringForce().setStiffness(40f).setDampingRatio(1f)
          );
        }
      }
      setSelectAnimationStartState();

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
            startLeft += isDefaultSong ? (chipCloseIconWidth / 2) : 0;
            chipTargetTranslationX = endLeft - startLeft;
          }
          if (isDefaultSong) {
            springAnimationDeselectSpatial.animateToFinalPosition(0);
            springAnimationDeselectEffects.animateToFinalPosition(0);
          } else {
            springAnimationSelectSpatial.animateToFinalPosition(1);
            springAnimationSelectEffects.animateToFinalPosition(1);
          }
        });
      }
    } else {
      setSelectAnimationStartState();
      setSpatialSelectFraction(isDefaultSong ? 0 : 1);
      setEffectsSelectFraction(isDefaultSong ? 0 : 1);
      setSelectAnimationEndState();
    }
  }

  private void setSelectAnimationStartState() {
    boolean isDefaultSong = currentSongId.equals(Constants.SONG_ID_DEFAULT);

    binding.buttonSongPickerCollapse.setEnabled(isDefaultSong);
    binding.frameSongPickerTop.setClickable(isDefaultSong);

    binding.recyclerSongPicker.setVisibility(VISIBLE);
    setRecyclerClicksEnabled(isDefaultSong);
    if (isDefaultSong) {
      binding.recyclerSongPicker.setAlpha(0);
    } else {
      binding.textSongPickerChip.setText(getSongNameFromId(currentSongId));

      // Don't make container visible yet to prevent flashing bevor proper placing
      binding.constraintSongPickerChipContainer.setTranslationX(0);

      ViewGroup.LayoutParams closeIconParams = binding.imageSongPickerChipClose.getLayoutParams();
      closeIconParams.width = 0;
      binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
    }
    binding.frameSongPickerChipClose.setClickable(!isDefaultSong);
    binding.frameSongPickerChipTouchTarget.setClickable(!isDefaultSong);
    binding.cardSongPickerChip.setClickable(!isDefaultSong);
    // card seems to ignore clickable false, disable manually
    binding.cardSongPickerChip.setEnabled(!isDefaultSong);
    binding.buttonSongPickerPart.setClickable(!isDefaultSong);
    binding.buttonSongPickerPartPrevious.setClickable(!isDefaultSong);
    binding.buttonSongPickerPartNext.setClickable(!isDefaultSong);

    binding.buttonSongPickerAddSong.setVisibility(VISIBLE);
    binding.buttonSongPickerAddSong.setClickable(isDefaultSong);
  }

  private void setSelectAnimationEndState() {
    boolean isDefaultSong = currentSongId.equals(Constants.SONG_ID_DEFAULT);

    binding.recyclerSongPicker.setVisibility(isDefaultSong ? VISIBLE : INVISIBLE);
    binding.constraintSongPickerChipContainer.setVisibility(isDefaultSong ? INVISIBLE : VISIBLE);

    binding.buttonSongPickerAddSong.setVisibility(isDefaultSong ? VISIBLE : GONE);
  }

  private void setSpatialSelectFraction(float fraction) {
    selectSpatialFraction = fraction;

    binding.buttonSongPickerCollapse.setRotation(90 * fraction);

    binding.constraintSongPickerChipContainer.setTranslationX(
        (1 - fraction) * chipTargetTranslationX
    );
    if (binding.constraintSongPickerChipContainer.getVisibility() == INVISIBLE) {
      binding.constraintSongPickerChipContainer.setVisibility(VISIBLE);
    }

    ViewGroup.LayoutParams closeIconParams = binding.imageSongPickerChipClose.getLayoutParams();
    closeIconParams.width = (int) (chipCloseIconWidth * fraction);
    binding.imageSongPickerChipClose.setLayoutParams(closeIconParams);
  }

  private float getSpatialSelectFraction() {
    return selectSpatialFraction;
  }

  private void setEffectsSelectFraction(float fraction) {
    selectEffectsFraction = fraction;

    binding.imageSongPickerChipClose.setAlpha(fraction);

    int colorBg = ColorUtils.blendARGB(colorSurfaceBright, colorTertiaryContainer, fraction);
    int colorText = ColorUtils.blendARGB(colorOnSurface, colorOnTertiaryContainer, fraction);
    int colorIcon = ColorUtils.blendARGB(colorPrimary, colorOnTertiaryContainer, fraction);
    boolean isDark = UiUtil.isDarkModeActive(context);
    int recyclerColorStroke = ResUtil.getColor(
        context, isDark ? R.attr.colorSurfaceBright : R.attr.colorOutlineVariant
    );
    int colorStroke = ColorUtils.blendARGB(recyclerColorStroke, colorOnTertiaryContainer, fraction);
    binding.cardSongPickerChip.setCardBackgroundColor(colorBg);
    binding.cardSongPickerChip.setStrokeColor(colorStroke);
    binding.textSongPickerChip.setTextColor(colorText);
    binding.imageSongPickerChipIcon.setColorFilter(colorIcon);
    binding.imageSongPickerChipClose.setColorFilter(colorIcon);

    gradientLeft.setLevel((int) (10000 * fraction));
    gradientRight.setLevel((int) (10000 * fraction));

    binding.buttonSongPickerCollapse.setIconTint(ColorStateList.valueOf(
        ColorUtils.blendARGB(colorOnSurfaceVariant, colorOnSurface, fraction)
    ));
    binding.buttonSongPickerCollapse.setAlpha(1 + (0.38f - 1) * fraction);
    binding.recyclerSongPicker.setAlpha(1 - fraction);
    binding.textSongPickerEmpty.setAlpha(1 - fraction);
    binding.buttonSongPickerAddSong.setAlpha(1 - fraction);

    binding.buttonSongPickerPart.setAlpha(fraction);
    binding.buttonSongPickerPartPrevious.setAlpha(fraction);
    binding.buttonSongPickerPartNext.setAlpha(fraction);
  }

  private float getEffectsSelectFraction() {
    return selectEffectsFraction;
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
    if (binding.recyclerSongPicker.getItemDecorationCount() == 0) {
      binding.recyclerSongPicker.addItemDecoration(decoration);
    }
    binding.recyclerSongPicker.getViewTreeObserver().addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            binding.recyclerSongPicker.invalidateItemDecorations();
            int itemCount = songsWithParts.size();
            if (itemCount > 0) {
              int totalWidth = 0;
              for (int i = 0; i < itemCount; i++) {
                View child = binding.recyclerSongPicker.getChildAt(i);
                if (child != null) {
                  // adapter item count sometimes leads to index out of bounds of recyclerview
                  // but child count sometimes leads to wrong number of children which leads to
                  // an animation to the wrong place, so simply ignore the index out of bounds
                  totalWidth += child.getWidth();
                }
              }
              totalWidth += innerPadding * 2 * (itemCount - 1);
              totalWidth += outerPadding * 2;
              boolean shouldCenter = totalWidth < widthMax;
              if (shouldCenter) {
                int padding = (widthMax - totalWidth) / 2;
                binding.recyclerSongPicker.setPadding(
                    isRtl ? 0 : padding, 0,
                    isRtl ? padding : 0, 0
                );
                binding.recyclerSongPicker.setOverScrollMode(View.OVER_SCROLL_NEVER);
              } else {
                binding.recyclerSongPicker.setPadding(0, 0, 0, 0);
                binding.recyclerSongPicker.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
              }
            }
            binding.recyclerSongPicker.getViewTreeObserver().removeOnGlobalLayoutListener(
                this
            );
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

  private int getPartCount() {
    for (SongWithParts songWithParts : songsWithParts) {
      if (songWithParts.getSong().getId().equals(currentSongId)) {
        return songWithParts.getParts().size();
      }
    }
    return 0;
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

  public interface SongPickerListener {
    void onCurrentSongChanged(@NonNull String currentSongId);
    void onCurrentSongClicked();
    void onCurrentPartClicked();
    void onPreviousPartClicked();
    void onNextPartClicked();
    void onSongLongClicked(@NonNull String songId);
    void onExpandCollapseClicked(boolean expand);
    void onOpenSongsClicked();
    void onMenuOrMenuItemClicked();
    void onBackupClicked();
    void onSortOrderChanged(int sortOrder);
    void onAddSongClicked();
    void onHeightChanged();
    void onExpandChanged(boolean expanded);
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
  private static final FloatPropertyCompat<SongPickerView> SELECT_SPATIAL_FRACTION =
      new FloatPropertyCompat<>("selectSpatialFraction") {
        @Override
        public float getValue(SongPickerView delegate) {
          return delegate.getSpatialSelectFraction();
        }

        @Override
        public void setValue(SongPickerView delegate, float value) {
          delegate.setSpatialSelectFraction(value);
        }
      };
  private static final FloatPropertyCompat<SongPickerView> SELECT_EFFECTS_FRACTION =
      new FloatPropertyCompat<>("selectEffectsFraction") {
        @Override
        public float getValue(SongPickerView delegate) {
          return delegate.getEffectsSelectFraction();
        }

        @Override
        public void setValue(SongPickerView delegate, float value) {
          delegate.setEffectsSelectFraction(value);
        }
      };
  private static final FloatPropertyCompat<SongPickerView> DESELECT_SPATIAL_FRACTION =
      new FloatPropertyCompat<>("deselectSpatialFraction") {
        @Override
        public float getValue(SongPickerView delegate) {
          return delegate.getSpatialSelectFraction();
        }

        @Override
        public void setValue(SongPickerView delegate, float value) {
          delegate.setSpatialSelectFraction(value);
        }
      };
  private static final FloatPropertyCompat<SongPickerView> DESELECT_EFFECTS_FRACTION =
      new FloatPropertyCompat<>("deselectEffectsFraction") {
        @Override
        public float getValue(SongPickerView delegate) {
          return delegate.getEffectsSelectFraction();
        }

        @Override
        public void setValue(SongPickerView delegate, float value) {
          delegate.setEffectsSelectFraction(value);
        }
      };
}
