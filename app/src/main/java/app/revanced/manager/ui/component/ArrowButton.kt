package app.revanced.manager.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R

@Composable
fun ArrowButton(expanded: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        val (icon, string) = if (expanded) Icons.Filled.KeyboardArrowUp to R.string.collapse_content else Icons.Filled.KeyboardArrowDown to R.string.expand_content

        Icon(
            imageVector = icon,
            contentDescription = stringResource(string)
        )
    }
}