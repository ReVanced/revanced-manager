package app.revanced.manager

import app.revanced.manager.backend.api.GitHubAPI
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitHubAPITest {
    @Test
    fun fetchRelease() = runBlocking {
        val release = GitHubAPI.Releases.latestRelease(Global.ghPatches)
        assertNotNull(release)
        assertTrue(
            release.assets.isNotEmpty(),
            "Expected release to contain at least a single asset"
        )
        // TODO(Sculas): more coverage for this API call
    }

    @Test
    fun fetchCommit() = runBlocking {
        val commit = GitHubAPI.Commits.latestCommit(Global.ghPatches, "HEAD")
        assertNotNull(commit)
        assertTrue(commit.shaHash.isNotEmpty(), "Expected commit sha to not be empty")
        assertNotNull(commit.commitObj)
        assertNotNull(commit.commitObj.message, "Expected commit object to contain a message")
        assertNotNull(commit.commitObj.author, "Expected commit object to contain an author")
        assertNotNull(commit.commitObj.committer, "Expected commit object to contain an committer")
        assertTrue(
            commit.commitObj.author.name.isNotEmpty(),
            "Expected author name to not be empty",
        )
        assertTrue(
            commit.commitObj.author.date.isNotEmpty(),
            "Expected author date to not be empty",
        )
        assertTrue(
            commit.commitObj.committer.name.isNotEmpty(),
            "Expected committer name to not be empty",
        )
        assertTrue(
            commit.commitObj.committer.date.isNotEmpty(),
            "Expected committer date to not be empty",
        )
    }
}