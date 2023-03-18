package app.revanced.manager.compose.network.api

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.revanced.manager.compose.domain.repository.ReVancedRepositoryImpl
import app.revanced.manager.compose.util.ghIntegrations
import app.revanced.manager.compose.util.ghPatches
import app.revanced.manager.compose.util.tag
import app.revanced.manager.compose.util.toast
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.io.File

class ManagerAPI(
    private val app: Application,
    private val client: HttpClient,
    private val revancedRepository: ReVancedRepositoryImpl
) {
    var downloadProgress: Float? by mutableStateOf(null)

    private suspend fun downloadAsset(downloadUrl: String, saveLocation: File) {
        client.get(downloadUrl) {
            onDownload { bytesSentTotal, contentLength ->
                downloadProgress = (bytesSentTotal.toFloat() / contentLength.toFloat())
            }
        }.bodyAsChannel().copyAndClose(saveLocation.writeChannel())
        downloadProgress = null
    }

    suspend fun downloadPatchBundle() {
        try {
            val downloadUrl = revancedRepository.findAsset(ghPatches, ".jar").downloadUrl
            val patchesFile = app.filesDir.resolve("patch-bundles").also { it.mkdirs() }
                .resolve("patchbundle.jar")
            downloadAsset(downloadUrl, patchesFile)
        } catch (e: Exception) {
            Log.e(tag, "Failed to download patch bundle", e)
            app.toast("Failed to download patch bundle")
        }
    }

    suspend fun downloadIntegrations() {
        try {
            val downloadUrl = revancedRepository.findAsset(ghIntegrations, ".apk").downloadUrl
            val integrationsFile = app.filesDir.resolve("integrations").also { it.mkdirs() }
                .resolve("integrations.apk")
            downloadAsset(downloadUrl, integrationsFile)
        } catch (e: Exception) {
            Log.e(tag, "Failed to download integrations", e)
            app.toast("Failed to download integrations")
        }
    }
}

data class PatchesAsset(
    val downloadUrl: String, val name: String
)

class MissingAssetException : Exception()