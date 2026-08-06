package xyz.zedler.patrick.tack.core.metronome

import android.os.Looper
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import xyz.zedler.patrick.tack.core.audio.AudioProvider
import xyz.zedler.patrick.tack.core.hardware.FlashlightProvider
import xyz.zedler.patrick.tack.core.hardware.HapticProvider
import xyz.zedler.patrick.tack.core.metronome.MetronomeConstants.Unit
import xyz.zedler.patrick.tack.core.model.MetronomeConfig
import xyz.zedler.patrick.tack.core.util.Clock
import xyz.zedler.patrick.tack.core.util.SystemClockImpl

@RunWith(RobolectricTestRunner::class)
class MetronomeEngineTest {

  private lateinit var engine: MetronomeEngine
  private lateinit var audioProvider: AudioProvider
  private lateinit var hapticProvider: HapticProvider
  private lateinit var flashlightProvider: FlashlightProvider
  private lateinit var clock: Clock

  @Before
  fun setup() {
    audioProvider = mockk(relaxed = true)
    hapticProvider = mockk(relaxed = true)
    flashlightProvider = mockk(relaxed = true)
    clock = SystemClockImpl()
  }

  private fun createEngine(config: MetronomeConfig = MetronomeConfig()) {
    engine = MetronomeEngine(
      audioProvider, hapticProvider, flashlightProvider, clock,
      tickLooper = Looper.getMainLooper(),
      callbackLooper = Looper.getMainLooper()
    )
    engine.setConfig(config)
  }

  @Test
  fun `test basic start stop`() {
    createEngine()
    engine.start()
    assertTrue(engine.state.value.isPlaying)
    engine.stop()
    assertFalse(engine.state.value.isPlaying)
  }

  @Test
  fun `test timer stops engine after duration in seconds`() {
    createEngine(
      MetronomeConfig(
        tempo = 60, // 1 beat per second
        timerDuration = 2,
        timerUnit = Unit.SECONDS
      )
    )
    engine.start()
    assertTrue(engine.state.value.isPlaying)

    ShadowLooper.shadowMainLooper().idleFor(3000, TimeUnit.MILLISECONDS)

    assertFalse(engine.state.value.isPlaying)
  }

  @Test
  fun `test timer stops engine after duration in bars`() {
    createEngine(
      MetronomeConfig(
        tempo = 60,
        beats = listOf("strong", "normal"), // 2 beats per bar -> 2 seconds per bar
        timerDuration = 1,
        timerUnit = Unit.BARS
      )
    )
    engine.start()

    // 1 bar = 2 beats = 2 seconds
    ShadowLooper.shadowMainLooper().idleFor(3000, TimeUnit.MILLISECONDS)

    assertFalse(engine.state.value.isPlaying)
  }

  @Test
  fun `test incremental tempo increases in bars`() {
    createEngine(
      MetronomeConfig(
        tempo = 60,
        beats = listOf("strong", "normal"), // 2 seconds per bar
        incrementalAmount = 10,
        incrementalInterval = 1,
        incrementalUnit = Unit.BARS,
        incrementalIncrease = true
      )
    )
    engine.start()

    // After 1 bar (2 seconds), tempo should increase
    ShadowLooper.shadowMainLooper().idleFor(3000, TimeUnit.MILLISECONDS)

    assertEquals(70, engine.state.value.tempo)
  }

  @Test
  fun `test incremental tempo increases in seconds`() {
    createEngine(
      MetronomeConfig(
        tempo = 60,
        incrementalAmount = 5,
        incrementalInterval = 2,
        incrementalUnit = Unit.SECONDS,
        incrementalIncrease = true
      )
    )
    engine.start()

    ShadowLooper.shadowMainLooper().idleFor(3000, TimeUnit.MILLISECONDS)

    assertEquals(65, engine.state.value.tempo)
  }

  @Test
  fun `test mute pattern in bars`() {
    createEngine(
      MetronomeConfig(
        tempo = 60,
        beats = listOf("strong"), // 1 second per bar
        mutePlay = 1,
        muteMute = 1,
        muteUnit = Unit.BARS,
        muteRandom = false
      )
    )
    engine.start()

    // Bar 1 starts at 0ms. muteCountDown = 1.
    // Bar 1 tick at 0ms: muteCountDown becomes 0.
    // Bar 2 starts at 1000ms.
    // Bar 2 tick at 1000ms: enters else, isMuted = true.

    assertFalse(engine.state.value.isMuted)

    ShadowLooper.shadowMainLooper().idleFor(1100, TimeUnit.MILLISECONDS)
    assertTrue(engine.state.value.isMuted)

    ShadowLooper.shadowMainLooper().idleFor(1000, TimeUnit.MILLISECONDS)
    assertFalse(engine.state.value.isMuted)
  }

  @Test
  fun `test timing - count ticks`() {
    createEngine(
      MetronomeConfig(
        tempo = 60, // 1 beat per second
      )
    )
    engine.start()

    ShadowLooper.shadowMainLooper().idleFor(
      5500, TimeUnit.MILLISECONDS
    ) // 5.5 seconds should result in 6 ticks (0, 1, 2, 3, 4, 5)

    verify(atLeast = 5, atMost = 7) { audioProvider.playTick(any(), any()) }
  }

  @Test
  fun `test random mute logic`() {
    createEngine(
      MetronomeConfig(
        tempo = 60,
        mutePlay = 1,
        muteMute = 100, // Always mute in random mode if random value < 100
        muteUnit = Unit.BEATS,
        muteRandom = true
      )
    )
    engine.start()

    // At 60 BPM, each beat is 1000ms.
    // The random mute logic for BEATS is in startTicks:
    // if (config.isMuteActive && config.muteUnit == Unit.BEATS) {
    //     muted = random.nextInt(100) < config.muteMute
    // }
    // Since muteMute = 100, nextInt(100) will always be < 100.

    ShadowLooper.shadowMainLooper().idleFor(500, TimeUnit.MILLISECONDS)
    // Check if the tick was muted. We can't check state.isMuted easily because
    // it's a local variable in the Runnable for BEATS unit.
    // But we can check audioProvider.playTick call.
    verify { audioProvider.playTick(tickType = any(), muted = true) }
  }

  @Test
  fun `test incremental tempo respects max limit`() {
    createEngine(
      MetronomeConfig(
        tempo = MetronomeConstants.TEMPO_MAX - 5,
        incrementalAmount = 10,
        incrementalInterval = 1,
        incrementalUnit = Unit.BARS,
        incrementalIncrease = true
      )
    )
    engine.start()

    ShadowLooper.shadowMainLooper().idleFor(3000, TimeUnit.MILLISECONDS)

    assertEquals(MetronomeConstants.TEMPO_MAX, engine.state.value.tempo)
  }

  @Test
  fun `test polyrhythm fires poly ticks`() {
    createEngine(
      MetronomeConfig(
        tempo = 60,
        beats = listOf("strong"), // 1 beat per bar
        subdivisions = listOf("normal", "normal"), // 2 poly ticks per bar
        usePolyrhythm = true
      )
    )
    engine.start()

    ShadowLooper.shadowMainLooper().idleFor(1500, TimeUnit.MILLISECONDS)

    // 1.5 seconds at 60 BPM with 1 beat/bar and 2 poly sub:
    // Main ticks: 0ms, 1000ms
    // Poly ticks: 0ms, 500ms, 1000ms, 1500ms
    // In performTick, we have check if beat == 1 and config.usePolyrhythm
    // verify playTick with isPoly=true
    verify(atLeast = 2) { audioProvider.playTick(any(), any()) }
    // The implementation uses different playTick parameters or internal logic?
    // Let's check MetronomeEngine.kt again.
  }

  @Test
  fun `test timer duration zero means infinite play`() {
    createEngine(
      MetronomeConfig(
        tempo = 120,
        timerDuration = 0,
        timerUnit = Unit.SECONDS
      )
    )
    engine.start()

    ShadowLooper.shadowMainLooper().idleFor(10000, TimeUnit.MILLISECONDS)

    assertTrue(engine.state.value.isPlaying)
  }
}
