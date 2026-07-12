package app.revanced.manager.network.service

import android.util.Log
import app.revanced.manager.network.utils.APIError
import app.revanced.manager.network.utils.APIFailure
import app.revanced.manager.network.utils.APIResponse
import app.revanced.manager.util.tag
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareGet
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Aliucord Authors, DiamondMiner88
 */
class HttpService(
    val json: Json,
    val http: HttpClient,
) {
    private val writerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend inline fun <reified T> request(builder: HttpRequestBuilder.() -> Unit = {}): APIResponse<T> {
        var body: String? = null

        val response = try {
            val response = http.request(builder)

            if (response.status.isSuccess()) {
                body = response.bodyAsText()

                if (T::class == String::class) {
                    return APIResponse.Success(body as T)
                }

                APIResponse.Success(json.decodeFromString<T>(body))
            } else {
                body = try {
                    response.bodyAsText()
                } catch (t: Throwable) {
                    null
                }

                Log.e(
                    tag,
                    "Failed to fetch: API error, http status: ${response.status}, body: $body"
                )
                APIResponse.Error(APIError(response.status, body))
            }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to fetch: error: $t, body: $body")
            APIResponse.Failure(APIFailure(t, body))
        }
        return response
    }

    suspend fun streamTo(
        outputStream: OutputStream,
        builder: HttpRequestBuilder.() -> Unit
    ) {
        http.prepareGet(builder).execute { httpResponse ->
            if (httpResponse.status.isSuccess()) {
                withContext(Dispatchers.IO) {
                    val channel: ByteReadChannel = httpResponse.body()
                    val sink = outputStream.asSink()
                    while (!channel.exhausted()) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        packet.transferTo(sink)
                    }
                }

            } else {
                throw HttpException(httpResponse.status)
            }
        }
    }

    // Make a GET request and returns the response body as a stream.
    // Caller is responsible for closing the returned stream.
    // Failures happening while streaming are thrown from the stream's read methods.
    fun getStream(builder: HttpRequestBuilder.() -> Unit): InputStream {
        val failure = AtomicReference<Throwable?>(null)
        val stream = PipedInputStream(DEFAULT_BUFFER_SIZE)
        val output = PipedOutputStream(stream)

        val writer = writerScope.launch {
            try {
                streamTo(output, builder)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                failure.set(t)
            } finally {
                runCatching { output.close() } // Unblocks the reader, failure must be recorded before this happens.
            }
        }

        return object : FilterInputStream(stream) {
            override fun read() = rethrowingFailure { super.read() }
            override fun read(b: ByteArray, off: Int, len: Int) =
                rethrowingFailure { super.read(b, off, len) }

            private inline fun rethrowingFailure(read: () -> Int): Int {
                val result = try {
                    read()
                } catch (e: IOException) {
                    throw failure.get() ?: e
                }
                // failure closes the pipe early which then looks like a normal end of stream.
                if (result == -1) failure.get()?.let { throw it }
                return result
            }

            override fun close() {
                writer.cancel()
                super.close()
            }
        }
    }

    suspend fun download(
        saveLocation: File,
        builder: HttpRequestBuilder.() -> Unit
    ) = saveLocation.outputStream().use { streamTo(it, builder) }

    class HttpException(status: HttpStatusCode) : Exception("Failed to fetch: http status: $status")
}