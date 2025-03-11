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

package xyz.zedler.patrick.tack.database.entity;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Arrays;
import java.util.Locale;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.model.MetronomeConfig;
import xyz.zedler.patrick.tack.util.MetronomeUtil;

@Entity(
    tableName = "parts",
    foreignKeys = @ForeignKey(
        entity = Song.class,
        parentColumns = "name",
        childColumns = "song_name",
        onDelete = ForeignKey.CASCADE),
    indices = {@Index("song_name")}
)
public class Part {

  @PrimaryKey(autoGenerate = true)
  private int id;

  @Nullable
  private String name;

  @ColumnInfo(name = "song_name")
  @NonNull
  private String songName;

  // count in
  private int countIn;
  // tempo
  private int tempo;
  // beats
  private String beats, subdivisions;
  // incremental tempo change
  private int incrementalAmount, incrementalInterval, incrementalLimit;
  private String incrementalUnit;
  private boolean incrementalIncrease;
  // duration
  private int timerDuration;
  private String timerUnit;
  // muted beats
  private int mutePlay, muteMute;
  private String muteUnit;
  private boolean muteRandom;

  public Part(
      int id,
      @Nullable String name, @NonNull String songName,
      int countIn, int tempo,
      String beats, String subdivisions,
      int incrementalAmount, int incrementalInterval, int incrementalLimit,
      String incrementalUnit, boolean incrementalIncrease,
      int timerDuration, String timerUnit,
      int mutePlay, int muteMute, String muteUnit, boolean muteRandom
  ) {
    this.id = id;
    this.name = name;
    this.songName = songName;

    this.countIn = countIn;

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

  @Ignore
  public Part(@Nullable String name, @NonNull String songName, @NonNull MetronomeConfig config) {
    this.name = name;
    this.songName = songName;

    setConfig(config);
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  @NonNull
  public String getSongName() {
    return songName;
  }

  public void setSongName(@NonNull String songName) {
    this.songName = songName;
  }

  public int getCountIn() {
    return countIn;
  }

  public void setCountIn(int countIn) {
    this.countIn = countIn;
  }

  public int getTempo() {
    return tempo;
  }

  public void setTempo(int tempo) {
    this.tempo = tempo;
  }

  public String getBeats() {
    return beats;
  }

  public void setBeats(String beats) {
    this.beats = beats;
  }

  public int getBeatsCount() {
    return beats.split(",").length;
  }

  public String getSubdivisions() {
    return subdivisions;
  }

  public void setSubdivisions(String subdivisions) {
    this.subdivisions = subdivisions;
  }

  public int getIncrementalAmount() {
    return incrementalAmount;
  }

  public void setIncrementalAmount(int incrementalAmount) {
    this.incrementalAmount = incrementalAmount;
  }

  public int getIncrementalInterval() {
    return incrementalInterval;
  }

  public void setIncrementalInterval(int incrementalInterval) {
    this.incrementalInterval = incrementalInterval;
  }

  public int getIncrementalLimit() {
    return incrementalLimit;
  }

  public void setIncrementalLimit(int incrementalLimit) {
    this.incrementalLimit = incrementalLimit;
  }

  public String getIncrementalUnit() {
    return incrementalUnit;
  }

  public void setIncrementalUnit(String incrementalUnit) {
    this.incrementalUnit = incrementalUnit;
  }

  public boolean isIncrementalIncrease() {
    return incrementalIncrease;
  }

  public void setIncrementalIncrease(boolean incrementalIncrease) {
    this.incrementalIncrease = incrementalIncrease;
  }

  public int getTimerDuration() {
    return timerDuration;
  }

  public void setTimerDuration(int timerDuration) {
    this.timerDuration = timerDuration;
  }

  public String getTimerUnit() {
    return timerUnit;
  }

  public void setTimerUnit(String timerUnit) {
    this.timerUnit = timerUnit;
  }

  public String getTimerDurationString(@NonNull Context context) {
    if (timerDuration == 0) {
      return context.getString(R.string.label_part_no_duration);
    }
    switch (timerUnit) {
      case UNIT.SECONDS:
      case UNIT.MINUTES:
        int seconds = timerDuration;
        if (timerUnit.equals(UNIT.MINUTES)) {
          seconds *= 60;
        }
        return MetronomeUtil.getTimeStringFromSeconds(seconds, false);
      default:
        return context.getResources().getQuantityString(
            R.plurals.options_unit_bars, timerDuration, timerDuration
        );
    }
  }

  public int getMutePlay() {
    return mutePlay;
  }

  public void setMutePlay(int mutePlay) {
    this.mutePlay = mutePlay;
  }

  public int getMuteMute() {
    return muteMute;
  }

  public void setMuteMute(int muteMute) {
    this.muteMute = muteMute;
  }

  public String getMuteUnit() {
    return muteUnit;
  }

  public void setMuteUnit(String muteUnit) {
    this.muteUnit = muteUnit;
  }

  public boolean isMuteRandom() {
    return muteRandom;
  }

  public void setMuteRandom(boolean muteRandom) {
    this.muteRandom = muteRandom;
  }

  @NonNull
  public MetronomeConfig toConfig() {
    return new MetronomeConfig(
        countIn,
        tempo,
        beats.split(","), subdivisions.split(","),
        incrementalAmount, incrementalInterval, incrementalLimit,
        incrementalUnit, incrementalIncrease,
        timerDuration, timerUnit,
        mutePlay, muteMute, muteUnit, muteRandom
    );
  }

  public void setConfig(@NonNull MetronomeConfig config) {
    countIn = config.getCountIn();

    tempo = config.getTempo();

    beats = String.join(",", config.getBeats());
    subdivisions = String.join(",", config.getSubdivisions());

    incrementalAmount = config.getIncrementalAmount();
    incrementalInterval = config.getIncrementalInterval();
    incrementalLimit = config.getIncrementalLimit();
    incrementalUnit = config.getIncrementalUnit();
    incrementalIncrease = config.isIncrementalIncrease();

    timerDuration = config.getTimerDuration();
    timerUnit = config.getTimerUnit();

    mutePlay = config.getMutePlay();
    muteMute = config.getMuteMute();
    muteUnit = config.getMuteUnit();
    muteRandom = config.isMuteRandom();
  }

  @NonNull
  @Override
  public String toString() {
    return "Part{" +
        "name='" + name + '\'' +
        ", songName='" + songName + '\'' +
        ", tempo=" + tempo +
        ", beats='" + beats + '\'' +
        ", subdivisions='" + subdivisions + '\'' +
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
