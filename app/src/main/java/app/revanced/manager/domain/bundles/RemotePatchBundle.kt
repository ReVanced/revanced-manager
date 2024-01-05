package app.revanced.manager.domain.bundles

import androidx.compose.runtime.Stable
import app.revanced.manager.data.room.bundles.VersionInfo
import app.revanced.manager.domain.repository.PatchBundlePersistenceRepository
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.network.api.ReVancedAPI.Extensions.findAssetByType
import app.revanced.manager.network.dto.BundleAsset
import app.revanced.manager.network.dto.BundleInfo
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.util.APK_MIMETYPE
import app.revanced.manager.util.JAR_MIMETYPE
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

@Stable
sealed class RemotePatchBundle(name: String, id: Int, directory: File, val endpoint: String) :
    PatchBundleSource(name, id, directory), KoinComponent {
    private val configRepository: PatchBundlePersistenceRepository by inject()
    protected val http: HttpService by inject()

    protected abstract suspend fun getLatestInfo(): BundleInfo

    private suspend fun download(info: BundleInfo) = withContext(Dispatchers.IO) {
        val (patches, integrations) = info
        coroutineScope {
            launch {
                patchBundleOutputStream().use {
                    http.streamTo(it) {
                        url(patches.url)
                    }
                }
            }

            launch {
                http.download(integrationsFile) {
                    url(integrations.url)
                }
            }
        }

        saveVersion(patches.version, integrations.version)
        reload()
    }

    suspend fun downloadLatest() {
        download(getLatestInfo())
    }

    suspend fun update(): Boolean = withContext(Dispatchers.IO) {
        val info = getLatestInfo()
        if (hasInstalled() && VersionInfo(
                info.patches.version,
                info.integrations.version
            ) == currentVersion()
        ) {
            return@withContext false
        }

        download(info)
        true
    }

    private suspend fun currentVersion() = configRepository.getProps(uid).first().versionInfo
    private suspend fun saveVersion(patches: String, integrations: String) =
        configRepository.updateVersion(uid, patches, integrations)

    suspend fun deleteLocalFiles() = withContext(Dispatchers.Default) {
        arrayOf(patchesFile, integrationsFile).forEach(File::delete)
        reload()
    }

    fun propsFlow() = configRepository.getProps(uid)

    suspend fun setAutoUpdate(value: Boolean) = configRepository.setAutoUpdate(uid, value)

    companion object {
        const val updateFailMsg = "Failed to update patch bundle(s)"
    }
}

class JsonPatchBundle(name: String, id: Int, directory: File, endpoint: String) :
    RemotePatchBundle(name, id, directory, endpoint) {
    override suspend fun getLatestInfo() = withContext(Dispatchers.IO) {
        http.request<BundleInfo> {
            url(endpoint)
        }.getOrThrow()
    }
}

class APIPatchBundle(name: String, id: Int, directory: File, endpoint: String) :
    RemotePatchBundle(name, id, directory, endpoint) {
    private val api: ReVancedAPI by inject()

    override suspend fun getLatestInfo() = coroutineScope {
        fun getAssetAsync(repo: String, mime: String) = async(Dispatchers.IO) {
            api
                .getLatestRelease(repo)
                .getOrThrow()
                .let {
                    BundleAsset(it.version, it.findAssetByType(mime).downloadUrl)
                }
        }

        val patches = getAssetAsync("revanced-patches", JAR_MIMETYPE)
        val integrations = getAssetAsync("revanced-integrations", APK_MIMETYPE)

        BundleInfo(
            patches.await(),
            integrations.await()
        )
    }
}