package app.revanced.manager.ui.screens

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.revanced.manager.ui.theme.ReVancedManagerTheme

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("QueryPermissionsNeeded")
class PatchesSelectorActivity(
    val patches: Array<String>,
    val filter: Array<String>
) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReVancedManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val rl = rememberLazyListState()
                    rl.interactionSource

                    LazyColumn {
                        items(count = patches.size) {
                            var selected by rememberSaveable { mutableStateOf(true) }
                            ListItem(modifier = Modifier.clickable {
                                selected = !selected
                            },
                                text = {
                                    Text(patches[it])
                                },
                                trailing = {
                                    Checkbox(checked = selected, onCheckedChange = {selected = !selected})
//                    androidx.compose.material3.Switch(checked = selected, onCheckedChange = { selected = !selected })
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
