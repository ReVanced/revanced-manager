package app.revanced.manager.network.api

import app.revanced.manager.BuildConfig
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.dto.ReVancedRelease
import app.revanced.manager.network.service.ReVancedService
import app.revanced.manager.network.utils.getOrThrow
import app.revanced.manager.network.utils.transform

class ReVancedAPI(
    private val service: ReVancedService,
    private val prefs: PreferencesManager
) {
    private suspend fun apiUrl() = prefs.api.get()

    suspend fun getContributors() = service.getContributors(apiUrl()).transform { it.repositories }

    suspend fun getLatestRelease(name: String) =
        service.getLatestRelease(apiUrl(), name).transform { it.release }

    suspend fun getReleases(name: String) =
        service.getReleases(apiUrl(), name).transform { it.releases }

    suspend fun getAppUpdate() =
        getLatestRelease("revanced-manager")
            .getOrThrow()
            .takeIf {
                val (major, minor, patch, dev) =
                    "${it.version.removePrefix("v").replace("-dev", "")}.0"
                    .split(".")
                    .map { num-> num.toInt() }
                val versioncode = major * 100000000 + minor * 100000 + patch * 100 + dev
                if (versioncode > BuildConfig.VERSION_CODE)
                    return it
                else
                    return null
            }

    companion object Extensions {
        fun ReVancedRelease.findAssetByType(mime: String) =
            assets.singleOrNull { it.contentType == mime } ?: throw MissingAssetException(mime)
    }
}

class MissingAssetException(type: String) : Exception("No asset with type $type")