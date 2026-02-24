package app.revanced.manager.ui.component.bundle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.ArrowButton
import app.revanced.manager.ui.component.FullscreenDialog
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.SearchView
import kotlinx.coroutines.flow.mapNotNull
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundlePatchesDialog(
    onDismissRequest: () -> Unit,
    src: PatchBundleSource,
) {
    val patchBundleRepository: PatchBundleRepository = koinInject()
    var query by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val patches by remember(src.uid) {
        patchBundleRepository.bundleInfoFlow.mapNotNull { it[src.uid]?.patches }
    }.collectAsStateWithLifecycle(emptyList())
    val filteredPatches = remember(patches, query) {
        if (query.isEmpty()) {
            patches
        } else {
            patches.filter { patch ->
                patch.name.contains(query, ignoreCase = true) ||
                patch.description?.contains(query, ignoreCase = true) == true ||
                patch.compatiblePackages?.any { compatiblePackage ->
                    compatiblePackage.packageName.contains(query, ignoreCase = true) ||
                    compatiblePackage.versions?.any { version ->
                        version.contains(query, ignoreCase = true)
                    } == true
                } == true
            }
        }
    }

    FullscreenDialog(
        onDismissRequest = onDismissRequest,
    ) {
        if (isSearchActive) {
            SearchView(
                query = query,
                onQueryChange = { query = it },
                onActiveChange = { isSearchActive = it },
                placeholder = { Text(stringResource(R.string.search)) }
            ) {
                when {
                    query.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.search_patches),
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.type_anything),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    filteredPatches.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_patch_found),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        PatchList(
                            patches = filteredPatches,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        } else {
            Scaffold(
                topBar = {
                    BundleTopBar(
                        title = stringResource(R.string.patches),
                        onBackClick = onDismissRequest,
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.search_patches)
                                )
                            }
                        },
                        backIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        },
                    )
                },
            ) { paddingValues ->
                PatchList(
                    patches = patches,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun PatchList(
    patches: List<PatchInfo>,
    modifier: Modifier = Modifier
) {
    LazyColumnWithScrollbar(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(items = patches) { patch ->
            PatchItem(patch)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatchItem(
    patch: PatchInfo
) {
    var expandedVersionPackages by rememberSaveable { mutableStateOf(setOf<String>()) }
    var expandOptions by rememberSaveable { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (patch.options.isNullOrEmpty()) Modifier else Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expandOptions = !expandOptions },
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
                        val expandVersions = packageName in expandedVersionPackages

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
                                        onClick = {
                                            expandedVersionPackages = if (expandVersions) {
                                                expandedVersionPackages - packageName
                                            } else {
                                                expandedVersionPackages + packageName
                                            }
                                        },
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
                                        text = option.name,
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