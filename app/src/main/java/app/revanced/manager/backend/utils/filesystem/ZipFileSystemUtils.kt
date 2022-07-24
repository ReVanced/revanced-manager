package app.revanced.manager.backend.utils.filesystem

import app.revanced.manager.backend.utils.filesystem.zipfs.ZipFileSystemProvider
import java.io.Closeable
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry

private val zfsProvider = ZipFileSystemProvider()

internal class ZipFileSystemUtils(input: File, output: File) : Closeable {
    private val inFileSystem = zfsProvider.newFileSystem(input.toPath(), mapOf<String, Any>())
    private val outFileSystem = zfsProvider.newFileSystem(output.toPath(), mapOf("noCompression" to true))

    private fun Path.deleteRecursively() {
        if (!Files.exists(this)) {
            throw IllegalStateException("File exists in input but not in output, cannot delete")
        }

        if (Files.isDirectory(this)) {
            Files.list(this).forEach { path ->
                path.deleteRecursively()
            }
        }

        Files.delete(this)
    }

    internal fun writeInput() {
        if (inFileSystem == null) {
            throw IllegalArgumentException("Input file not set")
        }
        val root = inFileSystem.getPath(inFileSystem.separator)

        Files.list(root).also { fileStream ->
            fileStream.forEach { filePath ->
                val fileSystemPath = filePath.getRelativePath(root)
                fileSystemPath.deleteRecursively()
            }
        }.close()

        Files.walk(root).also { fileStream ->
            // don't include build directory by skipping the root node.
            fileStream.skip(1).forEach { filePath ->
                val relativePath = filePath.getRelativePath(root)

                if (Files.isDirectory(filePath)) {
                    Files.createDirectory(relativePath)
                    return@forEach
                }

                Files.copy(filePath, relativePath)
            }
        }.close()
    }

    internal fun write(path: String, content: ByteArray) = Files.write(outFileSystem.getPath(path), content)

    private fun Path.getRelativePath(path: Path): Path = outFileSystem.getPath(path.relativize(this).toString())

    internal fun uncompress(vararg paths: String) =
        paths.forEach { Files.setAttribute(outFileSystem.getPath(it), "zip:method", ZipEntry.STORED) }

    override fun close() {
        inFileSystem?.close()
        outFileSystem.close()
    }
}