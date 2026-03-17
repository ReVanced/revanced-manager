package app.revanced.manager.network.dto

import android.os.Parcelable
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ReVancedAnnouncement(
    val id: Long,
    val author: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("archived_at")
    val archivedAt: Instant?,
    val level: Int,
) : Parcelable {
    val isArchived get() = archivedAt?.let { it < Clock.System.now() } ?: false
}