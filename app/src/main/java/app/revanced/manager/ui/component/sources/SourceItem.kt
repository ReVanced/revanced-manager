package app.revanced.manager.ui.component.sources

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.sources.LocalSource
import app.revanced.manager.domain.sources.RemoteSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.ui.viewmodel.SourcesViewModel
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceItem(source: Source, onDelete: () -> Unit, coroutineScope: CoroutineScope) {
    val composableScope = rememberCoroutineScope()
    var sheetActive by rememberSaveable { mutableStateOf(false) }

    val bundle by source.bundle.collectAsStateWithLifecycle()
    val patchCount = bundle.patches.size
    val padding = PaddingValues(16.dp, 0.dp)

    if (sheetActive) {
        val modalSheetState = rememberModalBottomSheetState(
            confirmValueChange = { it != SheetValue.PartiallyExpanded },
            skipPartiallyExpanded = true
        )

        ModalBottomSheet(
            sheetState = modalSheetState,
            onDismissRequest = { sheetActive = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleLarge
                )

                when (source) {
                    is RemoteSource -> RemoteSourceItem(source, coroutineScope)
                    is LocalSource -> LocalSourceItem(source, coroutineScope)
                }

                Button(
                    onClick = {
                        composableScope.launch {
                            modalSheetState.hide()
                            sheetActive = false
                            onDelete()
                        }
                    }
                ) {
                    Text("Delete this source")
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .clickable {
                sheetActive = true
            }
    ) {
        Text(
            text = source.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(padding)
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Text(
            text = pluralStringResource(R.plurals.patches_count, patchCount, patchCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun RemoteSourceItem(source: RemoteSource, coroutineScope: CoroutineScope) {
    val androidContext = LocalContext.current
    Text(text = "(api url here)")

    Button(onClick = {
        coroutineScope.launch {
            uiSafe(androidContext, R.string.source_download_fail, SourcesViewModel.failLogMsg) {
                source.update()
            }
        }
    }) {
        Text(text = "Check for updates")
    }
}

@Composable
private fun LocalSourceItem(source: LocalSource, coroutineScope: CoroutineScope) {
    val androidContext = LocalContext.current
    val resolver = remember { androidContext.contentResolver!! }

    fun loadAndReplace(
        uri: Uri,
        @StringRes toastMsg: Int,
        errorLogMsg: String,
        callback: suspend (InputStream) -> Unit
    ) = coroutineScope.launch {
        uiSafe(androidContext, toastMsg, errorLogMsg) {
            resolver.openInputStream(uri)!!.use {
                callback(it)
            }
        }
    }

    LocalBundleSelectors(
        onPatchesSelection = { uri ->
            loadAndReplace(uri, R.string.source_replace_fail, "Failed to replace patch bundle") {
                source.replace(it, null)
            }
        },
        onIntegrationsSelection = { uri ->
            loadAndReplace(
                uri,
                R.string.source_replace_integrations_fail,
                "Failed to replace integrations"
            ) {
                source.replace(null, it)
            }
        }
    )
}