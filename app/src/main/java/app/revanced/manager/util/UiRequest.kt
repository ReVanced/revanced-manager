package app.revanced.manager.util

import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel

// Communication layer for background code that needs the UI to fulfill a request,
// such as showing a system dialog. UI hosts receive requests from [uiRequestChannel]
// and complete [UiRequest.result] with the outcome.
sealed class UiRequest<T> {
    // The response slot. The sender suspends on it until the UI completes it.
    val result = CompletableDeferred<T>()
}

// A request to let the user pick a file, answered with a content:// URI
// or null if the picker was cancelled.
class FilePickRequest : UiRequest<Uri?>()

val uiRequestChannel = Channel<UiRequest<*>>()

// Sends this request to the UI and suspends until it is fulfilled.
suspend fun <T> UiRequest<T>.send(): T {
    uiRequestChannel.send(this)
    return result.await()
}

// Lets background code ask the user to pick a file without dealing with
// the request machinery. Answers with a content:// URI, or null if the
// picker was cancelled.
interface FilePicker {
    suspend fun pickFile(): Uri?
}

class UiFilePicker : FilePicker {
    override suspend fun pickFile() = FilePickRequest().send()
}
