package app.revanced.manager.compose.ui.component

import android.graphics.drawable.Drawable
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
import coil.compose.rememberAsyncImagePainter

@Composable
fun AppIcon(
    drawable: Drawable?,
    contentDescription: String?,
    size: Int = 48
) {
    if (drawable == null) {
        val image = rememberVectorPainter(Icons.Default.Android)
        val colorFilter = ColorFilter.tint(LocalContentColor.current)

        Image(
            image,
            contentDescription,
            Modifier.size(size.dp),
            colorFilter = colorFilter
        )
    } else {
        val image = rememberAsyncImagePainter(drawable)

        Image(
            image,
            contentDescription,
            Modifier.size(size.dp)
        )
    }
}