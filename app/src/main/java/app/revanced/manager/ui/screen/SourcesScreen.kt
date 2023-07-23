package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.ui.component.bundle.ImportBundleDialog
import app.revanced.manager.ui.component.SourceItem
import app.revanced.manager.ui.viewmodel.SourcesViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun SourcesScreen(
    vm: SourcesViewModel = getViewModel(),
) {
    var showNewSourceDialog by rememberSaveable { mutableStateOf(false) }
    val sources by vm.sources.collectAsStateWithLifecycle(initialValue = emptyList())

    if (showNewSourceDialog) {
        ImportBundleDialog(
            onDismissRequest = { showNewSourceDialog = false },
            onLocalSubmit = { name, patches, integrations ->
                showNewSourceDialog = false
                vm.addLocal(name, patches, integrations)
            },
            onRemoteSubmit = { name, url ->
                showNewSourceDialog = false
                vm.addRemote(name, url)
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        sources.forEach {
            SourceItem(
                source = it,
                onDelete = {
                    vm.delete(it)
                },
                coroutineScope = vm.viewModelScope
            )
        }

        Button(onClick = vm::redownloadAllSources) {
            Text(stringResource(R.string.reload_sources))
        }

        Button(onClick = { showNewSourceDialog = true }) {
            Text("Create new source")
        }

        Button(onClick = vm::deleteAllSources) {
            Text("Reset everything.")
        }
    }
}