package to.wim.screensavertimeout.system

import android.content.Context
import android.provider.Settings
import to.wim.screensavertimeout.data.SettingsStore
import to.wim.screensavertimeout.logic.Outcome
import to.wim.screensavertimeout.logic.Step
import to.wim.screensavertimeout.logic.TimeoutRules

/**
 * Reads and writes Android's screen-off timeout, which starts the Google TV screensaver.
 * Writing needs the "modify system settings" permission (WRITE_SETTINGS), granted once over ADB.
 */
class ScreenTimeout(private val context: Context) {
    private val store = SettingsStore(context)

    fun canWrite(): Boolean = Settings.System.canWrite(context)

    /** The TV's value in milliseconds, or null when it has none. */
    fun current(): Int? =
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, -1).takeIf { it > 0 }

    /** Puts the chosen value back if it was changed (see [TimeoutRules.reapply]). */
    suspend fun reapply(): Outcome = carryOut(TimeoutRules.reapply(store.current(), current(), canWrite()))

    /** Turns "keep this setting" off and goes back to the TV's own value. */
    suspend fun restore(): Outcome {
        store.setKeepSet(false)
        Watcher.cancel(context)
        return carryOut(TimeoutRules.restore(store.current(), canWrite()))
    }

    private suspend fun carryOut(step: Step): Outcome {
        val value = step.write ?: return step.outcome
        val written =
            try {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, value)
            } catch (e: SecurityException) {
                return Outcome.NoPermission
            }
        if (!written) return Outcome.NoPermission
        step.rememberOriginal?.let { store.setOriginal(it) }
        if (step.forgetOriginal) store.setOriginal(null)
        return step.outcome
    }
}
