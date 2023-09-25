package app.revanced.manager.patcher.patch

import androidx.compose.runtime.Immutable
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption
import app.revanced.patcher.patch.options.PatchOptions
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

data class PatchInfo(
    val name: String,
    val description: String?,
    val include: Boolean,
    val compatiblePackages: ImmutableList<CompatiblePackage>?,
    val options: ImmutableList<Option>?
) {
    constructor(patch: Patch<*>) : this(
        patch.name.orEmpty(),
        patch.description,
        patch.use,
        patch.compatiblePackages?.map { CompatiblePackage(it) }?.toImmutableList(),
        patch.options.takeUnless(PatchOptions::isEmpty)?.map { (_, option) -> Option(option) }?.toImmutableList()
    )

    fun compatibleWith(packageName: String) =
        compatiblePackages?.any { it.packageName == packageName } ?: true

    fun supportsVersion(versionName: String) =
        compatiblePackages?.any { it.versions?.takeUnless(Set<String>::isEmpty) == null || it.versions.any { version -> version == versionName } }
            ?: true
}

@Immutable
data class CompatiblePackage(
    val packageName: String,
    val versions: ImmutableSet<String>?
) {
    constructor(pkg: Patch.CompatiblePackage) : this(pkg.name, pkg.versions?.toImmutableSet())
}

@Immutable
data class Option(
    val title: String,
    val key: String,
    val description: String,
    val required: Boolean,
    val type: Class<out PatchOption<*>>,
    val defaultValue: Any?
) {
    constructor(option: PatchOption<*>) : this(
        option.title ?: option.key,
        option.key,
        option.description.orEmpty(),
        option.required,
        option::class.java,
        option.value
    )
}