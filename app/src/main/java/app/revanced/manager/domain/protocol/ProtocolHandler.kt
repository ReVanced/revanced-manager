package app.revanced.manager.domain.protocol

import android.app.Application
import android.net.Uri
import app.revanced.manager.network.service.HttpService
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream


// open streams for URIs of the [schemes] it is registered for.
interface ProtocolHandler {
    val schemes: Set<String>


    // here we open a stream to the resource behind [uri]. caller is responsible for closing it.
    suspend fun getStream(uri: Uri): InputStream
}

class HttpProtocolHandler(private val http: HttpService) : ProtocolHandler {
    override val schemes = setOf("http", "https")

    override suspend fun getStream(uri: Uri): InputStream = http.getStream {
        url(uri.toString())
    }
}

class FileProtocolHandler(private val app: Application) : ProtocolHandler {
    override val schemes = setOf("file", "content")

    override suspend fun getStream(uri: Uri): InputStream = withContext(Dispatchers.IO) {
        app.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
    }
}
