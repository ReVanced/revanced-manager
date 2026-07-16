package app.revanced.manager.ui.component.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.revanced.manager.ui.component.LazyColumnWithScrollbarEdgeShadow
import app.revanced.manager.util.AppInfo

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingAppList(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    suggestedVersions: Map<String, String?>,
    onAppClick: (String) -> Unit,
) {
    LazyColumnWithScrollbarEdgeShadow(
        modifier = modifier.fillMaxSize(),
        state = rememberLazyListState(
            cacheWindow = LazyLayoutCacheWindow(ahead = 100.dp, behind = 250.dp),
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        items(
            items = apps,
            key = { app -> app.packageName }
        ) { app ->
            OnboardingAppCard(
                packageName = app.packageName,
                patchCount = app.patches ?: 0,
                packageInfo = app.packageInfo,
                suggestedVersion = suggestedVersions[app.packageName],
                onClick = { onAppClick(app.packageName) },
            )
        }
    }
}
