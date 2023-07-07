package app.revanced.manager.ui.screen.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.revanced.manager.R
import app.revanced.manager.ui.component.AppTopBar
import app.revanced.manager.ui.component.GroupHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val memoryLimit = remember {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        context.getString(R.string.device_memory_limit_format, activityManager.memoryClass, activityManager.largeMemoryClass)
    }
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.advanced),
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
            GroupHeader(stringResource(R.string.device))
            ListItem(
                headlineContent = { Text(stringResource(R.string.device_model)) },
                supportingContent = { Text(Build.MODEL) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.device_android_version)) },
                supportingContent = { Text(Build.VERSION.RELEASE) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.device_architectures)) },
                supportingContent = { Text(Build.SUPPORTED_ABIS.joinToString(", ")) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.device_memory_limit)) },
                supportingContent = { Text(memoryLimit) }
            )
        }
    }
}