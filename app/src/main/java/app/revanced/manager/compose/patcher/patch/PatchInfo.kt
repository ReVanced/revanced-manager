package app.revanced.manager.compose.patcher.patch

import androidx.compose.runtime.Immutable
import app.revanced.manager.compose.patcher.PatchClass
import app.revanced.patcher.annotation.Package
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.include
import app.revanced.patcher.extensions.PatchExtensions.options
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.PatchOption
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class PatchInfo(
    val name: String,
    val description: String?,
    val dependencies: ImmutableList<String>?,
    val include: Boolean,
    val compatiblePackages: ImmutableList<CompatiblePackage>?,
    val options: ImmutableList<Option>?
) {
    constructor(patch: PatchClass) : this(
        patch.patchName,
        patch.description,
        patch.dependencies?.map { it.java.patchName }?.toImmutableList(),
        patch.include,
        patch.compatiblePackages?.map { CompatiblePackage(it) }?.toImmutableList(),
        patch.options?.map { Option(it) }?.toImmutableList())

    fun compatibleWith(packageName: String) = compatiblePackages?.any { it.name == packageName } ?: true

    fun supportsVersion(versionName: String) =
        compatiblePackages?.any { compatiblePackages.any { it.versions.isEmpty() || it.versions.any { version -> version == versionName } } }
            ?: true
}

@Immutable
data class CompatiblePackage(
    val name: String,
    val versions: ImmutableList<String>
) {
    constructor(pkg: Package) : this(pkg.name, pkg.versions.toList().toImmutableList())
}

data class Option(val title: String, val key: String, val description: String, val required: Boolean) {
    constructor(option: PatchOption<*>) : this(option.title, option.key, option.description, option.required)
}