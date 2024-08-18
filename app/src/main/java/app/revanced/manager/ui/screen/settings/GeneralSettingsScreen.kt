package app.revanced.manager.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.ui.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel
) {
    val prefs = viewModel.prefs
    val coroutineScope = viewModel.viewModelScope
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
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GroupHeader(stringResource(R.string.appearance))

            val theme by prefs.theme.getAsState()
            SettingsListItem(
                modifier = Modifier.clickable { showThemePicker = true },
                headlineContent = stringResource(R.string.theme),
                supportingContent = stringResource(R.string.theme_description),
                trailingContent = {
                    FilledTonalButton(
                        onClick = {
                            showThemePicker = true
                        }
                    ) {
                        Text(stringResource(theme.displayName))
                    }
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BooleanItem(
                    preference = prefs.dynamicColor,
                    coroutineScope = coroutineScope,
                    headline = R.string.dynamic_color,
                    description = R.string.dynamic_color_description
                )
            }
        }
    }
}

@Composable
private fun ThemePicker(
    onDismiss: () -> Unit,
    onConfirm: (Theme) -> Unit,
    prefs: PreferencesManager = koinInject()
) {
    var selectedTheme by rememberSaveable { mutableStateOf(prefs.theme.getBlocking()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column {
                Theme.entries.forEach {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTheme = it },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == it,
                            onClick = { selectedTheme = it })
                        Text(stringResource(it.displayName))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedTheme)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    )
}