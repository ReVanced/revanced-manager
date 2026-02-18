package app.revanced.manager.ui.component.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.LazyColumnWithScrollbarEdgeShadow
import app.revanced.manager.ui.component.LoadingIndicator
import app.revanced.manager.util.AppInfo

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingAppList(
    apps: List<AppInfo>?,
    suggestedVersions: Map<String, String?>,
    loadLabel: (android.content.pm.PackageInfo?) -> String,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var hasClicked by rememberSaveable { mutableStateOf(false) }

    if (apps == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
    } else {
        LazyColumnWithScrollbarEdgeShadow(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            itemsIndexed(
                items = apps,
                key = { _, app -> app.packageName }
            ) { index, app ->
                OnboardingAppListItem(
                    app = app,
                    index = index,
                    count = apps.size,
                    loadLabel = loadLabel,
                    suggestedVersion = suggestedVersions[app.packageName],
                    onClick = {
                        if (!hasClicked) {
                            hasClicked = true
                            onAppClick(app.packageName)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnboardingAppListItem(
    app: AppInfo,
    index: Int,
    count: Int,
    loadLabel: (android.content.pm.PackageInfo?) -> String,
    suggestedVersion: String?,
    onClick: () -> Unit
) {
    val patchCount = app.patches ?: 0
    val isInstalled = app.packageInfo != null

    SegmentedListItem(
        onClick = onClick,
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isInstalled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    packageInfo = app.packageInfo,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        supportingContent = {
            val supporting = when {
                isInstalled -> app.packageInfo.versionName ?: stringResource(R.string.not_installed)
                suggestedVersion != null -> stringResource(
                    R.string.onboarding_recommended_version,
                    suggestedVersion
                )
                else -> stringResource(R.string.not_installed)
            }
            Text(supporting)
        },
        trailingContent = {
            Text(
                text = pluralStringResource(R.plurals.patch_count, patchCount, patchCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        Text(
            text = if (isInstalled) loadLabel(app.packageInfo) else app.packageName,
            fontWeight = FontWeight.SemiBold
        )
    }
}
