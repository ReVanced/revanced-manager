package app.revanced.manager.ui.component.onboarding

import android.content.pm.PackageInfo
import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.revanced.manager.ui.component.LazyColumnWithScrollbarEdgeShadow
import app.revanced.manager.util.AppInfo
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingAppList(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    suggestedVersions: Map<String, String?>,
    onAppClick: (String) -> Unit,
) {
    val pm = LocalContext.current.packageManager
    val appIcons = remember { ConcurrentHashMap<String, Optional<ImageBitmap>>() }
    val appLabels = remember { ConcurrentHashMap<String, String>() }

    fun getAppIcon(packageInfo: PackageInfo): ImageBitmap? {
        return appIcons.computeIfAbsent(packageInfo.packageName) {
            val icon = packageInfo.applicationInfo
                ?.loadIcon(pm)
                ?.toBitmap(width = 128, height = 128, Bitmap.Config.ARGB_8888)
                ?.asImageBitmap()

            Optional.ofNullable(icon)
        }.getOrNull()
    }

    fun getAppLabel(packageInfo: PackageInfo): String {
        return appLabels.computeIfAbsent(packageInfo.packageName) {
            packageInfo.applicationInfo!!.loadLabel(pm).toString()
        }
    }

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
                loadAppLabel = { app.packageInfo?.let(::getAppLabel) },
                loadAppIcon = { app.packageInfo?.let(::getAppIcon) },
                onClick = { onAppClick(app.packageName) },
            )
        }
    }
}
