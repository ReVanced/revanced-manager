package app.revanced.manager.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable

@Composable
fun ContentSelector(mime: String, onSelect: (Uri) -> Unit, content: @Composable () -> Unit) {
    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onSelect)
    }
    Button(
        onClick = {
            activityLauncher.launch(mime)
        }
    ) {
        content()
    }
}