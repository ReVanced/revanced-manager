package app.revanced.manager.ui.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.OpenInNew
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
import androidx.compose.ui.graphics.Color
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
import app.revanced.manager.ui.component.AppScaffold
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ArrowButton
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.model.State
import app.revanced.manager.ui.model.Step
import app.revanced.manager.ui.model.StepCategory
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import app.revanced.manager.util.APK_MIMETYPE
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatcherScreen(
    onBackClick: () -> Unit,
    vm: PatcherViewModel
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val exportApkLauncher =
        rememberLauncherForActivityResult(CreateDocument(APK_MIMETYPE), vm::export)

    val patcherSucceeded by vm.patcherSucceeded.observeAsState(null)
    val canInstall by remember { derivedStateOf { patcherSucceeded == true && (vm.installedPackageName != null || !vm.isInstalling) } }
    var showInstallPicker by rememberSaveable { mutableStateOf(false) }

    val steps by remember {
        derivedStateOf {
            vm.steps.groupBy { it.category }
        }
    }

    if (showInstallPicker)
        InstallPickerDialog(
            onDismiss = { showInstallPicker = false },
            onConfirm = vm::install
        )

    AppScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.patcher),
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(
                        onClick = { exportApkLauncher.launch("${vm.packageName}.apk") },
                        enabled = canInstall
                    ) {
                        Icon(Icons.Outlined.Save, stringResource(id = R.string.save_apk))
                    }
                    IconButton(
                        onClick = { vm.exportLogs(context) },
                        enabled = patcherSucceeded != null
                    ) {
                        Icon(Icons.Outlined.PostAdd, stringResource(id = R.string.save_logs))
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(visible = canInstall) {
                        ExtendedFloatingActionButton(
                            text = {
                                Text(
                                    stringResource(if (vm.installedPackageName == null) R.string.install_app else R.string.open_app)
                                )
                            },
                            icon = {
                                vm.installedPackageName?.let {
                                    Icon(
                                        Icons.Outlined.OpenInNew,
                                        stringResource(R.string.open_app)
                                    )
                                } ?: Icon(
                                    Icons.Outlined.FileDownload,
                                    stringResource(R.string.install_app)
                                )
                            },
                            onClick = {
                                if (vm.installedPackageName == null)
                                    showInstallPicker = true
                                else vm.open()
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LinearProgressIndicator(
                progress = {
                    steps.flatMap { it.value }.let {
                        it.count { step -> step.state == State.COMPLETED }.toFloat() / it.size.toFloat()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    steps.forEach { (category, steps) ->
                        Steps(category, steps)
                    }
                }
            }
        }
    }
}

// Credits: https://github.com/Aliucord/AliucordManager/blob/main/app/src/main/kotlin/com/aliucord/manager/ui/component/installer/InstallGroup.kt
@Composable
fun Steps(category: StepCategory, steps: List<Step>) {
    val context = LocalContext.current

    var expanded by rememberSaveable { mutableStateOf(true) }

    val categoryColor by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
        label = "category"
    )

    val cardColor by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        label = "card"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(cardColor)
    ) {

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { expanded = !expanded }
                .background(categoryColor)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                val state = remember(steps) {
                    when {
                        steps.all { it.state == State.COMPLETED } -> State.COMPLETED
                        steps.any { it.state == State.FAILED } -> State.FAILED
                        steps.any { it.state == State.RUNNING } -> State.RUNNING
                        else -> State.WAITING
                    }
                }

                StepIcon(
                    state = state,
                    size = 24.dp
                )

                Text(
                    stringResource(category.displayName),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    "${steps.count { it.state == State.COMPLETED }}/${steps.size}",
                    style = MaterialTheme.typography.labelSmall
                )

                ArrowButton(modifier = Modifier.size(24.dp), expanded = expanded, onClick = null)
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (category != StepCategory.PATCHING) {
                    steps.forEach { step ->
                        val downloadProgress = step.downloadProgress?.collectAsStateWithLifecycle()

                        SubStep(
                            name = step.name,
                            state = step.state,
                            message = step.message,
                            downloadProgress = downloadProgress?.value
                        )
                    }
                } else {
                    val currentPatch = remember(steps) {
                        steps.find { it.state == State.RUNNING }?.name
                    }
                    val failedPatch = remember(steps) {
                        steps.find { it.state == State.FAILED }?.name
                    }

                    val name = remember(steps) {
                        when {
                            steps.any { it.state == State.RUNNING } -> context.getString(R.string.applying_patch, currentPatch)
                            steps.any { it.state == State.FAILED } -> context.getString(R.string.failed_to_apply_patch, failedPatch)
                            steps.all { it.state == State.WAITING } -> context.getString(R.string.apply_patches)
                            else -> context.resources.getQuantityString(R.plurals.patches_applied, steps.size, steps.size)
                        }
                    }

                    val state = remember(steps) {
                        when {
                            steps.all { it.state == State.COMPLETED } -> State.COMPLETED
                            steps.any { it.state == State.FAILED } -> State.FAILED
                            steps.any { it.state == State.RUNNING } -> State.RUNNING
                            else -> State.WAITING
                        }
                    }

                    SubStep(
                        name = name,
                        state = state,
                        message = steps.find { it.state == State.FAILED }?.message
                    )
                }
            }
        }
    }
}

@Composable
fun SubStep(
    name: String,
    state: State,
    message: String? = null,
    downloadProgress: Pair<Float, Float>? = null
) {
    var messageExpanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .run {
                if (message != null)
                    clickable { messageExpanded = !messageExpanded }
                else this
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                StepIcon(state, downloadProgress, size = 20.dp)
            }

            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, true),
            )

            if (message != null) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ArrowButton(
                        modifier = Modifier.size(20.dp),
                        expanded = messageExpanded,
                        onClick = null
                    )
                }
            } else {
                downloadProgress?.let { (current, total) ->
                    Text(
                        "$current/$total MB",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        AnimatedVisibility(visible = messageExpanded && message != null) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 52.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun StepIcon(state: State, progress: Pair<Float, Float>? = null, size: Dp) {
    val strokeWidth = Dp(floor(size.value / 10) + 1)

    when (state) {
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

        State.WAITING -> Icon(
            Icons.Outlined.Circle,
            contentDescription = stringResource(R.string.step_waiting),
            tint = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(size)
        )
        
        State.RUNNING ->
            LoadingIndicator(
                modifier = stringResource(R.string.step_running).let { description ->
                    Modifier
                        .size(size)
                        .semantics {
                            contentDescription = description
                        }
                },
                progress = { progress?.let { (current, total) -> current / total } },
                strokeWidth = strokeWidth
            )
    }
}

@Composable
fun InstallPickerDialog(
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