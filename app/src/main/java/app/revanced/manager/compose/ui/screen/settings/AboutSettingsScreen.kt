package app.revanced.manager.compose.ui.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.revanced.manager.compose.R
import app.revanced.manager.compose.ui.component.AppTopBar
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val icon = rememberDrawablePainter(context.packageManager.getApplicationIcon(context.packageName))

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.about),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(painter = icon, contentDescription = null)
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text("Version 1.0.0 (100000000)", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(onClick = { /*TODO*/ }) {
                        Text("Website")
                    }
                    FilledTonalButton(onClick = { /*TODO*/ }) {
                        Text("Donate")
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = { /*TODO*/ }) {
                        Text("GitHub")
                    }
                    OutlinedButton(onClick = { /*TODO*/ }) {
                        Text("Contact")
                    }
                    OutlinedButton(onClick = { /*TODO*/ }) {
                        Text("License")
                    }
                }
            }
            
            ListItem(
                modifier = Modifier.clickable {  },
                headlineContent = { Text(stringResource(R.string.contributors)) },
                supportingContent = { Text(stringResource(R.string.contributors_description)) }
            )
        }
    }
}