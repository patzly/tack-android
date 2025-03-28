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

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Entity(tableName = "songs", indices = {@Index(value = {"id"}, unique = true)})
public class Song implements Parcelable {

  @PrimaryKey
  @NonNull
  private String id;
  @Nullable
  private String name;
  private long lastPlayed;
  private int playCount;
  private boolean isLooped;

  public Song(
      @NonNull String id, @Nullable String name, long lastPlayed, int playCount, boolean isLooped
  ) {
    this.id = id;
    this.name = name;
    this.lastPlayed = lastPlayed;
    this.playCount = playCount;
    this.isLooped = isLooped;
  }

  @Ignore
  public Song(@Nullable String name) {
    this();
    this.name = name;
  }

  @Ignore
  public Song() {
    this.id = UUID.randomUUID().toString();
  }

  @Ignore
  public Song(@NonNull Song song) {
    this.id = song.id;
    this.name = song.name;
    this.lastPlayed = song.lastPlayed;
    this.playCount = song.playCount;
    this.isLooped = song.isLooped;
  }

  @Ignore
  protected Song(Parcel in) {
    id = Objects.requireNonNull(in.readString());
    name = in.readString();
    lastPlayed = in.readLong();
    playCount = in.readInt();
    isLooped = in.readByte() != 0;
  }

  @NonNull
  public String getId() {
    return id;
  }

  public void setId(@NonNull String id) {
    this.id = id;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public long getLastPlayed() {
    return lastPlayed;
  }

  public void setLastPlayed(long lastPlayed) {
    this.lastPlayed = lastPlayed;
  }

  public int getPlayCount() {
    return playCount;
  }

  public void setPlayCount(int playCount) {
    this.playCount = playCount;
  }

  public void incrementPlayCount() {
    playCount++;
  }

  public boolean isLooped() {
    return isLooped;
  }

  public void setLooped(boolean looped) {
    isLooped = looped;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Song)) {
      return false;
    }
    Song song = (Song) o;
    return lastPlayed == song.lastPlayed && isLooped == song.isLooped
        && Objects.equals(id, song.id) && Objects.equals(name, song.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, lastPlayed, isLooped);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(@NonNull Parcel dest, int flags) {
    dest.writeString(id);
    dest.writeString(name);
    dest.writeLong(lastPlayed);
    dest.writeInt(playCount);
    dest.writeByte((byte) (isLooped ? 1 : 0));
  }

  @NonNull
  @Override
  public String toString() {
    Date lastPlayed = new Date(this.lastPlayed);
    return "Song{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", lastPlayed=" + lastPlayed +
        ", isLooped=" + isLooped +
        '}';
  }

  public static final Creator<Song> CREATOR = new Creator<>() {
    @Override
    public Song createFromParcel(Parcel in) {
      return new Song(in);
    }

    @Override
    public Song[] newArray(int size) {
      return new Song[size];
    }
  };
}