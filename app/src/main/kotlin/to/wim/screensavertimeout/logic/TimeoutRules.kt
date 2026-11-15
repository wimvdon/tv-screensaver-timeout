package to.wim.screensavertimeout.logic

/**
 * Philips TVs start their own screensaver after about two minutes without input. Android's
 * screen-off timeout starts the Google TV screensaver (ambient mode), so keeping it shorter makes
 * the Google TV one start first.
 */
object Timeouts {
    /** The choices offered, all shorter than the Philips screensaver's two minutes. */
    val choices: List<Int> = listOf(30_000, 60_000, 90_000)
    const val DEFAULT: Int = 60_000

    /** A stored value that is no longer offered falls back to [DEFAULT]. */
    fun normalize(ms: Int): Int = if (ms in choices) ms else DEFAULT
}

/** What the user chose, plus the value the TV had before the app first changed it. */
data class TimeoutSettings(
    val keepSet: Boolean = true,
    val targetMs: Int = Timeouts.DEFAULT,
    /** The TV's own value before the first change, or null when the app hasn't changed it. */
    val originalMs: Int? = null,
)

enum class Outcome {
    /** The new value was written. */
    Applied,

    /** The TV already had the wanted value. */
    AlreadySet,

    /** "Keep this setting" is off, so nothing was changed. */
    Off,

    /** The app may not change system settings (the one-time ADB step is missing). */
    NoPermission,

    /** There is no earlier value to go back to. */
    NothingToRestore,
}

/**
 * One step to carry out: a value to write to the TV, what to store about the original value,
 * and the outcome to show.
 */
data class Step(
    val write: Int? = null,
    val rememberOriginal: Int? = null,
    val forgetOriginal: Boolean = false,
    val outcome: Outcome,
)

/** Decides what to do; the Android side only carries out the [Step]. */
object TimeoutRules {

    /**
     * Puts the chosen value back. [currentMs] is the TV's value now, or null if it can't be read.
     * The first time it changes the TV's value, it remembers that value so it can be restored.
     */
    fun reapply(settings: TimeoutSettings, currentMs: Int?, canWrite: Boolean): Step {
        if (!settings.keepSet) return Step(outcome = Outcome.Off)
        if (!canWrite) return Step(outcome = Outcome.NoPermission)
        val target = Timeouts.normalize(settings.targetMs)
        if (currentMs == target) return Step(outcome = Outcome.AlreadySet)
        val original = if (settings.originalMs == null && currentMs != null && currentMs > 0) currentMs else null
        return Step(write = target, rememberOriginal = original, outcome = Outcome.Applied)
    }

    /** Goes back to the TV's own value from before the first change. */
    fun restore(settings: TimeoutSettings, canWrite: Boolean): Step {
        val original = settings.originalMs ?: return Step(outcome = Outcome.NothingToRestore)
        if (!canWrite) return Step(outcome = Outcome.NoPermission)
        return Step(write = original, forgetOriginal = true, outcome = Outcome.Applied)
    }
}
