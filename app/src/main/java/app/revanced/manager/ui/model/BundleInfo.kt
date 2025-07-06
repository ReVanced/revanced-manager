package app.revanced.manager.ui.model

import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.flatMapLatestAndCombine
import kotlinx.coroutines.flow.map

/**
 * A data class that contains patch bundle metadata for use by UI code.
 */
data class BundleInfo(
    val name: String,
    val version: String?,
    val uid: Int,
    val compatible: List<PatchInfo>,
    val incompatible: List<PatchInfo>,
    val universal: List<PatchInfo>
) {
    val all = sequence {
        yieldAll(compatible)
        yieldAll(incompatible)
        yieldAll(universal)
    }

    fun patchSequence(allowIncompatible: Boolean) = if (allowIncompatible) {
        all
    } else {
        sequence {
            yieldAll(compatible)
            yieldAll(universal)
        }
    }

    companion object Extensions {
        inline fun Iterable<BundleInfo>.toPatchSelection(
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

        fun PatchBundleRepository.bundleInfoFlow(packageName: String, version: String?) =
            sources.flatMapLatestAndCombine(
                combiner = { it.filterNotNull() }
            ) { source ->
                // Regenerate bundle information whenever this source updates.
                source.state.map { state ->
                    val bundle = state.patchBundleOrNull() ?: return@map null

                    val compatible = mutableListOf<PatchInfo>()
                    val incompatible = mutableListOf<PatchInfo>()
                    val universal = mutableListOf<PatchInfo>()

                    bundle.patches.filter { it.compatibleWith(packageName) }.forEach {
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

                    BundleInfo(source.getName(), bundle.patchBundleManifestAttributes?.version, source.uid, compatible, incompatible, universal)
                }
            }

        /**
         * Algorithm for determining whether all required options have been set.
         */
        inline fun Iterable<BundleInfo>.requiredOptionsSet(
            crossinline isSelected: (BundleInfo, PatchInfo) -> Boolean,
            crossinline optionsForPatch: (BundleInfo, PatchInfo) -> Map<String, Any?>?
        ) = all bundle@{ bundle ->
            bundle
                .all
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

enum class BundleType {
    Local,
    Remote
}