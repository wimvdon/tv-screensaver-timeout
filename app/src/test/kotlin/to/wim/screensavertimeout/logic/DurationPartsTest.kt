package to.wim.screensavertimeout.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationPartsTest {

    @Test
    fun `whole minutes are shown in minutes`() {
        assertEquals(DurationParts.Minutes(1), DurationParts.of(60_000))
        assertEquals(DurationParts.Minutes(10), DurationParts.of(600_000))
    }

    @Test
    fun `other values are shown in seconds`() {
        assertEquals(DurationParts.Seconds(30), DurationParts.of(30_000))
        assertEquals(DurationParts.Seconds(90), DurationParts.of(90_000))
    }

    @Test
    fun `seconds are rounded`() {
        assertEquals(DurationParts.Seconds(15), DurationParts.of(14_600))
    }

    @Test
    fun `missing or impossible values are unknown`() {
        assertEquals(DurationParts.Unknown, DurationParts.of(null))
        assertEquals(DurationParts.Unknown, DurationParts.of(0))
        assertEquals(DurationParts.Unknown, DurationParts.of(-1))
    }
}
