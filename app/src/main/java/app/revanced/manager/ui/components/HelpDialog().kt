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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.revanced.manager.BuildConfig
import app.revanced.manager.R

@Composable
fun HelpDialog() {

    var showPopup by remember { mutableStateOf(false) }

    val onPopupDismissed = { showPopup = false }

    var currentUriHandler = LocalUriHandler.current

    PreferenceRow(
        title = stringResource(R.string.help),
        painter = painterResource(id = R.drawable.ic_baseline_help_24),
        onClick = { showPopup = true },
    )

    if (showPopup) {
        AlertDialog(
            backgroundColor = MaterialTheme.colorScheme.background,
            onDismissRequest = onPopupDismissed,
            text = {
                Column(Modifier.padding(8.dp)) {
                    Text(text = "In need of some help?\nJoin our Discord Server and ask in our dedicated support channel!")
                }
            },
            confirmButton = {
                TextButton(onClick = { currentUriHandler.openUri("https://discord.gg/mxsFc6nyqp") }) {
                    Text(text = "Open Discord")
                }
            },
            dismissButton = {
                TextButton(onClick = { onPopupDismissed() }) {
                    Text(text = "Close")
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.help)
                )
            },
        )}

}