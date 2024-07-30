package app.revanced.manager.ui.component.bundle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.component.ArrowButton
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.NotificationCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundlePatchesDialog(
    onDismissRequest: () -> Unit,
    bundle: PatchBundleSource,
) {
    var informationCardVisible by remember { mutableStateOf(true) }
    var showAllVersions by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    val state by bundle.state.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismissRequest, properties = DialogProperties(
            usePlatformDefaultWidth = false, dismissOnBackPress = true
        )
    ) {
        Scaffold(
            topBar = {
                BundleTopBar(
                    title = stringResource(R.string.bundle_patches),
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
                item {
                    AnimatedVisibility(visible = informationCardVisible) {
                        NotificationCard(icon = Icons.Outlined.Lightbulb,
                            text = stringResource(R.string.tap_on_patches),
                            onDismiss = { informationCardVisible = false })
                    }
                }

                state.patchBundleOrNull()?.let { bundle ->
                    items(bundle.patches.size) { bundleIndex ->
                        val patch = bundle.patches[bundleIndex]
                        PatchItem(patch,
                            showAllVersions,
                            onExpandVersions = { showAllVersions = !showAllVersions },
                            showOptions,
                            onExpandOptions = { showOptions = !showOptions })
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
                    .clickable { onExpandOptions() },
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    ArrowButton(expanded = expandOptions)
                }
            }
            patch.description?.let {
                Text(
                    text = it, style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!patch.compatiblePackages.isNullOrEmpty()) {
                    patch.compatiblePackages.forEach { compatiblePackage ->
                        val packageName = compatiblePackage.packageName
                        val versions = compatiblePackage.versions.orEmpty().reversed()
                        val itemCount = if (versions.isEmpty()) 1 else versions.size + 1

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PatchInfoChip(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                text = "\uD83D\uDCE6 $packageName"
                            )

                            if (versions.isNotEmpty()) {
                                if (expandVersions) {
                                    versions.forEach { version ->
                                        PatchInfoChip(
                                            modifier = Modifier.align(Alignment.CenterVertically),
                                            text = "\uD83C\uDFAF $version"
                                        )
                                    }
                                } else {
                                    PatchInfoChip(
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                        text = "\uD83C\uDFAF ${versions.first()}"
                                    )
                                }
                                if (versions.size > 1) {
                                    PatchInfoChip(
                                        onClick = onExpandVersions,
                                        text = if (expandVersions) "Less" else "+${itemCount - 2}"
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PatchInfoChip(
                            text = "\uD83D\uDCE6 Any package"
                        )
                        PatchInfoChip(
                            text = "\uD83C\uDFAF Any version"
                        )
                    }
                }
            }
            if (!patch.options.isNullOrEmpty()) {
                AnimatedVisibility(visible = expandOptions) {
                    val options = patch.options

                    Column {
                        options.forEachIndexed { i, option ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(), colors = CardColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = Color.Transparent,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface
                                ), shape = when {
                                    i == 0 && options.lastIndex == 0 -> RoundedCornerShape(8.dp)
                                    i == 0 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    i == options.lastIndex -> RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
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
                                        text = option.description, style = MaterialTheme.typography.bodyMedium
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
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, text: String, icon: @Composable (() -> Unit)? = null
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
        modifier = modifier.then(cardModifier), colors = CardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ), shape = shape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f))
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