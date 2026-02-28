package app.revanced.manager.network.api

import app.revanced.manager.BuildConfig
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.network.dto.ReVancedAnnouncementTag
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.dto.ReVancedGitRepository
import app.revanced.manager.network.dto.ReVancedInfo
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.APIResponse
import app.revanced.manager.network.utils.getOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.request.url

class ReVancedAPI(
    private val client: HttpService,
    private val prefs: PreferencesManager
) {
    private suspend fun apiUrl() = prefs.api.get()

    private suspend inline fun <reified T> request(
        api: String,
        route: String,
        version: String? = "v4"
    ): APIResponse<T> =
        withContext(
            Dispatchers.IO
        ) {
            client.request {
                url("$api/$version/$route")
            }
        }

    private suspend inline fun <reified T> request(
        route: String,
        version: String = "v4"
    ) = request<T>(apiUrl(), route, version)

    suspend fun getAnnouncements() = request<List<ReVancedAnnouncement>>("announcements")

    suspend fun getAnnouncementTags() = request<List<ReVancedAnnouncementTag>>("announcements/tags")

    suspend fun getAppUpdate() =
        getLatestAppInfo().getOrThrow().takeIf { it.version.removePrefix("v") != BuildConfig.VERSION_NAME }

    suspend fun getLatestAppInfo() =
        request<ReVancedAsset>("manager?prerelease=${prefs.useManagerPrereleases.get()}")

    suspend fun getAppHistory() = request<ReVancedAsset>("patches/history", "dev")

    suspend fun getPatchesUpdate() = request<ReVancedAsset>("patches?prerelease=${prefs.usePatchesPrereleases.get()}")

    suspend fun getContributors() = request<List<ReVancedGitRepository>>("contributors")

    suspend fun getInfo(api: String? = null) = request<ReVancedInfo>(api ?: apiUrl(), "about")
}