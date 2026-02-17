package eu.kanade.presentation.more.settings.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.util.system.toast
import exh.uconfig.EHConfigurator
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.core.common.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ConfigureExhDialog(run: Boolean, onRunning: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(run) {
        if (run) {
            showDialog = true
            onRunning()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(SYMR.strings.eh_settings_upload)) },
            text = { Text(stringResource(SYMR.strings.eh_settings_upload_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            running = true
                            try {
                                Injekt.get<EHConfigurator>().configureAll()
                                context.toast(SYMR.strings.eh_settings_uploaded)
                            } catch (e: Exception) {
                                logcat(LogPriority.ERROR, e)
                                context.toast(
                                    context.stringResource(
                                        SYMR.strings.eh_settings_upload_failed,
                                        e.message.toString(),
                                    ),
                                )
                            } finally {
                                running = false
                                showDialog = false
                            }
                        }
                    },
                    enabled = !running,
                ) {
                    Text(stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}
