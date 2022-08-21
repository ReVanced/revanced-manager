package app.revanced.manager.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.revanced.manager.R
import app.revanced.manager.util.ghOrganization
import app.revanced.manager.util.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialItem(@StringRes label: Int, vec: ImageVector, fn: () -> Unit) {
    val rsc = stringResource(label)
    ListItem(
        modifier = Modifier.clickable { fn() },
        leadingContent = {
            Icon(
                imageVector = vec,
                contentDescription = rsc
            )
        },
        headlineText = { Text(rsc) }
    )
}

@Preview
@Composable
fun SocialItemPreview() {
    val ctx = LocalContext.current.applicationContext
    SocialItem(R.string.github, Icons.Default.Code) {
        ctx.openUrl(ghOrganization)
    }
}