package app.revanced.manager.ui.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.revanced.manager.util.FilePickRequest
import app.revanced.manager.util.UiRequest
import app.revanced.manager.util.uiRequestChannel

// The request currently being fulfilled. Lives outside the composition so that a result
// delivered after the activity is recreated (e.g. a rotation while the file picker
// is open) still completes the request instead of dropping it.
private var currentRequest: UiRequest<*>? = null

// Fulfills UI requests sent by background code. Mounted once at the root of the UI,
// unconditionally, so that pending activity results are always delivered.
@Composable
fun UiRequestHost() {
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        (currentRequest as? FilePickRequest)?.result?.complete(uri)
        currentRequest = null
    }

    LaunchedEffect(Unit) {
        // A request may still be in flight from before a configuration change.
        currentRequest?.result?.await()

        for (request in uiRequestChannel) {
            currentRequest = request
            when (request) {
                is FilePickRequest -> filePicker.launch("*/*")
            }

            // One request at a time.
            request.result.await()
        }
    }
}
