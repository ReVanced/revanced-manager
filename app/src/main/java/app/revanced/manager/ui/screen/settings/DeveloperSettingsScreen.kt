package app.revanced.manager.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.DeveloperOptionsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperSettingsScreen(
    onBackClick: () -> Unit,
    vm: DeveloperOptionsViewModel = koinViewModel()
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = {
            scrollState.canScrollBackward || scrollState.canScrollForward
        }
    )
    val prefs: PreferencesManager = koinInject()

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.developer_options)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.then(
            scrollBehavior.let { Modifier.nestedScroll(it.nestedScrollConnection) }
        ),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier.padding(paddingValues),
            state = scrollState
        ) {
            ListSection(
                title = stringResource(R.string.manager),
                leadingContent = { Icon(Icons.Outlined.WorkOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                val apiUrl by vm.prefs.api.getAsState()
                var showApiUrlDialog by rememberSaveable { mutableStateOf(false) }

                if (showApiUrlDialog) {
                    APIUrlDialog(
                        currentUrl = apiUrl,
                        defaultUrl = vm.prefs.api.default,
                        onSubmit = {
                            showApiUrlDialog = false
                            it?.let(vm::setApiUrl)
                        }
                    )
                }

                BooleanItem(
                    preference = prefs.showDeveloperSettings,
                    headline = R.string.developer_options,
                    description = R.string.developer_options_description,
                )
                SettingsListItem(
                    headlineContent = stringResource(R.string.api_url),
                    supportingContent = stringResource(R.string.api_url_description),
                    onClick = { showApiUrlDialog = true }
                )
                SettingsListItem(
                    headlineContent = stringResource(R.string.reset_onboarding),
                    supportingContent = stringResource(R.string.reset_onboarding_description),
                    onClick = vm::resetOnboarding
                )
            }

            ListSection(
                title = stringResource(R.string.patches),
                leadingContent = { Icon(Icons.Outlined.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp)) }
            ) {
                SettingsListItem(
                    headlineContent = stringResource(R.string.patches_force_download),
                    onClick = vm::redownloadBundles
                )
                SettingsListItem(
                    headlineContent = stringResource(R.string.patches_reset),
                    onClick = vm::redownloadBundles
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun APIUrlDialog(currentUrl: String, defaultUrl: String, onSubmit: (String?) -> Unit) {
    var url by rememberSaveable(currentUrl) { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = { onSubmit(null) },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(url)
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.api_url_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onSubmit(null) }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
        icon = {
            Icon(Icons.Outlined.Api, null)
        },
        title = {
            Text(
                text = stringResource(R.string.api_url_dialog_title),
                style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.api_url_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.api_url_dialog_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.api_url)) },
                    trailingIcon = {
                        IconButton(onClick = { url = defaultUrl }, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Outlined.Restore, stringResource(R.string.api_url_dialog_reset))
                        }
                    }
                )
            }
        }
    )
}