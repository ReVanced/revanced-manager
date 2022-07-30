package app.revanced.manager.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import app.revanced.manager.Global
import app.revanced.manager.settings
import com.jamal.composeprefs3.ui.PrefsScreen
import com.jamal.composeprefs3.ui.prefs.EditTextPref
import com.jamal.composeprefs3.ui.prefs.SwitchPref
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

private const val tag = "SettingsScreen"

@OptIn(ExperimentalComposeUiApi::class)
@Destination
@RootNavGraph
@Composable
fun SettingsScreen(
) {

    val data = LocalContext.current.settings.data
    val prefs by remember { data }.collectAsState(initial = null)
    PrefsScreen(dataStore = LocalContext.current.settings) {
        prefsGroup("Appearance") {
            prefsItem {
                SwitchPref(
                    key = "dynamicTheming",
                    title = "Material You",
                )
            }
            prefsItem {
                SwitchPref(
                    key = "darklight",
                    title = "Change Theme",
                    summary = "Light/Dark",
                )
            }
        }
        prefsGroup("Sources") {
            prefsItem {
                EditTextPref(
                    key = "patches",
                    title = "Patches source",
                    summary = prefs?.get(stringPreferencesKey("patches")),
                    dialogTitle = "Patches source",
                    dialogMessage = "Specify where to grab patches.",
                    defaultValue = "${Global.ghPatches}",
                )
            }
            prefsItem {
                EditTextPref(
                    key = "integrations",
                    title = "Integrations source",
                    summary = prefs?.get(stringPreferencesKey("integrations")),
                    dialogTitle = "Integrations source",
                    dialogMessage = "Specify where to grab integrations.",
                    defaultValue = "${Global.ghIntegrations}",
                )
            }
        }
    }
}
