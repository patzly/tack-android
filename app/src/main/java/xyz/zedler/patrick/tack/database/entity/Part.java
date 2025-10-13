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
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.R;
import xyz.zedler.patrick.tack.model.MetronomeConfig;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;

@Entity(
    tableName = "parts",
    foreignKeys = @ForeignKey(
        entity = Song.class,
        parentColumns = "id",
        childColumns = "songId",
        onDelete = ForeignKey.CASCADE),
    indices = {@Index("songId")}
)
public class Part implements Parcelable {

  @PrimaryKey
  @NonNull
  private String id;
  @Nullable
  private String name;
  @NonNull
  private String songId;
  private int partIndex;

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
      @NonNull String id, @Nullable String name, @NonNull String songId, int partIndex,
      int countIn, int tempo,
      String beats, String subdivisions,
      int incrementalAmount, int incrementalInterval, int incrementalLimit,
      String incrementalUnit, boolean incrementalIncrease,
      int timerDuration, String timerUnit,
      int mutePlay, int muteMute, String muteUnit, boolean muteRandom
  ) {
    this.id = id;
    this.name = name;
    this.songId = songId;
    this.partIndex = partIndex;

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
  public Part(
      @Nullable String name, @NonNull String songId, int partIndex, @NonNull MetronomeConfig config
  ) {
    this.id = UUID.randomUUID().toString();
    this.name = name;
    this.songId = songId;
    this.partIndex = partIndex;

    setConfig(config);
  }

  @Ignore
  public Part(@NonNull Part part) {
    this.id = part.id;
    this.name = part.name;
    this.songId = part.songId;
    this.partIndex = part.partIndex;

    this.countIn = part.countIn;

    this.tempo = part.tempo;

    this.beats = part.beats;
    this.subdivisions = part.subdivisions;

    this.incrementalAmount = part.incrementalAmount;
    this.incrementalInterval = part.incrementalInterval;
    this.incrementalLimit = part.incrementalLimit;
    this.incrementalUnit = part.incrementalUnit;
    this.incrementalIncrease = part.incrementalIncrease;

    this.timerDuration = part.timerDuration;
    this.timerUnit = part.timerUnit;

    this.mutePlay = part.mutePlay;
    this.muteMute = part.muteMute;
    this.muteUnit = part.muteUnit;
    this.muteRandom = part.muteRandom;
  }

  protected Part(Parcel in) {
    id = Objects.requireNonNull(in.readString());
    name = in.readString();
    songId = Objects.requireNonNull(in.readString());
    partIndex = in.readInt();
    countIn = in.readInt();
    tempo = in.readInt();
    beats = in.readString();
    subdivisions = in.readString();
    incrementalAmount = in.readInt();
    incrementalInterval = in.readInt();
    incrementalLimit = in.readInt();
    incrementalUnit = in.readString();
    incrementalIncrease = in.readByte() != 0;
    timerDuration = in.readInt();
    timerUnit = in.readString();
    mutePlay = in.readInt();
    muteMute = in.readInt();
    muteUnit = in.readString();
    muteRandom = in.readByte() != 0;
  }

  @NonNull
  public String getId() {
    return id;
  }

  public void setId(@NonNull String id) {
    this.id = id;
  }

  public void setRandomId() {
    this.id = UUID.randomUUID().toString();
  }

  @Nullable
  public String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  @NonNull
  public String getSongId() {
    return songId;
  }

  public void setSongId(@NonNull String songId) {
    this.songId = songId;
  }

  public int getPartIndex() {
    return partIndex;
  }

  public void setPartIndex(int partIndex) {
    this.partIndex = partIndex;
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
        return MetronomeEngine.getTimeStringFromSeconds(seconds, false);
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

  public boolean equalsConfig(@NonNull MetronomeConfig config) {
    return countIn == config.getCountIn()
        && tempo == config.getTempo()
        && Arrays.equals(beats.split(","), config.getBeats())
        && Arrays.equals(subdivisions.split(","), config.getSubdivisions())
        && incrementalAmount == config.getIncrementalAmount()
        && incrementalInterval == config.getIncrementalInterval()
        && incrementalLimit == config.getIncrementalLimit()
        && incrementalUnit.equals(config.getIncrementalUnit())
        && incrementalIncrease == config.isIncrementalIncrease()
        && timerDuration == config.getTimerDuration()
        && timerUnit.equals(config.getTimerUnit())
        && mutePlay == config.getMutePlay()
        && muteMute == config.getMuteMute()
        && muteUnit.equals(config.getMuteUnit())
        && muteRandom == config.isMuteRandom();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Part)) {
      return false;
    }
    Part part = (Part) o;
    return partIndex == part.partIndex && countIn == part.countIn && tempo == part.tempo
        && incrementalAmount == part.incrementalAmount
        && incrementalInterval == part.incrementalInterval
        && incrementalLimit == part.incrementalLimit
        && incrementalIncrease == part.incrementalIncrease && timerDuration == part.timerDuration
        && mutePlay == part.mutePlay && muteMute == part.muteMute && muteRandom == part.muteRandom
        && Objects.equals(id, part.id) && Objects.equals(name, part.name)
        && Objects.equals(songId, part.songId)
        && Objects.equals(beats, part.beats)
        && Objects.equals(subdivisions, part.subdivisions)
        && Objects.equals(incrementalUnit, part.incrementalUnit)
        && Objects.equals(timerUnit, part.timerUnit) && Objects.equals(muteUnit, part.muteUnit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, songId, partIndex, countIn, tempo, beats, subdivisions,
        incrementalAmount, incrementalInterval, incrementalLimit, incrementalUnit,
        incrementalIncrease, timerDuration, timerUnit, mutePlay, muteMute, muteUnit, muteRandom);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(@NonNull Parcel dest, int flags) {
    dest.writeString(id);
    dest.writeString(name);
    dest.writeString(songId);
    dest.writeInt(partIndex);
    dest.writeInt(countIn);
    dest.writeInt(tempo);
    dest.writeString(beats);
    dest.writeString(subdivisions);
    dest.writeInt(incrementalAmount);
    dest.writeInt(incrementalInterval);
    dest.writeInt(incrementalLimit);
    dest.writeString(incrementalUnit);
    dest.writeByte((byte) (incrementalIncrease ? 1 : 0));
    dest.writeInt(timerDuration);
    dest.writeString(timerUnit);
    dest.writeInt(mutePlay);
    dest.writeInt(muteMute);
    dest.writeString(muteUnit);
    dest.writeByte((byte) (muteRandom ? 1 : 0));
  }

  @NonNull
  @Override
  public String toString() {
    return "Part{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", songId='" + songId + '\'' +
        ", partIndex=" + partIndex +
        ", countIn=" + countIn +
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

  public static final Creator<Part> CREATOR = new Creator<>() {
    @Override
    public Part createFromParcel(Parcel in) {
      return new Part(in);
    }

    @Override
    public Part[] newArray(int size) {
      return new Part[size];
    }
  };
}
