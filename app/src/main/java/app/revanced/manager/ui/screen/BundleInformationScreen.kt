package app.revanced.manager.ui.screen

import android.webkit.URLUtil.isValidUrl
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.sources.Extensions.asRemoteOrNull
import app.revanced.manager.domain.sources.LocalSource
import app.revanced.manager.domain.sources.Source
import app.revanced.manager.ui.component.ColumnWithScrollbar
import app.revanced.manager.ui.component.ConfirmDialog
import app.revanced.manager.ui.component.ExceptionViewerDialog
import app.revanced.manager.ui.component.ListSection
import app.revanced.manager.ui.component.TextInputDialog
import app.revanced.manager.ui.component.haptics.HapticSwitch
import app.revanced.manager.ui.component.settings.SafeguardBooleanItem
import app.revanced.manager.ui.component.settings.SettingsListItem
import app.revanced.manager.ui.viewmodel.BundleInformationViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BundleInformationScreen(
    onBackClick: () -> Unit,
    viewModel: BundleInformationViewModel
) {
    val srcState = viewModel.bundle.collectAsStateWithLifecycle(null)
    val src = srcState.value ?: return
    val patchCount by viewModel.patchCount.collectAsStateWithLifecycle(0)

    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    val isLocal = src is LocalSource<*>
    val bundleManifestAttributes = src.loaded?.manifestAttributes
    val (autoUpdate, endpoint) = src.asRemoteOrNull?.let { it.autoUpdate to it.endpoint }
        ?: (null to null)

    val subtitleAuthor = bundleManifestAttributes?.author?.let {
        stringResource(R.string.bundle_information_by_author, it)
    }
    val subtitleVersion = bundleManifestAttributes?.version?.let { "v$it" }
    val contentScrollState = rememberScrollState()
    val isContentScrollable by remember { derivedStateOf { contentScrollState.maxValue > 0 } }

    if (showDeleteConfirmationDialog) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = {
                viewModel.delete()
                onBackClick()
            },
            title = stringResource(R.string.delete),
            description = stringResource(
                R.string.patches_delete_single_dialog_description,
                src.name
            ),
            icon = Icons.Outlined.Delete
        )
    }

    val scrollBehavior = if (isContentScrollable) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    } else {
        null
    }

    Scaffold(
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(src.name) },
                subtitle = if (subtitleAuthor != null || subtitleVersion != null) {
                    {
                        val dot = "\u2022"          // •
                        val emSpace = "\u2002"      // en space, roughly half character width
                        val separator = "$emSpace$dot$emSpace"
                        Text("$subtitleAuthor$separator$subtitleVersion")
                    }
                } else {
                    null
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (!src.isDefault) {
                        IconButton(
                            onClick = { showDeleteConfirmationDialog = true },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                stringResource(R.string.delete)
                            )
                        }
                    }
                    val hasNetwork = remember { viewModel.networkInfo.isConnected() }
                    if (!isLocal && hasNetwork) {
                        IconButton(
                            onClick = viewModel::refresh,
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                stringResource(R.string.refresh)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.then(
            scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier
        ),
    ) { paddingValues ->
        ColumnWithScrollbar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            state = contentScrollState,
        ) {
            bundleManifestAttributes?.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    )
                )
            }

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                bundleManifestAttributes?.website?.let { website ->
                    TagValue(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.website),
                        value = website,
                        uri = website
                    )
                }

                bundleManifestAttributes?.contact?.let { contact ->
                    TagValue(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        title = stringResource(R.string.contact),
                        value = contact,
                        uri = if (contact.startsWith("mailto:")) contact else "mailto:$contact"
                    )
                }

                bundleManifestAttributes?.source?.let { source ->
                    TagValue(
                        icon = Icons.Outlined.Source,
                        title = stringResource(R.string.repository),
                        value = source
                    )
                }

                bundleManifestAttributes?.license?.let { license ->
                    TagValue(
                        icon = Icons.Outlined.Gavel,
                        title = stringResource(R.string.license),
                        value = license
                    )
                }
            }

            ListSection {
                if (autoUpdate != null) {
                    SettingsListItem(
                        headlineContent = stringResource(R.string.auto_update),
                        supportingContent = stringResource(R.string.auto_update_description),
                        trailingContent = {
                            HapticSwitch(
                                checked = autoUpdate,
                                onCheckedChange = viewModel::setAutoUpdate,
                                thumbContent = if (autoUpdate) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                } else {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                }
                            )
                        },
                        onClick = { viewModel.setAutoUpdate(!autoUpdate) }
                    )
                }

                if (src.isDefault) {
                    SafeguardBooleanItem(
                        preference = viewModel.prefs.usePatchesPrereleases,
                        headline = R.string.patches_prereleases,
                        description = stringResource(
                            R.string.patches_prereleases_description,
                            src.name
                        ),
                        dialogTitle = R.string.prerelease_title,
                        confirmationText = R.string.prereleases_warning,
                        onValueChange = viewModel::updateUsePrereleases
                    )
                }

                endpoint?.takeUnless { src.isDefault }?.let { url ->
                    var showUrlInputDialog by rememberSaveable { mutableStateOf(false) }

                    if (showUrlInputDialog) {
                        TextInputDialog(
                            initial = url,
                            title = stringResource(R.string.patches_url),
                            onDismissRequest = { showUrlInputDialog = false },
                            onConfirm = {
                                showUrlInputDialog = false
                                // TODO: Not implemented
                            },
                            validator = {
                                if (it.isEmpty()) return@TextInputDialog false
                                isValidUrl(it)
                            }
                        )
                    }

                    SettingsListItem(
                        headlineContent = stringResource(R.string.patches_url),
                        supportingContent = url.ifEmpty {
                            stringResource(R.string.field_not_set)
                        },
                        onClick = null
                    )
                }

                SettingsListItem(
                    headlineContent = stringResource(R.string.patches),
                    supportingContent = pluralStringResource(
                        id = R.plurals.patch_count,
                        count = patchCount,
                        patchCount
                    ),
                    onClick = null,
                    trailingContent = null
                )

                src.error?.let {
                    var showDialog by rememberSaveable { mutableStateOf(false) }

                    if (showDialog) {
                        ExceptionViewerDialog(
                            onDismiss = { showDialog = false },
                            text = remember(it) { it.stackTraceToString() }
                        )
                    }

                    SettingsListItem(
                        headlineContent = stringResource(R.string.patches_error),
                        supportingContent = stringResource(R.string.patches_error_description),
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                null
                            )
                        },
                        onClick = { showDialog = true }
                    )
                }

                if (src.state is Source.State.Missing && !isLocal) {
                    SettingsListItem(
                        headlineContent = stringResource(R.string.patches_error),
                        supportingContent = stringResource(R.string.patches_not_downloaded),
                        onClick = viewModel::refresh
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TagValue(
    icon: ImageVector,
    title: String,
    value: String,
    uri: String? = null
) {
    val uriHandler = LocalUriHandler.current
    val onClick: (() -> Unit)? = uri?.let { targetUri ->
        {
            try {
                uriHandler.openUri(targetUri)
            } catch (_: Exception) {
            }
        }
    }

    val buttonText = value
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("mailto:")
        .removeSuffix("/")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(end = if (onClick != null) 0.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (onClick != null) {
            TextButton(onClick = onClick, shapes = ButtonDefaults.shapes()) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}
