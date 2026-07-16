package app.revanced.manager.patcher.patch

import kotlinx.parcelize.IgnoredOnParcel
import android.os.Parcelable
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.loadPatches as loadPatcherPatches
import kotlinx.parcelize.Parcelize
import java.io.File
import java.io.IOException
import java.util.jar.JarFile
import java.util.zip.ZipException
import java.util.zip.ZipFile

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
            bundles.forEach { bundle ->
                this[bundle] = runCatching { bundle.loadPatchSet() }
            }
        }

        private fun PatchBundle.loadPatchSet(): Set<Patch> {
            val file = File(patchesJar)
            file.requireDexContainer()

            var loadFailure: Throwable? = null
            val patches = loadPatcherPatches(
                file,
                onFailedToLoad = { _, throwable ->
                    loadFailure = throwable
                }
            ).patchesByFile.entries
                .firstOrNull { (patchesFile, _) -> patchesFile.absolutePath == file.absolutePath }
                ?.value
                .orEmpty()

            loadFailure?.let { throw it }
            return patches
        }

        private fun File.requireDexContainer() {
            if (!isFile) throw InvalidPatchBundleException("Patch bundle file is missing")
            if (length() == 0L) throw InvalidPatchBundleException("Patch bundle file is empty")

            val header = ByteArray(4)
            val bytesRead = inputStream().use { it.read(header) }
            if (bytesRead < header.size) throw InvalidPatchBundleException("Patch bundle file is too small")

            when {
                header.isDexMagic() -> return
                header.isZipMagic() -> requireZipWithDexFile()
                else -> throw InvalidPatchBundleException(
                    "Patch bundle is not a DEX or ZIP container (magic: ${header.toHexString(bytesRead)})"
                )
            }
        }

        private fun File.requireZipWithDexFile() {
            try {
                ZipFile(this).use { zip ->
                    val hasDexFile = zip.entries().asSequence().any { entry ->
                        !entry.isDirectory && entry.name.substringAfterLast('/').matches(DEX_ENTRY_REGEX)
                    }

                    if (!hasDexFile) {
                        throw InvalidPatchBundleException("Patch bundle ZIP does not contain classes.dex")
                    }
                }
            } catch (error: ZipException) {
                throw InvalidPatchBundleException("Patch bundle ZIP is corrupt", error)
            }
        }

        private fun ByteArray.isDexMagic() =
            this[0] == 'd'.code.toByte() &&
                this[1] == 'e'.code.toByte() &&
                this[2] == 'x'.code.toByte() &&
                this[3] == '\n'.code.toByte()

        private fun ByteArray.isZipMagic() =
            this[0] == 0x50.toByte() &&
                this[1] == 0x4b.toByte() &&
                this[2] == 0x03.toByte() &&
                this[3] == 0x04.toByte()

        private fun ByteArray.toHexString(length: Int) =
            take(length).joinToString(" ") { "%02x".format(it.toInt() and 0xff) }

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

    private class InvalidPatchBundleException(message: String, cause: Throwable? = null) : IOException(message, cause)

    private companion object {
        private val DEX_ENTRY_REGEX = Regex("classes(\\d*)?\\.dex")
    }
}
