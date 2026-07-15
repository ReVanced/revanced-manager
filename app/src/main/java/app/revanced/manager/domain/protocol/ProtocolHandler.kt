package app.revanced.manager.domain.protocol

import android.app.Application
import android.net.Uri
import app.revanced.manager.network.service.HttpService
import io.ktor.client.request.url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
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
        http.getStream({ url(uri.toString()) }, block)
}

// Opens content:// URIs, which the platform grants the app access to.
class ContentProtocolHandler(private val app: Application) : ProtocolHandler {
    override suspend fun <T> getStream(uri: Uri, block: suspend (InputStream) -> T): T {
        val stream = withContext(Dispatchers.IO) {
            app.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        }

        return stream.use { block(it) }
    }
}

// A request for the UI to let the user pick a file, answered with a content:// URI.
class FilePickRequest {
    val result = CompletableDeferred<Uri?>()
}

// Reading file:// URIs directly requires storage permissions, which the app avoids.
// The user instead picks the file through the system file picker, which yields
// a content:// URI the app is allowed to open.
class FileProtocolHandler(
    private val pickRequests: Channel<FilePickRequest>,
    private val content: ContentProtocolHandler
) : ProtocolHandler {
    override suspend fun <T> getStream(uri: Uri, block: suspend (InputStream) -> T): T {
        val request = FilePickRequest()
        pickRequests.send(request)

        val picked = request.result.await() ?: throw IOException("No file was selected")
        return content.getStream(picked, block)
    }
}
