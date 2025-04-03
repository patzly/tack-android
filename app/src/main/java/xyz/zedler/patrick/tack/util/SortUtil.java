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

package xyz.zedler.patrick.tack.util;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import xyz.zedler.patrick.tack.Constants.SONGS_ORDER;
import xyz.zedler.patrick.tack.database.entity.Part;
import xyz.zedler.patrick.tack.database.relations.SongWithParts;

public class SortUtil {

  public static void sortSongsWithParts(List<SongWithParts> songsWithParts, int sortOrder) {
    if (sortOrder == SONGS_ORDER.NAME_ASC) {
      if (VERSION.SDK_INT >= VERSION_CODES.N) {
        Comparator<String> comparator = Comparator.nullsLast(
            Comparator.comparing(String::toLowerCase, Comparator.naturalOrder())
        );
        Collections.sort(
            songsWithParts,
            Comparator.comparing(
                o -> o.getSong().getName(),
                comparator
            )
        );
      } else {
        Collections.sort(songsWithParts, (s1, s2) -> {
          String name1 = (s1.getSong() != null) ? s1.getSong().getName() : null;
          String name2 = (s2.getSong() != null) ? s2.getSong().getName() : null;
          // Nulls last handling
          if (name1 == null && name2 == null) return 0;
          if (name1 == null) return 1;
          if (name2 == null) return -1;
          return name1.compareToIgnoreCase(name2);
        });
      }
    } else if (sortOrder == SONGS_ORDER.LAST_PLAYED_ASC) {
      Collections.sort(
          songsWithParts,
          (s1, s2) -> Long.compare(
              s2.getSong().getLastPlayed(), s1.getSong().getLastPlayed()
          )
      );
    } else if (sortOrder == SONGS_ORDER.MOST_PLAYED_ASC) {
      Collections.sort(
          songsWithParts,
          (s1, s2) -> Integer.compare(
              s2.getSong().getPlayCount(), s1.getSong().getPlayCount()
          )
      );
    }
  }

  public static void sortPartsByIndex(List<Part> parts) {
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
      Collections.sort(parts, Comparator.comparingInt(Part::getPartIndex));
    } else {
      Collections.sort(
          parts, (p1, p2) -> Integer.compare(p1.getPartIndex(), p2.getPartIndex())
      );
    }
  }
}