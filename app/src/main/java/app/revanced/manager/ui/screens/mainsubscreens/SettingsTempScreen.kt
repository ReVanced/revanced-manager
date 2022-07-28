package app.revanced.manager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.ui.models.SettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import org.koin.androidx.compose.getViewModel

@Destination
@RootNavGraph
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTempScreen(
    viewModel: SettingsViewModel = getViewModel(),
) {
    val context = LocalContext.current
    val prefs = viewModel.prefs
    Scaffold(
        modifier = Modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ListItem(
                modifier = Modifier.clickable { prefs.dynamicColor = !prefs.dynamicColor },
                headlineText = { Text("Dynamic Color") },
                trailingContent = {
                    Switch(
                        checked = prefs.dynamicColor,
                        onCheckedChange = { prefs.dynamicColor = it })
                }
            )

            ListItem(
                modifier = Modifier.clickable(enabled = !prefs.dynamicColor) {
                    prefs.midnightMode = !prefs.midnightMode
                },
                headlineText = { Text("Black background") },
                trailingContent = {
                    Switch(
                        enabled = !prefs.dynamicColor,
                        checked = prefs.midnightMode,
                        onCheckedChange = { prefs.midnightMode = it }
                    )
                }
            )

            ListItem(
                modifier = Modifier.clickable { prefs.autoUpdate = !prefs.autoUpdate },
                headlineText = { Text("Auto Update") },
                trailingContent = {
                    Switch(
                        checked = prefs.autoUpdate,
                        onCheckedChange = { prefs.autoUpdate = it })
                }
            )
            if (prefs.autoUpdate) {
                Toast.makeText(
                    context,
                    "Auto Update is enabled",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Auto Update is disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}