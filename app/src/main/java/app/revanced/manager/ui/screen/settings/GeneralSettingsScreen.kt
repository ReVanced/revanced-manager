package app.revanced.manager.ui.screen.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.haptics.HapticRadioButton
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.component.settings.ThemeSelector
import app.revanced.manager.ui.theme.Theme
import app.revanced.manager.ui.viewmodel.GeneralSettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GeneralSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: GeneralSettingsViewModel = koinViewModel()
) {
    val prefs = viewModel.prefs
    val coroutineScope = viewModel.viewModelScope
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }

    if (showLanguagePicker) {
        LanguagePicker(
            supportedLocales = viewModel.getSupportedLocales(),
            currentLocale = viewModel.getCurrentLocale(),
            onDismiss = { showLanguagePicker = false },
            onConfirm = { viewModel.setLocale(it) },
            getDisplayName = { viewModel.getLocaleDisplayName(it) }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.general),
                scrollBehavior = scrollBehavior,
                onBackClick = onBackClick
            )
        },
        containerColor = animateColorAsState(MaterialTheme.colorScheme.surface, MaterialTheme.motionScheme.defaultEffectsSpec(), "surface").value,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ListSection(title = stringResource(R.string.appearance)) {
                val currentLocale = viewModel.getCurrentLocale()
                val currentLanguageDisplay = remember(currentLocale) {
                    currentLocale?.let { viewModel.getLocaleDisplayName(it) }
                }
                val theme by prefs.theme.getAsState()
                
                ThemeSelector(
                    currentTheme = theme,
                    onThemeSelected = { viewModel.setTheme(it) }
                )

                SettingsListItem(
                    headlineContent = stringResource(R.string.language),
                    supportingContent = stringResource(R.string.language_description),
                    onClick = { showLanguagePicker = true },
                    trailingContent = {
                        FilledTonalButton(onClick = { showLanguagePicker = true }) {
                            Text(
                                currentLanguageDisplay
                                    ?: stringResource(R.string.language_system_default)
                            )
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
                AnimatedVisibility(theme != Theme.LIGHT) {
                    BooleanItem(
                        preference = prefs.pureBlackTheme,
                        coroutineScope = coroutineScope,
                        headline = R.string.pure_black_theme,
                        description = R.string.pure_black_theme_description
                    )
                }
            }

            ListSection(title = stringResource(R.string.networking)) {
                BooleanItem(
                    preference = prefs.allowMeteredNetworks,
                    coroutineScope = coroutineScope,
                    headline = R.string.allow_metered_networks,
                    description = R.string.allow_metered_networks_description
                )
            }
        }
    }
}

@Composable
private fun LanguagePicker(
    supportedLocales: List<Locale>,
    currentLocale: Locale?,
    onDismiss: () -> Unit,
    onConfirm: (Locale?) -> Unit,
    getDisplayName: (Locale) -> String
) {
    var selectedLocale by remember { mutableStateOf(currentLocale) }
    val systemDefaultString = stringResource(R.string.language_system_default)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLocale = null },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HapticRadioButton(
                        selected = selectedLocale == null,
                        onClick = { selectedLocale = null }
                    )
                    Text(systemDefaultString)
                }

                supportedLocales.forEach { locale ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLocale = locale },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HapticRadioButton(
                            selected = selectedLocale == locale,
                            onClick = { selectedLocale = locale }
                        )
                        Text(getDisplayName(locale))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedLocale)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    )
}