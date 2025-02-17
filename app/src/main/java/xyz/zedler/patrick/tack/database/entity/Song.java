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

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Calendar;
import java.util.Date;

@Entity(
    tableName = "songs",
    indices = {@Index(value = "name", unique = true)}
)
public class Song {

  @PrimaryKey
  @NonNull
  private String name;
  private long lastPlayed;
  private boolean isLooped;

  public Song(@NonNull String name, long lastPlayed, boolean isLooped) {
    this.name = name;
    this.lastPlayed = lastPlayed;
    this.isLooped = isLooped;
  }

  @Ignore
  public Song(@NonNull String name) {
    this(name, Calendar.getInstance().getTimeInMillis(), false);
  }

  @NonNull
  public String getName() {
    return name;
  }

  public void setName(@NonNull String name) {
    this.name = name;
  }

  public long getLastPlayed() {
    return lastPlayed;
  }

  public void setLastPlayed(long lastPlayed) {
    this.lastPlayed = lastPlayed;
  }

  public boolean isLooped() {
    return isLooped;
  }

  public void setLooped(boolean looped) {
    isLooped = looped;
  }

  @NonNull
  @Override
  public String toString() {
    Date lastPlayed = new Date(this.lastPlayed);
    return "Song{" +
        "name='" + name + '\'' +
        ", lastPlayed=" + lastPlayed +
        ", isLooped=" + isLooped +
        '}';
  }
}