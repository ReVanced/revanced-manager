package app.revanced.manager.backend.utils.filesystem

import android.util.Log
import app.revanced.manager.backend.utils.filesystem.zipfs.ZipFileSystemProvider
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry

private val zfsProvider = ZipFileSystemProvider()

internal class ZipFileSystemUtils(input: File, output: File) : Closeable {
    private val inFileSystem = zfsProvider.newFileSystem(input.toPath(), mapOf<String, Any>())
    private val outFileSystem = zfsProvider.newFileSystem(output.toPath(), mapOf("noCompression" to true))

    private fun Path.deleteRecursively() {
        Log.v("ZipFileSystemUtils", "Deleting file $this")

        if (!Files.exists(this)) {
            Log.w("ZipFileSystemUtils", "File ($this) exists in input but not in output, cannot delete")
            return
        }

        if (Files.isDirectory(this)) {
            Files.list(this).forEach { it.deleteRecursively() }
        }

        Files.delete(this)
        Log.v("ZipFileSystemUtils", "Deleted file $this")
    }

    internal fun writeInput() {
        if (inFileSystem == null) {
            throw IllegalArgumentException("Input file not set")
        }
        val root = inFileSystem.getPath(inFileSystem.separator)

        Files.list(root).also { fileStream ->
            fileStream.forEach { filePath ->
                Log.v("ZipFileSystemUtils", "Want to delete file ($filePath)")
                val fileSystemPath = outFileSystem.getPath(filePath.toString())
                if (filePath.toString() != fileSystemPath.toString()) {
                    throw IllegalStateException("File from input ($filePath) does not match file from output ($fileSystemPath)")
                }
                //fileSystemPath.deleteRecursively()
            }
        }.close()

        Files.walk(root).also { fileStream ->
            // skip the root directory (/) or else it will crash.
            fileStream.skip(1).forEach { filePath ->
                val relativePath = outFileSystem.getPath(filePath.toString())

                if (Files.isDirectory(filePath)) {
                    Files.createDirectory(relativePath)
                    return@forEach
                }

                Files.copy(filePath, relativePath)
            }
        }.close()
    }

    internal fun write(path: String, content: ByteArray) = Files.write(outFileSystem.getPath(path), content)

    internal fun uncompress(vararg paths: String) =
        paths.forEach { Files.setAttribute(outFileSystem.getPath(it), "zip:method", ZipEntry.STORED) }

    override fun close() {
        inFileSystem?.close()
        outFileSystem.close()
    }
}