package app.revanced.manager.ui.component.onboarding

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.FloatRange
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import app.revanced.manager.R
import app.revanced.manager.util.blurBackground

@Composable
fun OnboardingAppCard(
    packageName: String,
    patchCount: Int,
    packageInfo: PackageInfo?,
    suggestedVersion: String?,
    loadAppLabel: () -> String?,
    loadAppIcon: () -> ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isInstalled = packageInfo != null
    val versionName = remember(packageName) { packageInfo?.versionName }

    // Extra app data is loaded async
    var appLabel by remember(packageName) { mutableStateOf<String?>(null) }
    var appIcon by remember { mutableStateOf<ImageBitmap?>(null) }
    var appIconBlur by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        appLabel = loadAppLabel()
        appIcon = loadAppIcon()
        appIconBlur = appIcon?.let {
            blurBackground(context, it.asAndroidBitmap(), 18f).asImageBitmap()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    ) {
        Box {
            // https://stackoverflow.com/a/68742173/13964629
            androidx.compose.animation.AnimatedVisibility(
                visible = appIconBlur != null,
                enter = fadeIn(),
                exit = ExitTransition.None,
                modifier = Modifier.matchParentSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                ) {
                    Image(
                        bitmap = appIconBlur!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f))
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Crossfade(
                    targetState = appIcon,
                    animationSpec = tween(durationMillis = 100),
                ) { appIcon ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Image(
                                painter = rememberVectorPainter(Icons.Default.Android),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = appLabel ?: packageName,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 1500,
                            initialDelayMillis = 2500,
                            spacing = MarqueeSpacing.fractionOfContainer(1f / 5f),
                            velocity = 55.dp,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        style = if (isInstalled) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        color = if (isInstalled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = versionName
                            ?: suggestedVersion?.let {
                                stringResource(R.string.onboarding_recommended_version, it)
                            }
                            ?: stringResource(R.string.not_installed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = pluralStringResource(R.plurals.patch_count, patchCount, patchCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isInstalled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}