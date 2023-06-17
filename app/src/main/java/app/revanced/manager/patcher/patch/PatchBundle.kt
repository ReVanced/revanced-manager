package app.revanced.manager.patcher.patch

import android.util.Log
import app.revanced.manager.patcher.PatchClass
import app.revanced.manager.util.tag
import app.revanced.patcher.Patcher
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.util.patch.PatchBundle
import dalvik.system.PathClassLoader
import java.io.File

class PatchBundle(private val loader: Iterable<PatchClass>, val integrations: File?) {
    constructor(bundleJar: File, integrations: File?) : this(
        object : Iterable<PatchClass> {
            private val bundle = bundleJar.absolutePath.let {
                PatchBundle.Dex(
                    it,
                    PathClassLoader(it, Patcher::class.java.classLoader)
                )
            }

            override fun iterator() = bundle.loadPatches().iterator()
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