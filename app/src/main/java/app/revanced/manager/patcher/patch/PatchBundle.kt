package app.revanced.manager.patcher.patch

import kotlinx.parcelize.IgnoredOnParcel
import android.os.Parcelable
import app.revanced.patcher.patch.loadPatchesFromDex
import kotlinx.parcelize.Parcelize
import java.io.File
import java.io.IOException
import java.util.jar.JarFile
import kotlin.collections.filter

@Parcelize
data class PatchBundle(val patchesJar: String) : Parcelable {
    /**
     * The [java.util.jar.Manifest] of [patchesJar].
     */
    @IgnoredOnParcel
    private val manifest by lazy {
        try {
            JarFile(patchesJar).use { it.manifest }
        } catch (_: IOException) {
            null
        }
    }

    @IgnoredOnParcel
    val manifestAttributes by lazy {
        if (manifest != null)
            ManifestAttributes(
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
    }

    private fun readManifestAttribute(name: String) = manifest?.mainAttributes?.getValue(name)
        ?.takeIf { it.isNotBlank() } // If empty, set it to null instead.

    data class ManifestAttributes(
        val name: String?,
        val version: String?,
        val description: String?,
        val source: String?,
        val author: String?,
        val contact: String?,
        val website: String?,
        val license: String?
    )

    object Loader {
        private fun patches(bundles: Iterable<PatchBundle>) =
            loadPatchesFromDex(
                bundles.map { File(it.patchesJar) }.toSet()
            ).byPatchesFile.mapKeys { (file, _) ->
                val absPath = file.absolutePath
                bundles.single { absPath == it.patchesJar }
            }

        fun metadata(bundles: Iterable<PatchBundle>) =
            patches(bundles).mapValues { (_, patches) -> patches.map(::PatchInfo) }

        fun patches(bundles: Iterable<PatchBundle>, packageName: String) =
            patches(bundles).mapValues { (_, patches) ->
                patches.filter { patch ->
                    val compatiblePackages = patch.compatiblePackages
                        ?: // The patch has no compatibility constraints, which means it is universal.
                        return@filter true

                    if (!compatiblePackages.any { (name, _) -> name == packageName }) {
                        // Patch is not compatible with this package.
                        return@filter false
                    }

                    true
                }.toSet()
            }
    }
}
