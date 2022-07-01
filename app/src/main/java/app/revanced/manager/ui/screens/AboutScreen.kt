package app.revanced.manager.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import app.revanced.manager.R
import app.revanced.manager.Global.Companion.socialLinks
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import app.revanced.manager.Global.Companion.websiteUrl
import app.revanced.manager.ui.components.ExpandableCard
import app.revanced.manager.ui.components.PreferenceRow
import app.revanced.manager.ui.models.AboutViewModel

private const val tag = "AboutScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph
@Composable
fun AboutScreen(
//    navigator: NavController,
    vm: AboutViewModel = viewModel()
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

        ExpandableCard(stringResource(R.string.patcher_credits))

        var currentUriHandler = LocalUriHandler.current

        PreferenceRow(
            title = stringResource(R.string.whats_new),
            onClick = { currentUriHandler.openUri(websiteUrl) },
        )

        PreferenceRow(
            title = stringResource(R.string.help_translate),
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
