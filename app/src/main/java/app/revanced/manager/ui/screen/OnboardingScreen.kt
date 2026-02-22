package app.revanced.manager.ui.screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.ColumnWithScrollbarEdgeShadow
import app.revanced.manager.ui.screen.onboarding.AppsStepContent
import app.revanced.manager.ui.screen.onboarding.PermissionsStepContent
import app.revanced.manager.ui.screen.onboarding.SourcesStepContent
import app.revanced.manager.ui.screen.onboarding.UpdatesStepContent
import app.revanced.manager.ui.viewmodel.OnboardingStep
import app.revanced.manager.ui.viewmodel.OnboardingViewModel
import app.revanced.manager.util.RequestInstallAppsContract
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onAppClick: (String) -> Unit,
    vm: OnboardingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val suggestedVersions by vm.suggestedVersions.collectAsStateWithLifecycle()
    val plugins by vm.plugins.collectAsStateWithLifecycle()
    val currentStep = vm.currentStep
    val scope = rememberCoroutineScope()

    var managerUpdatesEnabled by rememberSaveable { mutableStateOf(true) }
    var patchesUpdatesEnabled by rememberSaveable { mutableStateOf(true) }
    var isNavigating by remember { mutableStateOf(false) }
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

    val onCompletedAppClick: (String) -> Unit = { packageName ->
        if (!isNavigating) {
            isNavigating = true
            scope.launch {
                vm.completeOnboarding()
                onAppClick(packageName)
            }
        }
    }

    val onNextClick: () -> Unit = {
        if (currentStep == OnboardingStep.Updates) {
            scope.launch {
                vm.applyAutoUpdatePrefs(
                    managerEnabled = managerUpdatesEnabled,
                    patchesEnabled = patchesUpdatesEnabled
                )
                vm.advance()
            }
        } else {
            vm.advance()
        }
    }

    val onFinishClick: () -> Unit = {
        if (!isNavigating) {
            isNavigating = true
            scope.launch {
                vm.completeOnboarding()
                onFinish()
            }
        }
    }

    BackHandler(enabled = currentStep != OnboardingStep.Permissions) {
        vm.retreat()
    }

    val stepTitle = when (currentStep) {
        OnboardingStep.Permissions -> stringResource(R.string.onboarding_permissions_subtitle)
        OnboardingStep.Updates -> stringResource(R.string.auto_updates_dialog_title)
        OnboardingStep.Sources -> stringResource(R.string.onboarding_sources_subtitle)
        OnboardingStep.Apps -> stringResource(R.string.select_app)
    }

    val stepDescription = when (currentStep) {
        OnboardingStep.Permissions -> stringResource(R.string.onboarding_permissions_skip_description)
        OnboardingStep.Updates -> stringResource(R.string.auto_updates_dialog_note)
        OnboardingStep.Sources -> stringResource(R.string.onboarding_sources_note)
        OnboardingStep.Apps -> stringResource(R.string.onboarding_subtitle)
    }

    val onboardingButtons: @Composable () -> Unit = {
        OnboardingButtons(
            currentStep = currentStep,
            allPermissionsGranted = vm.allPermissionsGranted,
            isNavigating = isNavigating,
            onSkipPermissionsClick = { showSkipPermissionsDialog = true },
            onNextClick = onNextClick,
            onFinishClick = onFinishClick
        )
    }

    val stepContent: @Composable (Boolean, Modifier) -> Unit = { showSubtitle, modifier ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { width -> width * direction } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width * direction } + fadeOut())
            },
            modifier = modifier,
            label = "onboarding_content"
        ) { step ->
            when (step) {
                OnboardingStep.Permissions -> PermissionsStepContent(
                    canInstallUnknownApps = vm.canInstallUnknownApps,
                    isNotificationsEnabled = vm.isNotificationsEnabled,
                    isBatteryOptimizationExempt = vm.isBatteryOptimizationExempt,
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
                    showSubtitle = showSubtitle
                )

                OnboardingStep.Updates -> UpdatesStepContent(
                    managerEnabled = managerUpdatesEnabled,
                    patchesEnabled = patchesUpdatesEnabled,
                    onManagerEnabledChange = { managerUpdatesEnabled = it },
                    onPatchesEnabledChange = { patchesUpdatesEnabled = it },
                    showSubtitle = showSubtitle
                )

                OnboardingStep.Sources -> SourcesStepContent(
                    plugins = plugins,
                    onTrustPlugin = vm::trustPlugin,
                    onRevokePluginTrust = vm::revokePluginTrust,
                    showSubtitle = showSubtitle
                )

                OnboardingStep.Apps -> AppsStepContent(
                    apps = apps,
                    suggestedVersions = suggestedVersions,
                    onAppClick = onCompletedAppClick,
                    showSubtitle = showSubtitle
                )
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
                                .padding(start = 16.dp, end = 12.dp, top = 24.dp, bottom = 24.dp)
                        ) {
                            OnboardingHeader()
                            Spacer(modifier = Modifier.height(24.dp))
                            StepDescription(title = stepTitle, description = stepDescription)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 24.dp)
                    ) {
                        stepContent(false, Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            onboardingButtons()
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(48.dp))

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        OnboardingHeader()
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    stepContent(true, Modifier.weight(1f))

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        onboardingButtons()
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showSkipPermissionsDialog) {
            AlertDialog(
                onDismissRequest = { showSkipPermissionsDialog = false },
                title = { Text(stringResource(R.string.onboarding_permissions_skip_title)) },
                text = { Text(stringResource(R.string.onboarding_permissions_skip_description)) },
                confirmButton = {
                    TextButton(onClick = {
                        showSkipPermissionsDialog = false
                        vm.advance()
                    }) {
                        Text(stringResource(R.string.onboarding_permissions_skip_anyway))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSkipPermissionsDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun StepDescription(title: String, description: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OnboardingHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = stringResource(R.string.onboarding_welcome_to),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_ring),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
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

@Composable
private fun OnboardingButtons(
    currentStep: OnboardingStep,
    allPermissionsGranted: Boolean,
    isNavigating: Boolean,
    onSkipPermissionsClick: () -> Unit,
    onNextClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    val actionBarPadding = PaddingValues(0.dp)

    when (currentStep) {
        OnboardingStep.Permissions -> {
            if (allPermissionsGranted) {
                BottomContentBar(contentPadding = actionBarPadding) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClick = onNextClick
                    ) {
                        Text(text = stringResource(R.string.next))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            } else {
                BottomContentBar(contentPadding = actionBarPadding) {
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClick = onSkipPermissionsClick
                    ) {
                        Text(text = stringResource(R.string.onboarding_skip))
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClick = onNextClick,
                        enabled = false
                    ) {
                        Text(text = stringResource(R.string.next))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }

        OnboardingStep.Updates, OnboardingStep.Sources -> {
            BottomContentBar(contentPadding = actionBarPadding) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = onNextClick
                ) {
                    Text(text = stringResource(R.string.next))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }

        OnboardingStep.Apps -> {
            BottomContentBar(contentPadding = actionBarPadding) {
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    onClick = onFinishClick,
                    enabled = !isNavigating
                ) {
                    Text(text = stringResource(R.string.onboarding_skip))
                }
            }
        }
    }
}
