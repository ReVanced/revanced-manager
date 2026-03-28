package app.revanced.manager.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.data.room.apps.installed.InstallType
import app.revanced.manager.data.room.apps.installed.InstalledApp
import app.revanced.manager.network.downloader.LoadedDownloader
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.ui.component.BottomContentBar
import app.revanced.manager.ui.component.LazyColumnWithScrollbarEdgeShadow
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.ui.component.scaffold.BannerScaffold
import app.revanced.manager.ui.component.scaffold.rememberBannerScrollBehavior
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.ui.model.navigation.Settings
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.util.EventEffect
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.blurBackground
import app.revanced.manager.util.enabled
import app.revanced.manager.util.toast
import app.revanced.manager.util.transparentListItemColors
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
    val networkMetered = remember { !networkInfo.isUnmetered() }

    val packageName = vm.selectedApp.packageName
    val version = vm.selectedApp.version
    val bundles by vm.bundleInfoFlow.collectAsStateWithLifecycle(emptyList())

    val allowIncompatiblePatches by vm.prefs.disablePatchVersionCompatCheck.getAsState()
    val patches by remember {
        derivedStateOf {
            vm.getPatches(bundles, allowIncompatiblePatches)
        }
    }
    val selectedPatchCount = patches.values.sumOf { it.size }
    val selectedPatchNames by remember {
        derivedStateOf {
            patches.values.flatMap { patchNames -> patchNames }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = vm::handleDownloaderActivityResult
    )
    EventEffect(flow = vm.launchActivityFlow) { intent ->
        launcher.launch(intent)
    }
    val composableScope = rememberCoroutineScope()

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
                Box(Modifier.fillMaxSize().clipToBounds()) {
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
                    verticalAlignment = Alignment.CenterVertically
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
                            text = version ?: stringResource(R.string.selected_app_meta_any_version),
                            color = contentColor.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        sheetContent = { insetPadding ->
            val downloaders by vm.downloaders.collectAsStateWithLifecycle(emptyList())
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

            if (vm.showSourceSelector) {
                val requiredVersion by vm.requiredVersion.collectAsStateWithLifecycle(null)

                AppSourceSelectorDialog(
                    downloaders = downloaders,
                    installedApp = vm.installedAppData,
                    searchApp = SelectedApp.Search(
                        vm.packageName,
                        vm.desiredVersion
                    ),
                    activeSearchJob = vm.activeDownloader,
                    hasRoot = vm.hasRoot,
                    onDismissRequest = vm::dismissSourceSelector,
                    onSelectDownloader = vm::searchUsingDownloader,
                    requiredVersion = requiredVersion,
                    onSelect = {
                        vm.selectedApp = it
                        vm.dismissSourceSelector()
                    }
                )
            }

            val needsInternet =
                vm.selectedApp.let { it is SelectedApp.Search || it is SelectedApp.Download }
            val showNetworkUnavailableWarning = needsInternet && !networkConnected
            val showMeteredWarning = needsInternet && networkConnected && networkMetered

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = if (isLandscape) 0.dp else insetPadding.calculateLeftPadding(layoutDirection),
                            end = if (isLandscape) 0.dp else insetPadding.calculateRightPadding(layoutDirection)
                        )
                ) {
                    LazyColumnWithScrollbarEdgeShadow(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        showScrollbar = false,
                    ) {
                        item {
                            Column(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ListSection {
                                    SettingsListItem(
                                        headlineContent = stringResource(R.string.apk_source_selector_item),
                                        supportingContent = when (val app = vm.selectedApp) {
                                            is SelectedApp.Search -> stringResource(R.string.apk_source_auto)
                                            is SelectedApp.Installed -> stringResource(R.string.apk_source_installed)
                                            is SelectedApp.Download -> stringResource(
                                                R.string.apk_source_downloader,
                                                downloaders.find {
                                                    it.packageName == app.data.downloaderPackageName &&
                                                        it.className == app.data.downloaderClassName
                                                }?.name ?: app.data.downloaderPackageName
                                            )

                                            is SelectedApp.Local -> stringResource(R.string.apk_source_local)
                                        },
                                        trailingContent = {
                                            Icon(Icons.AutoMirrored.Outlined.ArrowRight, null)
                                        },
                                        onClick = { vm.showSourceSelector() }
                                    )
                                    error?.let {
                                        DownloaderWarningSurface(
                                            text = stringResource(it.resourceId),
                                            actionText = stringResource(R.string.open_downloaders),
                                            onActionClick = { onSettingsClick(Settings.Downloads) },
                                        )
                                    }
                                }
                                ListSection {
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
                                                vm.getCustomPatches(bundles, allowIncompatiblePatches),
                                                vm.options
                                            )
                                        }
                                    )
                                    SelectedPatchesSurface(
                                        selectedPatchNames = selectedPatchNames,
                                    )
                                }
                                if (showNetworkUnavailableWarning || showMeteredWarning) {
                                    ListSection {
                                        when {
                                            showNetworkUnavailableWarning -> {
                                                DownloaderWarningSurface(
                                                    text = stringResource(R.string.network_unavailable_warning)
                                                )
                                            }

                                            showMeteredWarning -> {
                                                DownloaderWarningSurface(
                                                    text = stringResource(R.string.network_metered_warning)
                                                )
                                            }
                                        }
                                    }
                                }
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
                                if (!vm.hasSetRequiredOptions(patches)) {
                                    onRequiredOptions(
                                        vm.selectedApp,
                                        vm.getCustomPatches(bundles, allowIncompatiblePatches),
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

@Composable
private fun DownloaderWarningSurface(
    text: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (actionText != null && onActionClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onActionClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(actionText)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedPatchesSurface(
    selectedPatchNames: List<String>,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedPatchNames.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_patches_selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                selectedPatchNames.forEach { patchName ->
                    Text(
                        text = patchName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSourceSelectorDialog(
    downloaders: List<LoadedDownloader>,
    installedApp: Pair<SelectedApp.Installed, InstalledApp?>?,
    searchApp: SelectedApp.Search,
    activeSearchJob: LoadedDownloader?,
    hasRoot: Boolean,
    requiredVersion: String?,
    onDismissRequest: () -> Unit,
    onSelectDownloader: (LoadedDownloader) -> Unit,
    onSelect: (SelectedApp) -> Unit,
) {
    val canSelect = activeSearchJob == null

    AlertDialogExtended(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.app_source_dialog_title)) },
        textHorizontalPadding = PaddingValues(horizontal = 0.dp),
        text = {
            LazyColumn {
                item(key = "auto") {
                    val hasDownloader = downloaders.isNotEmpty()
                    ListItem(
                        modifier = Modifier
                            .clickable(enabled = canSelect && hasDownloader) { onSelect(searchApp) }
                            .enabled(hasDownloader),
                        headlineContent = { Text(stringResource(R.string.app_source_dialog_option_auto)) },
                        supportingContent = {
                            Text(
                                if (hasDownloader)
                                    stringResource(R.string.app_source_dialog_option_auto_description)
                                else
                                    stringResource(R.string.app_source_dialog_option_auto_unavailable)
                            )
                        },
                        colors = transparentListItemColors
                    )
                }

                installedApp?.let { (app, meta) ->
                    item(key = "installed") {
                        val (usable, text) = when {
                            meta?.installType == InstallType.MOUNT && !hasRoot -> false to stringResource(
                                R.string.app_source_dialog_option_installed_no_root
                            )
                            meta?.installType == InstallType.DEFAULT -> false to stringResource(R.string.already_patched)
                            requiredVersion != null && app.version != requiredVersion -> false to stringResource(
                                R.string.app_source_dialog_option_installed_version_not_suggested,
                                app.version
                            )

                            else -> true to app.version
                        }
                        ListItem(
                            modifier = Modifier
                                .clickable(enabled = canSelect && usable) { onSelect(app) }
                                .enabled(usable),
                            headlineContent = { Text(stringResource(R.string.installed)) },
                            supportingContent = { Text(text) },
                            colors = transparentListItemColors
                        )
                    }
                }

                items(downloaders, key = { "downloader_${it.packageName}_${it.className}" }) { downloader ->
                    ListItem(
                        modifier = Modifier.clickable(enabled = canSelect) {
                            onSelectDownloader(
                                downloader
                            )
                        },
                        headlineContent = { Text(downloader.name) },
                        trailingContent = (@Composable { LoadingIndicator() }).takeIf { activeSearchJob == downloader },
                        colors = transparentListItemColors
                    )
                }
            }
        }
    )
}