package app.revanced.manager.network.api

import android.os.Build
import app.revanced.manager.domain.manager.PreferencesManager
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

    private suspend inline fun <reified T> request(api: String, route: String): APIResponse<T> =
        withContext(
            Dispatchers.IO
        ) {
            client.request {
                url("$api/v4/$route")
            }
        }

    private suspend inline fun <reified T> request(route: String) = request<T>(apiUrl(), route)

    suspend fun getAppUpdate() =
        getLatestAppInfo().getOrThrow().takeIf { it.version != Build.VERSION.RELEASE }

    suspend fun getLatestAppInfo() =
        request<ReVancedAsset>("manager?prerelease=${prefs.useManagerPrereleases.get()}")

    suspend fun getPatchesUpdate() = request<ReVancedAsset>("patches")

    suspend fun getContributors() = request<List<ReVancedGitRepository>>("contributors")

    suspend fun getInfo(api: String? = null) = request<ReVancedInfo>(api ?: apiUrl(), "about")
}