package to.wim.screensavertimeout.system

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import to.wim.screensavertimeout.data.SettingsStore

/**
 * Keeps the timeout in place without a running service, with two jobs:
 * - a check shortly after the TV starts, in case its firmware writes its own value late;
 * - a job that runs whenever the setting changes, and puts the chosen value back.
 */
object Watcher {
    const val JOB_AFTER_BOOT = 1
    const val JOB_ON_CHANGE = 2

    private const val AFTER_BOOT_DELAY_MS = 90_000L

    private fun scheduler(context: Context) = context.getSystemService(JobScheduler::class.java)

    private fun service(context: Context) = ComponentName(context, ReapplyJob::class.java)

    fun start(context: Context) {
        scheduleAfterBoot(context)
        watchChanges(context)
    }

    fun scheduleAfterBoot(context: Context) {
        val job =
            JobInfo.Builder(JOB_AFTER_BOOT, service(context))
                .setMinimumLatency(AFTER_BOOT_DELAY_MS)
                .setOverrideDeadline(AFTER_BOOT_DELAY_MS * 2)
                .build()
        scheduler(context).schedule(job)
    }

    /** Content-trigger jobs run once, so [ReapplyJob] schedules this again after each run. */
    fun watchChanges(context: Context) {
        val uri = Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT)
        val job =
            JobInfo.Builder(JOB_ON_CHANGE, service(context))
                .addTriggerContentUri(JobInfo.TriggerContentUri(uri, 0))
                .setTriggerContentUpdateDelay(1_000)
                .setTriggerContentMaxDelay(5_000)
                .build()
        scheduler(context).schedule(job)
    }

    fun cancel(context: Context) {
        scheduler(context).cancel(JOB_AFTER_BOOT)
        scheduler(context).cancel(JOB_ON_CHANGE)
    }
}

class ReapplyJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            ScreenTimeout(applicationContext).reapply()
            if (params.jobId == Watcher.JOB_ON_CHANGE && SettingsStore(applicationContext).current().keepSet) {
                Watcher.watchChanges(applicationContext)
            }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

/** Sets the timeout again after the TV starts and after the app is updated. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (SettingsStore(app).current().keepSet) {
                    ScreenTimeout(app).reapply()
                    Watcher.start(app)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
