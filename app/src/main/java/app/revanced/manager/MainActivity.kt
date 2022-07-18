package app.revanced.manager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.revanced.manager.ui.screens.MainScreen
import app.revanced.manager.ui.theme.ReVancedManagerTheme
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settings: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val darklight: Flow<Boolean> = baseContext.settings.data.map { preferences ->
            preferences.get(booleanPreferencesKey("darklight")) ?: true
        }

        val dynamicColor: Flow<Boolean> = baseContext.settings.data.map { preferences ->
            preferences.get(booleanPreferencesKey("dynamicTheming")) ?: true
        }

        Shell.getShell()

        setContent {
            val darklightstate = darklight.collectAsState(initial = isSystemInDarkTheme())

            val dynamicColorstate = dynamicColor.collectAsState(initial = true)

            ReVancedManagerTheme(
                darkTheme = darklightstate.value,
                dynamicColor = dynamicColorstate.value
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }
}