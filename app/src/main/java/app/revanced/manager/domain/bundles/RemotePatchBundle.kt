package app.revanced.manager.domain.bundles

import android.util.Log
import androidx.compose.runtime.Stable
import app.revanced.manager.domain.bundles.RemotePatchBundleFetchResponse
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDateTime.Companion
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.inject
import java.io.File

class RemotePatchBundleFetchResponse(
    val response: ReVancedAsset,
    oldVersion: String?,
    oldLatestVersion: String?
) {
    val isNewLatestVersion = response.version != oldLatestVersion

    val canUpdateVersion= response.version != oldVersion

    val isLatestInstalled = response.version == oldVersion
}

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

        updateVersion(info.version)
        updateInstallationProps(info.description, info.createdAt.toString())
        reload()
    }

    suspend fun downloadLatest() {
        download(getLatestInfo())
    }

    suspend fun fetchLatestRemoteInfo(): RemotePatchBundleFetchResponse = withContext(Dispatchers.Default) {
        getLatestInfo().let {
            val result = RemotePatchBundleFetchResponse(it, getProps().version, getLatestProps().latestVersion)
            Log.i("testtt", "result; ${getProps().version}, ${getLatestProps().latestVersion}")
            configRepository.updateLatestRemoteInfo(
                uid,
                it.version,
                it.description,
                it.createdAt.toString()
            )
            result
        }
    }

    suspend fun update(): Boolean = withContext(Dispatchers.IO) {
        val fetchedInfo = fetchLatestRemoteInfo()
        if (!hasInstalled() || !fetchedInfo.canUpdateVersion)
            return@withContext false

        download(fetchedInfo.response)
        true
    }

    suspend fun deleteLocalFiles() = withContext(Dispatchers.Default) {
        patchesFile.delete()
        reload()
    }

    suspend fun setAutoUpdate(value: Boolean) = configRepository.setAutoUpdate(uid, value)

    suspend fun setSearchUpdate(value: Boolean) = configRepository.setSearchUpdate(uid, value)

    companion object {
        const val updateFailMsg = "Failed to update patch bundle(s)"
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