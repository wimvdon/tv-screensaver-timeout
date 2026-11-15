package to.wim.screensavertimeout.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import to.wim.screensavertimeout.R
import to.wim.screensavertimeout.logic.DurationParts
import to.wim.screensavertimeout.logic.TimeoutSettings
import to.wim.screensavertimeout.logic.Timeouts

/** The one screen. [resumes] changes each time the app comes to the front. */
@Composable
fun HomeScreen(resumes: Int) {
    val context = LocalContext.current
    val controller = remember { HomeController(context) }
    val scope = rememberCoroutineScope()
    val settings by controller.settings.collectAsState(initial = null)
    var canWrite by remember { mutableStateOf(controller.canWrite()) }
    var currentMs by remember { mutableStateOf(controller.currentMs()) }
    var noSettingsScreen by remember { mutableStateOf(false) }

    fun refresh() {
        canWrite = controller.canWrite()
        currentMs = controller.currentMs()
    }

    fun act(action: suspend () -> Unit) {
        scope.launch {
            action()
            refresh()
        }
    }

    LaunchedEffect(resumes) {
        controller.onShown()
        refresh()
    }

    val current = settings ?: return
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 27.dp),
    ) {
        if (canWrite) {
            TimeoutContent(
                state = HomeState(canWrite = true, currentMs = currentMs, settings = current),
                onChoose = { ms -> act { controller.choose(ms) } },
                onKeepSet = { keep -> act { controller.setKeepSet(keep) } },
                onRestore = { act { controller.restore() } },
            )
        } else {
            PermissionNeeded(
                packageName = controller.packageName,
                noSettingsScreen = noSettingsScreen,
                onCheckAgain = { act { controller.onShown() } },
                onOpenSettings = { noSettingsScreen = !controller.openPermissionSettings(context) },
            )
        }
    }
}

@Composable
private fun TimeoutContent(
    state: HomeState,
    onChoose: (Int) -> Unit,
    onKeepSet: (Boolean) -> Unit,
    onRestore: () -> Unit,
) {
    val first = remember { FocusRequester() }
    val settings: TimeoutSettings = state.settings
    LazyColumn(
        Modifier.width(720.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                val now = durationText(state.currentMs)
                Text(
                    if (now == null) stringResource(R.string.current_unknown) else stringResource(R.string.now_starts_after, now),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.philips_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionHeader(stringResource(R.string.section_start_after)) }
        items(Timeouts.choices) { ms ->
            val selected = settings.targetMs == ms
            ListItem(
                selected = selected,
                onClick = { onChoose(ms) },
                headlineContent = { Text(durationText(ms).orEmpty()) },
                trailingContent = { RadioButton(selected = selected, onClick = null) },
                modifier = if (ms == Timeouts.choices.first()) Modifier.focusRequester(first) else Modifier,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            ListItem(
                selected = false,
                onClick = { onKeepSet(!settings.keepSet) },
                headlineContent = { Text(stringResource(R.string.keep_set_title)) },
                supportingContent = { Text(stringResource(R.string.keep_set_summary)) },
                trailingContent = { Switch(checked = settings.keepSet, onCheckedChange = null) },
            )
        }
        val original = settings.originalMs
        if (original != null) {
            item {
                ListItem(
                    selected = false,
                    onClick = onRestore,
                    headlineContent = { Text(stringResource(R.string.restore_title)) },
                    supportingContent = {
                        Text(stringResource(R.string.restore_summary, durationText(original).orEmpty()))
                    },
                )
            }
        }
        item { Footer() }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}

/** DESIGN.md "Empty and setup states", drawn with TV Material: the screen can't work yet. */
@Composable
private fun PermissionNeeded(
    packageName: String,
    noSettingsScreen: Boolean,
    onCheckAgain: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val first = remember { FocusRequester() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .padding(bottom = 8.dp)
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                stringResource(R.string.setup_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.setup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.adb_command, packageName),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            if (noSettingsScreen) {
                Text(
                    stringResource(R.string.no_settings_screen),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCheckAgain, modifier = Modifier.focusRequester(first)) {
                    Text(stringResource(R.string.action_check_again))
                }
                if (!noSettingsScreen) {
                    OutlinedButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.action_open_settings))
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp).semantics { heading() },
    )
}

@Composable
private fun Footer() {
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    Text(
        stringResource(R.string.footer, stringResource(R.string.app_name), version),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

/** "1 minute", "30 seconds", or null when the TV reports no value. */
@Composable
private fun durationText(ms: Int?): String? =
    when (val parts = DurationParts.of(ms)) {
        is DurationParts.Minutes -> pluralStringResource(R.plurals.duration_minutes, parts.count, parts.count)
        is DurationParts.Seconds -> pluralStringResource(R.plurals.duration_seconds, parts.count, parts.count)
        DurationParts.Unknown -> null
    }
