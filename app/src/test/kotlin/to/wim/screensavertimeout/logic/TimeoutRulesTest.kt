package to.wim.screensavertimeout.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeoutRulesTest {

    @Test
    fun `every choice is shorter than the Philips screensaver`() {
        assertTrue(Timeouts.choices.all { it < 120_000 })
        assertTrue(Timeouts.DEFAULT in Timeouts.choices)
    }

    @Test
    fun `a value that is no longer offered falls back to the default`() {
        assertEquals(Timeouts.DEFAULT, Timeouts.normalize(45_000))
        assertEquals(90_000, Timeouts.normalize(90_000))
    }

    @Test
    fun `writes the chosen value and remembers the TV's own value the first time`() {
        val step = TimeoutRules.reapply(TimeoutSettings(targetMs = 60_000), currentMs = 600_000, canWrite = true)
        assertEquals(Step(write = 60_000, rememberOriginal = 600_000, outcome = Outcome.Applied), step)
    }

    @Test
    fun `keeps the first original value when the TV resets it later`() {
        val settings = TimeoutSettings(targetMs = 60_000, originalMs = 600_000)
        val step = TimeoutRules.reapply(settings, currentMs = 300_000, canWrite = true)
        assertEquals(60_000, step.write)
        assertNull(step.rememberOriginal)
    }

    @Test
    fun `does nothing when the TV already has the value`() {
        val step = TimeoutRules.reapply(TimeoutSettings(targetMs = 30_000), currentMs = 30_000, canWrite = true)
        assertEquals(Step(outcome = Outcome.AlreadySet), step)
    }

    @Test
    fun `does nothing when keep this setting is off`() {
        val step = TimeoutRules.reapply(TimeoutSettings(keepSet = false), currentMs = 600_000, canWrite = true)
        assertEquals(Step(outcome = Outcome.Off), step)
    }

    @Test
    fun `reports the missing permission instead of writing`() {
        val step = TimeoutRules.reapply(TimeoutSettings(), currentMs = 600_000, canWrite = false)
        assertEquals(Step(outcome = Outcome.NoPermission), step)
    }

    @Test
    fun `an unreadable value is overwritten but not remembered`() {
        val step = TimeoutRules.reapply(TimeoutSettings(), currentMs = null, canWrite = true)
        assertEquals(Step(write = Timeouts.DEFAULT, outcome = Outcome.Applied), step)
    }

    @Test
    fun `a stored target that is no longer offered writes the default`() {
        val step = TimeoutRules.reapply(TimeoutSettings(targetMs = 5_000), currentMs = 600_000, canWrite = true)
        assertEquals(Timeouts.DEFAULT, step.write)
    }

    @Test
    fun `restore writes the original value and forgets it`() {
        val step = TimeoutRules.restore(TimeoutSettings(originalMs = 600_000), canWrite = true)
        assertEquals(Step(write = 600_000, forgetOriginal = true, outcome = Outcome.Applied), step)
    }

    @Test
    fun `restore without an original value does nothing`() {
        assertEquals(Step(outcome = Outcome.NothingToRestore), TimeoutRules.restore(TimeoutSettings(), canWrite = true))
    }

    @Test
    fun `restore without permission does nothing`() {
        val step = TimeoutRules.restore(TimeoutSettings(originalMs = 600_000), canWrite = false)
        assertEquals(Step(outcome = Outcome.NoPermission), step)
    }
}
