package app.revanced.manager.ui.component.bundle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.ContextualFlowRowOverflowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.CardColors
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    val state by bundle.state.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
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
                        NotificationCard(
                            icon = Icons.Outlined.Lightbulb,
                            text = stringResource(R.string.tap_on_patches),
                            onDismiss = { informationCardVisible = false }
                        )
                    }
                }

                state.patchBundleOrNull()?.let { bundle ->
                    items(bundle.patches.size) { bundleIndex ->
                        val patch = bundle.patches[bundleIndex]
                        PatchItem(
                            patch,
                            showAllVersions,
                            onExpandVersions = { showAllVersions = !showAllVersions })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatchItem(patch: PatchInfo, expandVersions: Boolean, onExpandVersions: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = patch.name,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            patch.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                patch.compatiblePackages?.forEach { compatiblePackage ->
                    val packageName = compatiblePackage.packageName
                    val versions = compatiblePackage.versions.orEmpty()
                    val itemCount = if (versions.isEmpty()) 1 else versions.size + 1
                    var maxLines by remember { mutableIntStateOf(1) }

                    val moreOrCollapseIndicator =
                        @Composable { scope: ContextualFlowRowOverflowScope ->
                            val remainingItems = itemCount - scope.shownItemCount
                            PatchInfoChip(
                                text = if (remainingItems == 0) "Less" else "+$remainingItems",
                                onClick = {
                                    maxLines = if (remainingItems == 0) {
                                        1
                                    } else {
                                        Int.MAX_VALUE
                                    }
                                }
                            )
                        }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PatchInfoChip(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            text = "\uD83D\uDCE6 $packageName",
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
//                                ArrowButton(
//                                    modifier = Modifier.align(Alignment.CenterVertically),
//                                    expanded = expandVersions,
//                                    onClick = onExpandVersions,
//                                    rotationInitial = -90f,
//                                    rotationFinal = 90f
//                                )
                            }
                        }
                    }

                    ContextualFlowRow(
                        itemCount = itemCount,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        maxLines = maxLines,
                        overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                            minRowsToShowCollapse = 2,
                            expandIndicator = moreOrCollapseIndicator,
                            collapseIndicator = moreOrCollapseIndicator
                        ),
                    ) { index ->
                        if (index == 0) {
                            PatchInfoChip(
                                icon = {
                                    Icon(
                                        painterResource(R.drawable.ic_package),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.align(Alignment.CenterVertically),
                                onClick = { /* TODO: Handle click event */ },
                                text = packageName
                            )
                        } else {
                            PatchInfoChip(
                                icon = {
                                    Icon(
                                        painterResource(R.drawable.ic_bullseye),
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.align(Alignment.CenterVertically),
                                onClick = { /* TODO: Handle click event */ },
                                text = "${versions.elementAt(index - 1)}"
                            )
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
    text: String,
    icon: @Composable (() -> Unit)? = null
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
//    InputChip(
//        label = {
//            Text(
//                text,
//                overflow = TextOverflow.Ellipsis,
//                softWrap = false,
//                style = MaterialTheme.typography.labelLarge
//            )
//        },
//        selected = false,
//        onClick = onClick
//    )
//    SuggestionChip(
//        label = {
//            Text(
//                text,
//                overflow = TextOverflow.Ellipsis,
//                softWrap = false,
//                style = MaterialTheme.typography.labelLarge
//            )
//        },
//        onClick = onClick
//    )
//    ElevatedSuggestionChip(
//        label = {
//            Text(
//                text,
//                overflow = TextOverflow.Ellipsis,
//                softWrap = false,
//                style = MaterialTheme.typography.labelLarge
//            )
//        },
//        onClick = onClick
//    )
//    FilterChip(
//        label = {
//            Text(
//                text,
//                overflow = TextOverflow.Ellipsis,
//                softWrap = false,
//                style = MaterialTheme.typography.labelLarge
//            )
//        },
//        selected = false,
//        onClick = onClick
//    )

//    AssistChip(
//        modifier = modifier,
//        onClick = onClick,
//        leadingIcon = icon,
//        label = {
//            Text(
//                text,
//                overflow = TextOverflow.Ellipsis,
//                softWrap = false,
//                style = MaterialTheme.typography.labelLarge
//            )
//        },
//        border = AssistChipDefaults.assistChipBorder(
//            true,
//            MaterialTheme.colorScheme.outlineVariant
//        )
//    )
//    ElevatedAssistChip(
//        label = {
//            Text(
//                text,
//                overflow = TextOverflow.Ellipsis,
//                softWrap = false,
//                style = MaterialTheme.typography.labelLarge
//            )
//        },
//        onClick = onClick
//    )
}