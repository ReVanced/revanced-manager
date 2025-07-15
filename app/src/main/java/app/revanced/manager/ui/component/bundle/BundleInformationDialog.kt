package app.revanced.manager.ui.component.bundle

import android.webkit.URLUtil.isValidUrl
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.R.string.auto_update
import app.revanced.manager.R.string.auto_update_description
import app.revanced.manager.R.string.field_not_set
import app.revanced.manager.R.string.patches
import app.revanced.manager.R.string.patches_url
import app.revanced.manager.R.string.view_patches
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.bundles.LocalPatchBundle
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.asRemoteOrNull
import app.revanced.manager.domain.bundles.PatchBundleSource.Extensions.isDefault
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ExceptionViewerDialog
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.TextInputDialog
import app.revanced.manager.ui.component.haptics.HapticSwitch
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundleInformationDialog(
    src: PatchBundleSource,
    patchCount: Int,
    onDismissRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    onUpdate: () -> Unit,
) {
    val bundleRepo = koinInject<PatchBundleRepository>()
    val networkInfo = koinInject<NetworkInfo>()
    val hasNetwork = remember { networkInfo.isConnected() }
    val composableScope = rememberCoroutineScope()
    var viewCurrentBundlePatches by remember { mutableStateOf(false) }
    val isLocal = src is LocalPatchBundle
    val bundleManifestAttributes = src.patchBundle?.manifestAttributes
    val (autoUpdate, endpoint) = src.asRemoteOrNull?.let { it.autoUpdate to it.endpoint } ?: (null to null)

    fun onAutoUpdateChange(new: Boolean) = composableScope.launch {
        with(bundleRepo) {
            src.asRemoteOrNull?.setAutoUpdate(new)
        }
    }

    if (viewCurrentBundlePatches) {
        BundlePatchesDialog(
            src = src,
            onDismissRequest = {
                viewCurrentBundlePatches = false
            }
        )
    }

    FullscreenDialog(
        onDismissRequest = onDismissRequest,
    ) {
        Scaffold(
            topBar = {
                BundleTopBar(
                    title = src.name,
                    onBackClick = onDismissRequest,
                    backIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    },
                    actions = {
                        if (!src.isDefault) {
                            IconButton(onClick = onDeleteRequest) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    stringResource(R.string.delete)
                                )
                            }
                        }
                        if (!isLocal && hasNetwork) {
                            IconButton(onClick = onUpdate) {
                                Icon(
                                    Icons.Outlined.Update,
                                    stringResource(R.string.refresh)
                                )
                            }
                        }
                    }
                )
            },
        ) { paddingValues ->
            ColumnWithScrollbar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Tag(Icons.Outlined.Sell, src.name)
                    bundleManifestAttributes?.description?.let {
                        Tag(Icons.Outlined.Description, it)
                    }
                    bundleManifestAttributes?.source?.let {
                        Tag(Icons.Outlined.Commit, it)
                    }
                    bundleManifestAttributes?.author?.let {
                        Tag(Icons.Outlined.Person, it)
                    }
                    bundleManifestAttributes?.contact?.let {
                        Tag(Icons.AutoMirrored.Outlined.Send, it)
                    }
                    bundleManifestAttributes?.website?.let {
                        Tag(Icons.Outlined.Language, it, isUrl = true)
                    }
                    bundleManifestAttributes?.license?.let {
                        Tag(Icons.Outlined.Gavel, it)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                if (autoUpdate != null) {
                    BundleListItem(
                        headlineText = stringResource(auto_update),
                        supportingText = stringResource(auto_update_description),
                        trailingContent = {
                            HapticSwitch(
                                checked = autoUpdate,
                                onCheckedChange = ::onAutoUpdateChange
                            )
                        },
                        modifier = Modifier.clickable {
                            onAutoUpdateChange(!autoUpdate)
                        }
                    )
                }

                endpoint?.takeUnless { src.isDefault }?.let { url ->
                    var showUrlInputDialog by rememberSaveable {
                        mutableStateOf(false)
                    }
                    if (showUrlInputDialog) {
                        TextInputDialog(
                            initial = url,
                            title = stringResource(patches_url),
                            onDismissRequest = { showUrlInputDialog = false },
                            onConfirm = {
                                showUrlInputDialog = false
                                TODO("Not implemented.")
                            },
                            validator = {
                                if (it.isEmpty()) return@TextInputDialog false

                                isValidUrl(it)
                            }
                        )
                    }

                    BundleListItem(
                        modifier = Modifier.clickable(
                            enabled = false,
                            onClick = {
                                showUrlInputDialog = true
                            }
                        ),
                        headlineText = stringResource(patches_url),
                        supportingText = url.ifEmpty {
                            stringResource(field_not_set)
                        }
                    )
                }

                val patchesClickable = patchCount > 0
                BundleListItem(
                    headlineText = stringResource(patches),
                    supportingText = stringResource(view_patches),
                    modifier = Modifier.clickable(
                        enabled = patchesClickable,
                        onClick = {
                            viewCurrentBundlePatches = true
                        }
                    )
                ) {
                    if (patchesClickable) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowRight,
                            stringResource(patches)
                        )
                    }
                }

                src.error?.let {
                    var showDialog by rememberSaveable {
                        mutableStateOf(false)
                    }
                    if (showDialog) ExceptionViewerDialog(
                        onDismiss = { showDialog = false },
                        text = remember(it) { it.stackTraceToString() }
                    )

                    BundleListItem(
                        headlineText = stringResource(R.string.patches_error),
                        supportingText = stringResource(R.string.patches_error_description),
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowRight,
                                null
                            )
                        },
                        modifier = Modifier.clickable { showDialog = true }
                    )
                }
                if (src.state is PatchBundleSource.State.Missing && !isLocal) {
                    BundleListItem(
                        headlineText = stringResource(R.string.patches_error),
                        supportingText = stringResource(R.string.patches_not_downloaded),
                        modifier = Modifier.clickable(onClick = onUpdate)
                    )
                }
            }
        }
    }
}

@Composable
private fun Tag(
    icon: ImageVector,
    text: String,
    isUrl: Boolean = false
) {
    val uriHandler = LocalUriHandler.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (isUrl) {
            Modifier
                .clickable {
                    try {
                        uriHandler.openUri(text)
                    } catch (_: Exception) {
                    }
                }
        } else
            Modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
    }
}