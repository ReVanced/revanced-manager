package app.revanced.manager.ui.screens.mainsubscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.ui.components.HelpDialog
import app.revanced.manager.ui.components.LogoHeader
import app.revanced.manager.ui.components.PreferenceRow
import app.revanced.manager.ui.screens.destinations.AboutScreenDestination
import app.revanced.manager.ui.screens.destinations.SettingsScreenDestination
import app.revanced.manager.ui.screens.destinations.SettingsTempScreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

@Destination
@Composable
@RootNavGraph
fun MoreSubscreen(
    navigator: NavController,
) {
    Column(Modifier.padding(8.dp)) {
        LogoHeader()

        PreferenceRow(
            title = stringResource(R.string.screen_settings_title),
            painter = painterResource(id = R.drawable.ic_baseline_settings_24),
            onClick = { navigator.navigate(SettingsScreenDestination().route) }
        )
        PreferenceRow(
            title = ("Settings TEMP"),
            painter = painterResource(id = R.drawable.ic_baseline_settings_24),
            onClick = { navigator.navigate(SettingsTempScreenDestination().route) }
        )

        PreferenceRow(
            title = stringResource(R.string.screen_about_title),
            painter = painterResource(id = R.drawable.ic_baseline_info_24),
            onClick = { navigator.navigate(AboutScreenDestination().route) }
        )

        HelpDialog()
    }
}