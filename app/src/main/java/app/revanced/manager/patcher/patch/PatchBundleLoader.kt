package app.revanced.manager.patcher.patch

import android.os.Build
import app.revanced.patcher.patch.Patch
import dalvik.system.DelegateLastClassLoader
import dalvik.system.PathClassLoader
import lanchon.multidexlib2.BasicDexFileNamer
import lanchon.multidexlib2.MultiDexIO
import java.io.File

class PatchBundleLoader() : ClassLoader(Patch::class.java.classLoader) {
    private val registry = mutableMapOf<PatchBundle, Entry>()

    constructor(bundles: Iterable<PatchBundle>) : this() {
        bundles.forEach(::register)
    }

    override fun findClass(name: String?): Class<*> {
        registry.values.find { entry -> name in entry.classes }?.let {
            return it.classLoader.loadClass(name)
        }

        return super.findClass(name)
    }

    // Taken from: https://github.com/ReVanced/revanced-patcher/blob/f57e571a147d33eed189b533eee3aa62388fb354/src/main/kotlin/app/revanced/patcher/PatchBundleLoader.kt#L127-L130
    private fun readClassNames(bundlePath: File): Set<String> = MultiDexIO.readDexFile(
        true,
        bundlePath,
        BasicDexFileNamer(),
        null,
        null
    ).classes.map { classDef ->
        classDef.type.substring(1, classDef.length - 1).replace('/', '.')
    }.toSet()

    private fun createClassLoader(bundlePath: File) =
        bundlePath.also(File::setReadOnly).absolutePath.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                // We need the delegate last policy for cross-bundle dependencies.
                DelegateLastClassLoader(it, this)
            } else {
                PathClassLoader(it, parent)
            }
        }

    fun register(bundle: PatchBundle) {
        registry[bundle] =
            Entry(readClassNames(bundle.patchesJar), createClassLoader(bundle.patchesJar))
    }

    private fun loadPatches(bundle: PatchBundle): List<Patch<*>> {
        val entry = registry[bundle]
            ?: throw Exception("Attempted to load classes from a patch bundle that has not been registered.")

        // Taken from: https://github.com/ReVanced/revanced-patcher/blob/f57e571a147d33eed189b533eee3aa62388fb354/src/main/kotlin/app/revanced/patcher/PatchBundleLoader.kt#L48-L54
        return entry.classes
            .map { entry.classLoader.loadClass(it) }
            .filter { Patch::class.java.isAssignableFrom(it) }
            .mapNotNull { it.getInstance() }
            .filter { it.name != null }
    }

    fun loadPatches(bundle: PatchBundle, packageName: String) =
        loadPatches(bundle).filter { patch ->
            val compatiblePackages = patch.compatiblePackages
                ?: // The patch has no compatibility constraints, which means it is universal.
                return@filter true

            if (!compatiblePackages.any { it.name == packageName }) {
                // Patch is not compatible with this package.
                return@filter false
            }

            true
        }

    fun loadMetadata(bundle: PatchBundle) = loadPatches(bundle).map(::PatchInfo)

    private companion object {
        fun Class<*>.getInstance(): Patch<*>? {
            try {
                // Get the Kotlin singleton instance.
                return getField("INSTANCE").get(null) as Patch<*>
            } catch (_: NoSuchFieldException) {
            }

            try {
                // Try to instantiate the class.
                return getDeclaredConstructor().newInstance() as Patch<*>
            } catch (_: Exception) {
            }

            return null
        }
    }

    private data class Entry(val classes: Set<String>, val classLoader: ClassLoader)
}