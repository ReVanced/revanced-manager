package app.revanced.manager.ui.model

import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.util.flatMapLatestAndCombine
import kotlinx.coroutines.flow.map

data class BundleInfo(
    val name: String,
    val uid: Int,
    val all: List<PatchInfo>,
    val supported: List<PatchInfo>,
    val unsupported: List<PatchInfo>,
    val universal: List<PatchInfo>
) {
    fun sequence(allowUnsupported: Boolean) = if (allowUnsupported) {
        all.asSequence()
    } else {
        sequence {
            yieldAll(supported)
            yieldAll(universal)
        }
    }
}

// TODO: does this belong here or in the PatchBundleRepository?
fun PatchBundleRepository.bundleInfoFlow(packageName: String, version: String) =
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
                    it.supportsVersion(
                        packageName,
                        version
                    ) -> supported

                    else -> unsupported
                }

                targetList.add(it)
            }

            BundleInfo(source.name, source.uid, bundle.patches, supported, unsupported, universal)
        }
    }