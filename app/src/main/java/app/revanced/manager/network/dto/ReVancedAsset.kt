package app.revanced.manager.network.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReVancedAsset (
    @SerialName("download_url")
    val downloadUrl: String,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
    @SerialName("signature_download_url")
    val signatureDownloadUrl: String? = null,
    val description: String,
    val version: String,
)

