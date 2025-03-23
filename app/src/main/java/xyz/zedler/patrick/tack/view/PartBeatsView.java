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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.util.ResUtil;
import xyz.zedler.patrick.tack.util.UiUtil;

public class PartBeatsView extends View {

  private int circleSize, circleSizeMuted;
  private int circleSpace;
  private int colorNormal, colorStrong, colorSub, colorMuted;
  private Paint paintSolid, paintOutline;
  private String[] beats = new String[]{};

  public PartBeatsView(Context context) {
    super(context);
    init(context);
  }

  public PartBeatsView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public void setBeats(String[] beats) {
    this.beats = beats;
    invalidate();
  }

  private void init(Context context) {
    circleSize = UiUtil.dpToPx(context, 10);
    circleSizeMuted = UiUtil.dpToPx(context, 6);
    circleSpace = UiUtil.dpToPx(context, 8);
    paintSolid = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintSolid.setStyle(Style.FILL);
    paintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintOutline.setStyle(Style.STROKE);
    paintOutline.setStrokeWidth(UiUtil.dpToPx(context, 2));

    colorNormal = ResUtil.getColor(context, R.attr.colorPrimary);
    if (BeatView.isColorRed(colorNormal)) {
      colorNormal = ResUtil.getColor(context, R.attr.colorTertiary);
    }
    colorStrong = ResUtil.getColor(context, R.attr.colorError);
    colorSub = ResUtil.getColor(context, R.attr.colorOnSurfaceVariant);
    colorMuted = ResUtil.getColor(context, R.attr.colorOutline);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    int measuredWidth;
    if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
      measuredWidth = widthSize;
    } else {
      measuredWidth = 0;
    }

    int measuredHeight;
    if (heightMode == MeasureSpec.EXACTLY) {
      measuredHeight = heightSize;
    } else {
      measuredHeight = circleSize + getPaddingTop() + getPaddingBottom();
      if (heightMode == MeasureSpec.AT_MOST) {
        measuredHeight = Math.min(measuredHeight, heightSize);
      }
    }

    setMeasuredDimension(measuredWidth, measuredHeight);
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    int availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();
    int diameter = Math.min(circleSize, availableHeight);
    float radius = diameter / 2f;

    float startX = getPaddingLeft() + radius;
    float centerY = getPaddingTop() + radius;
    float strokeWidth = paintOutline.getStrokeWidth() / 2;
    for (String beat : beats) {
      adjustPaint(beat);
      float radiusFinal = radius;
      if (beat.equals("muted")) {
        radiusFinal = circleSizeMuted / 2f;
      }
      canvas.drawCircle(startX, centerY, radiusFinal - strokeWidth, paintSolid);
      canvas.drawCircle(startX, centerY, radiusFinal - strokeWidth, paintOutline);
      startX += circleSize + circleSpace;
    }
  }

  private void adjustPaint(String beat) {
    switch (beat) {
      case "normal":
        paintSolid.setColor(colorNormal);
        paintSolid.setAlpha((int) (0.3 * 255));
        paintOutline.setColor(colorNormal);
        break;
      case "strong":
        paintSolid.setColor(colorStrong);
        paintSolid.setAlpha(255);
        paintOutline.setColor(colorStrong);
        break;
      case "sub":
        paintSolid.setAlpha(0);
        paintOutline.setColor(colorSub);
        break;
      case "muted":
        paintSolid.setColor(colorMuted);
        paintSolid.setAlpha(255);
        paintOutline.setColor(colorMuted);
        break;
    }
  }
}
