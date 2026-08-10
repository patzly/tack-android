package xyz.zedler.patrick.tack.presentation.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface Route : Parcelable {
  @Parcelize data object Main : Route
  @Parcelize data object About : Route
  @Parcelize data object Settings : Route
  @Parcelize data object Log : Route
  @Parcelize data object Songs : Route
  @Parcelize data class Song(val songId: String) : Route
}
