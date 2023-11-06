package app.revanced.manager.patcher.patch

import androidx.compose.runtime.Immutable
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.options.PatchOption
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
        patch.options.map { (_, option) -> Option(option) }.ifEmpty { null }?.toImmutableList()
    )

    fun compatibleWith(packageName: String) =
        compatiblePackages?.any { it.packageName == packageName } ?: true

    fun supportsVersion(packageName: String, versionName: String): Boolean {
        val packages = compatiblePackages ?: return true // Universal patch

        return packages.any { pkg ->
            if (pkg.packageName != packageName) {
                return@any false
            }

            pkg.versions == null || pkg.versions.contains(versionName)
        }
    }
}

@Immutable
data class CompatiblePackage(
    val packageName: String,
    val versions: ImmutableSet<String>?
) {
    constructor(pkg: Patch.CompatiblePackage) : this(
        pkg.name,
        pkg.versions?.toImmutableSet()
    )
}

@Immutable
data class Option(
    val title: String,
    val key: String,
    val description: String,
    val required: Boolean,
    val type: String,
    val default: Any?
) {
    constructor(option: PatchOption<*>) : this(
        option.title ?: option.key,
        option.key,
        option.description.orEmpty(),
        option.required,
        option.valueType,
        option.default,
    )
}