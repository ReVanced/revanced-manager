package app.revanced.manager.compose.patcher.data

import app.revanced.manager.compose.patcher.PatchClass
import app.revanced.patcher.Patcher
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.util.patch.PatchBundle
import dalvik.system.PathClassLoader
import java.io.File

class PatchBundle(private val loader: Iterable<PatchClass>, val integrations: File?) {
    constructor(bundleJar: String, integrations: File?) : this(
        object : Iterable<PatchClass> {
            private val bundle = PatchBundle.Dex(
                bundleJar,
                PathClassLoader(bundleJar, Patcher::class.java.classLoader)
            )

            override fun iterator() = bundle.loadPatches().iterator()
        },
        integrations
    )

    /**
     * @return A list of patches that are compatible with this Apk.
     */
    fun loadPatchesFiltered(packageName: String) = loader.filter { patch ->
        val compatiblePackages = patch.compatiblePackages
            ?: // The patch has no compatibility constraints, which means it is universal.
            return@filter true

        if (!compatiblePackages.any { it.name == packageName }) {
            // Patch is not compatible with this package.
            return@filter false
        }

        true
    }

    fun loadAllPatches() = loader.toList()
}