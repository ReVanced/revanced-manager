package app.revanced.manager.network.dto

import android.os.Parcelable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Parcelize
@Serializable
data class ReVancedAnnouncement(
    val id: Long,
    val author: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
    @SerialName("archived_at")
    val archivedAt: LocalDateTime?,
    val level: Int,
) : Parcelable {
    val isArchived get() = archivedAt?.toInstant(TimeZone.UTC)?.let { it < Clock.System.now() } ?: false
}