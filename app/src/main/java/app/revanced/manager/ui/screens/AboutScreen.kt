package app.revanced.manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.IconButton
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.BuildConfig
import app.revanced.manager.Global.Companion.socialLinks
import app.revanced.manager.Global.Companion.websiteUrl
import app.revanced.manager.R
import app.revanced.manager.ui.components.PreferenceRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

private const val tag = "AboutScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph
@Composable
fun AboutScreen(
    //    navigator: NavController,
) {
    Column(Modifier.padding(8.dp)) {
        Box() {
            Icon(
                painterResource(id = R.drawable.ic_revanced),
                contentDescription = "Header Icon",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(32.dp)
                    .size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Divider(Modifier.alpha(.5f))

        var currentUriHandler = LocalUriHandler.current

        PreferenceRow(
            title = stringResource(R.string.app_version),
            subtitle = "${BuildConfig.VERSION_TYPE} ${BuildConfig.VERSION_NAME}",
            painter = painterResource(id = R.drawable.ic_baseline_info_24
            ),
            onClick = { /* TODO: COPY DEVICE INFORMATION AND APP INFO */ },
        )
        PreferenceRow(
            title = stringResource(R.string.whats_new),
            painter = painterResource(id = R.drawable.ic_baseline_new_releases_24
            ),
            onClick = { currentUriHandler.openUri(websiteUrl) },
        )
        PreferenceRow(
            title = stringResource(R.string.help_translate),
            painter = painterResource(id = R.drawable.ic_translate_black_24dp),
            onClick = { currentUriHandler.openUri(websiteUrl) }
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            for ((social_ic, uri) in socialLinks.entries) {
                IconButton(onClick = { currentUriHandler.openUri(uri) }) {
                    Icon(painter = painterResource(social_ic), contentDescription = "Links", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}