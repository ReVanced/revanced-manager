package app.revanced.manager.ui.component


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.sources.RemoteSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.ui.component.bundle.BundleInformationDialog
import app.revanced.manager.ui.viewmodel.SourcesViewModel
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SourceItem(
    source: Source, onDelete: () -> Unit,
    coroutineScope: CoroutineScope,
) {
    var viewBundleDialogPage by rememberSaveable { mutableStateOf(false) }

    val bundle by source.bundle.collectAsStateWithLifecycle()
    val patchCount = bundle.patches.size
    val padding = PaddingValues(16.dp, 0.dp)

    val androidContext = LocalContext.current

    if (viewBundleDialogPage) {
        BundleInformationDialog(
            onDismissRequest = { viewBundleDialogPage = false },
            onDeleteRequest = {
                viewBundleDialogPage = false
                onDelete()
            },
            source = source,
            patchCount = patchCount,
            onRefreshButton = {
                coroutineScope.launch {
                    uiSafe(
                        androidContext,
                        R.string.source_download_fail,
                        SourcesViewModel.failLogMsg
                    ) {
                        if (source is RemoteSource) source.update()
                    }
                }
            },
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .clickable {
                viewBundleDialogPage = true
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