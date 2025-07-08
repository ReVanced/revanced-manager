package app.revanced.manager.patcher.patch

import android.util.Log
import app.revanced.manager.util.tag
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchLoader
import java.io.File
import java.io.IOException
import java.util.jar.JarFile

class PatchBundleManifestAttributes(
    val name: String?,
    val version: String?,
    val description: String?,
    val source: String?,
    val author: String?,
    val contact: String?,
    val website: String?,
    val license: String?
)

class PatchBundle(val patchesJar: File) {
    private val loader = object : Iterable<Patch<*>> {
        private fun load(): Iterable<Patch<*>> {
            patchesJar.setReadOnly()
            return PatchLoader.Dex(setOf(patchesJar))
        }

        override fun iterator(): Iterator<Patch<*>> = load().iterator()
    }

    init {
        Log.d(tag, "Loaded patch bundle: $patchesJar")
    }

    /**
     * A list containing the metadata of every patch inside this bundle.
     */
    val patches = loader.map(::PatchInfo)

    /**
     * The [java.util.jar.Manifest] of [patchesJar].
     */
    private val manifest = try {
        JarFile(patchesJar).use { it.manifest }
    } catch (_: IOException) {
        null
    }

    val patchBundleManifestAttributes = if(manifest != null)
        PatchBundleManifestAttributes(
            name = readManifestAttribute("name"),
            version = readManifestAttribute("version"),
            description = readManifestAttribute("description"),
            source = readManifestAttribute("source"),
            author = readManifestAttribute("author"),
            contact = readManifestAttribute("contact"),
            website = readManifestAttribute("website"),
            license = readManifestAttribute("license")
        ) else
            null

    private fun readManifestAttribute(name: String) = manifest?.mainAttributes?.getValue(name)?.takeIf { it.isNotBlank() } // If empty, set it to null instead.

    /**
     * Load all patches compatible with the specified package.
     */
    fun patches(packageName: String) = loader.filter { patch ->
        val compatiblePackages = patch.compatiblePackages
            ?: // The patch has no compatibility constraints, which means it is universal.
            return@filter true

        if (!compatiblePackages.any { (name, _) -> name == packageName }) {
            // Patch is not compatible with this package.
            return@filter false
        }

        true
    }
}
