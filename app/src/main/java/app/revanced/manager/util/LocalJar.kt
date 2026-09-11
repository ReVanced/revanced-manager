package app.revanced.manager.util

import java.io.File
import java.util.jar.JarFile

object LocalJar {
    private const val VERSION_ATTRIBUTE = "version"

    fun versionOf(file: File): String =
        runCatching { JarFile(file).use { it.manifestVersion() } }.getOrNull()
            ?: file.contentVersion()

    private fun JarFile.manifestVersion() = manifest?.version()

    private fun java.util.jar.Manifest.version() = mainAttributes?.getValue(VERSION_ATTRIBUTE)

    private fun File.contentVersion() = "${length()}-${lastModified()}"
}
