# Walkthrough - Fix: FOREIGN KEY Constraint Failure

Ich habe den Fehler behoben, der beim Speichern eines neuen Songs zum Absturz führte.

## Änderungen

### 1. Garantierte Reihenfolge im ViewModel
In `SongViewModel.kt` wurde eine neue Methode `insertSongWithParts` hinzugefügt.
- **Was sie macht**: Sie speichert den Song und seine zugehörigen Parts innerhalb einer einzigen Hintergrundaufgabe (Coroutine).
- **Warum das wichtig ist**: Dadurch ist garantiert, dass der Song-Eintrag in der Datenbank existiert, **bevor** die Parts gespeichert werden. Da die Parts auf die ID des Songs verweisen (Foreign Key), verhinder dies den Integritätsfehler.

### 2. Umstellung des Speichervorgangs
In `SongFragment.kt` wurde die Logik beim Klicken auf den "Speichern"-Button angepasst:
- Bei neuen Songs wird nun die kombinierte Methode `insertSongWithParts` verwendet, statt Song und Parts in getrennten, asynchronen Schritten zu senden.

## Verifizierung
- Das Projekt wurde mit `./gradlew :app:assembleDebug` erfolgreich gebaut.
- Die strukturelle Ursache des Race-Conditions wurde durch die Sequenzierung der Datenbankoperationen in einer einzigen Coroutine beseitigt.

Das Speichern von Songs in der Bibliothek sollte nun reibungslos funktionieren.
