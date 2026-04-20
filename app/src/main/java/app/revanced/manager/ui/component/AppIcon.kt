package app.revanced.manager.ui.component

import android.content.pm.PackageInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.yield

@Composable
fun AppIcon(
    packageInfo: PackageInfo?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val placeholderPainter = rememberVectorPainter(Icons.Default.Android)
    val placeholderColorFilter = ColorFilter.tint(LocalContentColor.current)

    if (packageInfo == null) {
        Image(
            painter = placeholderPainter,
            contentDescription = contentDescription,
            modifier = modifier,
            colorFilter = placeholderColorFilter
        )
    } else {
        var showPlaceHolder by remember(packageInfo.packageName) { mutableStateOf(true) }
        var shouldLoadIcon by remember(packageInfo.packageName) { mutableStateOf(false) }

        val request = remember(context, packageInfo.packageName) {
            ImageRequest.Builder(context)
                .data(packageInfo)
//                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
        }

        LaunchedEffect(packageInfo.packageName) {
            yield()
            shouldLoadIcon = true
        }

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            if (shouldLoadIcon) {
                AsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    modifier = Modifier.alpha(if (showPlaceHolder) 0f else 1f),
                    onSuccess = { showPlaceHolder = false },
                    onError = { showPlaceHolder = true }
                )
            }

            if (showPlaceHolder) {
                Image(
                    painter = placeholderPainter,
                    contentDescription = contentDescription,
                    colorFilter = placeholderColorFilter
                )
            }
        }
    }
}