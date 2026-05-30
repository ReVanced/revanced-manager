package app.revanced.manager.network.api

import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.manager.base.Preference
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.dto.ReVancedAssetHistory
import app.revanced.manager.network.dto.ReVancedGitRepository
import app.revanced.manager.network.dto.ReVancedInfo
import app.revanced.manager.network.service.HttpService
import app.revanced.manager.network.utils.APIFailure
import app.revanced.manager.network.utils.APIResponse
import io.ktor.client.plugins.retry
import io.ktor.client.request.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ReVancedAPI(
    private val http: HttpService,
    private val prefs: PreferencesManager,
    private val endpointState: EndpointState,
) {
    private val defaultApiVersion = "v5"

    suspend fun getAnnouncements(): APIResponse<List<ReVancedAnnouncement>> =
        request("announcements")

    suspend fun getLatestAppInfo(): APIResponse<ReVancedAsset> =
        request("manager${prefs.useManagerPrereleases.prereleaseString()}")

    suspend fun getAppHistory(): APIResponse<List<ReVancedAssetHistory>> =
        request("manager/history${prefs.useManagerPrereleases.prereleaseString()}")

    suspend fun getPatchesUpdate(): APIResponse<ReVancedAsset> =
        request("patches${prefs.usePatchesPrereleases.prereleaseString()}")

    suspend fun getPatchesHistory(
        apiUrl: String,
        prerelease: Boolean,
    ): APIResponse<List<ReVancedAssetHistory>> {
        val route = "patches/history${prerelease.prereleaseString()}"
        val normalized = apiUrl.trimEnd('/')
        // Only the official source (the configured primary endpoint) participates in the primary => fallback logic.
        // A user-added custom remote source must query its own server directly and must never be redirected to ReVanced's backup endpoint.
        return if (normalized == endpointState.primaryUrl()) {
            request(route)
        } else {
            directRequest(normalized, route)
        }
    }

    suspend fun getDownloaderUpdate(): APIResponse<ReVancedAsset> =
        request("manager/downloaders${prefs.useDownloaderPrerelease.prereleaseString()}")

    suspend fun getContributors(): APIResponse<List<ReVancedGitRepository>> =
        request("contributors")

    suspend fun getInfo(): APIResponse<ReVancedInfo> {
        val (response, servedBy) = requestTracked<ReVancedInfo>("about")
        if (response is APIResponse.Success && servedBy == EndpointState.ActiveEndpoint.PRIMARY) {
            endpointState.updateFallbackFromAbout(response.data.api?.fallback)
        }
        return response
    }

    suspend fun probePrimary(): Boolean = withContext(Dispatchers.IO) {
        withTimeoutOrNull(PROBE_TIMEOUT_MS) {
            val url = "${endpointState.primaryUrl()}/$defaultApiVersion/about"
            http.request<ReVancedInfo> { url(url) } is APIResponse.Success
        } ?: false
    }

    private suspend inline fun <reified T> request(route: String): APIResponse<T> =
        requestTracked<T>(route).first

    private suspend inline fun <reified T> requestTracked(
        route: String,
    ): Pair<APIResponse<T>, EndpointState.ActiveEndpoint?> = withContext(Dispatchers.IO) {
        var lastFailure: APIResponse<T>? = null
        for ((endpoint, baseUrl) in endpointState.endpoints()) {
            val response = directRequest<T>(baseUrl, route)
            if (response is APIResponse.Success) {
                if (endpoint == EndpointState.ActiveEndpoint.FALLBACK) {
                    endpointState.switchToFallback()
                }
                endpointState.markEndpointResponseSucceeded(endpoint)
                return@withContext response to endpoint
            }
            lastFailure = response
        }
        (lastFailure ?: noAttemptsFailure<T>()) to null
    }

    private suspend inline fun <reified T> directRequest(
        baseUrl: String,
        route: String,
    ): APIResponse<T> = http.request {
        url("$baseUrl/$defaultApiVersion/$route")
        retry {
            maxRetries = MAX_RETRIES
            retryOnServerErrors()
            retryOnException(retryOnTimeout = true)
            exponentialDelay(base = 2.0, baseDelayMs = BACKOFF_BASE_MS)
        }
    }

    private fun <T> noAttemptsFailure(): APIResponse<T> =
        APIResponse.Failure(APIFailure(IllegalStateException("No request attempts were made"), null))

    private companion object {
        const val MAX_RETRIES = 3
        const val BACKOFF_BASE_MS = 250L
        const val PROBE_TIMEOUT_MS = 5_000L

        suspend fun Preference<Boolean>.prereleaseString() = if (get()) "/prerelease" else ""
        fun Boolean.prereleaseString() = if (this) "/prerelease" else ""
    }
}
