package app.revanced.manager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.screens.mainsubscreens.PatchClass
import app.revanced.patcher.annotation.Package
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages

@Composable
fun HelpDialog() {

    var showPopup by remember { mutableStateOf(false) }

    val onPopupDismissed = { showPopup = false }

    val currentUriHandler = LocalUriHandler.current

    PreferenceRow(
        title = stringResource(R.string.help),
        painter = painterResource(id = R.drawable.ic_baseline_help_24),
        onClick = { showPopup = true },
    )

    if (showPopup) {
        AlertDialog(
            onDismissRequest = onPopupDismissed,
            shape = RoundedCornerShape(12.dp),
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
                    Text(text = "Dismiss")
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.help)
                )
            },
        )
    }
}


@Composable
fun PatchCompatibilityDialog(
    patchClass: PatchClass,
    onClose: () -> Unit
) {
    val patch = patchClass.patch
    AlertDialog(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(stringResource(id = R.string.compatible_versions))
        },
        text = {
            patch.compatiblePackages!!.forEach { p: Package ->
                Text(
                    p.versions.reversed().joinToString(", ")
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = "Dismiss")
            }
        }
    )
}