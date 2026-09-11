package app.revanced.manager.ui.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.revanced.manager.util.UiFilePicker
import app.revanced.manager.util.filePickerRequestChannel
import org.koin.compose.koinInject

// Fulfills file pick requests sent by background code. Mounted once at the root
// of the UI, unconditionally, so pending activity results are always delivered.
@Composable
fun FilePickerRequestHost(picker: UiFilePicker = koinInject()) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        picker.currentRequest?.result?.complete(uri)
        picker.currentRequest = null
    }

    LaunchedEffect(Unit) {
        // A request may still be in flight from before a configuration change.
        picker.currentRequest?.result?.await()

        for (request in filePickerRequestChannel) {
            picker.currentRequest = request
            launcher.launch("*/*")

            // One request at a time.
            request.result.await()
        }
    }
}
