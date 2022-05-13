package app.revanced.manager.backend.api

import app.revanced.manager.Global
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val client = HttpClient(Android) {
    BrowserUserAgent()
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

object ManagerAPI {
    suspend fun downloadPatches(): PatchesAsset {
        val patchesAsset = findPatchesAsset()
        val (_, asset) = patchesAsset
        // TODO

        return patchesAsset
    }

    private suspend fun findPatchesAsset(): PatchesAsset {
        val release = GitHubAPI.Releases.latestRelease(Global.ghPatches)
        val asset = release.assets.findAsset() ?: throw MissingAssetException()
        return PatchesAsset(release, asset)
    }

    data class PatchesAsset(
        val release: GitHubAPI.Releases.Release,
        val asset: GitHubAPI.Releases.ReleaseAsset
    )

    private fun List<GitHubAPI.Releases.ReleaseAsset>.findAsset() = find { asset ->
        !asset.name.contains("-sources") && !asset.name.contains("-javadoc")
    }
}

class MissingAssetException : Exception()