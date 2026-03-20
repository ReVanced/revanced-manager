package app.revanced.manager.domain.sources

import app.revanced.manager.data.redux.ActionContext
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.APIResponse
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.patcher.patch.PatchBundle
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

typealias RemotePatchBundle = RemoteSource<PatchBundle>
typealias JsonPatchBundle = JsonSource<PatchBundle>
typealias APIPatchBundle = APISource<PatchBundle>

sealed class RemoteSource<T>(
    name: String,
    uid: Int,
    protected val versionHash: String?,
    error: Throwable?,
    file: File,
    val endpoint: String,
    val autoUpdate: Boolean,
    loader: Loader<T>
) : Source<T>(name, uid, error, file, loader), KoinComponent {
    protected val http: HttpService by inject()

    protected abstract suspend fun getLatestInfo(): ReVancedAsset
    abstract fun copy(error: Throwable? = this.error, name: String = this.name, autoUpdate: Boolean = this.autoUpdate): RemoteSource<T>
    override fun copy(error: Throwable?, name: String): RemoteSource<T> = copy(error, name, this.autoUpdate)

    private suspend fun download(info: ReVancedAsset) = withContext(Dispatchers.IO) {
        outputStream().use {
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
    suspend fun ActionContext.getUpdateInfo() = getLatestInfo().takeUnless { hasInstalled() && it.version == versionHash }
    suspend fun ActionContext.update(): String? = withContext(Dispatchers.IO) {
        getUpdateInfo()?.let { download(it) }
    }

    companion object {
        const val updateFailMsg = "Failed to update"
    }
}

class JsonSource<T>(
    name: String,
    uid: Int,
    versionHash: String?,
    error: Throwable?,
    file: File,
    endpoint: String,
    autoUpdate: Boolean,
    loader: Loader<T>
) : RemoteSource<T>(name, uid, versionHash, error, file, endpoint, autoUpdate, loader) {
    override suspend fun getLatestInfo() = withContext(Dispatchers.IO) {
        http.request<ReVancedAsset> {
            url(endpoint)
        }.getOrThrow()
    }

    override fun copy(error: Throwable?, name: String, autoUpdate: Boolean) = JsonSource(
        name,
        uid,
        versionHash,
        error,
        file,
        endpoint,
        autoUpdate,
        loader
    )
}

class APISource<T>(
    name: String,
    uid: Int,
    versionHash: String?,
    error: Throwable?,
    file: File,
    endpoint: String,
    autoUpdate: Boolean,
    loader: Loader<T>,
    private val getUpdate: suspend ReVancedAPI.() -> APIResponse<ReVancedAsset>
) : RemoteSource<T>(name, uid, versionHash, error, file, endpoint, autoUpdate, loader) {
    private val api: ReVancedAPI by inject()

    override suspend fun getLatestInfo() = api.getUpdate().getOrThrow()
    override fun copy(error: Throwable?, name: String, autoUpdate: Boolean) = APISource(
        name,
        uid,
        versionHash,
        error,
        file,
        endpoint,
        autoUpdate,
        loader,
        getUpdate
    )
}