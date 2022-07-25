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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settings: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val darkLight: Flow<Boolean> = baseContext.settings.data.map { preferences ->
            preferences[booleanPreferencesKey("darklight")] ?: true
        }

        val dynamicColor: Flow<Boolean> = baseContext.settings.data.map { preferences ->
            preferences[booleanPreferencesKey("dynamicTheming")] ?: true
        }

        setContent {
            val dlState = darkLight.collectAsState(initial = isSystemInDarkTheme())
            val dcState = dynamicColor.collectAsState(initial = true)

            ReVancedManagerTheme(
                darkTheme = dlState.value,
                dynamicColor = dcState.value
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
}