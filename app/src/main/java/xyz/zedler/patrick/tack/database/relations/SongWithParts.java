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

package xyz.zedler.patrick.tack.database.relations;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.tack.Constants;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.metronome.MetronomeEngine;

public class SongWithParts {

  @Embedded
  private Song song;

  @Relation(
      parentColumn = "id",
      entityColumn = "songId"
  )
  private List<Part> parts;

  public SongWithParts() {
    this.song = new Song();
    this.parts = new ArrayList<>();
  }

  @Ignore
  public SongWithParts(@NonNull Song song, @NonNull List<Part> parts) {
    this.song = song;
    this.parts = parts;
  }

  public Song getSong() {
    return song;
  }

  public void setSong(Song song) {
    this.song = song;
  }

  public List<Part> getParts() {
    return parts;
  }

  public void setParts(List<Part> parts) {
    this.parts = parts;
  }

  public String getDurationString() {
    float seconds = 0;
    for (Part part : parts) {
      switch (part.getTimerUnit()) {
        case UNIT.SECONDS:
          seconds += part.getTimerDuration();
          break;
        case UNIT.MINUTES:
          seconds += 60 * part.getTimerDuration();
          break;
        default: // Bars
          int incrementalAmount = part.getIncrementalAmount();
          if (incrementalAmount > 0) {
            // complex duration calculation with incremental tempo changes
            String incrementalUnit = part.getIncrementalUnit();
            int interval = part.getIncrementalInterval();
            if (incrementalUnit.equals(UNIT.BARS)) {
              int tempo = part.getTempo();
              for (int i = 0; i < part.getTimerDuration(); i++) {
                float factor = ((float) 60 / tempo) * part.getBeatsCount();
                seconds += factor * interval;
                if (i % interval == 0) {
                  int incrementalLimit = part.getIncrementalLimit();
                  if (part.isIncrementalIncrease()) {
                    int upperLimit = incrementalLimit != 0 ? incrementalLimit : Constants.TEMPO_MAX;
                    if (tempo + incrementalAmount <= upperLimit) {
                      tempo += incrementalAmount;
                    }
                  } else {
                    int lowerLimit = incrementalLimit != 0 ? incrementalLimit : Constants.TEMPO_MIN;
                    if (tempo - incrementalAmount >= lowerLimit) {
                      tempo -= incrementalAmount;
                    }
                  }
                }
              }
            } else {
              // TODO: implement incremental tempo changes for seconds and minutes
              float factor = ((float) 60 / part.getTempo()) * part.getBeatsCount();
              seconds += factor * part.getTimerDuration();
            }
          } else {
            float factor = ((float) 60 / part.getTempo()) * part.getBeatsCount();
            seconds += factor * part.getTimerDuration();
          }
          break;
      }
    }
    return MetronomeEngine.getTimeStringFromSeconds((int) seconds, false);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SongWithParts)) {
      return false;
    }
    SongWithParts that = (SongWithParts) o;
    return Objects.equals(song, that.song) && Objects.equals(parts, that.parts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(song, parts);
  }

  @NonNull
  @Override
  public String toString() {
    return "SongWithParts{" +
        "song=" + song +
        ", parts=" + parts +
        '}';
  }
}