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
 * Copyright (c) 2020-2024 by Patrick Zedler
 */

package xyz.zedler.patrick.tack.util;

import java.util.LinkedList;
import java.util.Queue;

public class TempoTapUtil {

  private static final String TAG = TempoTapUtil.class.getSimpleName();
  private static final int MAX_TAPS = 20;
  private static final double TEMPO_FACTOR = 0.5;
  private static final int INTERVAL_FACTOR = 3;

  private final Queue<Long> intervals = new LinkedList<>();
  private long previous;

  public boolean tap() {
    boolean enoughData = false;
    long current = System.currentTimeMillis();
    if (previous > 0) {
      enoughData = true;
      long interval = current - previous;
      if (!intervals.isEmpty() && shouldReset(interval)) {
        intervals.clear();
        enoughData = false;
      } else if (intervals.size() >= MAX_TAPS) {
        intervals.poll();
      }
      intervals.offer(interval);
    }
    previous = current;
    return enoughData;
  }

  public int getTempo() {
    return getTempo(getAverage());
  }

  private int getTempo(long interval) {
    if (interval > 0) {
      return (int) (60000 / interval);
    } else {
      return 0;
    }
  }

  private long getAverage() {
    long sum = 0;
    for (long interval : intervals) {
      sum += interval;
    }
    if (intervals.size() > 0) {
      return (long) sum / intervals.size();
    } else {
      return 0;
    }
  }

  private boolean shouldReset(long interval) {
    return getTempo(interval) >= getTempo() * (1 + TEMPO_FACTOR)
        || getTempo(interval) <= getTempo() * (1 - TEMPO_FACTOR)
        || interval > getAverage() * INTERVAL_FACTOR;
  }
}