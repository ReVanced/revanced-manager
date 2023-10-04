package app.revanced.manager.network.dto

import app.revanced.manager.domain.repository.SerializedSelection
import kotlinx.serialization.Serializable

@Serializable
data class LegacySettings(
    val keystorePassword: String,
    val themeMode: Int? = null,
    val useDynamicTheme: Int? = null,
    val apiUrl: String? = null,
    val experimentalPatchesEnabled: Int? = null,
    val patchesAutoUpdate: Int? = null,
    val patchesChangeEnabled: Int? = null,
    val showPatchesChangeWarning: Int? = null,
    val keystore: String? = null,
    val patches: SerializedSelection? = null,
)
