package app.revanced.manager.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BundleInfo(val patches: BundleAsset, val integrations: BundleAsset)

@Serializable
data class BundleAsset(val version: String, val url: String)