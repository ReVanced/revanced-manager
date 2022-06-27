package app.revanced.manager.backend.utils.filesystem

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal class ZipFileSystemUtils(file: File, private val dir: String) : Closeable {
    private var zf = ZipFile(file)

    fun writePathRecursively(path: Path, doNotCompress: List<String>) {
        Files.list(path).use { fileStream ->
            fileStream.forEach { filePath ->
                if (Files.isRegularFile(filePath)) {
                    val relPath = filePath.toString().replace(dir, "")
                    zf.removeFile(relPath)
                }
            }
        }

        Files.walk(path).use { fileStream ->
            fileStream.forEach { filePath ->
                if (Files.isRegularFile(filePath) && !filePath.toString().contains(".zip")) { // this is ugly.
                    val relPath = filePath.toString().replace(dir, "")
                    write(relPath, filePath.toFile(), !doNotCompress.contains(relPath))
                }
            }
        }
    }

    fun write(path: String, content: File, compress: Boolean = true) {
        zf.addFile(content, ZipParameters().also { zp ->
            zp.compressionMethod =
                if (compress) CompressionMethod.DEFLATE else CompressionMethod.STORE
            zp.fileNameInZip = path
            zp.entrySize = content.length()
        })
    }

    override fun close() = zf.close()
}