package app.revanced.manager.dto.github

import kotlinx.serialization.Serializable

@Serializable
class ApiCommit(
    val sha: String,
    val commit: Object
) {
    @Serializable
    class Object(
        val message: String,
        val author: Author,
        val committer: Author
    )

    @Serializable
    class Author(
        val name: String,
        val date: String
    )
}