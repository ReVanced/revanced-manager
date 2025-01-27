package app.revanced.manager.ui.screen

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.ui.component.AppScaffold
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.InstallerStatusDialog
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.patcher.InstallPickerDialog
import app.revanced.manager.ui.component.patcher.Steps
import app.revanced.manager.ui.model.StepCategory
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.EventEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatcherScreen(
    onBackClick: () -> Unit,
    vm: PatcherViewModel
) {
    fun leaveScreen() {
        vm.onBack()
        onBackClick()
    }
    BackHandler(onBack = ::leaveScreen)

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

    if (patcherSucceeded == null) {
        DisposableEffect(Unit) {
            val window = (context as Activity).window
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    if (showInstallPicker)
        InstallPickerDialog(
            onDismiss = { showInstallPicker = false },
            onConfirm = vm::install
        )

    vm.packageInstallerStatus?.let {
        InstallerStatusDialog(it, vm, vm::dismissPackageInstallerDialog)
    }

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = vm::handleActivityResult
    )
    EventEffect(flow = vm.launchActivityFlow) { intent ->
        activityLauncher.launch(intent)
    }

    vm.activityPromptDialog?.let { title ->
        AlertDialog(
            onDismissRequest = vm::rejectInteraction,
            confirmButton = {
                TextButton(
                    onClick = vm::allowInteraction
                ) {
                    Text(stringResource(R.string.continue_))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = vm::rejectInteraction
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(title) },
            text = {
                Text(stringResource(R.string.plugin_activity_dialog_body))
            }
        )
    }

    AppScaffold(
        topBar = { scrollBehavior ->
            AppTopBar(
                title = stringResource(R.string.patcher),
                scrollBehavior = scrollBehavior,
                onBackClick = ::leaveScreen
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(
                        onClick = { exportApkLauncher.launch("${vm.packageName}.apk") },
                        enabled = patcherSucceeded == true
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
                        HapticExtendedFloatingActionButton(
                            text = {
                                Text(
                                    stringResource(if (vm.installedPackageName == null) R.string.install_app else R.string.open_app)
                                )
                            },
                            icon = {
                                vm.installedPackageName?.let {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.OpenInNew,
                                        stringResource(R.string.open_app)
                                    )
                                } ?: Icon(
                                    Icons.Outlined.FileDownload,
                                    stringResource(R.string.install_app)
                                )
                            },
                            onClick = {
                                if (vm.installedPackageName == null)
                                    if (vm.isDeviceRooted()) showInstallPicker = true
                                    else vm.install(InstallType.DEFAULT)
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
                progress = { vm.progress },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(
                    items = steps.toList(),
                    key = { it.first }
                ) { (category, steps) ->
                    Steps(
                        category = category,
                        steps = steps,
                        stepCount = if (category == StepCategory.PATCHING) vm.patchesProgress else null,
                        stepProgressProvider = vm
                    )
                }
            }
        }
    }
}
