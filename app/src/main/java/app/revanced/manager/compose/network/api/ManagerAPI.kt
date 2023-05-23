package app.revanced.manager.compose.network.api

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.revanced.manager.compose.domain.repository.ReVancedRepository
import app.revanced.manager.compose.util.ghIntegrations
import app.revanced.manager.compose.util.ghManager
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
    private val revancedRepository: ReVancedRepository
) {
    var downloadProgress: Float? by mutableStateOf(null)
    var downloadedSize: Long? by mutableStateOf(null)
    var totalSize: Long? by mutableStateOf(null)

    private suspend fun downloadAsset(downloadUrl: String, saveLocation: File) {
        client.get(downloadUrl) {
            onDownload { bytesSentTotal, contentLength, ->
                downloadProgress = (bytesSentTotal.toFloat() / contentLength.toFloat())
                downloadedSize = bytesSentTotal
                totalSize = contentLength
            }
        }.bodyAsChannel().copyAndClose(saveLocation.writeChannel())
        downloadProgress = null
    }

    suspend fun downloadPatchBundle(): File? {
        try {
            val downloadUrl = revancedRepository.findAsset(ghPatches, ".jar").downloadUrl
            val patchesFile = app.filesDir.resolve("patch-bundles").also { it.mkdirs() }
                .resolve("patchbundle.jar")
            downloadAsset(downloadUrl, patchesFile)

            return patchesFile
        } catch (e: Exception) {
            Log.e(tag, "Failed to download patch bundle", e)
            app.toast("Failed to download patch bundle")
        }

        return null
    }

    suspend fun downloadIntegrations(): File? {
        try {
            val downloadUrl = revancedRepository.findAsset(ghIntegrations, ".apk").downloadUrl
            val integrationsFile = app.filesDir.resolve("integrations").also { it.mkdirs() }
                .resolve("integrations.apk")
            downloadAsset(downloadUrl, integrationsFile)

            return integrationsFile
        } catch (e: Exception) {
            Log.e(tag, "Failed to download integrations", e)
            app.toast("Failed to download integrations")
        }

        return null
    }

    suspend fun downloadManager(): File? {
        try {
            val managerAsset = revancedRepository.findAsset(ghManager, ".apk")
            val managerFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).also { it.mkdirs() }
                .resolve("revanced-manager.apk")
            downloadAsset(managerAsset.downloadUrl, managerFile)
            println("Downloaded manager at ${managerFile.absolutePath}")
            return managerFile
        } catch (e: Exception) {
            Log.e(tag, "Failed to download manager", e)
            app.toast("Failed to download manager")
        }
        return null
    }
}
class MissingAssetException : Exception()