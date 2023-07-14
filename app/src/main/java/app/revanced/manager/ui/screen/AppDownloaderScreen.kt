package app.revanced.manager.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.viewmodel.AppDownloaderViewModel
import app.revanced.manager.util.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDownloaderScreen(
    onBackClick: () -> Unit,
    onApkClick: (AppInfo) -> Unit,
    viewModel: AppDownloaderViewModel
) {
    SideEffect {
        viewModel.onComplete = onApkClick
    }

    val downloadProgress by viewModel.appDownloader.downloadProgress.collectAsStateWithLifecycle()
    val compatibleVersions by viewModel.compatibleVersions.collectAsStateWithLifecycle(emptyMap())
    val downloadedVersions by viewModel.downloadedVersions.collectAsStateWithLifecycle(emptyList())

    val list by remember {
        derivedStateOf {
            (downloadedVersions + viewModel.availableVersions)
                .distinct()
                .sortedWith(
                    compareByDescending<String> {
                        downloadedVersions.contains(it)
                    }.thenByDescending { compatibleVersions[it] }
                        .thenByDescending { it }
                )
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.select_version),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.HelpOutline, stringResource(R.string.help))
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Search, stringResource(R.string.search))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                !viewModel.isDownloading && list.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        list.forEach { version ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    viewModel.downloadApp(version)
                                },
                                headlineContent = { Text(version) },
                                supportingContent =
                                    if (downloadedVersions.contains(version)) {
                                        { Text(stringResource(R.string.already_downloaded)) }
                                    } else null,
                                trailingContent = compatibleVersions[version]?.let {
                                    {
                                        Text(
                                            pluralStringResource(
                                                R.plurals.patches_count,
                                                count = it,
                                                it
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        if (viewModel.errorMessage != null) {
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
                        } else if (viewModel.isLoading)
                            LoadingIndicator()
                    }
                }

                viewModel.errorMessage != null -> {
                    Text(stringResource(R.string.error_occurred))
                    Text(
                        text = viewModel.errorMessage!!,
                        modifier = Modifier.padding(horizontal = 15.dp)
                    )
                }

                else -> {
                    LoadingIndicator(
                        progress = downloadProgress?.let { (it.first / it.second) },
                        text = downloadProgress?.let { stringResource(R.string.downloading_app, it.first, it.second) }
                    )
                }
            }
        }
    }
}