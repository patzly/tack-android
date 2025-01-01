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

package com.google.android.material.slider;

import static android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS;
import static android.view.accessibility.AccessibilityManager.FLAG_CONTENT_TEXT;
import static com.google.android.material.slider.LabelFormatter.LABEL_FLOATING;
import static com.google.android.material.slider.LabelFormatter.LABEL_GONE;
import static com.google.android.material.slider.LabelFormatter.LABEL_VISIBLE;
import static com.google.android.material.slider.LabelFormatter.LABEL_WITHIN_BOUNDS;
import static com.google.android.material.theme.overlay.MaterialThemeOverlay.wrap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.internal.DescendantOffsetUtils;
import com.google.android.material.internal.ViewOverlayImpl;
import com.google.android.material.internal.ViewUtils;
import com.google.android.material.motion.MotionUtils;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.tooltip.TooltipDrawable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import xyz.zedler.patrick.tack.R;

/**
 * A widget that allows picking a value within a given range by sliding a thumb along a horizontal
 * line. In comparison to {@link Slider}, this slider animates its thumb and value changes.
 *
 * <p>{@inheritDoc}
 *
 * <p>{@code android:value}: <b>Optional.</b> The initial value of the slider. If not specified, the
 * slider's minimum value {@code android:valueFrom} is used.
 *
 * @attr ref com.google.android.material.R.styleable#SingleSlider_android_value
 */
@SuppressLint("RestrictedApi")
public class AnimatedSlider extends Slider {

  private static final String TAG = BaseSlider.class.getSimpleName();
  private static final String EXCEPTION_ILLEGAL_VALUE =
      "Slider value(%s) must be greater or equal to valueFrom(%s), and lower or equal to"
          + " valueTo(%s)";
  private static final String EXCEPTION_ILLEGAL_DISCRETE_VALUE =
      "Value(%s) must be equal to valueFrom(%s) plus a multiple of stepSize(%s) when using"
          + " stepSize(%s)";
  private static final String EXCEPTION_ILLEGAL_VALUE_FROM =
      "valueFrom(%s) must be smaller than valueTo(%s)";
  private static final String EXCEPTION_ILLEGAL_VALUE_TO =
      "valueTo(%s) must be greater than valueFrom(%s)";
  private static final String EXCEPTION_ILLEGAL_STEP_SIZE =
      "The stepSize(%s) must be 0, or a factor of the valueFrom(%s)-valueTo(%s) range";
  private static final String EXCEPTION_ILLEGAL_MIN_SEPARATION =
      "minSeparation(%s) must be greater or equal to 0";
  private static final String EXCEPTION_ILLEGAL_MIN_SEPARATION_STEP_SIZE_UNIT =
      "minSeparation(%s) cannot be set as a dimension when using stepSize(%s)";
  private static final String EXCEPTION_ILLEGAL_MIN_SEPARATION_STEP_SIZE =
      "minSeparation(%s) must be greater or equal and a multiple of stepSize(%s) when using"
          + " stepSize(%s)";
  private static final String WARNING_FLOATING_POINT_ERROR =
      "Floating point value used for %s(%s). Using floats can have rounding errors which may"
          + " result in incorrect values. Instead, consider using integers with a custom"
          + " LabelFormatter to display the value correctly.";
  private static final String WARNING_PARSE_ERROR =
      "Error parsing value(%s), valueFrom(%s), and valueTo(%s) into a float.";

  private static final int TIMEOUT_SEND_ACCESSIBILITY_EVENT = 200;
  private static final int MIN_TIMEOUT_TOOLTIP_WITH_ACCESSIBILITY = 10000;
  private static final int MAX_TIMEOUT_TOOLTIP_WITH_ACCESSIBILITY = 120000;
  private static final int HALO_ALPHA = 63;
  private static final double THRESHOLD = .0001;
  private static final float THUMB_WIDTH_PRESSED_RATIO = .5f;
  private static final int TRACK_CORNER_SIZE_UNSET = -1;

  static final int DEF_STYLE_RES = R.style.Widget_MaterialComponents_Slider;
  static final int UNIT_VALUE = 1;
  static final int UNIT_PX = 0;

  private static final int DEFAULT_LABEL_ANIMATION_ENTER_DURATION = 83;
  private static final int DEFAULT_LABEL_ANIMATION_EXIT_DURATION = 117;
  private static final int LABEL_ANIMATION_ENTER_DURATION_ATTR = R.attr.motionDurationMedium4;
  private static final int LABEL_ANIMATION_EXIT_DURATION_ATTR = R.attr.motionDurationShort3;
  private static final int LABEL_ANIMATION_ENTER_EASING_ATTR =
      R.attr.motionEasingEmphasizedInterpolator;
  private static final int LABEL_ANIMATION_EXIT_EASING_ATTR =
      R.attr.motionEasingEmphasizedAccelerateInterpolator;

  @Dimension(unit = Dimension.DP)
  private static final int MIN_TOUCH_TARGET_DP = 48;

  // @NonNull private final Paint inactiveTrackPaint;
  // @NonNull private final Paint activeTrackPaint;
  // @NonNull private final Paint thumbPaint;
  // @NonNull private final Paint haloPaint;
  // @NonNull private final Paint inactiveTicksPaint;
  // @NonNull private final Paint activeTicksPaint;
  // @NonNull private final Paint stopIndicatorPaint;
  private final AccessibilityManager accessibilityManager;

  private int labelStyle;
  private final List<TooltipDrawable> labels = new ArrayList<>();

  // Whether the labels are showing or in the process of animating in.
  private boolean labelsAreAnimatedIn = false;
  private ValueAnimator labelsInAnimator;
  private ValueAnimator labelsOutAnimator;

  private final int scaledTouchSlop;

  private int minTrackSidePadding;
  private int defaultThumbRadius;
  private int defaultTrackThickness;
  private int defaultTickActiveRadius;
  private int defaultTickInactiveRadius;
  private int minTickSpacing;

  @Px
  private int minTouchTargetSize;

  @Orientation private int widgetOrientation;
  private int minWidgetThickness;
  private int widgetThickness;
  private int labelBehavior;
  private int trackThickness;
  private int trackSidePadding;
  private int thumbWidth;
  private int thumbHeight;
  private int haloRadius;
  private int thumbTrackGapSize;
  private int defaultThumbWidth = -1;
  private int defaultThumbTrackGapSize = -1;
  private int trackStopIndicatorSize;
  private int trackCornerSize;
  private int trackInsideCornerSize;
  @Nullable private Drawable trackIconActiveStart;
  @Nullable private Drawable trackIconActiveEnd;
  @Nullable private ColorStateList trackIconActiveColor;
  @Nullable private Drawable trackIconInactiveStart;
  @Nullable private Drawable trackIconInactiveEnd;
  @Nullable private ColorStateList trackIconInactiveColor;
  @Px private int trackIconSize;
  private int labelPadding;
  private float touchDownX;
  private MotionEvent lastEvent;
  private LabelFormatter formatter;
  private boolean thumbIsPressed = false;
  // The index of the currently focused thumb.
  private float stepSize = 0.0f;
  private float[] ticksCoordinates;
  private boolean tickVisible = true;
  private int tickActiveRadius;
  private int tickInactiveRadius;
  private int trackWidth;
  private boolean forceDrawCompatHalo;
  private boolean isLongPress = false;
  private boolean dirtyConfig;

  @NonNull private ColorStateList haloColor;
  @NonNull private ColorStateList tickColorActive;
  @NonNull private ColorStateList tickColorInactive;
  @NonNull private ColorStateList trackColorActive;
  @NonNull private ColorStateList trackColorInactive;

  @NonNull private final Path trackPath = new Path();
  @NonNull private final RectF activeTrackRect = new RectF();
  @NonNull private final RectF inactiveTrackRect = new RectF();
  @NonNull private final RectF cornerRect = new RectF();
  @NonNull private final Rect labelRect = new Rect();
  @NonNull private final RectF iconRectF = new RectF();
  @NonNull private final Rect iconRect = new Rect();
  @NonNull private final Matrix rotationMatrix = new Matrix();
  @NonNull private final MaterialShapeDrawable defaultThumbDrawable = new MaterialShapeDrawable();
  @Nullable private Drawable customThumbDrawable;
  @NonNull private List<Drawable> customThumbDrawablesForValues = Collections.emptyList();

  private float touchPosition;
  @SeparationUnit private int separationUnit = UNIT_PX;

  private final int tooltipTimeoutMillis;

  @NonNull
  private final ViewTreeObserver.OnScrollChangedListener onScrollChangedListener =
      this::updateLabels;

  @NonNull
  private final ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = this::updateLabels;

  @NonNull
  private final Runnable resetActiveThumbIndex = () -> {
    setActiveThumbIndex(-1);
    invalidate();
  };

  private boolean thisAndAncestorsVisible;

  /**
   * Determines the behavior of the label which can be any of the following.
   *
   * <ul>
   *   <li>{@code LABEL_FLOATING}: The label will only be visible on interaction. It will float
   *       above the slider and may cover views above this one. This is the default and recommended
   *       behavior.
   *   <li>{@code LABEL_WITHIN_BOUNDS}: The label will only be visible on interaction. The label
   *       will always be drawn within the bounds of this view. This means extra space will be
   *       visible above the slider when the label is not visible.
   *   <li>{@code LABEL_GONE}: The label will never be drawn.
   *   <li>{@code LABEL_VISIBLE}: The label will never be hidden.
   * </ul>
   */
  @IntDef({LABEL_FLOATING, LABEL_WITHIN_BOUNDS, LABEL_GONE, LABEL_VISIBLE})
  @Retention(RetentionPolicy.SOURCE)
  @interface LabelBehavior {}

  @IntDef({UNIT_PX, UNIT_VALUE})
  @Retention(RetentionPolicy.SOURCE)
  @interface SeparationUnit {}

  public AnimatedSlider(@NonNull Context context) {
    this(context, null);
  }

  public AnimatedSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.sliderStyle);
  }

  public AnimatedSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    // Ensure we are using the correctly themed context rather than the context that was passed in.
    context = getContext();

    // Initialize with just this view's visibility.
    thisAndAncestorsVisible = isShown();

    /*inactiveTrackPaint = new Paint();
    activeTrackPaint = new Paint();

    thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    thumbPaint.setStyle(Style.FILL);
    thumbPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));

    haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    haloPaint.setStyle(Style.FILL);

    inactiveTicksPaint = new Paint();
    inactiveTicksPaint.setStyle(Style.STROKE);
    inactiveTicksPaint.setStrokeCap(Cap.ROUND);

    activeTicksPaint = new Paint();
    activeTicksPaint.setStyle(Style.STROKE);
    activeTicksPaint.setStrokeCap(Cap.ROUND);

    stopIndicatorPaint = new Paint();
    stopIndicatorPaint.setStyle(Style.FILL);
    stopIndicatorPaint.setStrokeCap(Cap.ROUND);

    loadResources(context.getResources());
    processAttributes(context, attrs, defStyleAttr);*/

    setFocusable(true);
    setClickable(true);

    // Set up the thumb drawable to always show the compat shadow.
    defaultThumbDrawable.setShadowCompatibilityMode(
        MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS);

    scaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    accessibilityManager =
        (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
    if (VERSION.SDK_INT >= VERSION_CODES.Q) {
      tooltipTimeoutMillis = accessibilityManager.getRecommendedTimeoutMillis(
          MIN_TIMEOUT_TOOLTIP_WITH_ACCESSIBILITY,
          FLAG_CONTENT_CONTROLS | FLAG_CONTENT_TEXT
      );
    } else {
      tooltipTimeoutMillis = MAX_TIMEOUT_TOOLTIP_WITH_ACCESSIBILITY;
    }

    dirtyConfig = true;
  }

  // private void loadResources(@NonNull Resources resources)

  // private void processAttributes(Context context, AttributeSet attrs, int defStyleAttr)

  // private boolean maybeIncreaseTrackSidePadding()

  // private void validateValueFrom()

  // private void validateValueTo()

  // private boolean valueLandsOnTick(float value)

  // private boolean isMultipleOfStepSize(double value)

  // private void validateStepSize()

  // private void validateValues()

  // private void validateMinSeparation()

  // private void warnAboutFloatingPointError()

  private void validateConfigurationIfDirty() {
    try {
      Method method = BaseSlider.class.getDeclaredMethod("validateConfigurationIfDirty");
      method.setAccessible(true);
      method.invoke(this);
      dirtyConfig = false;
    } catch (Exception e) {
      Log.e(TAG, "validateConfigurationIfDirty: ", e);
    }
  }

  // public void scheduleTooltipTimeout()

  // public float getValueFrom()

  @Override
  public void setValueFrom(float valueFrom) {
    dirtyConfig = true;
    super.setValueFrom(valueFrom);
  }

  // public float getValueTo()

  @Override
  public void setValueTo(float valueTo) {
    dirtyConfig = true;
    super.setValueTo(valueTo);
  }

  // List<Float> getValues()

  @Override
  void setValues(@NonNull Float... values) {
    super.setValues(values);
    createLabelPool();
  }

  @Override
  void setValues(@NonNull List<Float> values) {
    super.setValues(values);
    createLabelPool();
  }

  // private void setValuesInternal(@NonNull ArrayList<Float> values)

  private void createLabelPool() {
    if (labels == null) {
      return;
    }

    // If there are too many labels, remove the extra ones from the end.
    if (labels.size() > getValues().size()) {
      List<TooltipDrawable> tooltipDrawables = labels.subList(getValues().size(), labels.size());
      for (TooltipDrawable label : tooltipDrawables) {
        if (isAttachedToWindow()) {
          detachLabelFromContentView(label);
        }
      }
      tooltipDrawables.clear();
    }

    // If there's not enough labels, add more.
    while (labels.size() < getValues().size()) {
      // Because there's currently no way to copy the TooltipDrawable we use this to make more
      // if more thumbs are added.
      TooltipDrawable tooltipDrawable =
          TooltipDrawable.createFromAttributes(getContext(), null, 0, labelStyle);
      labels.add(tooltipDrawable);
      if (isAttachedToWindow()) {
        attachLabelToContentView(tooltipDrawable);
      }
    }

    // Add a stroke if there is more than one label for when they overlap.
    int strokeWidth = labels.size() == 1 ? 0 : 1;
    for (TooltipDrawable label : labels) {
      label.setStrokeWidth(strokeWidth);
    }
  }

  // public float getStepSize()

  @Override
  public void setStepSize(float stepSize) {
    dirtyConfig = true;
    super.setStepSize(stepSize);
  }

  // void setCustomThumbDrawable(@DrawableRes int drawableResId)

  // void setCustomThumbDrawable(@NonNull Drawable drawable)

  // void setCustomThumbDrawablesForValues(@NonNull @DrawableRes int... customThumbDrawableResIds)

  // void setCustomThumbDrawablesForValues(@NonNull Drawable... customThumbDrawables)

  // private Drawable initializeCustomThumbDrawable(Drawable originalDrawable)

  // private void adjustCustomThumbDrawableBounds(Drawable drawable)

  // public int getFocusedThumbIndex()

  // public void setFocusedThumbIndex(int index)

  // protected void setActiveThumbIndex(int index)

  // public int getActiveThumbIndex()

  // public void addOnChangeListener(@NonNull L listener)

  // public void removeOnChangeListener(@NonNull L listener)

  // public void clearOnChangeListeners()

  // public void addOnSliderTouchListener(@NonNull T listener)

  // public void removeOnSliderTouchListener(@NonNull T listener)

  // public void clearOnSliderTouchListeners()

  // public boolean hasLabelFormatter()

  // public void setLabelFormatter(@Nullable LabelFormatter formatter)

  // public float getThumbElevation()

  // public void setThumbElevation(float elevation)

  // public void setThumbElevationResource(@DimenRes int elevation)

  // public int getThumbRadius()

  // public void setThumbRadius(@IntRange(from = 0) @Px int radius)

  // public void setThumbRadiusResource(@DimenRes int radius)

  // public int getThumbWidth()

  // public void setThumbWidth(@IntRange(from = 0) @Px int width)

  // public void setThumbWidthResource(@DimenRes int width)

  // public int getThumbHeight()

  // public void setThumbHeight(@IntRange(from = 0) @Px int height)

  // public void setThumbHeightResource(@DimenRes int height)

  // public void setThumbStrokeColor(@Nullable ColorStateList thumbStrokeColor)

  // public void setThumbStrokeColorResource(@ColorRes int thumbStrokeColorResourceId)

  // public ColorStateList getThumbStrokeColor()

  // public void setThumbStrokeWidth(float thumbStrokeWidth)

  // public void setThumbStrokeWidthResource(@DimenRes int thumbStrokeWidthResourceId)

  // public float getThumbStrokeWidth()

  // public int getHaloRadius()

  // public void setHaloRadius(@IntRange(from = 0) @Px int radius)

  // public void setHaloRadiusResource(@DimenRes int radius)

  // public int getLabelBehavior()

  // public void setLabelBehavior(@LabelBehavior int labelBehavior)

  /**
   * Returns whether the labels should be always shown based on the {@link LabelBehavior}.
   *
   * @see LabelBehavior
   * @attr ref com.google.android.material.R.styleable#Slider_labelBehavior
   */
  private boolean shouldAlwaysShowLabel() {
    return this.labelBehavior == LABEL_VISIBLE;
  }

  // public int getTrackSidePadding()

  // public int getTrackWidth()

  // public int getTrackHeight()

  // public void setTrackHeight(@IntRange(from = 0) @Px int trackHeight)

  // public int getTickActiveRadius()

  // public void setTickActiveRadius(@IntRange(from = 0) @Px int tickActiveRadius)

  // public int getTickInactiveRadius()

  // public void setTickInactiveRadius(@IntRange(from = 0) @Px int tickInactiveRadius)

  // private void updateWidgetLayout(boolean forceRefresh)

  // private boolean maybeIncreaseWidgetThickness()

  // private void updateRotationMatrix()

  // public ColorStateList getHaloTintList()

  // public void setHaloTintList(@NonNull ColorStateList haloColor)

  // public ColorStateList getThumbTintList()

  // public void setThumbTintList(@NonNull ColorStateList thumbColor)

  // public ColorStateList getTickTintList()

  // public void setTickTintList(@NonNull ColorStateList tickColor)

  // public ColorStateList getTickActiveTintList()

  // public void setTickActiveTintList(@NonNull ColorStateList tickColor)

  // public ColorStateList getTickInactiveTintList()

  // public void setTickInactiveTintList(@NonNull ColorStateList tickColor)

  // public boolean isTickVisible()

  // public void setTickVisible(boolean tickVisible)

  // public ColorStateList getTrackTintList()

  // public void setTrackTintList(@NonNull ColorStateList trackColor)

  // public ColorStateList getTrackActiveTintList()

  // public void setTrackActiveTintList(@NonNull ColorStateList trackColor)

  // public ColorStateList getTrackInactiveTintList()

  // public void setTrackInactiveTintList(@NonNull ColorStateList trackColor)

  // public int getThumbTrackGapSize()

  // public void setThumbTrackGapSize(@Px int thumbTrackGapSize)

  // public int getTrackStopIndicatorSize()

  // public void setTrackStopIndicatorSize(@Px int trackStopIndicatorSize)

  // public int getTrackCornerSize()

  // public void setTrackCornerSize(@Px int cornerSize)

  // public int getTrackInsideCornerSize()

  // public void setTrackInsideCornerSize(@Px int cornerSize)

  // public void setTrackIconActiveStart(@Nullable Drawable icon)

  // public void setTrackIconActiveStart(@DrawableRes int iconResourceId)

  // public Drawable getTrackIconActiveStart()

  // public void setTrackIconActiveEnd(@Nullable Drawable icon)

  // public void setTrackIconActiveEnd(@DrawableRes int iconResourceId)

  // public Drawable getTrackIconActiveEnd()

  // public void setTrackIconSize(@Px int size)

  // public int getTrackIconSize()

  // public void setTrackIconActiveColor(@Nullable ColorStateList color)

  // public ColorStateList getTrackIconActiveColor()

  // public void setTrackIconInactiveStart(@Nullable Drawable icon)

  // public void setTrackIconInactiveStart(@DrawableRes int iconResourceId)

  // public Drawable getTrackIconInactiveStart()

  // public void setTrackIconInactiveEnd(@Nullable Drawable icon)

  // public void setTrackIconInactiveEnd(@DrawableRes int iconResourceId)

  // public Drawable getTrackIconInactiveEnd()

  // public void setTrackIconInactiveColor(@Nullable ColorStateList color)

  // public ColorStateList getTrackIconInactiveColor()

  // protected void onVisibilityChanged(@NonNull View changedView, int visibility)

  // public void setEnabled(boolean enabled)

  // public void setOrientation(@Orientation int orientation)

  // protected void onAttachedToWindow()

  private void attachLabelToContentView(TooltipDrawable label) {
    label.setRelativeToView(ViewUtils.getContentView(this));
  }

  // protected void onDetachedFromWindow()

  private void detachLabelFromContentView(TooltipDrawable label) {
    ViewOverlayImpl contentViewOverlay = ViewUtils.getContentViewOverlay(this);
    if (contentViewOverlay != null) {
      contentViewOverlay.remove(label);
      label.detachView(ViewUtils.getContentView(this));
    }
  }

  // protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)

  // protected void onSizeChanged(int w, int h, int oldw, int oldh)

  private void maybeCalculateTicksCoordinates() {
    /*if (getStepSize() <= 0.0f && continuousTicksCount == 0) {
      return;
    }
    validateConfigurationIfDirty();

    int tickCount = getTickCount();
    if (ticksCoordinates == null || ticksCoordinates.length != tickCount * 2) {
      ticksCoordinates = new float[tickCount * 2];
    }
    float interval = getTickInterval(tickCount);
    for (int i = 0; i < tickCount * 2; i += 2) {
      ticksCoordinates[i] = getTrackSidePadding() + i / 2f * interval;
      ticksCoordinates[i + 1] = calculateTrackCenter();
    }*/
  }

  // private void updateTrackWidth(int width)

  // private void updateHaloHotspot()

  private int calculateTrackCenter() {
    return widgetThickness / 2
        + (labelBehavior == LABEL_WITHIN_BOUNDS || shouldAlwaysShowLabel()
        ? labels.get(0).getIntrinsicHeight()
        : 0);
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    if (dirtyConfig) {
      validateConfigurationIfDirty();

      // Update the visible tick coordinates.
      maybeCalculateTicksCoordinates();
    }/*

    super.onDraw(canvas);

    int yCenter = calculateTrackCenter();

    float first = values.get(0);
    float last = values.get(values.size() - 1);
    if (last < valueTo || (values.size() > 1 && first > valueFrom)) {
      drawInactiveTrack(canvas, trackWidth, yCenter);
    }
    if (last > valueFrom) {
      drawActiveTrack(canvas, trackWidth, yCenter);
    }
    drawTrackIcons(canvas, activeTrackRect, inactiveTrackRect);

    maybeDrawTicks(canvas);
    maybeDrawStopIndicator(canvas, yCenter);

    if ((thumbIsPressed || isFocused()) && isEnabled()) {
      maybeDrawCompatHalo(canvas, trackWidth, yCenter);
    }*/

    updateLabels();

    // drawThumbs(canvas, trackWidth, yCenter);
  }

  // private float[] getActiveRange()

  // private void drawInactiveTrack(@NonNull Canvas canvas, int width, int yCenter)

  /**
   * Returns a number between 0 and 1 indicating where on the track this value should sit with 0
   * being on the far left, and 1 on the far right.
   */
  private float normalizeValue(float value) {
    float normalized = (value - getValueFrom()) / (getValueTo() - getValueFrom());
    if (isRtl() || isVertical()) {
      return 1 - normalized;
    }
    return normalized;
  }

  // private void drawActiveTrack(@NonNull Canvas canvas, int width, int yCenter)

  // private float calculateStartTrackCornerSize(float trackCornerSize)

  // private float calculateEndTrackCornerSize(float trackCornerSize)

  // private void drawTrackIcons(
  //      @NonNull Canvas canvas,
  //      @NonNull RectF activeTrackBounds,
  //      @NonNull RectF inactiveTrackBounds)

  // private void calculateBoundsAndDrawTrackIcon(
  //      @NonNull Canvas canvas,
  //      @NonNull RectF trackBounds,
  //      @Nullable Drawable icon,
  //      @Nullable ColorStateList iconColor,
  //      boolean isStart)

  // private void drawTrackIcon(
  //      @NonNull Canvas canvas,
  //      @NonNull RectF iconBounds,
  //      @NonNull Drawable icon,
  //      @Nullable ColorStateList color)

  // private void calculateBoundsAndDrawTrackIcon(
  //      @NonNull Canvas canvas,
  //      @NonNull RectF trackBounds,
  //      @Nullable Drawable icon,
  //      @Nullable ColorStateList iconColor,
  //      boolean isStart)

  // private void drawTrackIcon(
  //      @NonNull Canvas canvas,
  //      @NonNull RectF iconBounds,
  //      @NonNull Drawable icon,
  //      @Nullable ColorStateList color)

  // private void calculateTrackIconBounds(
  //      @NonNull RectF trackBounds, @NonNull RectF iconBounds, @Px int iconSize, boolean isStart)

  // private boolean hasGapBetweenThumbAndTrack()

  // The direction where the track has full corners.
  private enum FullCornerDirection {
    BOTH,
    LEFT,
    RIGHT,
    NONE
  }

  // private void updateTrack(
  //      Canvas canvas, Paint paint, RectF bounds, float cornerSize, FullCornerDirection direction)

  // private float[] getCornerRadii(float leftSide, float rightSide)

  // private void maybeDrawTicks(@NonNull Canvas canvas)

  // private void maybeDrawStopIndicator(@NonNull Canvas canvas, int yCenter)

  // private void drawStopIndicator(@NonNull Canvas canvas, float x, float y)

  // private void drawThumbs(@NonNull Canvas canvas, int width, int yCenter)

  // private void drawThumbDrawable(
  //      @NonNull Canvas canvas, int width, int top, float value, @NonNull Drawable thumbDrawable)

  // private void maybeDrawCompatHalo(@NonNull Canvas canvas, int width, int top)

  // private boolean shouldDrawCompatHalo()

  // public boolean onTouchEvent(@NonNull MotionEvent event)

  // private void updateThumbWidthWhenPressed()

  // private double snapPosition(float position)

  // protected boolean pickActiveThumb()

  private float getValueOfTouchPositionAbsolute() {
    float position = touchPosition;
    if (isRtl() || isVertical()) {
      position = 1 - position;
    }
    return (position * (getValueTo() - getValueFrom()) + getValueFrom());
  }

  // private boolean snapTouchPosition()

  // private boolean snapActiveThumbToValue(float value)

  // private boolean snapThumbToValue(int idx, float value)

  // private float getClampedValue(int idx, float value)

  // private float dimenToValue(float dimen)

  // protected void setSeparationUnit(int separationUnit)

  // protected float getMinSeparation()

  // private float getValueOfTouchPosition()

  private float valueToX(float value) {
    return normalizeValue(value) * trackWidth + trackSidePadding;
  }

  /**
   * A helper method to get the current animated value of a {@link ValueAnimator}. If the target
   * animator is null or not running, return the default value provided.
   */
  private static float getAnimatorCurrentValueOrDefault(
      ValueAnimator animator, float defaultValue) {
    // If the in animation is interrupting the out animation, attempt to smoothly interrupt by
    // getting the current value of the out animator.
    if (animator != null && animator.isRunning()) {
      float value = (float) animator.getAnimatedValue();
      animator.cancel();
      return value;
    }

    return defaultValue;
  }

  /**
   * Create an animator that shows or hides all slider labels.
   *
   * @param enter True if this animator should show (reveal) labels. False if this animator should
   *     hide labels.
   * @return A value animator that, when run, will animate all labels in or out using {@link
   *     TooltipDrawable#setRevealFraction(float)}.
   */
  private ValueAnimator createLabelAnimator(boolean enter) {
    float startFraction = enter ? 0F : 1F;
    // Update the start fraction to the current animated value of the label, if any.
    startFraction = getAnimatorCurrentValueOrDefault(
        enter ? labelsOutAnimator : labelsInAnimator, startFraction
    );
    float endFraction = enter ? 1F : 0F;
    ValueAnimator animator = ValueAnimator.ofFloat(startFraction, endFraction);
    int duration;
    TimeInterpolator interpolator;
    if (enter) {
      duration = MotionUtils.resolveThemeDuration(
          getContext(),
          LABEL_ANIMATION_ENTER_DURATION_ATTR,
          DEFAULT_LABEL_ANIMATION_ENTER_DURATION
      );
      interpolator = MotionUtils.resolveThemeInterpolator(
          getContext(),
          LABEL_ANIMATION_ENTER_EASING_ATTR,
          AnimationUtils.DECELERATE_INTERPOLATOR
      );
    } else {
      duration = MotionUtils.resolveThemeDuration(
          getContext(),
          LABEL_ANIMATION_EXIT_DURATION_ATTR,
          DEFAULT_LABEL_ANIMATION_EXIT_DURATION
      );
      interpolator = MotionUtils.resolveThemeInterpolator(
          getContext(),
          LABEL_ANIMATION_EXIT_EASING_ATTR,
          AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
      );
    }
    animator.setDuration(duration);
    animator.setInterpolator(interpolator);
    animator.addUpdateListener(animation -> {
      float fraction = (float) animation.getAnimatedValue();
      for (TooltipDrawable label : labels) {
        label.setRevealFraction(fraction);
      }
      // Ensure the labels are redrawn even if the slider has stopped moving
      postInvalidateOnAnimation();
    });
    return animator;
  }

  private void updateLabels() {
    switch (labelBehavior) {
      case LABEL_GONE:
        ensureLabelsRemoved();
        break;
      case LABEL_VISIBLE:
        if (isEnabled() && isSliderVisibleOnScreen()) {
          ensureLabelsAdded();
        } else {
          ensureLabelsRemoved();
        }
        break;
      case LABEL_FLOATING:
      case LABEL_WITHIN_BOUNDS:
        if (getActiveThumbIndex() != -1 && isEnabled()) {
          ensureLabelsAdded();
        } else {
          ensureLabelsRemoved();
        }
        break;
      default:
        throw new IllegalArgumentException("Unexpected labelBehavior: " + labelBehavior);
    }
  }

  private boolean isSliderVisibleOnScreen() {
    final Rect contentViewBounds = new Rect();
    ViewGroup contentView = ViewUtils.getContentView(this);
    if (contentView != null) {
      contentView.getHitRect(contentViewBounds);
    }
    return getLocalVisibleRect(contentViewBounds) && isThisAndAncestorsVisible();
  }

  private boolean isThisAndAncestorsVisible() {
    // onVisibilityAggregated is only available on N+ devices, so on pre-N devices we check if this
    // view and its ancestors are visible each time, in case one of the visibilities has changed.
    return (VERSION.SDK_INT >= VERSION_CODES.N) ? thisAndAncestorsVisible : isShown();
  }

  @Override
  public void onVisibilityAggregated(boolean isVisible) {
    super.onVisibilityAggregated(isVisible);
    this.thisAndAncestorsVisible = isVisible;
  }

  private void ensureLabelsRemoved() {
    // If the labels are animated in or in the process of animating in, create and start a new
    // animator to animate out the labels and remove them once the animation ends.
    if (labelsAreAnimatedIn) {
      labelsAreAnimatedIn = false;
      labelsOutAnimator = createLabelAnimator(false);
      labelsInAnimator = null;
      labelsOutAnimator.addListener(
          new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              super.onAnimationEnd(animation);
              ViewOverlayImpl contentViewOverlay = ViewUtils.getContentViewOverlay(
                  AnimatedSlider.this
              );
              for (TooltipDrawable label : labels) {
                if (contentViewOverlay != null) {
                  contentViewOverlay.remove(label);
                }
              }
            }
          });
      labelsOutAnimator.start();
    }
  }

  private void ensureLabelsAdded() {
    // If the labels are not animating in, start an animator to show them. ensureLabelsAdded will
    // be called multiple times by BaseSlider's draw method, making this check necessary to avoid
    // creating and starting an animator for each draw call.
    if (!labelsAreAnimatedIn) {
      labelsAreAnimatedIn = true;
      labelsInAnimator = createLabelAnimator(true);
      labelsOutAnimator = null;
      labelsInAnimator.start();
    }

    Iterator<TooltipDrawable> labelItr = labels.iterator();

    for (int i = 0; i < getValues().size() && labelItr.hasNext(); i++) {
      if (i == getFocusedThumbIndex()) {
        // We position the focused thumb last so it's displayed on top, so skip it for now.
        continue;
      }

      setValueForLabel(labelItr.next(), getValues().get(i));
    }

    if (!labelItr.hasNext()) {
      throw new IllegalStateException(
          String.format(
              "Not enough labels(%d) to display all the values(%d)",
              labels.size(), getValues().size()
          )
      );
    }

    // Now set the label for the focused thumb so it's on top.
    setValueForLabel(labelItr.next(), getValues().get(getFocusedThumbIndex()));
  }

  private String formatValue(float value) {
    if (hasLabelFormatter()) {
      try {
        Field formatterField = BaseSlider.class.getDeclaredField("formatter");
        formatterField.setAccessible(true);
        Object result = formatterField.get(this);
        if (result instanceof LabelFormatter) {
          return ((LabelFormatter) result).getFormattedValue(value);
        }
      } catch (Exception ignore) {}
    }
    return String.format((int) value == value ? "%.0f" : "%.2f", value);
  }

  private void setValueForLabel(TooltipDrawable label, float value) {
    label.setText(formatValue(value));
    positionLabel(label, value);
    ViewOverlayImpl contentViewOverlay = ViewUtils.getContentViewOverlay(this);
    if (contentViewOverlay != null) {
      contentViewOverlay.add(label);
    }
  }

  private void positionLabel(TooltipDrawable label, float value) {
    // Calculate the difference between the bounds of this view and the bounds of the root view to
    // correctly position this view in the overlay layer.
    calculateLabelBounds(label, value);
    if (isVertical()) {
      RectF labelBounds = new RectF(labelRect);
      rotationMatrix.mapRect(labelBounds);
      labelBounds.round(labelRect);
    }
    ViewGroup parent = ViewUtils.getContentView(this);
    if (parent != null) {
      DescendantOffsetUtils.offsetDescendantRect(parent, this, labelRect);
    }
    label.setBounds(labelRect);
  }

  private void calculateLabelBounds(TooltipDrawable label, float value) {
    int left;
    int right;
    int bottom;
    int top;
    if (isVertical() && !isRtl()) {
      left =
          trackSidePadding
              + (int) (normalizeValue(value) * trackWidth)
              - label.getIntrinsicHeight() / 2;
      right = left + label.getIntrinsicHeight();
      top = calculateTrackCenter() + (labelPadding + thumbHeight / 2);
      bottom = top + label.getIntrinsicWidth();
    } else {
      left =
          trackSidePadding
              + (int) (normalizeValue(value) * trackWidth)
              - label.getIntrinsicWidth() / 2;
      right = left + label.getIntrinsicWidth();
      bottom = calculateTrackCenter() - (labelPadding + thumbHeight / 2);
      top = bottom - label.getIntrinsicHeight();
    }
    labelRect.set(left, top, right, bottom);
  }

  // private void invalidateTrack()

  // private boolean isInVerticalScrollingContainer()

  // private static boolean isMouseEvent(MotionEvent event)

  // private boolean isPotentialVerticalScroll(MotionEvent event)

  // private void dispatchOnChangedProgrammatically()

  // private void dispatchOnChangedFromUser(int idx)

  // private void onStartTrackingTouch()

  // private void onStopTrackingTouch()

  // protected void drawableStateChanged()

  // private int getColorForState(@NonNull ColorStateList colorStateList)

  // void forceDrawCompatHalo(boolean force)

  // public boolean onKeyDown(int keyCode, @NonNull KeyEvent event)

  // private Boolean onKeyDownNoActiveThumb(int keyCode, @NonNull KeyEvent event)

  // public boolean onKeyUp(int keyCode, @NonNull KeyEvent event)

  // final boolean isRtl()

  // final boolean isVertical()

  // private boolean moveFocus(int direction)

  // private boolean moveFocusInAbsoluteDirection(int direction)

  // private Float calculateIncrementForKey(int keyCode)

  // private float calculateStepIncrement()

  // private float calculateStepIncrement(int stepFactor)

  // protected void onFocusChanged(
  //      boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect)

  // private void focusThumbOnFocusGained(int direction)

  // final int getAccessibilityFocusedVirtualViewId()

  // public CharSequence getAccessibilityClassName()

  // public boolean dispatchHoverEvent(@NonNull MotionEvent event)

  // public boolean dispatchKeyEvent(@NonNull KeyEvent event)

  // private void scheduleAccessibilityEventSender(int idx)

  // private class AccessibilityEventSender implements Runnable

  // protected Parcelable onSaveInstanceState()

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    super.onRestoreInstanceState(state);
    createLabelPool();
  }

  // static class SliderState extends BaseSavedState

  // void updateBoundsForVirtualViewId(int virtualViewId, Rect virtualViewBounds)

  // public static class AccessibilityHelper extends ExploreByTouchHelper
}