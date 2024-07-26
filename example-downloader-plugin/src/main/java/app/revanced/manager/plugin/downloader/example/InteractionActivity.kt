package app.revanced.manager.plugin.downloader.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier

class InteractionActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkTheme = isSystemInDarkTheme()

            val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("User interaction example") }
                        )
                    }
                ) { paddingValues ->
                    Column(modifier = Modifier.padding(paddingValues)) {
                        Text("This is an example interaction.")
                        Row {
                            TextButton(
                                onClick = {
                                    setResult(RESULT_CANCELED)
                                    finish()
                                }
                            ) {
                                Text("Cancel")
                            }

                            TextButton(
                                onClick = {
                                    setResult(RESULT_OK)
                                    finish()
                                }
                            ) {
                                Text("Continue")
                            }
                        }
                    }
                }
            }

        }
    }
}