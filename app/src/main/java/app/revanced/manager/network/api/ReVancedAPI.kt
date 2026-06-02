package app.revanced.manager.network.api

import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.manager.base.Preference
import app.revanced.manager.network.dto.ReVancedAnnouncement
import app.revanced.manager.network.dto.ReVancedAsset
import app.revanced.manager.network.dto.ReVancedAssetHistory
import app.revanced.manager.network.dto.ReVancedGitRepository
import app.revanced.manager.network.dto.ReVancedInfo
import app.revanced.manager.network.service.HttpService
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
    ): APIResponse<List<ReVancedAssetHistory>> =
        requestForSource(apiUrl, "patches/history${prerelease.prereleaseString()}")

    suspend fun getDownloaderUpdate(): APIResponse<ReVancedAsset> =
        request("manager/downloaders${prefs.useDownloaderPrerelease.prereleaseString()}")

    suspend fun getContributors(): APIResponse<List<ReVancedGitRepository>> =
        request("contributors")

    suspend fun getInfo(): APIResponse<ReVancedInfo> {
        val (response, servedBy) = requestTracked<ReVancedInfo>("about")
        if (response is APIResponse.Success && servedBy == endpointState.primaryUrl()) {
            endpointState.updateFallbackFromAbout(response.data.api?.fallback)
        }
        return response
    }

    
    // Probes the higher-priority endpoints above the active one and restores the earliest reachable one (the primary wins over any intermedieate backup). returns the restored endpoint or null if none higher than the active endpoint is currently reachable.
    suspend fun restoreHigherPriorityEndpoint(): EndpointState.ApiEndpoint? = withContext(Dispatchers.IO) {
        endpointState.restoreCandidates().firstOrNull { endpoint ->
            probe(endpoint.url)
        }?.also { endpointState.setActive(it) }
    }

    private suspend fun probe(baseUrl: String): Boolean =
        withTimeoutOrNull(PROBE_TIMEOUT_MS) {
            http.request<ReVancedInfo> { url("$baseUrl/$defaultApiVersion/about") } is APIResponse.Success
        } ?: false

    private suspend inline fun <reified T> request(route: String): APIResponse<T> =
        requestTracked<T>(route).first

    /**
     * Routes a request for a patch-bundle source. Only the official source (the configured
     * primary endpoint) participates in the primary -> fallback logic; a user-added custom
     * remote source queries its own server directly and is never redirected to the backup.
     */
    private suspend inline fun <reified T> requestForSource(
        apiUrl: String,
        route: String,
    ): APIResponse<T> =
        if (apiUrl == endpointState.primaryUrl()) {
            request(route)
        } else {
            directRequest(apiUrl, route)
        }

    // Issue a request against the active endpoint and returns the first success with the URL that served it. 
    // If the active endpoint is unreachable, the whole chain is walked from the primary downward so a higher-priority endpoint is preferred over a lower-priority one: i.e recovery up the chain is attempted before falling further down. The serving endpoint becomes active.
    private suspend inline fun <reified T> requestTracked(
        route: String,
    ): Pair<APIResponse<T>, String?> = withContext(Dispatchers.IO) {
        val active = endpointState.activeEndpoint()
        val activeResponse = directRequest<T>(active.url, route)
        if (activeResponse is APIResponse.Success) {
            return@withContext activeResponse to active.url
        }

        var lastFailure: APIResponse<T> = activeResponse
        endpointState.chain().asSequence()
            .filter { it.url != active.url }
            .forEach { endpoint ->
                val response = directRequest<T>(endpoint.url, route)
                if (response is APIResponse.Success) {
                    endpointState.setActive(endpoint)
                    return@withContext response to endpoint.url
                }
                lastFailure = response
            }
        lastFailure to null
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

    private companion object {
        const val MAX_RETRIES = 3
        const val BACKOFF_BASE_MS = 250L
        const val PROBE_TIMEOUT_MS = 5_000L

        suspend fun Preference<Boolean>.prereleaseString() = if (get()) "/prerelease" else ""
        fun Boolean.prereleaseString() = if (this) "/prerelease" else ""
    }
}
