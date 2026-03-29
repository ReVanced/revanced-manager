package app.revanced.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbarEdgeShadow
import app.revanced.manager.ui.component.scaffold.BannerScaffold
import app.revanced.manager.ui.component.scaffold.rememberBannerScrollBehavior
import app.revanced.manager.ui.component.selectedapp.AppSourceSelectorDialog
import app.revanced.manager.ui.component.selectedapp.BuildSectionLayouts
import app.revanced.manager.ui.component.selectedapp.ErrorListItem
import app.revanced.manager.ui.component.selectedapp.InfoListItem
import app.revanced.manager.ui.component.selectedapp.VersionSelectorDialog
import app.revanced.manager.ui.component.selectedapp.error
import app.revanced.manager.ui.component.selectedapp.info
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.navigation.Settings
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.blurBackground
import app.revanced.manager.util.toast
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SelectedAppInfoScreen(
    onPatchSelectorClick: (SelectedApp, PatchSelection?, Options) -> Unit,
    onRequiredOptions: (SelectedApp, PatchSelection?, Options) -> Unit,
    onPatchClick: () -> Unit,
    onBackClick: () -> Unit,
    onSettingsClick: (Settings.Destination) -> Unit,
    vm: SelectedAppInfoViewModel
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val layoutDirection = LocalLayoutDirection.current
    val networkInfo = koinInject<NetworkInfo>()
    val networkConnected = remember { networkInfo.isConnected() }
    val networkMetered = remember { networkInfo.isUnmetered() }

    val packageName = vm.selectedApp.packageName
    val version = vm.selectedApp.version
    val bundles by vm.bundleInfoFlow.collectAsStateWithLifecycle(emptyList())

    val allowIncompatiblePatches by vm.prefs.disablePatchVersionCompatCheck.getAsState()
    val effectiveAllowIncompatible = allowIncompatiblePatches || vm.selectedApp.version == null

    val patches by remember(bundles, effectiveAllowIncompatible) {
        derivedStateOf {
            vm.getPatches(bundles, effectiveAllowIncompatible)
        }
    }
    val versionOptions by remember(bundles, patches, packageName, allowIncompatiblePatches) {
        derivedStateOf {
            buildVersionOptions(
                bundles = bundles,
                selectedPatches = patches,
                packageName = packageName,
                allowIncompatible = allowIncompatiblePatches
            )
        }
    }
    val strictVersionOptions by remember(bundles, patches, packageName) {
        derivedStateOf {
            buildVersionOptions(
                bundles = bundles,
                selectedPatches = patches,
                packageName = packageName,
                allowIncompatible = false
            )
        }
    }
    val selectedVersionLabel by remember(vm.selectedApp.version) {
        derivedStateOf {
            vm.selectedApp.version ?: resources.getString(R.string.selected_app_meta_any_version)
        }
    }
    var showVersionSelector by remember { mutableStateOf(false) }
    val selectedPatchCount = patches.values.sumOf { it.size }
    val hasModifiedPatchSelection by remember(bundles, effectiveAllowIncompatible) {
        derivedStateOf {
            vm.hasModifiedPatchSelection(bundles, effectiveAllowIncompatible)
        }
    }
    val showVersionCompatibilityWarning by remember(
        vm.selectedApp.version,
        allowIncompatiblePatches,
        strictVersionOptions
    ) {
        derivedStateOf {
            val selectedVersion = vm.selectedApp.version ?: return@derivedStateOf false
            allowIncompatiblePatches &&
                    strictVersionOptions.versions.isNotEmpty() &&
                    selectedVersion !in strictVersionOptions.versions
        }
    }

    LaunchedEffect(versionOptions, vm.selectedApp.version) {
        if (versionOptions.unrestricted) return@LaunchedEffect

        val selectedVersion = vm.selectedApp.version
        if (selectedVersion != null && selectedVersion in versionOptions.versions) {
            return@LaunchedEffect
        }

        vm.setTargetVersion(versionOptions.versions.firstOrNull())
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = vm::handleDownloaderActivityResult
    )
    EventEffect(flow = vm.launchActivityFlow) { intent ->
        launcher.launch(intent)
    }
    val composableScope = rememberCoroutineScope()

    val sourcePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let(vm::handleStorageResult) }
    )
    EventEffect(flow = vm.storageSelectionFlow) { app ->
        vm.selectedApp = app
        vm.dismissSourceSelector()
    }

    val error by vm.errorFlow.collectAsStateWithLifecycle(null)

    val listState = rememberLazyListState()
    val scrollBehavior = rememberBannerScrollBehavior(sheetLazyListState = listState)

    val pm = context.packageManager
    val appInfo = vm.selectedAppInfo ?: remember(packageName) {
        try {
            pm.getPackageInfo(packageName, 0)
        } catch (_: Exception) {
            null
        }
    }
    val appIconBitmap = remember(appInfo) {
        appInfo?.applicationInfo?.loadIcon(pm)?.toBitmap()?.asImageBitmap()
    }
    val appIconBlurBitmap = remember(appIconBitmap) {
        appIconBitmap?.let { blurBackground(context, it.asAndroidBitmap(), 25f).asImageBitmap() }
    }

    BannerScaffold(
        onBackClick = onBackClick,
        scrollBehavior = scrollBehavior,
        minCollapsedBannerSize = 100.dp,
        bannerBackground = {
            if (appIconBlurBitmap != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    Image(
                        bitmap = appIconBlurBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.8f)
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                }
            }
        },
        bannerContent = {
            val collapsedFraction = if (isLandscape) 0f else collapseFraction
            var isCompact by remember { mutableStateOf(false) }
            LaunchedEffect(collapsedFraction) {
                if (isCompact && collapsedFraction < 0.4f) {
                    isCompact = false
                } else if (!isCompact && collapsedFraction > 0.6f) {
                    isCompact = true
                }
            }
            val compactAlpha = if (isCompact) 1f else 0f
            val expandedAlpha = if (isCompact) 0f else 1f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .align(Alignment.Center)
                        .alpha(expandedAlpha),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIcon(
                        appInfo,
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(bottom = 5.dp)
                    )
                    AppLabel(
                        appInfo,
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        defaultText = packageName
                    )
                    Text(
                        text = version ?: stringResource(R.string.selected_app_meta_any_version),
                        modifier = Modifier.fillMaxWidth(),
                        color = contentColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(start = 24.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
                        .alpha(compactAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(
                        appInfo,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        AppLabel(
                            appInfo,
                            style = MaterialTheme.typography.titleMedium,
                            defaultText = packageName
                        )
                        Text(
                            text = version
                                ?: stringResource(R.string.selected_app_meta_any_version),
                            color = contentColor.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        sheetContent = { insetPadding ->
            val downloaders by vm.downloaders.collectAsStateWithLifecycle(emptyMap())
            val windowInfo = LocalWindowInfo.current
            val isLandscape = windowInfo.containerSize.width > windowInfo.containerSize.height

            if (vm.showSourceSelector) {
                val selectedVersion = vm.selectedApp.version
                val autoSelection = vm.resolveAutoSource(selectedVersion)

                AppSourceSelectorDialog(
                    downloaders = downloaders,
                    downloadedApps = vm.downloadedApps,
                    activeSearchJob = vm.activeDownloader,
                    requiredVersion = selectedVersion,
                    autoSelection = autoSelection,
                    onDismissRequest = vm::dismissSourceSelector,
                    onSelectAuto = {
                        vm.selectedApp = autoSelection
                        vm.dismissSourceSelector()
                    },
                    onSelectDownloader = vm::searchUsingDownloader,
                    onSelectFromStorage = { sourcePickerLauncher.launch(APK_MIMETYPE) },
                    onSelect = {
                        vm.selectedApp = it
                        vm.dismissSourceSelector()
                    }
                )
            }


            if (showVersionSelector) {
                VersionSelectorDialog(
                    selectedVersion = vm.selectedApp.version,
                    availableVersions = versionOptions.versions,
                    allowAnyVersion = versionOptions.unrestricted,
                    onDismissRequest = { showVersionSelector = false },
                    onSelect = { version ->
                        vm.setTargetVersion(version)
                        showVersionSelector = false
                    }
                )
            }

            val needsInternet =
                vm.selectedApp.let { it is SelectedApp.Search || it is SelectedApp.Download }
            val showNetworkUnavailableWarning = needsInternet && !networkConnected
            val showMeteredWarning = needsInternet && networkMetered

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = if (isLandscape) 0.dp else insetPadding.calculateLeftPadding(
                                layoutDirection
                            ),
                            end = if (isLandscape) 0.dp else insetPadding.calculateRightPadding(
                                layoutDirection
                            )
                        )
                ) {
                    LazyColumnWithScrollbarEdgeShadow(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        showScrollbar = false,
                    ) {
                        val items = buildList {
                            item {
                                SettingsListItem(
                                    headlineContent = stringResource(R.string.patch_selector_item),
                                    supportingContent = stringResource(
                                        R.string.patch_selector_item_description,
                                        selectedPatchCount
                                    ),
                                    trailingContent = {
                                        Icon(Icons.AutoMirrored.Outlined.ArrowRight, null)
                                    },
                                    onClick = {
                                        onPatchSelectorClick(
                                            vm.selectedApp,
                                            vm.getCustomPatches(
                                                bundles,
                                                allowIncompatiblePatches
                                            ),
                                            vm.options
                                        )
                                    }
                                )
                            }

                            if (!hasModifiedPatchSelection) {
                                info {
                                    InfoListItem(
                                        stringResource(R.string.patch_selection_changed_warning)
                                    )
                                }
                            }

                            if (versionOptions.versions.isNotEmpty()) {
                                item {
                                    SettingsListItem(
                                        headlineContent = stringResource(R.string.version),
                                        supportingContent = selectedVersionLabel,
                                        trailingContent = {
                                            Icon(Icons.AutoMirrored.Outlined.ArrowRight, null)
                                        },
                                        onClick = { showVersionSelector = true }
                                    )
                                }
                            }

                            if (!showVersionCompatibilityWarning) {
                                info {
                                    InfoListItem(
                                        stringResource(R.string.version_compatibility_warning)
                                    )
                                }
                            }

                            item {
                                val autoSourceSubtitle = run {
                                    val resolved = vm.resolveAutoSource(vm.selectedApp.version)
                                    when {
                                        resolved is SelectedApp.Installed -> stringResource(R.string.apk_source_auto_installed)
                                        resolved is SelectedApp.Local -> stringResource(R.string.apk_source_auto_downloaded)
                                        downloaders.isNotEmpty() -> stringResource(R.string.apk_source_auto_downloader)
                                        else -> stringResource(R.string.apk_source_auto_storage)
                                    }
                                }

                                SettingsListItem(
                                    headlineContent = stringResource(R.string.apk_source_selector_item),
                                    supportingContent = when (val app = vm.selectedApp) {
                                        is SelectedApp.Search -> autoSourceSubtitle
                                        is SelectedApp.Installed -> stringResource(R.string.apk_source_installed)
                                        is SelectedApp.Download -> stringResource(
                                            R.string.apk_source_downloader,
                                            downloaders.values
                                                .flatten()
                                                .find { it.className == app.data.downloaderClassName }
                                                ?.packageLabel ?: app.data.downloaderPackageName
                                        )

                                        is SelectedApp.Local -> stringResource(R.string.apk_source_local)
                                    },
                                    trailingContent = {
                                        Icon(Icons.AutoMirrored.Outlined.ArrowRight, null)
                                    },
                                    onClick = { vm.showSourceSelector() }
                                )
                            }

                            error?.let {
                                error {
                                    ErrorListItem(
                                        text = stringResource(it.resourceId),
                                        actionText = stringResource(R.string.open_downloaders),
                                        onActionClick = { onSettingsClick(Settings.Downloads) },
                                    )
                                }
                            }

                            if (showNetworkUnavailableWarning || showMeteredWarning) {
                                error {
                                    when {
                                        showNetworkUnavailableWarning -> {
                                            ErrorListItem(
                                                text = stringResource(R.string.network_unavailable_warning)
                                            )
                                        }

                                        showMeteredWarning -> {
                                            ErrorListItem(
                                                text = stringResource(R.string.network_metered_warning)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                BuildSectionLayouts(items)
                            }
                        }
                    }
                }

                BottomContentBar(modifier = Modifier.navigationBarsPadding()) {
                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = error == null,
                        onClick = patchClick@{
                            if (selectedPatchCount == 0) {
                                context.toast(resources.getString(R.string.no_patches_selected))
                                return@patchClick
                            }
                            composableScope.launch {
                                if (!vm.hasSetRequiredOptions(
                                        patches,
                                        effectiveAllowIncompatible
                                    )
                                ) {
                                    onRequiredOptions(
                                        vm.selectedApp,
                                        vm.getCustomPatches(bundles, effectiveAllowIncompatible),
                                        vm.options
                                    )
                                    return@launch
                                }

                                onPatchClick()
                            }
                        }
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.patch))
                    }
                }
            }
        },
    )
}

private data class VersionOptions(
    val versions: List<String>,
    val unrestricted: Boolean
)

private fun buildVersionOptions(
    bundles: List<PatchBundleInfo.Scoped>,
    selectedPatches: PatchSelection,
    packageName: String,
    allowIncompatible: Boolean
): VersionOptions {
    val selected = bundles.flatMap { bundle ->
        val selectedNames = selectedPatches[bundle.uid].orEmpty()
        bundle.patches.filter { it.name in selectedNames }
    }

    val constraints = selected.mapNotNull { patch ->
        patch.versionConstraintFor(packageName)
    }

    if (constraints.isEmpty() || allowIncompatible) {
        val knownVersions = bundles.asSequence()
            .flatMap { bundle -> bundle.patches.asSequence() }
            .mapNotNull { patch -> patch.versionConstraintFor(packageName) }
            .flatMap { versions -> versions.asSequence() }
            .distinct()
            .sortedDescending()
            .toList()

        return VersionOptions(versions = knownVersions, unrestricted = true)
    }

    val intersection = constraints
        .map { it.toSet() }
        .reduce { acc, versions -> acc intersect versions }
        .toList()
        .sortedDescending()

    return VersionOptions(versions = intersection, unrestricted = false)
}

private fun PatchInfo.versionConstraintFor(packageName: String): Set<String>? {
    val pkg = compatiblePackages?.firstOrNull { it.packageName == packageName } ?: return null
    return pkg.versions?.toSet()?.takeIf { it.isNotEmpty() }
}

