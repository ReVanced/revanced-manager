package app.revanced.manager.backend.utils.filesystem

import java.io.Closeable
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal class ZipFileSystemUtils(destFile: File) : Closeable {
    private val dest = ZipOutputStream(destFile.outputStream())
    private val written = mutableListOf<String>()

    internal fun copyOver(srcFile: File, uncompressed: List<String>) {
        val src = ZipFile(srcFile)
        for (entry in src.entries()) {
            val path = entry.name
            if (written.contains(path)) continue
            val compressionMethod = if (uncompressed.contains(path)) {
                ZipEntry.STORED
            } else {
                entry.method
            }
            val zipEntry = ZipEntry(entry).apply { method = compressionMethod }
            dest.putNextEntry(zipEntry)
            src.getInputStream(entry).copyTo(dest)
            dest.closeEntry()
            written.add(path)
        }
        src.close()
    }

    internal fun write(path: String, content: ByteArray) {
        dest.putNextEntry(ZipEntry(path))
        dest.write(content)
        dest.closeEntry()
        written.add(path)
    }

    override fun close() {
        dest.close()
    }
}