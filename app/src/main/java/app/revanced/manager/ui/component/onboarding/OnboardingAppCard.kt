package app.revanced.manager.ui.component.onboarding

import android.content.pm.PackageInfo
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.ExitTransition
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppIcon
import app.revanced.manager.ui.component.AppLabel
import app.revanced.manager.util.blurBackground
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OnboardingAppCard(
    packageName: String,
    patchCount: Int,
    packageInfo: PackageInfo?,
    suggestedVersion: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isInstalled = packageInfo != null
    val versionName = remember(packageName) { packageInfo?.versionName }

    var appIconBlur by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        appIconBlur = null
        if (packageInfo != null) {
            val result = withContext(Dispatchers.IO) {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(packageInfo)
                        .size(128, 128)
                        .allowHardware(false) // software bitmap!?
                        .build()
                )
            }
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            bitmap?.let {
                appIconBlur = withContext(Dispatchers.Default) {
                    blurBackground(context, it, 18f).asImageBitmap()
                }
            }
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
                        modifier = Modifier.fillMaxSize()
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        packageInfo = packageInfo,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Column(Modifier.weight(1f)) {
                    AppLabel(
                        packageInfo = packageInfo,
                        defaultText = packageName,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 1500,
                            initialDelayMillis = 2500,
                            spacing = MarqueeSpacing.fractionOfContainer(1f / 5f),
                            velocity = 55.dp,
                        ),
                        style = LocalTextStyle.current.merge(
                            if (isInstalled) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleSmall
                            }
                        ).copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isInstalled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
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