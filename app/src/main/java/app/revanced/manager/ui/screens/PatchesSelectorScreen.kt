package app.revanced.manager.ui.screens

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Switch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import app.revanced.manager.R
import app.revanced.manager.ui.components.AppList
import app.revanced.manager.ui.components.DialogAppBar
import app.revanced.manager.ui.components.placeholders.applist.AppIcon

@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("QueryPermissionsNeeded")
@Composable
fun PatchesSelectorScreen(
    patches: Array<String>,
    filter: Array<String>
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