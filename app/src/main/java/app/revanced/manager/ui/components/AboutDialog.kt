@file:OptIn(ExperimentalMaterial3Api::class)

package app.revanced.manager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.BuildConfig
import app.revanced.manager.R

@Composable
fun AboutDialog() {

    var showPopup by remember { mutableStateOf(false) }

    val onPopupDismissed = { showPopup = false }
    
    Column(
        Modifier
            .clickable { showPopup = true }
            .padding(horizontal = 12.dp)
            .height(56.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_info_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Card(
                modifier = Modifier.padding(horizontal = 28.dp),
                border = null,
            ) {
                Text(text = stringResource(id = R.string.app_version),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = BuildConfig.VERSION_TYPE + " " + BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }

    if (showPopup) {
    AlertDialog(
        backgroundColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onPopupDismissed,
        text = {
            Column(Modifier.padding(8.dp)) {
                Text(text = "Application Version = " + BuildConfig.VERSION_NAME)
                Text(text = "Version Type = " + BuildConfig.VERSION_TYPE)
                Text(text = "Build Type = " + BuildConfig.BUILD_TYPE)
            }
        },
        confirmButton = {

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