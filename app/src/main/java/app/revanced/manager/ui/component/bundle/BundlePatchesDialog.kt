package app.revanced.manager.ui.component.bundle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.ui.component.LazyColumnWithScrollbar
import app.revanced.manager.ui.component.NotificationCard
import kotlinx.coroutines.flow.mapNotNull
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundlePatchesDialog(
    bundle: PatchBundleSource,
    onDismissRequest: () -> Unit,
) {
    var informationCardVisible by remember { mutableStateOf(true) }
    val patchBundleRepository: PatchBundleRepository = koinInject()
    val patches by remember(bundle.uid) {
        patchBundleRepository.bundleInfoFlow.mapNotNull { it[bundle.uid]?.patches }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

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
                    onBackIcon = {
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
                    .padding(paddingValues)
                    .padding(16.dp)
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

                items(patches.size) { index ->
                    val patch = patches[index]

                    ListItem(
                        headlineContent = {
                            Text(
                                text = patch.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            patch.description?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
