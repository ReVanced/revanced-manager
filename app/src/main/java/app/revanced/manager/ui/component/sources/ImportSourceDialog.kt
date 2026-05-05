package app.revanced.manager.ui.component.sources

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.APIResponse
import app.revanced.manager.ui.component.AlertDialogExtended
import app.revanced.manager.ui.component.TextHorizontalPadding
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.ui.component.haptics.HapticCheckbox
import app.revanced.manager.ui.component.haptics.HapticRadioButton
import app.revanced.manager.ui.screen.TagValue
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.BIN_MIMETYPE
import app.revanced.manager.util.PGP_MIMETYPE
import app.revanced.manager.util.relativeTime
import app.revanced.manager.util.transparentListItemColors
import io.ktor.client.request.url
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import kotlin.time.Instant

private enum class SourceType {
    Local,
    Remote
}

enum class ImportSourceDialogStrings(
    val title: Int,
    val type_remote_description: Int,
    val type_local_description: Int,
    val import_local: Int,
    val import_remote: Int
) {
    PATCHES(
        R.string.add_patches,
        R.string.remote_patches_description,
        R.string.local_patches_description,
        R.string.patches,
        R.string.patches_url
    ),
    DOWNLOADERS(
        R.string.downloader_add,
        R.string.remote_downloaders_description,
        R.string.local_downloaders_description,
        R.string.downloaders,
        R.string.downloader_url
    ),
}

@Serializable
data class GithubRelease(
    val name: String? = null,
    @SerialName("tag_name") val tagName: String,
    val prerelease: Boolean,
    val assets: List<GithubAsset> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImportSourceDialog(
    strings: ImportSourceDialogStrings,
    onDismiss: () -> Unit,
    onRemoteSubmit: (String, Boolean) -> Unit,
    onLocalSubmit: (Uri) -> Unit
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var sourceType by rememberSaveable { mutableStateOf(SourceType.Remote) }
    var local by rememberSaveable { mutableStateOf<Uri?>(null) }
    var remoteUrl by rememberSaveable { mutableStateOf("") }
    var autoUpdate by rememberSaveable { mutableStateOf(true) }

    val githubMatch by
    remember(remoteUrl) {
        derivedStateOf {
            Regex("^https://github\\.com/([^/]+)/([^/]+)/?$").find(remoteUrl.trim())
        }
    }
    val isGithubRepoUrl by
    remember(sourceType, githubMatch) {
        derivedStateOf { sourceType == SourceType.Remote && githubMatch != null }
    }

    var selectedGithubAssetUrl by rememberSaveable { mutableStateOf<String?>(null) }

    val fileActivityLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { local = it }
        }

    fun launchFileActivity() {
        when (strings) {
            ImportSourceDialogStrings.PATCHES -> fileActivityLauncher.launch(BIN_MIMETYPE)
            ImportSourceDialogStrings.DOWNLOADERS -> fileActivityLauncher.launch(APK_MIMETYPE)
        }
    }

    val steps = mutableListOf<@Composable () -> Unit>()

    steps.add {
        SelectSourceTypeStep(strings, sourceType) { selectedType -> sourceType = selectedType }
    }

    steps.add {
        ImportSourceStep(
            strings,
            sourceType,
            local,
            remoteUrl,
            autoUpdate,
            ::launchFileActivity,
            { remoteUrl = it },
            { autoUpdate = it }
        )
    }

    if (isGithubRepoUrl) {
        steps.add {
            GithubReleaseStep(
                owner = githubMatch!!.groupValues[1],
                repo = githubMatch!!.groupValues[2],
                strings = strings,
                onAssetSelected = { selectedGithubAssetUrl = it }
            )
        }
    }

    val inputsAreValid by remember(
        currentStep,
        sourceType,
        local,
        remoteUrl,
        isGithubRepoUrl,
        selectedGithubAssetUrl
    ) {
        derivedStateOf {
            if (currentStep < steps.lastIndex) return@derivedStateOf true
            if (sourceType == SourceType.Local) return@derivedStateOf local != null
            if (isGithubRepoUrl) return@derivedStateOf selectedGithubAssetUrl != null
            remoteUrl.isNotEmpty()
        }
    }

    AlertDialogExtended(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(strings.title))
        },
        text = {
            if (currentStep in steps.indices) steps[currentStep]()
        },
        confirmButton = {
            if (currentStep == steps.lastIndex) {
                TextButton(
                    enabled = inputsAreValid,
                    onClick = {
                        when (sourceType) {
                            SourceType.Local -> local?.let(onLocalSubmit)
                            SourceType.Remote -> onRemoteSubmit(
                                if (isGithubRepoUrl) selectedGithubAssetUrl!!
                                else remoteUrl,
                                autoUpdate
                            )
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.add))
                }
            } else {
                TextButton(
                    enabled = inputsAreValid,
                    onClick = { currentStep++ },
                    shapes = ButtonDefaults.shapes()
                ) { Text(stringResource(R.string.next)) }
            }
        },
        dismissButton = {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.back))
                }
            } else {
                TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        textHorizontalPadding = PaddingValues(0.dp)
    )
}

@Composable
private fun SelectSourceTypeStep(
    strings: ImportSourceDialogStrings,
    sourceType: SourceType,
    onSourceTypeSelected: (SourceType) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            ListItem(
                modifier = Modifier.clickable(
                    role = Role.RadioButton,
                    onClick = { onSourceTypeSelected(SourceType.Remote) }
                ),
                headlineContent = { Text(stringResource(R.string.enter_url)) },
                overlineContent = { Text(stringResource(R.string.recommended)) },
                supportingContent = { Text(stringResource(strings.type_remote_description)) },
                leadingContent = {
                    HapticRadioButton(
                        selected = sourceType == SourceType.Remote,
                        onClick = null
                    )
                },
                colors = transparentListItemColors
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            ListItem(
                modifier = Modifier.clickable(
                    role = Role.RadioButton,
                    onClick = { onSourceTypeSelected(SourceType.Local) }
                ),
                headlineContent = { Text(stringResource(R.string.select_from_storage)) },
                supportingContent = { Text(stringResource(strings.type_local_description)) },
                overlineContent = { },
                leadingContent = {
                    HapticRadioButton(
                        selected = sourceType == SourceType.Local,
                        onClick = null
                    )
                },
                colors = transparentListItemColors
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImportSourceStep(
    strings: ImportSourceDialogStrings,
    sourceType: SourceType,
    local: Uri?,
    remoteUrl: String,
    autoUpdate: Boolean,
    launchFileActivity: () -> Unit,
    onRemoteUrlChange: (String) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        when (sourceType) {
            SourceType.Local -> {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(strings.import_local))
                        },
                        supportingContent = { Text(stringResource(if (local != null) R.string.file_field_set else R.string.file_field_not_set)) },
                        trailingContent = {
                            TooltipIconButton(
                                onClick = launchFileActivity,
                                tooltip = stringResource(strings.import_local)
                            ) {
                                Icon(imageVector = Icons.Default.Topic, contentDescription = null)
                            }
                        },
                        modifier = Modifier.clickable { launchFileActivity() },
                        colors = transparentListItemColors
                    )
                }
            }

            SourceType.Remote -> {
                Column(
                    modifier = Modifier.padding(TextHorizontalPadding)
                ) {
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = onRemoteUrlChange,
                        label = { Text(stringResource(strings.import_remote)) }
                    )
                }
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    ListItem(
                        modifier = Modifier.clickable(
                            role = Role.Checkbox,
                            onClick = { onAutoUpdateChange(!autoUpdate) }
                        ),
                        headlineContent = { Text(stringResource(R.string.auto_update)) },
                        leadingContent = {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                HapticCheckbox(
                                    checked = autoUpdate,
                                    onCheckedChange = {
                                        onAutoUpdateChange(!autoUpdate)
                                    }
                                )
                            }
                        },
                        colors = transparentListItemColors
                    )
                }
            }
        }
    }
}

@Composable
private fun GithubReleaseStep(
    owner: String,
    repo: String,
    strings: ImportSourceDialogStrings,
    onAssetSelected: (String?) -> Unit
) {
    val httpService: HttpService = koinInject()
    var releases by remember { mutableStateOf<List<GithubRelease>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val fetchFailedMessage = stringResource(R.string.github_releases_fetch_failed)

    LaunchedEffect(owner, repo) {
        val response =
            httpService.request<List<GithubRelease>> {
                url("https://api.github.com/repos/$owner/$repo/releases")
            }
        if (response is APIResponse.Success) {
            releases = response.data
        } else {
            error = fetchFailedMessage
        }
    }

    Column {
        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp)
            )
        } else if (releases == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val latestRelease = releases?.filter { !it.prerelease } ?: emptyList()
            val release = latestRelease.firstOrNull()
            val targetAsset = release?.let { r ->
                when (strings) {
                    ImportSourceDialogStrings.PATCHES -> r.assets.filter {
                        it.name.endsWith(".rvp") && it.contentType != PGP_MIMETYPE
                    }

                    ImportSourceDialogStrings.DOWNLOADERS -> r.assets.filter {
                        it.name.endsWith(".apk") && it.contentType == APK_MIMETYPE
                    }
                }.firstOrNull()
            }

            SideEffect { onAssetSelected(targetAsset?.browserDownloadUrl) }

            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp)
            ) {
                if (latestRelease.isEmpty()) {
                    Text(stringResource(R.string.github_release_none_found))
                } else if (targetAsset == null) {
                    Text(stringResource(R.string.github_asset_none_found))
                } else {
                    TagValue(
                        icon = Icons.Outlined.AttachFile,
                        title = "File",
                        value = targetAsset.name
                    )
                    TagValue(
                        icon = Icons.Outlined.Sell,
                        title = "Version",
                        value = release.name ?: release.tagName
                    )
                    TagValue(
                        icon = Icons.Outlined.Update,
                        title = "Updated",
                        value = release.createdAt?.let {
                            Instant.parse(it).toLocalDateTime(TimeZone.UTC)
                                .relativeTime(LocalContext.current)
                        } ?: "Unknown"
                    )
                }
            }
        }
    }
}