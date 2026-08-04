# Fix: FOREIGN KEY Constraint Failure beim Speichern von Songs

Dieses Projekt behebt die `SQLiteConstraintException: FOREIGN KEY constraint failed`, die auftritt, wenn ein neuer Song mit seinen Parts gespeichert wird.

## Ursache des Problems

In der migrierten Kotlin-Version von `SongFragment` wurden beim Anlegen eines neuen Songs zwei separate Aufrufe an das `SongViewModel` getätigt: `insertSong(songResult)` und `insertParts(partsResult)`.

Da beide Methoden im `SongViewModel` jeweils eine eigene Coroutine auf `Dispatchers.IO` starten, ist die Reihenfolge der Datenbankoperationen nicht garantiert. Wenn der Versuch, die Parts einzufügen, die Datenbank erreicht, bevor der eigentliche Song-Eintrag erstellt wurde, schlägt der Foreign Key Check fehl, da die `songId` der Parts noch auf keinen existierenden Song verweist.

## Proposed Changes

### Datenbank-Operationen

#### [MODIFY] [SongViewModel.kt](file:///Users/pz/AndroidStudioProjects/tack-android/app/src/main/java/xyz/zedler/patrick/tack/viewmodel/SongViewModel.kt)
- Hinzufügen einer neuen Methode `insertSongWithParts(song: Song, parts: List<Part>, runOnInserted: Runnable? = null)`.
- Diese Methode führt beide Operationen nacheinander innerhalb **derselben** Coroutine aus, um die korrekte Reihenfolge sicherzustellen.

### Fragment-Logik

#### [MODIFY] [SongFragment.kt](file:///Users/pz/AndroidStudioProjects/tack-android/app/src/main/java/xyz/zedler/patrick/tack/fragment/SongFragment.kt)
- Umstellung des Speicher-Vorgangs bei `isNewSong` auf die neue kombinierte Methode `insertSongWithParts`.
- Dies stellt sicher, dass die App erst navigiert, wenn die Datenbankoperationen erfolgreich abgeschlossen oder zumindest korrekt sequenziert sind.

---

## Verification Plan

### Automatisierte Tests
- Kompilierung mit `./gradlew :app:assembleDebug`.

### Manuelle Verifizierung
- App starten.
- Song-Bibliothek öffnen.
- Einen neuen Song erstellen (über das "+" FAB).
- Name eingeben und mindestens einen Part hinzufügen/bearbeiten.
- Auf "Speichern" klicken.
- Die App darf nicht abstürzen und der Song muss korrekt in der Liste erscheinen.
