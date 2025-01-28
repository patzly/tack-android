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

package xyz.zedler.patrick.tack.model;

import androidx.annotation.NonNull;
import java.util.Arrays;
import xyz.zedler.patrick.tack.Constants.UNIT;

public class Config {

  // tempo
  private final int tempo;
  // beats
  private final String[] beats, subdivisions;
  // incremental tempo change
  private final int incrementalAmount, incrementalInterval, incrementalLimit;
  private final String incrementalUnit;
  private final boolean incrementalIncrease;
  // duration
  private final int timerDuration;
  private final String timerUnit;
  // muted beats
  private final int mutePlay, muteMute;
  private final String muteUnit;
  private final boolean muteRandom;

  public Config(
      int tempo,
      String[] beats, String[] subdivisions,
      int incrementalAmount, int incrementalInterval, int incrementalLimit,
      String incrementalUnit, boolean incrementalIncrease,
      int timerDuration, String timerUnit,
      int mutePlay, int muteMute, String muteUnit, boolean muteRandom
  ) {
    this.tempo = tempo;

    this.beats = beats;
    this.subdivisions = subdivisions;

    this.incrementalAmount = incrementalAmount;
    this.incrementalInterval = incrementalInterval;
    this.incrementalLimit = incrementalLimit;
    this.incrementalUnit = incrementalUnit;
    this.incrementalIncrease = incrementalIncrease;

    this.timerDuration = timerDuration;
    this.timerUnit = timerUnit;

    this.mutePlay = mutePlay;
    this.muteMute = muteMute;
    this.muteUnit = muteUnit;
    this.muteRandom = muteRandom;
  }

  public Config(int tempo, String[] beats, String[] subdivisions) {
    this(
        tempo, beats, subdivisions,
        0, 1, 0, UNIT.BARS, true,
        0, UNIT.BARS,
        0, 1, UNIT.BARS, false
    );
  }

  public Config(
      int tempo, String[] beats, String[] subdivisions, int timerDuration, String timerUnit
  ) {
    this(
        tempo, beats, subdivisions,
        0, 1, 0, UNIT.BARS, true,
        timerDuration, timerUnit,
        0, 1, UNIT.BARS, false
    );
  }

  public int getTempo() {
    return tempo;
  }

  public String[] getBeats() {
    return beats;
  }

  public String[] getSubdivisions() {
    return subdivisions;
  }

  public int getIncrementalAmount() {
    return incrementalAmount;
  }

  public int getIncrementalInterval() {
    return incrementalInterval;
  }

  public int getIncrementalLimit() {
    return incrementalLimit;
  }

  public String getIncrementalUnit() {
    return incrementalUnit;
  }

  public boolean isIncrementalIncrease() {
    return incrementalIncrease;
  }

  public int getTimerDuration() {
    return timerDuration;
  }

  public String getTimerUnit() {
    return timerUnit;
  }

  public int getMutePlay() {
    return mutePlay;
  }

  public int getMuteMute() {
    return muteMute;
  }

  public String getMuteUnit() {
    return muteUnit;
  }

  public boolean isMuteRandom() {
    return muteRandom;
  }

  @NonNull
  @Override
  public String toString() {
    return "Config{" +
        "tempo=" + tempo +
        ", beats=" + Arrays.toString(beats) +
        ", subdivisions=" + Arrays.toString(subdivisions) +
        ", incrementalAmount=" + incrementalAmount +
        ", incrementalInterval=" + incrementalInterval +
        ", incrementalLimit=" + incrementalLimit +
        ", incrementalUnit='" + incrementalUnit + '\'' +
        ", incrementalIncrease=" + incrementalIncrease +
        ", timerDuration=" + timerDuration +
        ", timerUnit='" + timerUnit + '\'' +
        ", mutePlay=" + mutePlay +
        ", muteMute=" + muteMute +
        ", muteUnit='" + muteUnit + '\'' +
        ", muteRandom=" + muteRandom +
        '}';
  }
}
