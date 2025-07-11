package app.revanced.manager.ui.component.bundle

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.tooltip.TooltipIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BundleTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    backIcon: @Composable () -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (onBackClick != null) {
                TooltipIconButton(
                    modifier = Modifier,
                    tooltip = stringResource(R.string.back),
                    onClick = onBackClick
                ) {
                    backIcon()
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}