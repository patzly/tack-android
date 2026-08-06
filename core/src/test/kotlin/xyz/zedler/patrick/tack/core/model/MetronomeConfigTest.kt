package xyz.zedler.patrick.tack.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.zedler.patrick.tack.core.audio.Constants.TickType
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.Unit

class MetronomeConfigTest {

  @Test
  fun `test snapped mute mute for seconds`() {
    val config = MetronomeConfig(muteUnit = Unit.SECONDS)
    assertEquals(MetronomeConstants.MUTE_MUTE_MIN, config.getSnappedMuteMute(0))
    assertEquals(1, config.getSnappedMuteMute(1))
    assertEquals(2, config.getSnappedMuteMute(2))
    assertEquals(MetronomeConstants.MUTE_MUTE_MAX, config.getSnappedMuteMute(21))
  }

  @Test
  fun `test snapped mute mute for beats`() {
    val config = MetronomeConfig(muteUnit = Unit.BEATS)
    assertEquals(MetronomeConstants.MUTE_MUTE_MIN_BEATS, config.getSnappedMuteMute(0))
    assertEquals(5, config.getSnappedMuteMute(5))
    assertEquals(10, config.getSnappedMuteMute(8)) // ceil(8-0)/5 = 2 -> 0 + 2*5 = 10
    assertEquals(15, config.getSnappedMuteMute(12))
    assertEquals(MetronomeConstants.MUTE_MUTE_MAX_BEATS, config.getSnappedMuteMute(105))
  }

  @Test
  fun `test swing patterns`() {
    assertEquals(
      listOf(TickType.BEAT_SUB, TickType.MUTED, TickType.NORMAL),
      MetronomeConfig.swing3()
    )
    assertEquals(
      listOf(TickType.BEAT_SUB, TickType.MUTED, TickType.MUTED, TickType.NORMAL, TickType.MUTED),
      MetronomeConfig.swing5()
    )
    assertEquals(
      listOf(
        TickType.BEAT_SUB,
        TickType.MUTED,
        TickType.MUTED,
        TickType.MUTED,
        TickType.NORMAL,
        TickType.MUTED,
        TickType.MUTED
      ),
      MetronomeConfig.swing7()
    )
  }
}
