package app.revanced.manager.network.dto

import app.revanced.manager.domain.repository.SerializedSelection
import kotlinx.serialization.Serializable

@Serializable
data class LegacySettings(
    val keystorePassword: String,
    val themeMode: Int? = null,
    val useDynamicTheme: Boolean? = null,
    val apiUrl: String? = null,
    val experimentalPatchesEnabled: Boolean? = null,
    val patchesAutoUpdate: Boolean? = null,
    val patchesChangeEnabled: Boolean? = null,
    val showPatchesChangeWarning: Boolean? = null,
    val keystore: String? = null,
    val patches: SerializedSelection? = null,
)
