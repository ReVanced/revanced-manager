package app.revanced.manager.compose.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.revanced.manager.compose.R
import app.revanced.manager.compose.domain.manager.PreferencesManager
import app.revanced.manager.compose.ui.component.AppTopBar
import app.revanced.manager.compose.ui.component.GroupHeader
import app.revanced.manager.compose.ui.theme.Theme
import app.revanced.manager.compose.ui.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel
) {
    val prefs = viewModel.prefs
    var showThemePicker by rememberSaveable { mutableStateOf(false) }

    if (showThemePicker) {
        ThemePicker(
            onDismiss = { showThemePicker = false },
            onConfirm = { viewModel.setTheme(it) }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.general),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            GroupHeader(stringResource(R.string.appearance))
            ListItem(
                modifier = Modifier.clickable { showThemePicker = true },
                headlineContent = { Text(stringResource(R.string.theme)) },
                supportingContent = { Text(stringResource(R.string.theme_description)) },
                trailingContent = {
                    Button({
                        showThemePicker = true
                    }) { Text(stringResource(prefs.theme.displayName)) }
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    modifier = Modifier.clickable { prefs.dynamicColor = !prefs.dynamicColor },
                    headlineContent = { Text(stringResource(R.string.dynamic_color)) },
                    supportingContent = { Text(stringResource(R.string.dynamic_color_description)) },
                    trailingContent = {
                        Switch(
                            checked = prefs.dynamicColor,
                            onCheckedChange = { prefs.dynamicColor = it })
                    }
                )
            }

        }
    }
}

@Composable
fun ThemePicker(
    onDismiss: () -> Unit,
    onConfirm: (Theme) -> Unit,
    prefs: PreferencesManager = koinInject()
) {
    var selectedTheme by rememberSaveable { mutableStateOf(prefs.theme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column {
                Theme.values().forEach {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedTheme = it },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedTheme == it, onClick = { selectedTheme = it })
                        Text(stringResource(it.displayName))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(selectedTheme)
                onDismiss()
            }) {
                Text(stringResource(R.string.apply))
            }
        }
    )
}