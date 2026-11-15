package to.wim.screensavertimeout.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import to.wim.screensavertimeout.logic.TimeoutSettings
import to.wim.screensavertimeout.logic.Timeouts

/**
 * Everything is device-only and never backed up (docs/architecture.md, "Backup"): the settings only
 * work after the one-time permission step on this TV, and the original value belongs to this TV.
 */
private val Context.deviceStore by preferencesDataStore(
    name = "device_state",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val keepSet = booleanPreferencesKey("keep_set")
        val target = intPreferencesKey("target_ms")
        val original = intPreferencesKey("original_ms")
    }

    val settings: Flow<TimeoutSettings> =
        context.deviceStore.data.map { p ->
            TimeoutSettings(
                keepSet = p[Keys.keepSet] ?: true,
                targetMs = Timeouts.normalize(p[Keys.target] ?: Timeouts.DEFAULT),
                originalMs = p[Keys.original],
            )
        }

    suspend fun current(): TimeoutSettings = settings.first()

    suspend fun setKeepSet(value: Boolean) = context.deviceStore.edit { it[Keys.keepSet] = value }

    suspend fun setTarget(ms: Int) = context.deviceStore.edit { it[Keys.target] = Timeouts.normalize(ms) }

    suspend fun setOriginal(ms: Int?) =
        context.deviceStore.edit {
            if (ms == null) it.remove(Keys.original) else it[Keys.original] = ms
        }
}
