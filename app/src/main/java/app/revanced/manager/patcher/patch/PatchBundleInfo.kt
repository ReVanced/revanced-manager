package app.revanced.manager.patcher.patch

import app.revanced.manager.util.PatchSelection

/**
 * A base class for storing [PatchBundle] metadata.
 */
sealed class PatchBundleInfo {
    /**
     * The name of the bundle.
     */
    abstract val name: String

    /**
     * The version of the bundle.
     */
    abstract val version: String?

    /**
     * The unique ID of the bundle.
     */
    abstract val uid: Int

    /**
     * The patch list.
     */
    abstract val patches: List<PatchInfo>

    /**
     * Information about a bundle and all the patches it contains.
     *
     * @see [PatchBundleInfo]
     */
    data class Global(
        override val name: String,
        override val version: String?,
        override val uid: Int,
        override val patches: List<PatchInfo>
    ) : PatchBundleInfo() {
        /**
         * Create a [PatchBundleInfo.Scoped] that only contains information about patches that are relevant for a specific [packageName].
         */
        fun forPackage(packageName: String, version: String?): Scoped {
            val relevantPatches = patches.filter { it.compatibleWith(packageName) }
            val compatible = mutableListOf<PatchInfo>()
            val incompatible = mutableListOf<PatchInfo>()
            val universal = mutableListOf<PatchInfo>()

            relevantPatches.forEach {
                val targetList = when {
                    it.compatiblePackages == null -> universal
                    it.supports(
                        packageName,
                        version
                    ) -> compatible

                    else -> incompatible
                }

                targetList.add(it)
            }

            return Scoped(
                name,
                this.version,
                uid,
                relevantPatches,
                compatible,
                incompatible,
                universal
            )
        }
    }

    /**
     * Contains information about a bundle that is relevant for a specific package name.
     *
     * @param compatible Patches that are compatible with the specified package name and version.
     * @param incompatible Patches that are compatible with the specified package name but not version.
     * @param universal Patches that are compatible with all packages.
     * @see [PatchBundleInfo.Global.forPackage]
     * @see [PatchBundleInfo]
     */
    data class Scoped(
        override val name: String,
        override val version: String?,
        override val uid: Int,
        override val patches: List<PatchInfo>,
        val compatible: List<PatchInfo>,
        val incompatible: List<PatchInfo>,
        val universal: List<PatchInfo>
    ) : PatchBundleInfo() {
        fun patchSequence(allowIncompatible: Boolean) = if (allowIncompatible) {
            patches.asSequence()
        } else {
            sequence {
                yieldAll(compatible)
                yieldAll(universal)
            }
        }
    }

    companion object Extensions {
        inline fun Iterable<Scoped>.toPatchSelection(
            allowIncompatible: Boolean,
            condition: (Int, PatchInfo) -> Boolean
        ): PatchSelection = this.associate { bundle ->
            val patches =
                bundle.patchSequence(allowIncompatible)
                    .mapNotNullTo(mutableSetOf()) { patch ->
                        patch.name.takeIf {
                            condition(
                                bundle.uid,
                                patch
                            )
                        }
                    }

            bundle.uid to patches
        }

        /**
         * Algorithm for determining whether all required options have been set.
         */
        inline fun Iterable<Scoped>.requiredOptionsSet(
            allowIncompatible: Boolean,
            crossinline isSelected: (Scoped, PatchInfo) -> Boolean,
            crossinline optionsForPatch: (Scoped, PatchInfo) -> Map<String, Any?>?
        ) = all bundle@{ bundle ->
            bundle
                .patchSequence(allowIncompatible)
                .filter { isSelected(bundle, it) }
                .all patch@{
                    if (it.options.isNullOrEmpty()) return@patch true
                    val opts by lazy { optionsForPatch(bundle, it).orEmpty() }

                    it.options.all option@{ option ->
                        if (!option.required || option.default != null) return@option true

                        option.key in opts
                    }
                }
        }
    }
}