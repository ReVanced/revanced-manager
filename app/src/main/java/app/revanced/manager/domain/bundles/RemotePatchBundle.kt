package app.revanced.manager.domain.bundles

import androidx.compose.runtime.Stable
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.io.File

@Stable
sealed class RemotePatchBundle(name: String, id: Int, directory: File, val endpoint: String) :
    PatchBundleSource(name, id, directory) {
    protected val http: HttpService by inject()

    protected abstract suspend fun getLatestInfo(): ReVancedAsset

    private suspend fun download(info: ReVancedAsset) = withContext(Dispatchers.IO) {
        patchBundleOutputStream().use {
            http.streamTo(it) {
                url(info.downloadUrl)
            }
        }

        saveVersionHash(info.version)
        reload()
    }

    suspend fun downloadLatest() {
        download(getLatestInfo())
    }

    suspend fun update(): Boolean = withContext(Dispatchers.IO) {
        val info = getLatestInfo()
        if (hasInstalled() && info.version == currentVersionHash())
            return@withContext false

        download(info)
        true
    }

    suspend fun deleteLocalFiles() = withContext(Dispatchers.Default) {
        patchesFile.delete()
        reload()
    }

    suspend fun setAutoUpdate(value: Boolean) = configRepository.setAutoUpdate(uid, value)

    companion object {
        const val updateFailMsg = "Failed to update patches"
    }
}

class JsonPatchBundle(name: String, id: Int, directory: File, endpoint: String) :
    RemotePatchBundle(name, id, directory, endpoint) {
    override suspend fun getLatestInfo() = withContext(Dispatchers.IO) {
        http.request<ReVancedAsset> {
            url(endpoint)
        }.getOrThrow()
    }
}

class APIPatchBundle(name: String, id: Int, directory: File, endpoint: String) :
    RemotePatchBundle(name, id, directory, endpoint) {
    private val api: ReVancedAPI by inject()

    override suspend fun getLatestInfo() = api.getPatchesUpdate().getOrThrow()
}