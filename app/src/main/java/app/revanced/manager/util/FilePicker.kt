package app.revanced.manager.util

import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel

// A request to let the user pick a file, answered with a content:// URI
// or null if the picker was cancelled. The sender suspends on [result]
// until the UI completes it.
class FilePickerRequest {
    val result = CompletableDeferred<Uri?>()
}

val filePickerRequestChannel = Channel<FilePickerRequest>()

// Lets background code ask the user to pick a file without dealing with
// the request machinery.
interface FilePicker {
    suspend fun pickFile(): Uri?
}

class UiFilePicker : FilePicker {
    // The request currently shown to the user. Lives here rather than in the
    // composition so a result delivered after activity recreation still
    // completes it.
    var currentRequest: FilePickerRequest? = null

    override suspend fun pickFile(): Uri? {
        val request = FilePickerRequest()
        filePickerRequestChannel.send(request)

        return request.result.await()
    }
}
