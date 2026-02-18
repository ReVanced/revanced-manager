package app.revanced.manager.network.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ReVancedAnnouncement(
    val id: Long,
    val author: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val attachments: List<String>,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
    @SerialName("archived_at")
    val archivedAt: LocalDateTime,
    val level: Int,
) {
}