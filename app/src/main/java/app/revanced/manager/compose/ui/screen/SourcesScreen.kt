package app.revanced.manager.compose.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.sources.NewSourceDialog
import app.revanced.manager.compose.ui.component.sources.SourceItem
import app.revanced.manager.compose.ui.viewmodel.SourcesScreenViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@Composable
fun SourcesScreen(vm: SourcesScreenViewModel = getViewModel()) {
    var showNewSourceDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val sources by vm.sources.collectAsStateWithLifecycle()

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
            .fillMaxWidth(),
    ) {
        sources.forEach { (name, source) ->
            SourceItem(
                name = name,
                source = source,
                onDelete = {
                    vm.deleteSource(source)
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