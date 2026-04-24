package app.revanced.manager.network.api

import android.util.Log
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.manager.base.Preference
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.dto.ReVancedAssetHistory
import app.revanced.manager.network.dto.ReVancedGitRepository
import app.revanced.manager.network.dto.ReVancedInfo
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.APIResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.request.url

class ReVancedAPI(
    private val client: HttpService,
    private val prefs: PreferencesManager
) {
    private suspend fun apiUrl() = prefs.api.get()
    private val defaultApiVersion = "v5"

    private suspend inline fun <reified T> request(api: String, apiVersion: String, route: String): APIResponse<T> =
        withContext(Dispatchers.IO) {
            val fullUrl = "$api/$apiVersion/$route"
            try {
                Log.d("API", "Requesting: $fullUrl")

                client.request {
                    url(fullUrl)
                }

            } catch (e: Exception) {
                Log.e("API", "Failed request: $fullUrl", e)
                throw e
            }
        }

    private suspend inline fun <reified T> request(route: String, apiVersion: String = defaultApiVersion) = request<T>(apiUrl(), apiVersion, route)

    suspend fun getAnnouncements() = request<List<ReVancedAnnouncement>>("announcements")

    suspend fun getLatestAppInfo() =
        request<ReVancedAsset>("manager${prefs.useManagerPrereleases.prereleaseString()}")

    suspend fun getAppHistory() = request<List<ReVancedAssetHistory>>("manager/history${prefs.useManagerPrereleases.prereleaseString()}")

    suspend fun getPatchesUpdate() = request<ReVancedAsset>("patches${prefs.usePatchesPrereleases.prereleaseString()}")

    suspend fun getPatchesHistory(apiUrl: String, prerelease: Boolean) =
        request<List<ReVancedAssetHistory>>(apiUrl, defaultApiVersion, "patches/history${prerelease.prereleaseString()}")

    suspend fun getDownloaderUpdate() = request<ReVancedAsset>("manager/downloaders${prefs.useDownloaderPrerelease.prereleaseString()}")

    suspend fun getContributors() = request<List<ReVancedGitRepository>>("contributors")

    suspend fun getInfo() = request<ReVancedInfo>("about")

    private companion object {
        suspend fun Preference<Boolean>.prereleaseString() = if (get()) "/prerelease" else ""
        fun Boolean.prereleaseString() = if (this) "/prerelease" else ""
    }
}