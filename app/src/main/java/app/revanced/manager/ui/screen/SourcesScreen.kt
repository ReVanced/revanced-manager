package app.revanced.manager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.sources.NewSourceDialog
import app.revanced.manager.ui.component.sources.SourceItem
import app.revanced.manager.ui.viewmodel.SourcesViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun SourcesScreen(vm: SourcesViewModel = getViewModel()) {
    var showNewSourceDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val sources by vm.sources.collectAsStateWithLifecycle(initialValue = emptyList())

    if (showNewSourceDialog) NewSourceDialog(
        onDismissRequest = { showNewSourceDialog = false },
        onLocalSubmit = { name, patches, integrations ->
            showNewSourceDialog = false
            scope.launch {
                vm.addLocal(name, patches, integrations)
            }
        },
        onRemoteSubmit = { name, url ->
            showNewSourceDialog = false
            scope.launch {
                vm.addRemote(name, url)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        sources.forEach {
            SourceItem(
                source = it,
                onDelete = {
                    vm.delete(it)
                }
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