package app.revanced.manager.ui.components.placeholders.applist

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun AppIcon(drawable: Drawable?, contentDescription: String?) {
    Image(
        rememberDrawablePainter(drawable),
        contentDescription,
        Modifier.size(48.dp)
    )
}