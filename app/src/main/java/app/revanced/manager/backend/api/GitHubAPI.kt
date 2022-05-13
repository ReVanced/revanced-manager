package app.revanced.manager.backend.api

import android.util.Log
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

object GitHubAPI {
    private const val tag = "GitHubAPI"
    private const val baseUrl = "https://api.github.com/repos"

    object Releases {
        suspend fun latestRelease(repo: String): Release {
            Log.d(tag, "Fetching latest releases for repo ($repo)")
            val res: List<Release> = client.get("$baseUrl/$repo/releases") {
                parameter("per_page", 1)
            }.body()
            return res.first()
        }

        @Serializable
        class Release(
            @SerialName("tag_name") val tagName: String,
            @SerialName("published_at") val publishedAt: String,
            @SerialName("prerelease") val isPrerelease: Boolean,
            val assets: List<ReleaseAsset>,
            val body: String
        )

        @Serializable
        class ReleaseAsset(
            @SerialName("browser_download_url") val downloadUrl: String,
            val name: String
        )
    }

    object Commits {
        suspend fun latestCommit(repo: String, ref: String): Commit {
            Log.d(tag, "Fetching latest commit for repo ($repo) with ref ($ref)")
            val res: Commit = client.get("$baseUrl/$repo/commits/$ref") {
                parameter("per_page", 1)
            }.body()
            return res
        }

        @Serializable
        class Commit(
            @SerialName("sha") val shaHash: String,
            @SerialName("commit") val commitObj: CommitObject
        ) {
            @Serializable
            class CommitObject(
                val message: String,
                val author: Author,
                val committer: Author
            ) {
                @Serializable
                class Author(
                    val name: String,
                    val date: String
                )
            }
        }
    }
}