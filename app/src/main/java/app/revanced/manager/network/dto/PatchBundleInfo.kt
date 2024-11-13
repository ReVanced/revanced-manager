package app.revanced.manager.network.dto

import kotlinx.serialization.Serializable

@Serializable
// TODO: replace this
data class PatchBundleInfo(val version: String, val url: String)