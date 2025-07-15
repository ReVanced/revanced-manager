package app.revanced.manager.domain.bundles

import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

sealed class RemotePatchBundle(
    name: String,
    uid: Int,
    protected val versionHash: String?,
    error: Throwable?,
    directory: File,
    val endpoint: String,
    val autoUpdate: Boolean,
) : PatchBundleSource(name, uid, error, directory), KoinComponent {
    protected val http: HttpService by inject()

    protected abstract suspend fun getLatestInfo(): ReVancedAsset
    abstract fun copy(error: Throwable? = this.error, name: String = this.name, autoUpdate: Boolean = this.autoUpdate): RemotePatchBundle
    override fun copy(error: Throwable?, name: String): RemotePatchBundle = copy(error, name, this.autoUpdate)

    private suspend fun download(info: ReVancedAsset) = withContext(Dispatchers.IO) {
        patchBundleOutputStream().use {
            http.streamTo(it) {
                url(info.downloadUrl)
            }
        }

        info.version
    }

    /**
     * Downloads the latest version regardless if there is a new update available.
     */
    suspend fun ActionContext.downloadLatest() = download(getLatestInfo())

    suspend fun ActionContext.update(): String? = withContext(Dispatchers.IO) {
        val info = getLatestInfo()
        if (hasInstalled() && info.version == versionHash)
            return@withContext null

        download(info)
    }

    companion object {
        const val updateFailMsg = "Failed to update patches"
    }
}

class JsonPatchBundle(
    name: String,
    uid: Int,
    versionHash: String?,
    error: Throwable?,
    directory: File,
    endpoint: String,
    autoUpdate: Boolean,
) : RemotePatchBundle(name, uid, versionHash, error, directory, endpoint, autoUpdate) {
    override suspend fun getLatestInfo() = withContext(Dispatchers.IO) {
        http.request<ReVancedAsset> {
            url(endpoint)
        }.getOrThrow()
    }

    override fun copy(error: Throwable?, name: String, autoUpdate: Boolean) = JsonPatchBundle(
        name,
        uid,
        versionHash,
        error,
        directory,
        endpoint,
        autoUpdate,
    )
}

class APIPatchBundle(
    name: String,
    uid: Int,
    versionHash: String?,
    error: Throwable?,
    directory: File,
    endpoint: String,
    autoUpdate: Boolean,
) : RemotePatchBundle(name, uid, versionHash, error, directory, endpoint, autoUpdate) {
    private val api: ReVancedAPI by inject()

    override suspend fun getLatestInfo() = api.getPatchesUpdate().getOrThrow()
    override fun copy(error: Throwable?, name: String, autoUpdate: Boolean) = APIPatchBundle(
        name,
        uid,
        versionHash,
        error,
        directory,
        endpoint,
        autoUpdate,
    )
}