@file:OptIn(ExperimentalMaterial3Api::class)

package app.revanced.manager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.revanced.manager.BuildConfig
import app.revanced.manager.R

@Composable
fun AboutDialog() {

    var showPopup by remember { mutableStateOf(false) }

    val onPopupDismissed = { showPopup = false }

    val localClipboardManager = LocalClipboardManager.current

    PreferenceRow(
        title = stringResource(R.string.app_version),
        subtitle = "${BuildConfig.VERSION_TYPE} ${BuildConfig.VERSION_NAME}",
        painter = painterResource(id = R.drawable.ic_baseline_info_24),
        onClick = { showPopup = true },
        onLongClick = { localClipboardManager.setText(AnnotatedString("Debug Info:\n" + DebugInfo())) }
    )

    if (showPopup) {
        AlertDialog(
            backgroundColor = MaterialTheme.colorScheme.background,
            onDismissRequest = onPopupDismissed,
            text = {
                Column(Modifier.padding(8.dp)) {
                    Text(text = DebugInfo())
                }
            },
            // TODO: MAKE CLIPBOARD REUSABLE, ADD TOAST MESSAGE *CLEANLY*
            confirmButton = {
                TextButton(onClick = { localClipboardManager.setText(AnnotatedString("Debug Info:\n" + DebugInfo())) } ) {
                    Text(text = "Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { onPopupDismissed() }) {
                    Text(text = "Close")
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.app_version)
                )
            },
        )}

}