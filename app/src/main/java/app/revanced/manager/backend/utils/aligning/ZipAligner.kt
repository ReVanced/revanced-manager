package app.revanced.manager.backend.utils.aligning

import app.revanced.manager.backend.utils.zip.ZipFile
import java.io.File

internal object ZipAligner {
    private const val DEFAULT_ALIGNMENT = 4
    private const val LIBRARY_ALIGNMENT = 4096

    fun align(input: File, output: File) {
        val inputZip = ZipFile(input)
        val outputZip = ZipFile(output)

        for (entry in inputZip.entries) {
            val data = inputZip.getDataForEntry(entry)

            if (entry.compression == 0.toUShort()) {
                val alignment =
                    if (entry.fileName.endsWith(".so")) LIBRARY_ALIGNMENT else DEFAULT_ALIGNMENT

                outputZip.addEntryCopyData(entry, data, alignment)
            } else {
                outputZip.addEntryCopyData(entry, data)
            }
        }

        outputZip.close()
    }
}
