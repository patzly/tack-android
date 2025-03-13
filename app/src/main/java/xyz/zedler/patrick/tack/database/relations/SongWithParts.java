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

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;
import java.util.Objects;
import xyz.zedler.patrick.tack.Constants.UNIT;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.entity.Song;
import xyz.zedler.patrick.tack.util.MetronomeUtil;

public class SongWithParts {

  @Embedded
  private Song song;

  @Relation(
      parentColumn = "name",
      entityColumn = "song_name"
  )
  private List<Part> parts;

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
    int seconds = 0;
    for (Part part : parts) {
      float factor;
      switch (part.getTimerUnit()) {
        case UNIT.SECONDS:
          factor = 1;
          break;
        case UNIT.MINUTES:
          factor = 60;
          break;
        default:
          factor = ((float) 60 / part.getTempo()) * part.getBeatsCount();
          break;
      }
      seconds += (int) (factor * part.getTimerDuration());
    }
    return MetronomeUtil.getTimeStringFromSeconds(seconds, false);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
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