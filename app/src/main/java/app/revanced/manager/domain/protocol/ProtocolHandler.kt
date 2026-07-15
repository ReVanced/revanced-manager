package app.revanced.manager.domain.protocol

import android.content.ContentResolver
import android.net.Uri
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.util.FilePicker
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

// Opens streams for the URI scheme it is registered for.
interface ProtocolHandler {
    // Opens a stream to the resource behind [uri] and passes it to [block].
    // The stream is only valid inside [block] and is closed automatically afterwards.
    suspend fun <T> getStream(uri: Uri, block: suspend (InputStream) -> T): T
}

class HttpProtocolHandler(private val http: HttpService) : ProtocolHandler {
    override suspend fun <T> getStream(uri: Uri, block: suspend (InputStream) -> T) =
        http.getStream(block) { url(uri.toString()) }
}

// Opens content:// URIs, which the platform grants the app access to.
class ContentProtocolHandler(private val contentResolver: ContentResolver) : ProtocolHandler {
    override suspend fun <T> getStream(uri: Uri, block: suspend (InputStream) -> T): T {
        val stream = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        }

        return stream.use { block(it) }
    }
}

// Reading file:// URIs directly requires storage permissions, which the app avoids.
// The user instead picks the file through the system file picker, which yields
// a content:// URI the app is allowed to open.
class FileProtocolHandler(
    private val filePicker: FilePicker,
    private val contentProtocolHandler: ContentProtocolHandler
) : ProtocolHandler {
    override suspend fun <T> getStream(uri: Uri, block: suspend (InputStream) -> T): T {
        val picked = filePicker.pickFile() ?: throw IOException("No file was selected")
        return contentProtocolHandler.getStream(picked, block)
    }
}
