package app.revanced.manager.patcher.patch

import app.revanced.manager.util.PatchSelection

/**
 * A base class for storing [PatchBundle] metadata.
 *
 * @param name The name of the bundle.
 * @param uid The unique ID of the bundle.
 * @param patches The patch list.
 */
sealed class PatchBundleInfo(val name: String, val uid: Int, val patches: List<PatchInfo>) {
    /**
     * Information about a bundle and all the patches it contains.
     *
     * @see [PatchBundleInfo]
     */
    class Global(name: String, uid: Int, patches: List<PatchInfo>) :
        PatchBundleInfo(name, uid, patches) {

        /**
         * Create a [PatchBundleInfo.Scoped] that only contains information about patches that are relevant for a specific [packageName].
         */
        fun forPackage(packageName: String, version: String): Scoped {
            val relevantPatches = patches.filter { it.compatibleWith(packageName) }
            val supported = mutableListOf<PatchInfo>()
            val unsupported = mutableListOf<PatchInfo>()
            val universal = mutableListOf<PatchInfo>()

            relevantPatches.forEach {
                val targetList = when {
                    it.compatiblePackages == null -> universal
                    it.supportsVersion(
                        packageName,
                        version
                    ) -> supported

                    else -> unsupported
                }

                targetList.add(it)
            }

            return Scoped(name, uid, relevantPatches, supported, unsupported, universal)
        }
    }

    /**
     * Contains information about a bundle that is relevant for a specific package name.
     *
     * @param supportedPatches Patches that are compatible with the specified package name and version.
     * @param unsupportedPatches Patches that are compatible with the specified package name but not version.
     * @param universalPatches Patches that are compatible with all packages.
     * @see [PatchBundleInfo.Global.forPackage]
     * @see [PatchBundleInfo]
     */
    class Scoped(
        name: String,
        uid: Int,
        patches: List<PatchInfo>,
        val supportedPatches: List<PatchInfo>,
        val unsupportedPatches: List<PatchInfo>,
        val universalPatches: List<PatchInfo>
    ) : PatchBundleInfo(name, uid, patches) {
        fun patchSequence(allowUnsupported: Boolean) = if (allowUnsupported) {
            patches.asSequence()
        } else {
            sequence {
                yieldAll(supportedPatches)
                yieldAll(universalPatches)
            }
        }
    }

    companion object Extensions {
        inline fun Iterable<Scoped>.toPatchSelection(
            allowUnsupported: Boolean,
            condition: (Int, PatchInfo) -> Boolean
        ): PatchSelection = this.associate { bundle ->
            val patches =
                bundle.patchSequence(allowUnsupported)
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
    }
}