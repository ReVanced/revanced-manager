package app.revanced.manager.domain.bundles

import androidx.compose.runtime.Stable
import app.revanced.manager.data.room.bundles.VersionInfo
import app.revanced.manager.domain.bundles.APIPatchBundle.Companion.toBundleAsset
import app.revanced.manager.domain.repository.Assets
import app.revanced.manager.domain.repository.PatchBundlePersistenceRepository
import app.revanced.manager.domain.repository.ReVancedRepository
import app.revanced.manager.network.dto.Asset
import app.revanced.manager.network.dto.BundleAsset
import app.revanced.manager.network.dto.BundleInfo
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.util.ghIntegrations
import app.revanced.manager.util.ghPatches
import io.ktor.client.request.url
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
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
            mapOf(
                patches.url to patchesFile,
                integrations.url to integrationsFile
            ).forEach { (asset, file) ->
                launch {
                    http.download(file) {
                        url(asset)
                    }
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
        if (hasInstalled() && VersionInfo(info.patches.version, info.integrations.version) == currentVersion()) {
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
    private val api: ReVancedRepository by inject()

    override suspend fun getLatestInfo() = api.getAssets().toBundleInfo()

    private companion object {
        fun Assets.toBundleInfo(): BundleInfo {
            val patches = find(ghPatches, ".jar")
            val integrations = find(ghIntegrations, ".apk")

            return BundleInfo(patches.toBundleAsset(), integrations.toBundleAsset())
        }

        fun Asset.toBundleAsset() = BundleAsset(version, downloadUrl)
    }
}