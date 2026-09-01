package app.revanced.manager.util

import java.io.File
import java.util.zip.ZipFile

/**
 * Returns the ABIs for native libraries packaged in this APK.
 *
 * An empty set means the APK has no ABI-specific native libraries and is therefore
 * architecture-independent for the purpose of this safeguard.
 */
fun File.apkNativeAbis(): Set<String> = ZipFile(this).use { apk ->
    apk.entries().asSequence()
        .filterNot { it.isDirectory }
        .mapNotNull { entry ->
            val path = entry.name.split('/', limit = 3)
            if (
                path.size == 3 &&
                path[0] == "lib" &&
                path[1].isNotBlank() &&
                path[2].endsWith(".so")
            ) path[1] else null
        }
        .toSet()
}
