package app.revanced.manager.ui.screens.mainsubscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.revanced.manager.Global.Companion.websiteUrl
import app.revanced.manager.R
import app.revanced.manager.ui.components.FullScreenDialog
import app.revanced.manager.ui.components.HelpDialog
import app.revanced.manager.ui.components.IconHeader
import app.revanced.manager.ui.components.PreferenceRow
import app.revanced.manager.ui.screens.destinations.AboutScreenDestination
import app.revanced.manager.ui.screens.destinations.ContributorsScreenDestination
import app.revanced.manager.ui.screens.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

private const val tag = "MoreScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
@RootNavGraph
fun MoreSubscreen(
    navigator: NavController,
) {
    Column(Modifier.padding(8.dp)) {
        IconHeader()

        var currentUriHandler = LocalUriHandler.current

        PreferenceRow(
            title = stringResource(R.string.screen_settings_title),
            painter = painterResource(id = R.drawable.ic_baseline_settings_24),
            onClick = { navigator.navigate(SettingsScreenDestination().route) }
        )
        PreferenceRow(
            title = stringResource(R.string.screen_contributors_title),
            painter = painterResource(id = R.drawable.ic_baseline_favorite_24 ),
            onClick = { navigator.navigate(ContributorsScreenDestination().route) }
        )
        PreferenceRow(
            title = stringResource(R.string.screen_about_title),
            painter = painterResource(id = R.drawable.ic_baseline_info_24),
            onClick = { navigator.navigate(AboutScreenDestination().route) }
        )

        // TODO MAKE THESE DIALOGS REUSABLE
        HelpDialog()

        FullScreenDialog()
    }
}