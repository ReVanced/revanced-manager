package app.revanced.manager.network.api

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.revanced.manager.domain.repository.ReVancedRepository
import app.revanced.manager.util.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.io.File

class ManagerAPI(
    private val client: HttpClient,
    private val revancedRepository: ReVancedRepository
) {
    var downloadProgress: Float? by mutableStateOf(null)
    var downloadedSize: Long? by mutableStateOf(null)
    var totalSize: Long? by mutableStateOf(null)

    private suspend fun downloadAsset(downloadUrl: String, saveLocation: File) {
        client.get(downloadUrl) {
            onDownload { bytesSentTotal, contentLength ->
                downloadProgress = (bytesSentTotal.toFloat() / contentLength.toFloat())
                downloadedSize = bytesSentTotal
                totalSize = contentLength
            }
        }.bodyAsChannel().copyAndClose(saveLocation.writeChannel())
        downloadProgress = null
    }

    private suspend fun patchesAsset() = revancedRepository.findAsset(ghPatches, ".jar")
    private suspend fun integrationsAsset() = revancedRepository.findAsset(ghIntegrations, ".apk")

    suspend fun getLatestBundleVersion() = patchesAsset().version to integrationsAsset().version

    suspend fun downloadBundle(patchBundle: File, integrations: File): Pair<String, String> {
        val patchBundleAsset = patchesAsset()
        val integrationsAsset = integrationsAsset()

        downloadAsset(patchBundleAsset.downloadUrl, patchBundle)
        downloadAsset(integrationsAsset.downloadUrl, integrations)

        return patchBundleAsset.version to integrationsAsset.version
    }

    suspend fun downloadManager(location: File) {
        val managerAsset = revancedRepository.findAsset(ghManager, ".apk")
        downloadAsset(managerAsset.downloadUrl, location)
    }
}

class MissingAssetException : Exception()