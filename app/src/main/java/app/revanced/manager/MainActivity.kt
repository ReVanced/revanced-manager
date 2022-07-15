package app.revanced.manager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.revanced.manager.ui.screens.MainScreen
import app.revanced.manager.ui.theme.ReVancedManagerTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settings: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darklight: Flow<Boolean> = baseContext.settings.data.map { preferences ->
            preferences.get(booleanPreferencesKey("darklight")) ?: false
        }
        setContent {
            val darklightstate = darklight.collectAsState(initial = false)
            ReVancedManagerTheme(darkTheme = darklightstate.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val systemUiController = rememberSystemUiController()
                    val useDarkIcons = !darklightstate.value
                    val background = MaterialTheme.colorScheme.background
                    SideEffect {
                        systemUiController.setSystemBarsColor(
                            color = background,
                            darkIcons = useDarkIcons
                        )

                    }

                    MainScreen()
                }
            }
        }
    }
}

@Preview
@Composable
fun FullPreview() {
    ReVancedManagerTheme {
        MainScreen()
    }
}