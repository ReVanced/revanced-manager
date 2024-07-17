package app.revanced.manager.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R

@Composable
fun ArrowButton(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onClick: (() -> Unit)?,
    rotationInitial: Float = 0f,
    rotationFinal: Float = 180f
) {
    val description = if (expanded) R.string.collapse_content else R.string.expand_content
    val rotation by animateFloatAsState(
        targetValue = if (expanded) rotationInitial else rotationFinal,
        label = "rotation"
    )

    onClick?.let {
        IconButton(onClick = it) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(description),
                modifier = Modifier
                    .rotate(rotation)
                    .then(modifier)
            )
        }
    } ?: Icon(
        imageVector = Icons.Filled.KeyboardArrowUp,
        contentDescription = stringResource(description),
        modifier = Modifier
            .rotate(rotation)
            .then(modifier)
    )
}