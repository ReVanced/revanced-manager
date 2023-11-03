package app.revanced.manager.ui.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.patcher.worker.State
import app.revanced.manager.patcher.worker.Step
import app.revanced.manager.ui.component.AppScaffold
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ArrowButton
import app.revanced.manager.ui.viewmodel.InstallerViewModel
import app.revanced.manager.util.APK_MIMETYPE
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallerScreen(
    onBackClick: () -> Unit,
    vm: InstallerViewModel
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val exportApkLauncher =
        rememberLauncherForActivityResult(CreateDocument(APK_MIMETYPE), vm::export)
    val patcherState by vm.patcherState.observeAsState(null)
    val steps by vm.progress.collectAsStateWithLifecycle()
    val canInstall by remember { derivedStateOf { patcherState == true && (vm.installedPackageName != null || !vm.isInstalling) } }
    var showInstallPicker by rememberSaveable { mutableStateOf(false) }

    if (showInstallPicker)
        InstallPicker(
            onDismiss = { showInstallPicker = false },
            onConfirm = { vm.install(it) }
        )

    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.installer),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            AnimatedVisibility(patcherState != null) {
                BottomAppBar(
                    actions = {
                        if (canInstall) {
                            IconButton(onClick = { exportApkLauncher.launch("${vm.packageName}.apk") }) {
                                Icon(Icons.Outlined.Save, stringResource(id = R.string.save_apk))
                            }
                        }
                        IconButton(onClick = { vm.exportLogs(context) }) {
                            Icon(Icons.Outlined.PostAdd, stringResource(id = R.string.save_logs))
                        }
                    },
                    floatingActionButton = {
                        if (canInstall) {
                            ExtendedFloatingActionButton(
                                text = { Text(stringResource(vm.appButtonText)) },
                                icon = { Icon(Icons.Outlined.FileDownload, stringResource(id = R.string.install_app)) },
                                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                                onClick = {
                                    if (vm.installedPackageName == null)
                                        showInstallPicker = true
                                    else
                                        vm.open()
                                }
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            steps.forEach {
                InstallStep(it)
            }
        }
    }
}

@Composable
fun InstallPicker(
    onDismiss: () -> Unit,
    onConfirm: (InstallType) -> Unit
) {
    var selectedInstallType by rememberSaveable { mutableStateOf(InstallType.DEFAULT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedInstallType)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.install_app))
            }
        },
        title = { Text(stringResource(R.string.select_install_type)) },
        text = {
            Column {
                InstallType.values().forEach {
                    ListItem(
                        modifier = Modifier.clickable { selectedInstallType = it },
                        leadingContent = {
                            RadioButton(
                                selected = selectedInstallType == it,
                                onClick = null
                            )
                        },
                        headlineContent = { Text(stringResource(it.stringResource)) }
                    )
                }
            }
        }
    )
}


// Credits: https://github.com/Aliucord/AliucordManager/blob/main/app/src/main/kotlin/com/aliucord/manager/ui/component/installer/InstallGroup.kt

@Composable
fun InstallStep(step: Step) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .run {
                if (expanded) {
                    background(MaterialTheme.colorScheme.secondaryContainer)
                } else this
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
                .background(if (expanded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
        ) {
            StepIcon(step.state, size = 24.dp)

            Text(text = stringResource(step.name), style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.weight(1f))

            ArrowButton(modifier = Modifier.size(24.dp), expanded = expanded) {
                expanded = !expanded
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background.copy(0.6f))
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                step.subSteps.forEach { subStep ->
                    var messageExpanded by rememberSaveable { mutableStateOf(true) }
                    val stacktrace = subStep.message
                    val downloadProgress = subStep.progress?.collectAsStateWithLifecycle()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StepIcon(subStep.state, downloadProgress?.value, size = 24.dp)

                        Text(
                            text = subStep.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, true),
                        )

                        if (stacktrace != null) {
                            ArrowButton(
                                modifier = Modifier.size(24.dp),
                                expanded = messageExpanded
                            ) {
                                messageExpanded = !messageExpanded
                            }
                        } else {
                            downloadProgress?.value?.let { (downloaded, total) ->
                                Text(
                                    "$downloaded/$total MB",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = messageExpanded && stacktrace != null) {
                        Text(
                            text = stacktrace ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepIcon(status: State, downloadProgress: Pair<Float, Float>? = null, size: Dp) {
    val strokeWidth = Dp(floor(size.value / 10) + 1)

    when (status) {
        State.COMPLETED -> Icon(
            Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.step_completed),
            tint = MaterialTheme.colorScheme.surfaceTint,
            modifier = Modifier.size(size)
        )

        State.FAILED -> Icon(
            Icons.Filled.Cancel,
            contentDescription = stringResource(R.string.step_failed),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(size)
        )

        State.WAITING ->
            downloadProgress?.let { (downloaded, total) ->
                CircularProgressIndicator(
                    progress = downloaded / total,
                    strokeWidth = strokeWidth,
                    modifier = stringResource(R.string.step_running).let { description ->
                        Modifier
                            .size(size)
                            .semantics {
                                contentDescription = description
                            }
                    }
                )
            } ?: CircularProgressIndicator(
                strokeWidth = strokeWidth,
                modifier = stringResource(R.string.step_running).let { description ->
                    Modifier
                        .size(size)
                        .semantics {
                            contentDescription = description
                        }
                }
            )
    }
}
