package app.revanced.manager.compose.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import app.revanced.manager.compose.util.AppInfo
import coil.compose.AsyncImage

@Composable
fun AppIcon(
    app: AppInfo,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (app.packageInfo == null) {
        val image = rememberVectorPainter(Icons.Default.Android)
        val colorFilter = ColorFilter.tint(LocalContentColor.current)

        Image(
            image,
            contentDescription,
            Modifier.size(36.dp).then(modifier),
            colorFilter = colorFilter
        )
    } else {
        AsyncImage(
            app.packageInfo,
            contentDescription,
            Modifier.size(36.dp).then(modifier)
        )
    }
}