package app.revanced.manager.ui.component.patches

import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchInfo
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_INCOMPATIBLE
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel.Companion.SHOW_UNIVERSAL

fun buildBundleSections(
    bundles: List<PatchBundleInfo.Scoped>,
    query: String = "",
    filter: Int,
    collapsedBundleUids: List<Int>,
    selectedPackageNames: Set<String> = emptySet(),
    forceExpanded: Boolean = false
): List<BundleSection> {
    fun PatchInfo.matchesPackageFilter(): Boolean {
        if (selectedPackageNames.isEmpty()) return true
        val packages = compatiblePackages ?: return true
        return packages.any { it.packageName in selectedPackageNames }
    }

    fun PatchInfo.matchesSearchQuery(): Boolean {
        if (query.isBlank()) return true

        return name.contains(query, ignoreCase = true) ||
                description?.contains(query, ignoreCase = true) == true ||
                compatiblePackages?.any { pkg ->
                    pkg.packageName.contains(query, ignoreCase = true)
                } == true
    }

    fun List<PatchInfo>.searched() = filter {
        if (query.isBlank()) it.matchesPackageFilter() else it.matchesSearchQuery() && it.matchesPackageFilter()
    }

    return bundles.mapNotNull { bundle ->
        BundleSection(
            bundle = bundle,
            compatible = bundle.compatible.searched(),
            universal = if (filter and SHOW_UNIVERSAL != 0) bundle.universal.searched() else emptyList(),
            incompatible = if (filter and SHOW_INCOMPATIBLE != 0) bundle.incompatible.searched() else emptyList(),
            expanded = forceExpanded || bundle.uid !in collapsedBundleUids
        ).takeIf { query.isBlank() || it.hasVisiblePatches }
    }
}