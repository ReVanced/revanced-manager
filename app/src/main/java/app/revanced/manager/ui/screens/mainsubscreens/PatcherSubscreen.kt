package app.revanced.manager.ui.screens.mainsubscreens


import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.revanced.manager.Global.Companion.showBar
import app.revanced.manager.R
import app.revanced.manager.backend.vital.ApkUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// patcher_subscreen
fun PatcherSubscreen(

) {
    var selectedAppPackage by rememberSaveable { mutableStateOf("") }
    val sela = stringResource(id = R.string.card_application_body)
    val selb = stringResource(id = R.string.card_application_body_selected)
    var e = ""
    val pm = ApkUtil(LocalContext.current)

    val applications = pm.getInstalledApplications()
//    resultRecipient.onNavResult { result ->
//        when (result) {
//            is NavResult.Canceled -> {}
//            is  NavResult.Value ->  {
//                selectedAppPackage = result.value
//                showBar = true
//                println(pm.pathFromPackageName(packageName = result.value))
//            }
//        }
//    }
    Scaffold(floatingActionButton = {
        ExtendedFloatingActionButton(onClick = {  null }, icon = {Icon(imageVector = Icons.Default.Build, contentDescription = "sd")}, text = {Text(text = "Patch")})
    }) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)) {
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                onClick = {
                    showBar = false
//                    navigator.navigate(
//                        AppSelectorScreenDestination(applications, arrayOf("aboba")).route
//                    )
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_application_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    when(selectedAppPackage) {
                        "" -> {
                            e = sela
                        }
                        else -> {
                            e = selectedAppPackage
                        }
                    }
                    Text(
                        text = e,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                onClick = {
//                    navigator.navigate(
//                        PatchesSelectorScreenDestination(arrayOf("patch-1", "patch-2", "patch-3"), arrayOf("aboba")).route
//                    )
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_patches_header),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(id = R.string.card_patches_body_patches),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                }
            }

        }
    }

}

@Preview
@Composable
fun PatcherSubscreenPreview() {
    // PatcherSubscreen()
}