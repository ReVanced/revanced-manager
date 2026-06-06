package app.revanced.manager.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize

@Composable
fun AnimateVisibility(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val motionSpec: FiniteAnimationSpec<IntSize> = MaterialTheme.motionScheme.fastSpatialSpec()

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(motionSpec) + fadeIn(),
        exit = shrinkVertically(motionSpec) + fadeOut(),
        content = content,
    )
}
