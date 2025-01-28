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
import androidx.annotation.Nullable;

public class Part {

  @Nullable
  String name;
  Config config;

  public Part(@Nullable String name, Config config) {
    this.name = name;
    this.config = config;
  }

  public Config getConfig() {
    return config;
  }

  @NonNull
  @Override
  public String toString() {
    return "Part{" +
        "name='" + name + '\'' +
        ", config=" + config +
        '}';
  }
}