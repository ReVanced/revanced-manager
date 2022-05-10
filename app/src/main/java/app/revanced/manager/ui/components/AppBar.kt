package app.revanced.manager.ui.components

import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import app.revanced.manager.R
import app.revanced.manager.backend.utils.openDiscord
import app.revanced.manager.backend.utils.openGitHub


@Composable
fun AppBar() {
    val currentUriHandler = LocalUriHandler.current

    SmallTopAppBar(
        title = {
            Text("ReVanced Manager")
        },
        actions = {
            IconButton(onClick = { openDiscord(currentUriHandler) }) {
                Icon(resourceId = R.drawable.ic_discord_24, contentDescription = "Discord")
            }
            IconButton(onClick = { openGitHub(currentUriHandler) }) {
                Icon(resourceId = R.drawable.ic_github_24, contentDescription = "GitHub")
            }
        }
    )
}

@Preview(name = "Top App Bar Preview")
@Composable
fun AppBarPreview() {
    AppBar()
}