package app.revanced.manager.ui.component

import android.content.pm.PackageInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import coil.compose.AsyncImage
import io.github.fornewid.placeholder.material3.placeholder

@Composable
fun AppIcon(
    packageInfo: PackageInfo?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var showPlaceHolder by rememberSaveable { mutableStateOf(true) }

    if (packageInfo == null) {
        val image = rememberVectorPainter(Icons.Default.Android)
        val colorFilter = ColorFilter.tint(LocalContentColor.current)

        Image(
            image,
            contentDescription,
            modifier,
            colorFilter = colorFilter
        )
    } else {
        AsyncImage(
            packageInfo,
            contentDescription,
            Modifier.placeholder(visible = showPlaceHolder, color = MaterialTheme.colorScheme.inverseOnSurface, shape = RoundedCornerShape(100)).then(modifier),
            onSuccess = { showPlaceHolder = false }
        )
    }
}