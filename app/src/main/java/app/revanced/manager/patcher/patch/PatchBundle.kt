package app.revanced.manager.patcher.patch

import kotlinx.parcelize.IgnoredOnParcel
import android.os.Parcelable
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.loadPatches
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
        private fun patches(bundles: Iterable<PatchBundle>) = buildMap {
            val bundleMap = bundles.associateBy { it.patchesJar }

            loadPatches(
                *bundleMap.keys.map(::File).toTypedArray(),
                onFailedToLoad = { file, throwable ->
                    this[bundleMap[file.absolutePath]!!] = Result.failure(throwable)
                }
            ).patchesByFile.forEach { (file, patches) ->
                putIfAbsent(bundleMap[file.absolutePath]!!, Result.success(patches))
            }
        }

        fun metadata(bundles: Iterable<PatchBundle>): Map<PatchBundle, Result<Set<PatchInfo>>> =
            patches(bundles).mapValues { (_, result) ->
                result.map { patches ->
                    patches.mapTo(
                        HashSet(patches.size),
                        ::PatchInfo
                    )
                }
            }

        fun patches(bundles: Iterable<PatchBundle>, packageName: String): Map<PatchBundle, Set<Patch>> =
            patches(bundles).mapValues { (_, result) ->
                val patches = result.getOrDefault(emptySet())

                patches.filterTo(HashSet(patches.size)) { patch ->
                    val compatiblePackages = patch.compatiblePackages
                        ?: // The patch has no compatibility constraints, which means it is universal.
                        return@filterTo true

                    if (!compatiblePackages.any { (name, _) -> name == packageName }) {
                        // Patch is not compatible with this package.
                        return@filterTo false
                    }

                    true
                }
            }
    }
}