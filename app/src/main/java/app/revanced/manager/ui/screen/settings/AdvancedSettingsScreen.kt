package app.revanced.manager.ui.screen.settings

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MediumFlexibleTopAppBar
import app.revanced.manager.ui.component.haptics.HapticRadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import app.revanced.manager.BuildConfig
import app.revanced.manager.R
import app.revanced.manager.patcher.logger.LogLevel
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.settings.BooleanItem
import app.revanced.manager.ui.component.settings.IntegerItem
import app.revanced.manager.ui.component.settings.SafeguardBooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.AdvancedSettingsViewModel
import app.revanced.manager.util.toast
import app.revanced.manager.util.transparentListItemColors
import app.revanced.manager.util.withHapticFeedback
import androidx.annotation.StringRes
import app.revanced.manager.patcher.logger.displayName
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdvancedSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: AdvancedSettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val memoryLimit = remember(resources) {
        val activityManager = context.getSystemService<ActivityManager>()!!
        resources.getString(
            R.string.device_memory_limit_format,
            activityManager.memoryClass,
            activityManager.largeMemoryClass
        )
    }
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = {
            scrollState.canScrollBackward || scrollState.canScrollForward
        }
    )

    val showDeveloperSettings by viewModel.prefs.showDeveloperSettings.getAsState()
    var developerTaps by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(developerTaps) {
        if (developerTaps < 10) return@LaunchedEffect

        if (showDeveloperSettings) {
            context.toast(context.getString(R.string.developer_options_already_enabled))
        } else {
            viewModel.prefs.showDeveloperSettings.update(true)
            context.toast(context.getString(R.string.developer_options_enabled))
        }
        developerTaps = 0
    }

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.advanced)) },
                navigationIcon = {
                    TooltipIconButton(
                        onClick = onBackClick,
                        tooltip = stringResource(R.string.back)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.then(
            scrollBehavior.let { Modifier.nestedScroll(it.nestedScrollConnection) }
        ),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = scrollState
        ) {
            ListSection(
                title = stringResource(R.string.safeguards),
                leadingContent = {
                    Icon(
                        Icons.Outlined.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) {
                SafeguardBooleanItem(
                    preference = viewModel.prefs.disablePatchVersionCompatCheck,
                    coroutineScope = viewModel.viewModelScope,
                    headline = R.string.patch_compat_check,
                    description = R.string.patch_compat_check_description,
                dialogTitle = R.string.patch_compat_check_title,
                    confirmationText = R.string.patch_compat_check_confirmation
                )
                SafeguardBooleanItem(
                    preference = viewModel.prefs.suggestedVersionSafeguard,
                    coroutineScope = viewModel.viewModelScope,
                    headline = R.string.suggested_version_safeguard,
                    description = R.string.suggested_version_safeguard_description,
                dialogTitle = R.string.suggested_version_safeguard_title,
                    confirmationText = R.string.suggested_version_safeguard_confirmation
                )
                SafeguardBooleanItem(
                    preference = viewModel.prefs.disableSelectionWarning,
                    coroutineScope = viewModel.viewModelScope,
                    headline = R.string.patch_selection_safeguard,
                    description = R.string.patch_selection_safeguard_description,
                dialogTitle = R.string.patch_selection_safeguard_title,
                    confirmationText = R.string.patch_selection_safeguard_confirmation
                )
                SafeguardBooleanItem(
                    preference = viewModel.prefs.disableUniversalPatchCheck,
                    coroutineScope = viewModel.viewModelScope,
                    headline = R.string.universal_patches_safeguard,
                    description = R.string.universal_patches_safeguard_description,
                    dialogTitle = R.string.universal_patches_safeguard_title,
                    confirmationText = R.string.universal_patches_safeguard_confirmation
                )
            }

            ListSection(
                title = stringResource(R.string.patcher),
                leadingContent = {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) {
                val useProcessRuntime by viewModel.prefs.useProcessRuntime.getAsState()

                BooleanItem(
                    preference = viewModel.prefs.useProcessRuntime,
                    coroutineScope = viewModel.viewModelScope,
                    headline = R.string.process_runtime,
                    description = R.string.process_runtime_description,
                )
                AnimatedVisibility(
                    visible = useProcessRuntime,
                ) {
                    IntegerItem(
                        preference = viewModel.prefs.patcherProcessMemoryLimit,
                        coroutineScope = viewModel.viewModelScope,
                        headline = R.string.process_runtime_memory_limit,
                        description = R.string.process_runtime_memory_limit_description,
                        unit = "MiB",
                    )
                }

                var showLogLevelDialog by rememberSaveable { mutableStateOf(false) }
                val minLogLevel by viewModel.prefs.minPatcherLogLevel.getAsState()

                if (showLogLevelDialog) {
                    var selected by rememberSaveable { mutableStateOf(minLogLevel) }
                    AlertDialog(
                        onDismissRequest = { showLogLevelDialog = false },
                        title = { Text(stringResource(R.string.min_patcher_log_level)) },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                LogLevel.entries.forEach { level ->
                                    ListItem(
                                        modifier = Modifier.clickable { selected = level },
                                        leadingContent = {
                                            HapticRadioButton(selected = selected == level, onClick = null)
                                        },
                                        headlineContent = {
                                            Text(stringResource(level.displayName))
                                        },
                                        colors = transparentListItemColors,
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        viewModel.prefs.minPatcherLogLevel.update(selected)
                                        showLogLevelDialog = false
                                    }
                                },
                                shapes = ButtonDefaults.shapes()
                            ) { Text(stringResource(R.string.confirm)) }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showLogLevelDialog = false },
                                shapes = ButtonDefaults.shapes()
                            ) { Text(stringResource(R.string.cancel)) }
                        }
                    )
                }

                SettingsListItem(
                    headlineContent = stringResource(R.string.min_patcher_log_level),
                    supportingContent = stringResource(minLogLevel.displayName),
                    onClick = { showLogLevelDialog = true }
                )
            }

            ListSection(
                title = stringResource(R.string.debugging),
                leadingContent = {
                    Icon(
                        Icons.Outlined.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) {
                val exportDebugLogsLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {
                        it?.let(viewModel::exportDebugLogs)
                    }
                val clipboard = remember { context.getSystemService<ClipboardManager>()!! }
                val deviceContent = """
                    Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                    Build type: ${BuildConfig.BUILD_TYPE}
                    Model: ${Build.MODEL}
                    Android version: ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})
                    Supported Archs: ${Build.SUPPORTED_ABIS.joinToString(", ")}
                    Memory limit: $memoryLimit
                """.trimIndent()
                SettingsListItem(
                    headlineContent = stringResource(R.string.debug_logs_export),
                    onClick = { exportDebugLogsLauncher.launch(viewModel.debugLogFileName) }
                )
                SettingsListItem(
                    headlineContent = stringResource(R.string.about_device),
                    supportingContent = deviceContent,
                    onClick = { developerTaps++ },
                    onLongClickLabel = stringResource(R.string.copy_to_clipboard),
                    onLongClick = {
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Device Information", deviceContent)
                        )
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) context.toast(
                            resources.getString(R.string.toast_copied_to_clipboard)
                        )
                    }.withHapticFeedback(HapticFeedbackConstantsCompat.LONG_PRESS)
                )
            }
        }
    }
}