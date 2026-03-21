package app.revanced.manager.ui.screen

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.ui.component.AppScaffold
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.InstallerStatusDialog
import app.revanced.manager.ui.component.ShareSheet
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.haptics.HapticExtendedFloatingActionButton
import app.revanced.manager.ui.component.patcher.AdbSetupDialog
import app.revanced.manager.ui.component.patcher.InstallPickerDialog
import app.revanced.manager.ui.component.patcher.Steps
import app.revanced.manager.ui.model.StepCategory
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PatcherScreen(
    onBackClick: () -> Unit,
    viewModel: PatcherViewModel
) {
    fun onLeave() {
        viewModel.onBack()
        onBackClick()
    }

    val context = LocalContext.current
    val resources = LocalResources.current
    var showLogExportSheet by rememberSaveable { mutableStateOf(false) }
    val exportApkLauncher = rememberLauncherForActivityResult(CreateDocument(APK_MIMETYPE), viewModel::export)
    val saveLogsLauncher = rememberLauncherForActivityResult(CreateDocument("text/plain")) { uri ->
            viewModel.saveLogs(uri)
            showLogExportSheet = false
            viewModel.clearPreparedLogExport()
        }

    val patcherSucceeded by viewModel.patcherSucceeded.observeAsState(null)
    val canInstall by remember { derivedStateOf { patcherSucceeded == true && (viewModel.installedPackageName != null || !viewModel.isInstalling) } }
    var showInstallPicker by rememberSaveable { mutableStateOf(false) }
    var showAdbSetupDialog by rememberSaveable { mutableStateOf(false) }
    var showDismissConfirmationDialog by rememberSaveable { mutableStateOf(false) }
 
    val installTypes by remember {
        derivedStateOf {
            buildList {
                add(InstallType.DEFAULT)
                if (viewModel.isDeviceRooted()) add(InstallType.MOUNT)
                if (viewModel.isShizukuAvailable()) add(InstallType.SHIZUKU)
                add(InstallType.ADB)
            }
        }
    }

    fun onPageBack() = when {
        patcherSucceeded == null -> showDismissConfirmationDialog = true
        viewModel.isInstalling -> context.toast(resources.getString(R.string.patcher_install_in_progress))
        else -> onLeave()
    }

    BackHandler(onBack = ::onPageBack)

    val steps by remember {
        derivedStateOf {
            viewModel.steps.groupBy { it.category }.toList()
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
            installTypes = installTypes,
            isAdbConnected = viewModel.isAdbConnected,
            onRefreshAdb = { showAdbSetupDialog = true },
            onDismiss = { showInstallPicker = false },
            onConfirm = viewModel::install
        )

    if (showAdbSetupDialog) {
        AdbSetupDialog(
            isShizukuAuthorized = viewModel.isShizukuAvailable() && viewModel.isShizukuAvailable(), // Actually should check permission
            isAdbConnected = viewModel.isAdbConnected,
            isPairing = viewModel.isPairing,
            adbPort = viewModel.adbPort,
            adbPairingPort = viewModel.adbPairingPort,
            adbPairingCode = viewModel.adbPairingCode,
            onPortChange = { viewModel.adbPort = it },
            onPairingPortChange = { viewModel.adbPairingPort = it },
            onPairingCodeChange = { viewModel.adbPairingCode = it },
            onBootstrapAdb = viewModel::bootstrapAdb,
            onConnectAdb = viewModel::connectAdb,
            onPairAdb = viewModel::pairAdb,
            onDismiss = { showAdbSetupDialog = false }
        )
    }

    if (showDismissConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDismissConfirmationDialog = false },
            onConfirm = ::onLeave,
            title = stringResource(R.string.patcher_stop_confirm_title),
            description = stringResource(R.string.patcher_stop_confirm_description),
            icon = Icons.Outlined.Cancel
        )
    }

    if (showLogExportSheet) {
        ShareSheet(
            onDismissRequest = {
                showLogExportSheet = false
                viewModel.clearPreparedLogExport()
            },
            title = stringResource(R.string.export_patcher_logs),
            preview = viewModel.logPreviewText,
            shareUri = viewModel.preparedLogUri,
            onSaveToFilesClick = {
                saveLogsLauncher.launch(viewModel.logFileName())
            },
            onCopyToClipboard = {
                viewModel.copyLogs(context)
                showLogExportSheet = false
                viewModel.clearPreparedLogExport()
            }
        )
    }

    viewModel.packageInstallerStatus?.let {
        InstallerStatusDialog(it, viewModel, viewModel::dismissPackageInstallerDialog)
    }

    val activityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = viewModel::handleActivityResult
    )
    EventEffect(flow = viewModel.launchActivityFlow) { intent ->
        activityLauncher.launch(intent)
    }

    viewModel.activityPromptDialog?.let { title ->
        AlertDialog(
            onDismissRequest = viewModel::rejectInteraction,
            confirmButton = {
                TextButton(
                    onClick = viewModel::allowInteraction,
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.continue_))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::rejectInteraction,
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(title) },
            text = {
                Text(stringResource(R.string.downloader_activity_dialog_body))
            }
        )
    }

    AppScaffold(
        topBar = { scrollBehavior ->
            AppTopBar(
                title = when {
                    viewModel.isInstalling -> stringResource(R.string.installing)
                    patcherSucceeded == null -> stringResource(R.string.patching)
                    else -> stringResource(R.string.patcher)
                },
                scrollBehavior = scrollBehavior,
                onBackClick = ::onPageBack
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    TooltipIconButton(
                        onClick = { exportApkLauncher.launch("${viewModel.packageName}_${viewModel.version}_revanced_patched.apk") },
                        tooltip = stringResource(id = R.string.save_apk),
                        enabled = patcherSucceeded == true,
                    ) { contentDescription ->
                        Icon(Icons.Outlined.Save, contentDescription)
                    }
                    TooltipIconButton(
                        onClick = {
                            viewModel.prepareLogExport()
                            showLogExportSheet = true
                        },
                        tooltip = stringResource(id = R.string.save_logs),
                        enabled = patcherSucceeded != null,
                    ) { contentDescription ->
                        Icon(Icons.Outlined.PostAdd, contentDescription)
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(visible = canInstall) {
                        HapticExtendedFloatingActionButton(
                            tooltip = stringResource(
                                if (viewModel.installedPackageName == null) R.string.install_app else R.string.open_app
                            ),
                            text = {
                                Text(
                                    stringResource(if (viewModel.installedPackageName == null) R.string.install_app else R.string.open_app)
                                )
                            },
                            icon = {
                                viewModel.installedPackageName?.let {
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
                                if (viewModel.installedPackageName == null)
                                    if (installTypes.size > 1) showInstallPicker = true
                                    else viewModel.install(InstallType.DEFAULT)
                                else viewModel.open()
                            },
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp,
                            )
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
            var expandedCategory by rememberSaveable { mutableStateOf<StepCategory?>(null) }

            val expandCategory: (StepCategory?) -> Unit = { category ->
                expandedCategory = category
            }

            LinearWavyProgressIndicator(
                progress = { viewModel.progress },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(
                    items = steps,
                    key = { it.first }
                ) { (category, steps) ->
                    Steps(
                        category = category,
                        steps = steps,
                        isExpanded = expandedCategory == category,
                        onExpand = { expandCategory(category) },
                        onClick = {
                            expandCategory(if (expandedCategory == category) null else category)
                        }
                    )
                }
            }
        }
    }
}