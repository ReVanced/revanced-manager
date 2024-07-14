package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.GroupHeader
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.NonSuggestedVersionDialog
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.viewmodel.VersionSelectorViewModel
import app.revanced.manager.util.isScrollingUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionSelectorScreen(
    onBackClick: () -> Unit,
    onAppClick: (SelectedApp) -> Unit,
    viewModel: VersionSelectorViewModel
) {
    val supportedVersions by viewModel.supportedVersions.collectAsStateWithLifecycle(emptyMap())
    val downloadedVersions by viewModel.downloadedVersions.collectAsStateWithLifecycle(emptyList())

    val list by remember {
        derivedStateOf {
            val apps = (downloadedVersions + viewModel.downloadableVersions)
                .distinctBy { it.version }
                .sortedWith(
                    compareByDescending<SelectedApp> {
                        it is SelectedApp.Local
                    }.thenByDescending { supportedVersions[it.version] }
                        .thenByDescending { it.version }
                )

            viewModel.requiredVersion?.let { requiredVersion ->
                apps.filter { it.version == requiredVersion }
            } ?: apps
        }
    }

    if (viewModel.showNonSuggestedVersionDialog)
        NonSuggestedVersionDialog(
            suggestedVersion = viewModel.requiredVersion.orEmpty(),
            onDismiss = viewModel::dismissNonSuggestedVersionDialog
        )

    val lazyListState = rememberLazyListState()
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.select_version),
                onBackClick = onBackClick,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.select_version)) },
                icon = { Icon(Icons.Default.Check, null) },
                expanded = lazyListState.isScrollingUp,
                onClick = { viewModel.selectedVersion?.let(onAppClick) }
            )
        }
    ) { paddingValues ->
        LazyColumnWithScrollbar(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = lazyListState
        ) {
            viewModel.installedApp?.let { (packageInfo, installedApp) ->
                SelectedApp.Installed(
                    packageName = viewModel.packageName,
                    version = packageInfo.versionName
                ).let {
                    item {
                        SelectedAppItem(
                            selectedApp = it,
                            selected = viewModel.selectedVersion == it,
                            onClick = { viewModel.select(it) },
                            patchCount = supportedVersions[it.version],
                            enabled =
                            !(installedApp?.installType == InstallType.ROOT && !viewModel.rootInstaller.hasRootAccess()),
                            alreadyPatched = installedApp != null && installedApp.installType != InstallType.ROOT
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth()) {
                    GroupHeader(stringResource(R.string.downloadable_versions))
                }
            }

            items(
                items = list,
                key = { it.version }
            ) {
                SelectedAppItem(
                    selectedApp = it,
                    selected = viewModel.selectedVersion == it,
                    onClick = { viewModel.select(it) },
                    patchCount = supportedVersions[it.version]
                )
            }

            if (viewModel.errorMessage != null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.error_occurred))
                        Text(
                            text = viewModel.errorMessage!!,
                            modifier = Modifier.padding(horizontal = 15.dp)
                        )
                    }
                }
            } else if (viewModel.isLoading) {
                item {
                    LoadingIndicator()
                }
            }
        }
    }
}

@Composable
fun SelectedAppItem(
    selectedApp: SelectedApp,
    selected: Boolean,
    onClick: () -> Unit,
    patchCount: Int?,
    enabled: Boolean = true,
    alreadyPatched: Boolean = false,
) {
    ListItem(
        leadingContent = { RadioButton(selected, null) },
        headlineContent = { Text(selectedApp.version) },
        supportingContent = when (selectedApp) {
            is SelectedApp.Installed ->
                if (alreadyPatched) {
                    { Text(stringResource(R.string.already_patched)) }
                } else {
                    { Text(stringResource(R.string.installed)) }
                }

            is SelectedApp.Local -> {
                { Text(stringResource(R.string.already_downloaded)) }
            }

            else -> null
        },
        trailingContent = patchCount?.let {
            {
                Text(pluralStringResource(R.plurals.patch_count, it, it))
            }
        },
        modifier = Modifier
            .clickable(enabled = !alreadyPatched && enabled, onClick = onClick)
            .run {
                if (!enabled || alreadyPatched) alpha(0.5f)
                else this
            }
    )
}