package app.revanced.manager.ui.screens

import ExpandingText
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.components.LoadingIndicator
import app.revanced.manager.ui.components.PatchCompatibilityDialog
import app.revanced.manager.ui.screens.mainsubscreens.PatchClass
import app.revanced.manager.ui.screens.mainsubscreens.PatcherViewModel
import app.revanced.manager.ui.theme.Typography
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.version
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("QueryPermissionsNeeded", "UnrememberedMutableState")
@Destination
@RootNavGraph
@Composable
fun PatchesSelectorScreen(
    navigator: NavController,
    pvm: PatcherViewModel = viewModel(LocalContext.current as ComponentActivity)
) {
    val patches = rememberSaveable { pvm.getFilteredPatches() }
    val patchesState by pvm.patches
    var query by mutableStateOf("")

    when (patchesState) {
        is Resource.Success -> {
            Scaffold(floatingActionButton = {
                if (pvm.anyPatchSelected()) {
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Default.Check, "Done") },
                        text = { Text("Done") },
                        onClick = { navigator.navigateUp() },
                    )
                }
            }) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp, 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                value = query,
                                onValueChange = {
                                        newValue -> query = newValue
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, "Search")
                                },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = {
                                            query = ""
                                        }) {
                                            Icon(Icons.Default.Clear, "Clear")
                                        }
                                    }
                                },
                            )
                        }
                    }
                        LazyColumn {

                        if (query.isEmpty() || query.isBlank()) {
                            items(count = patches.size) {
                                val patch = patches[it]
                                val name = patch.patch.patchName
                                PatchSelectable(patch, pvm.isPatchSelected(name)) {
                                    pvm.selectPatch(name, !pvm.isPatchSelected(name))
                                }
                            }
                        } else {
                            items(count = patches.size) {
                                val patch = patches[it]
                                val name = patch.patch.patchName
                                if (name.contains(query.lowercase())) {
                                    PatchSelectable(patch, pvm.isPatchSelected(name)) {
                                        pvm.selectPatch(name, !pvm.isPatchSelected(name))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> LoadingIndicator(R.string.loading_fetching_patches)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchSelectable(patchClass: PatchClass, isSelected: Boolean, onSelected: () -> Unit) {
    val patch = patchClass.patch
    val name = patch.patchName

    var showDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .padding(16.dp, 4.dp),
        onClick = { onSelected() }
    ) {
        Column(modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 12.dp)) {
            Row {
                Column(
                    Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = name.replace("-", " ").split(" ")
                            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
//                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.width(4.dp))
                Row(
                    Modifier
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        text  = patch.version ?: "unknown",
                        style = Typography.bodySmall
                    )
                }
                Spacer(Modifier.weight(1f, true))
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onSelected() }
                            )
                        }
                    }
                }
            }
            patch.description?.let { desc ->
                ExpandingText(
                    text = desc,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            Column {
                Row {
                    if (showDialog) {
                        PatchCompatibilityDialog(onClose = { showDialog = false }, patchClass = patchClass)
                    }
                    InputChip(
                        onClick = { showDialog = true },
                        leadingIcon = {
                            if (patchClass.unsupported) Icon(
                                Icons.Default.Warning,
//                                tint = Color.Yellow,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = stringResource(id = R.string.unsupported_version)
                            ) else Icon(
                                Icons.Default.Info,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = stringResource(id = R.string.version_info)
                            )
                        },
                        label = { Text(stringResource(id = R.string.supported_versions)) }
                    )
                }
            }
        }
    }
}