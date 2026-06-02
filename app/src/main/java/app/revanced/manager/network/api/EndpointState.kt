package app.revanced.manager.network.api

import android.util.Log
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.util.tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EndpointState(
    private val prefs: PreferencesManager,
) {
    
    // A node in the prioritised API endpoint chain. 
    // [fallback] is the next lower-priority endpoint to try while this one is unreachable so the chain reads from the primary/head downward.
    // Restoration to a higher-priority endpoint is derived by walking from the head (Ctrl + F("restoreCandidates")., the chain is single-direction because a doubly-linked immutable chain is not constructible.
     
    data class ApiEndpoint(
        val url: String,
        val fallback: ApiEndpoint?,
    ) {
        fun asSequence(): Sequence<ApiEndpoint> = generateSequence(this) { it.fallback }
    }

    // URL of the endpoint currently in use, null means the primary (chain head).
    private val _activeUrl = MutableStateFlow<String?>(null)
    val activeUrl: StateFlow<String?> = _activeUrl.asStateFlow()

    suspend fun primaryUrl(): String = prefs.api.get().trimEnd('/')

    // the persisted endpoints, ordered from primary downward. The API currently advertises a single backup but the chain is built from a list so additional tiers need no structural changes.

    private suspend fun endpointUrls(): List<String> =
        listOf(primaryUrl(), prefs.apiFallback.get().trimEnd('/')).distinct()

    // The endpoint chain built from persisted configuration, head (primary) first.
    suspend fun chain(): ApiEndpoint {
        var node: ApiEndpoint? = null
        endpointUrls().asReversed().forEach { url -> node = ApiEndpoint(url, node) }
        return node!!
    }

    // The endpoint currently in use or the chain head when on the primary.
    suspend fun activeEndpoint(): ApiEndpoint {
        val chain = chain()
        val active = _activeUrl.value ?: return chain
        return chain.asSequence().firstOrNull { it.url == active } ?: chain
    }

    // Higher-priority endpoints to probe for restoration, ordered from the primary downward.
    suspend fun restoreCandidates(): List<ApiEndpoint> {
        val active = _activeUrl.value ?: return emptyList()
        return chain().asSequence().takeWhile { it.url != active }.toList()
    }

    // Records [endpoint] as the active endpoint. Resets to the primary when the head is selected.
    suspend fun setActive(endpoint: ApiEndpoint) {
        _activeUrl.value = endpoint.url.takeIf { it != primaryUrl() }
    }

    suspend fun updateFallbackFromAbout(advertised: String?) {
        val normalized = advertised?.trim()?.trimEnd('/').orEmpty()
        if (normalized.isEmpty()) return
        if (!normalized.startsWith("https://")) {
            Log.w(tag, "EndpointState: ignoring non-HTTPS fallback URL from /about: $normalized")
            return
        }
        if (normalized == prefs.apiFallback.get().trimEnd('/')) return
        Log.i(tag, "EndpointState: updating persisted fallback endpoint to $normalized")
        prefs.apiFallback.update(normalized)
    }

    companion object {
        const val DEFAULT_PRIMARY_API_URL = "https://api.revanced.app"
        const val DEFAULT_FALLBACK_API_URL = "https://backup-api.revanced.app"
    }
}
