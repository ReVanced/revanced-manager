package app.revanced.manager.backend.utils.filesyste


// TODO: adapt zipfilesystemutils code to android
//import java.io.Closeable
//import java.io.File
//import java.nio.file.FileSystems
//import java.nio.file.Files
//import java.nio.file.Path
//import java.util.zip.ZipEntry
//
//internal class ZipFileSystemUtils(
//    file: File
//) : Closeable {
//    private var zipFileSystem = FileSystems.newFileSystem(file.toPath(), mapOf("noCompression" to true))
//
//    private fun Path.deleteRecursively() {
//        if (Files.isDirectory(this)) {
//            Files.list(this).forEach { path ->
//                path.deleteRecursively()
//            }
//        }
//
//        Files.delete(this)
//    }
//
//    internal fun writePathRecursively(path: Path) {
//        Files.list(path).let { fileStream ->
//            fileStream.forEach { filePath ->
//                val fileSystemPath = filePath.getRelativePath(path)
//                fileSystemPath.deleteRecursively()
//            }
//
//            fileStream
//        }.close()
//
//        Files.walk(path).let { fileStream ->
//            fileStream.skip(1).forEach { filePath ->
//                val relativePath = filePath.getRelativePath(path)
//
//                if (Files.isDirectory(filePath)) {
//                    Files.createDirectory(relativePath)
//                    return@forEach
//                }
//
//                Files.copy(filePath, relativePath)
//            }
//
//            fileStream
//        }.close()
//    }
//
//    internal fun write(path: String, content: ByteArray) = Files.write(zipFileSystem.getPath(path), content)
//
//    private fun Path.getRelativePath(path: Path): Path = zipFileSystem.getPath(path.relativize(this).toString())
//
//    internal fun uncompress(vararg paths: String) =
//        paths.forEach { Files.setAttribute(zipFileSystem.getPath(it), "zip:method", ZipEntry.STORED) }
//
//    override fun close() = zipFileSystem.close()
//}