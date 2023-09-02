package app.revanced.manager.patcher.patch

import android.util.Log
import app.revanced.manager.util.tag
import app.revanced.patcher.PatchBundleLoader
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.patch.PatchClass
import java.io.File

class PatchBundle(private val loader: Iterable<PatchClass>, val integrations: File?) {
    constructor(bundleJar: File, integrations: File?) : this(
        object : Iterable<PatchClass> {
            private fun load(): List<PatchClass> = PatchBundleLoader.Dex(bundleJar)

            override fun iterator() = load().iterator()
        },
        integrations
    ) {
        Log.d(tag, "Loaded patch bundle: $bundleJar")
    }

    /**
     * A list containing the metadata of every patch inside this bundle.
     */
    val patches = loader.map(::PatchInfo)

    /**
     * Load all patches compatible with the specified package.
     */
    fun patchClasses(packageName: String) = loader.filter { patch ->
        val compatiblePackages = patch.compatiblePackages
            ?: // The patch has no compatibility constraints, which means it is universal.
            return@filter true

        if (!compatiblePackages.any { it.name == packageName }) {
            // Patch is not compatible with this package.
            return@filter false
        }

        true
    }
}