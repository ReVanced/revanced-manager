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
import io.ktor.utils.io.core.isNotEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream

/**
 * @author Aliucord Authors, DiamondMiner88
 */
class HttpService(
    val json: Json,
    val http: HttpClient,
) {
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
                val channel: ByteReadChannel = httpResponse.body()
                withContext(Dispatchers.IO) {
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        while (packet.isNotEmpty) {
                            val bytes = packet.readBytes()
                            outputStream.write(bytes)
                        }
                    }
                }

            } else {
                throw HttpException(httpResponse.status)
            }
        }
    }

    suspend fun download(
        saveLocation: File,
        builder: HttpRequestBuilder.() -> Unit
    ) = saveLocation.outputStream().use { streamTo(it, builder) }

    class HttpException(status: HttpStatusCode) : Exception("Failed to fetch: http status: $status")
}