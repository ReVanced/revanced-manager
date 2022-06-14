package app.revanced.manager.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import app.revanced.manager.R
import app.revanced.manager.backend.utils.openDiscord
import app.revanced.manager.backend.utils.openGitHub
import app.revanced.manager.ui.components.placeholders.Icon


@Composable
fun AppBar(title: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit = {}, navigationIcon: @Composable () -> Unit = {}) {
    SmallTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions
    )
}

@Preview(name = "Top App Bar Preview")
@Composable
fun AppBarPreview() {
    AppBar(
        title = { Text("ReVanced Manager") },
        actions = {
            Icon(resourceId = R.drawable.ic_discord_24, contentDescription = "Discord")
            Icon(resourceId = R.drawable.ic_github_24, contentDescription = "GitHub")
        }
    )
}