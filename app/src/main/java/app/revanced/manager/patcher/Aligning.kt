package app.revanced.manager.patcher

import app.revanced.manager.patcher.alignment.ZipAligner
import app.revanced.manager.patcher.alignment.zip.ZipFile
import app.revanced.manager.patcher.alignment.zip.structures.ZipEntry
import app.revanced.patcher.PatcherResult
import java.io.File

// This is the same aligner used by the CLI.
// It will be removed eventually.
object Aligning {
    fun align(result: PatcherResult, inputFile: File, outputFile: File) {
        // logger.info("Aligning ${inputFile.name} to ${outputFile.name}")

        if (outputFile.exists()) outputFile.delete()

        ZipFile(outputFile).use { file ->
            result.dexFiles.forEach {
                file.addEntryCompressData(
                    ZipEntry.createWithName(it.name),
                    it.stream.readBytes()
                )
            }

            result.resourceFile?.let {
                file.copyEntriesFromFileAligned(
                    ZipFile(it),
                    ZipAligner::getEntryAlignment
                )
            }

            file.copyEntriesFromFileAligned(
                ZipFile(inputFile),
                ZipAligner::getEntryAlignment
            )
        }
    }
}