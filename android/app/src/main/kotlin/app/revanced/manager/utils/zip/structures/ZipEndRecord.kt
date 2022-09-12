package app.revanced.manager.flutter.utils.zip.structures

import app.revanced.manager.flutter.utils.zip.putUInt
import app.revanced.manager.flutter.utils.zip.putUShort
import app.revanced.manager.flutter.utils.zip.readUIntLE
import app.revanced.manager.flutter.utils.zip.readUShortLE
import java.io.DataInput
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ZipEndRecord(
    val diskNumber: UShort,
    val startingDiskNumber: UShort,
    val diskEntries: UShort,
    val totalEntries: UShort,
    val centralDirectorySize: UInt,
    val centralDirectoryStartOffset: UInt,
    val fileComment: String,
) {

    companion object {
        const val ECD_HEADER_SIZE = 22
        const val ECD_SIGNATURE = 0x06054b50u

        fun fromECD(input: DataInput): ZipEndRecord {
            val signature = input.readUIntLE()

            if (signature != ECD_SIGNATURE)
                throw IllegalArgumentException("Input doesn't start with end record signature")

            val diskNumber = input.readUShortLE()
            val startingDiskNumber = input.readUShortLE()
            val diskEntries = input.readUShortLE()
            val totalEntries = input.readUShortLE()
            val centralDirectorySize = input.readUIntLE()
            val centralDirectoryStartOffset = input.readUIntLE()
            val fileCommentLength = input.readUShortLE()
            var fileComment = ""

            if (fileCommentLength > 0u) {
                val fileCommentBytes = ByteArray(fileCommentLength.toInt())
                input.readFully(fileCommentBytes)
                fileComment = fileCommentBytes.toString(Charsets.UTF_8)
            }

            return ZipEndRecord(
                diskNumber,
                startingDiskNumber,
                diskEntries,
                totalEntries,
                centralDirectorySize,
                centralDirectoryStartOffset,
                fileComment
            )
        }
    }

    fun toECD(): ByteBuffer {
        val commentBytes = fileComment.toByteArray(Charsets.UTF_8)

        val buffer = ByteBuffer.allocate(ECD_HEADER_SIZE + commentBytes.size)
            .also { it.order(ByteOrder.LITTLE_ENDIAN) }

        buffer.putUInt(ECD_SIGNATURE)
        buffer.putUShort(diskNumber)
        buffer.putUShort(startingDiskNumber)
        buffer.putUShort(diskEntries)
        buffer.putUShort(totalEntries)
        buffer.putUInt(centralDirectorySize)
        buffer.putUInt(centralDirectoryStartOffset)
        buffer.putUShort(commentBytes.size.toUShort())

        buffer.put(commentBytes)

        buffer.flip()
        return buffer
    }
}
