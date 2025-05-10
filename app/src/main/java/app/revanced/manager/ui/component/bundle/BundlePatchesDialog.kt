package app.revanced.manager.ui.component.bundle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.ArrowButton
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundlePatchesDialog(
    onDismissRequest: () -> Unit,
    bundle: PatchBundleSource,
) {
    var showAllVersions by rememberSaveable { mutableStateOf(false) }
    var showOptions by rememberSaveable { mutableStateOf(false) }
    val state by bundle.state.collectAsStateWithLifecycle()

    FullscreenDialog(
        onDismissRequest = onDismissRequest,
    ) {
        Scaffold(
            topBar = {
                BundleTopBar(
                    title = stringResource(R.string.patches),
                    onBackClick = onDismissRequest,
                    backIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    },
                )
            },
        ) { paddingValues ->
            LazyColumnWithScrollbar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                state.patchBundleOrNull()?.let { bundle ->
                    items(bundle.patches) { patch ->
                        PatchItem(
                            patch,
                            showAllVersions,
                            onExpandVersions = { showAllVersions = !showAllVersions },
                            showOptions,
                            onExpandOptions = { showOptions = !showOptions }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatchItem(
    patch: PatchInfo,
    expandVersions: Boolean,
    onExpandVersions: () -> Unit,
    expandOptions: Boolean,
    onExpandOptions: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (patch.options.isNullOrEmpty()) Modifier else Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onExpandOptions),
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = patch.name,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (!patch.options.isNullOrEmpty()) {
                    ArrowButton(expanded = expandOptions, onClick = null)
                }
            }
            patch.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (patch.compatiblePackages.isNullOrEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PatchInfoChip(
                            text = "$PACKAGE_ICON ${stringResource(R.string.patches_view_any_package)}"
                        )
                        PatchInfoChip(
                            text = "$VERSION_ICON ${stringResource(R.string.patches_view_any_version)}"
                        )
                    }
                } else {
                    patch.compatiblePackages.forEach { compatiblePackage ->
                        val packageName = compatiblePackage.packageName
                        val versions = compatiblePackage.versions.orEmpty().reversed()

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PatchInfoChip(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                text = "$PACKAGE_ICON $packageName"
                            )

                            if (versions.isNotEmpty()) {
                                if (expandVersions) {
                                    versions.forEach { version ->
                                        PatchInfoChip(
                                            modifier = Modifier.align(Alignment.CenterVertically),
                                            text = "$VERSION_ICON $version"
                                        )
                                    }
                                } else {
                                    PatchInfoChip(
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                        text = "$VERSION_ICON ${versions.first()}"
                                    )
                                }
                                if (versions.size > 1) {
                                    PatchInfoChip(
                                        onClick = onExpandVersions,
                                        text = if (expandVersions) stringResource(R.string.less) else "+${versions.size - 1}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (!patch.options.isNullOrEmpty()) {
                AnimatedVisibility(visible = expandOptions) {
                    val options = patch.options

                    Column {
                        options.forEachIndexed { i, option ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = Color.Transparent,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface
                                ), shape = when {
                                    options.size == 1 -> RoundedCornerShape(8.dp)
                                    i == 0 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    i == options.lastIndex -> RoundedCornerShape(
                                        bottomStart = 8.dp,
                                        bottomEnd = 8.dp
                                    )

                                    else -> RoundedCornerShape(0.dp)
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatchInfoChip(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    text: String
) {
    val shape = RoundedCornerShape(8.0.dp)
    val cardModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    OutlinedCard(
        modifier = modifier.then(cardModifier),
        colors = CardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

const val PACKAGE_ICON = "\uD83D\uDCE6"
const val VERSION_ICON = "\uD83C\uDFAF"