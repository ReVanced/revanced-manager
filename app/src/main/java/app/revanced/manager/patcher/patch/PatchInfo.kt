package app.revanced.manager.patcher.patch

import androidx.compose.runtime.Immutable
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.Option as PatchOption
import app.revanced.patcher.patch.resourcePatch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlin.reflect.KType

data class PatchInfo(
    val name: String,
    val description: String?,
    val include: Boolean,
    val compatiblePackages: ImmutableList<CompatiblePackage>?,
    val options: ImmutableList<Option<*>>?
) {
    constructor(patch: Patch<*>) : this(
        patch.name.orEmpty(),
        patch.description,
        patch.use,
        patch.compatiblePackages?.map { (pkgName, versions) ->
            CompatiblePackage(
                pkgName,
                versions?.toImmutableSet()
            )
        }?.toImmutableList(),
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

    /**
     * Create a fake [Patch] with the same metadata as the [PatchInfo] instance.
     * The resulting patch cannot be executed.
     * This is necessary because some functions in ReVanced Library only accept full [Patch] objects.
     */
    fun toPatcherPatch(): Patch<*> =
        resourcePatch(name = name, description = description, use = include) {
            compatiblePackages?.let { pkgs ->
                compatibleWith(*pkgs.map { it.packageName to it.versions }.toTypedArray())
            }
        }
}

@Immutable
data class CompatiblePackage(
    val packageName: String,
    val versions: ImmutableSet<String>?
)

@Immutable
data class Option<T>(
    val title: String,
    val key: String,
    val description: String,
    val required: Boolean,
    val type: KType,
    val default: T?,
    val presets: Map<String, T?>?,
    val validator: (T?) -> Boolean,
) {
    constructor(option: PatchOption<T>) : this(
        option.title ?: option.key,
        option.key,
        option.description.orEmpty(),
        option.required,
        option.type,
        option.default,
        option.values,
        { option.validator(option, it) },
    )
}