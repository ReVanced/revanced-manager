package app.revanced.manager.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.ColumnWithScrollbarEdgeShadow
import app.revanced.manager.ui.screen.onboarding.AppsStepContent
import app.revanced.manager.ui.screen.onboarding.PermissionsStepContent
import app.revanced.manager.ui.screen.onboarding.UpdatesStepContent
import app.revanced.manager.ui.viewmodel.OnboardingStep
import app.revanced.manager.ui.viewmodel.OnboardingViewModel
import app.revanced.manager.util.RequestInstallAppsContract
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("BatteryLife")
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onAppClick: (String) -> Unit,
    vm: OnboardingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle(initialValue = null)
    val suggestedVersions by vm.suggestedVersions.collectAsStateWithLifecycle(initialValue = emptyMap())
    val hasNetworkError by vm.hasNetworkError.collectAsStateWithLifecycle(initialValue = false)
    val currentStep = vm.currentStep
    val scope = rememberCoroutineScope()

    var managerUpdatesEnabled by rememberSaveable { mutableStateOf(true) }
    var patchesUpdatesEnabled by rememberSaveable { mutableStateOf(true) }
    var downloaderUpdatesEnabled by rememberSaveable { mutableStateOf(true) }
    var showSkipPermissionsDialog by remember { mutableStateOf(false) }

    val installAppsLauncher = rememberLauncherForActivityResult(RequestInstallAppsContract) {
        vm.refreshPermissionStates()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        vm.refreshPermissionStates()
    }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        vm.refreshPermissionStates()
    }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, _ ->
            vm.refreshPermissionStates()
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }

    BackHandler(enabled = currentStep != OnboardingStep.Permissions) {
        vm.retreat()
    }

    val (stepTitle, stepDescription, stepButtons) = when (currentStep) {
        OnboardingStep.Permissions -> Triple(
            stringResource(R.string.onboarding_permissions_subtitle),
            stringResource(R.string.onboarding_permissions_skip_description),
            StepButtons(
                primaryAction = { vm.advance() },
                primaryEnabled = vm.allPermissionsGranted,
                secondaryAction = if (!vm.allPermissionsGranted) {
                    { showSkipPermissionsDialog = true }
                } else null
            )
        )

        OnboardingStep.Updates -> Triple(
            stringResource(R.string.onboarding_updates_subtitle),
            stringResource(R.string.auto_updates_dialog_note, vm.apiUrl),
            StepButtons(
                primaryAction = {
                    scope.launch {
                        vm.applyAutoUpdatePrefs(
                            managerEnabled = managerUpdatesEnabled,
                            patchesEnabled = patchesUpdatesEnabled,
                            downloadersEnabled = downloaderUpdatesEnabled,
                        )
                    }
                    vm.advance()
                },
            )
        )

        OnboardingStep.Apps -> Triple(
            stringResource(R.string.select_app),
            stringResource(R.string.onboarding_apps_subtitle),
            StepButtons(
                primaryTextRes = null,
                secondaryAction = {
                    scope.launch {
                        vm.completeOnboarding()
                        onFinish()
                    }
                }
            )
        )
    }

    val onboardingButtons: @Composable () -> Unit = {
        OnboardingButtons(stepButtons)
    }

    val stepContent: @Composable ColumnScope.(showDetails: Boolean) -> Unit = { showDetails ->
        AnimatedContent(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .weight(1f),
            targetState = currentStep,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { width -> width * direction } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width * direction } + fadeOut())
            },
            label = "onboarding_content",
        ) { step ->
            ColumnWithScrollbarEdgeShadow(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showDetails) StepTitle(stepTitle)
                when (step) {
                    OnboardingStep.Permissions -> PermissionsStepContent(
                        canInstallUnknownApps = vm.canInstallUnknownApps,
                        isNotificationsEnabled = vm.isNotificationsEnabled,
                        isBatteryOptimizationExempt = vm.isBatteryOptimizationExempt,
                        isShizukuAvailable = vm.isShizukuAvailable,
                        isShizukuAuthorized = vm.isShizukuAuthorized,
                        onRequestInstallApps = { installAppsLauncher.launch(context.packageName) },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onRequestBatteryOptimization = {
                            batteryOptimizationLauncher.launch(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        },
                        onRequestShizuku = {
                            try {
                                Shizuku.requestPermission(0)
                            } catch (_: Exception) {}
                        }
                    )

                    OnboardingStep.Updates -> UpdatesStepContent(
                        managerEnabled = managerUpdatesEnabled,
                        patchesEnabled = patchesUpdatesEnabled,
                        downloadersEnabled = downloaderUpdatesEnabled,
                        onManagerEnabledChange = { managerUpdatesEnabled = it },
                        onPatchesEnabledChange = { patchesUpdatesEnabled = it },
                        onDownloadersEnabledChange = { downloaderUpdatesEnabled = it },
                    )

                    OnboardingStep.Apps -> AppsStepContent(
                        modifier = Modifier.weight(1f),
                        apps = apps,
                        hasNetworkError = hasNetworkError,
                        suggestedVersions = suggestedVersions,
                        onAppClick = { packageName ->
                            scope.launch {
                                vm.completeOnboarding()
                                onAppClick(packageName)
                            }
                        }
                    )
                }
                if (showDetails) StepDescription(stepDescription)
            }
        }
    }

    Scaffold { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val useSplitLayout = maxWidth >= maxHeight

            if (useSplitLayout) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        ColumnWithScrollbarEdgeShadow(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, end = 12.dp, top = 24.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            OnboardingHeader()
                            StepDetails(title = stepTitle, description = stepDescription)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        stepContent(false)
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            onboardingButtons()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp, 24.dp),
                ) {
                    OnboardingHeader()
                    stepContent(true)
                    onboardingButtons()
                }
            }
        }

        if (showSkipPermissionsDialog) {
            AlertDialog(
                onDismissRequest = { showSkipPermissionsDialog = false },
                title = { Text(stringResource(R.string.onboarding_permissions_skip_title)) },
                text = { Text(stringResource(R.string.onboarding_permissions_skip_description)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSkipPermissionsDialog = false
                            vm.advance()
                        },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(R.string.onboarding_permissions_skip_anyway))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSkipPermissionsDialog = false },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (!vm.isDeviceSupported) {
            AlertDialog(
                onDismissRequest = {},
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text(stringResource(R.string.onboarding_unsupported_device_title)) },
                text = { Text(stringResource(R.string.onboarding_unsupported_device_description)) },
                confirmButton = {}
            )
        }
    }
}

@Composable
private fun StepDetails(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StepDescription(description: String) {
    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OnboardingHeader() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val icon = rememberDrawablePainter(drawable = remember(resources) {
        AppCompatResources.getDrawable(context, R.drawable.ic_logo_ring)
    })

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.onboarding_welcome_to),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnboardingButtons(stepButtons: StepButtons) {
    BottomContentBar(contentPadding = PaddingValues(0.dp)) {
        stepButtons.secondaryAction?.let { action ->
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = action,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(text = stringResource(stepButtons.secondaryTextRes!!))
            }
        }
        stepButtons.primaryTextRes?.let { textRes ->
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = stepButtons.primaryAction,
                enabled = stepButtons.primaryEnabled,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(text = stringResource(textRes))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

data class StepButtons(
    val primaryTextRes: Int? = R.string.next,
    val primaryAction: () -> Unit = {},
    val primaryEnabled: Boolean = true,
    val secondaryTextRes: Int? = R.string.onboarding_skip,
    val secondaryAction: (() -> Unit)? = null
)