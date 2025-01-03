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
    val uid: Int,
    val supported: List<PatchInfo>,
    val unsupported: List<PatchInfo>,
    val universal: List<PatchInfo>
) {
    val all = sequence {
        yieldAll(supported)
        yieldAll(unsupported)
        yieldAll(universal)
    }

    val patchCount get() = supported.size + unsupported.size + universal.size

    fun patchSequence(allowUnsupported: Boolean) = if (allowUnsupported) {
        all
    } else {
        sequence {
            yieldAll(supported)
            yieldAll(universal)
        }
    }

    companion object Extensions {
        inline fun Iterable<BundleInfo>.toPatchSelection(
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

        fun PatchBundleRepository.bundleInfoFlow(packageName: String, version: String?) =
            sources.flatMapLatestAndCombine(
                combiner = { it.filterNotNull() }
            ) { source ->
                // Regenerate bundle information whenever this source updates.
                source.state.map { state ->
                    val bundle = state.patchBundleOrNull() ?: return@map null

                    val supported = mutableListOf<PatchInfo>()
                    val unsupported = mutableListOf<PatchInfo>()
                    val universal = mutableListOf<PatchInfo>()

                    bundle.patches.filter { it.compatibleWith(packageName) }.forEach {
                        val targetList = when {
                            it.compatiblePackages == null -> universal
                            it.supports(
                                packageName,
                                version
                            ) -> supported

                            else -> unsupported
                        }

                        targetList.add(it)
                    }

                    BundleInfo(source.getName(), source.uid, supported, unsupported, universal)
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