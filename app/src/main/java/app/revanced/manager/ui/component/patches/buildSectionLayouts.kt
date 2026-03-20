package app.revanced.manager.ui.component.patches

import app.revanced.manager.patcher.patch.PatchBundleInfo
import app.revanced.manager.patcher.patch.PatchInfo

fun buildSectionLayouts(sections: List<BundleSection>) = buildList {
    var itemIndex = 0

    sections.forEach { section ->
        add(BundleSectionLayout(section.bundle, itemIndex))
        itemIndex += 1

        if (!section.expanded) return@forEach

        itemIndex += section.compatible.size
        if (section.universal.isNotEmpty()) {
            itemIndex += 1 + section.universal.size
        }
        if (section.incompatible.isNotEmpty()) {
            itemIndex += 1 + section.incompatible.size
        }
    }
}

data class BundleSection(
    val bundle: PatchBundleInfo.Scoped,
    val compatible: List<PatchInfo>,
    val universal: List<PatchInfo>,
    val incompatible: List<PatchInfo>,
    val expanded: Boolean
) {
    val hasVisiblePatches: Boolean
        get() = compatible.isNotEmpty() || universal.isNotEmpty() || incompatible.isNotEmpty()
}

data class BundleSectionLayout(
    val bundle: PatchBundleInfo.Scoped,
    val headerIndex: Int
)