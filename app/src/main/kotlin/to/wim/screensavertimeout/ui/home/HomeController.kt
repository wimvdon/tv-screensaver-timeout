package to.wim.screensavertimeout.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.flow.Flow
import to.wim.screensavertimeout.data.SettingsStore
import to.wim.screensavertimeout.logic.TimeoutSettings
import to.wim.screensavertimeout.system.ScreenTimeout
import to.wim.screensavertimeout.system.Watcher

/** What the home screen shows. */
data class HomeState(
    val canWrite: Boolean,
    val currentMs: Int?,
    val settings: TimeoutSettings,
)

/** Carries out the home screen's actions; the composables only show [HomeState]. */
class HomeController(context: Context) {
    private val app = context.applicationContext
    private val store = SettingsStore(app)
    private val timeout = ScreenTimeout(app)

    val packageName: String = app.packageName
    val settings: Flow<TimeoutSettings> = store.settings

    fun canWrite(): Boolean = timeout.canWrite()

    fun currentMs(): Int? = timeout.current()

    /** On opening the app: put the value back and make sure the watch job is running. */
    suspend fun onShown() {
        timeout.reapply()
        if (store.current().keepSet && timeout.canWrite()) Watcher.start(app)
    }

    suspend fun choose(ms: Int) {
        store.setTarget(ms)
        setKeepSet(true)
    }

    suspend fun setKeepSet(keep: Boolean) {
        store.setKeepSet(keep)
        if (keep) {
            timeout.reapply()
            Watcher.watchChanges(app)
        } else {
            Watcher.cancel(app)
        }
    }

    suspend fun restore() {
        timeout.restore()
    }

    /** Opens Android's "modify system settings" screen; false when this TV doesn't have one. */
    fun openPermissionSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
