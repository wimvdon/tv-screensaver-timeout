package to.wim.screensavertimeout.logic

/** A duration as the screen shows it: whole minutes when it divides evenly, else seconds. */
sealed interface DurationParts {
    data class Minutes(val count: Int) : DurationParts

    data class Seconds(val count: Int) : DurationParts

    /** The TV reports no value, or one that makes no sense. */
    data object Unknown : DurationParts

    companion object {
        fun of(ms: Int?): DurationParts =
            when {
                ms == null || ms <= 0 -> Unknown
                ms % 60_000 == 0 -> Minutes(ms / 60_000)
                else -> Seconds((ms + 500) / 1_000)
            }
    }
}
