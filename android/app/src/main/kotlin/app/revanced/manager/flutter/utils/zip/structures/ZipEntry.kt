package app.revanced.manager.flutter.utils.zip.structures

import app.revanced.manager.flutter.utils.zip.*
import java.io.DataInput
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ZipEntry(
    val version: UShort,
    val versionNeeded: UShort,
    val flags: UShort,
    var compression: UShort,
    val modificationTime: UShort,
    val modificationDate: UShort,
    var crc32: UInt,
    var compressedSize: UInt,
    var uncompressedSize: UInt,
    val diskNumber: UShort,
    val internalAttributes: UShort,
    val externalAttributes: UInt,
    var localHeaderOffset: UInt,
    val fileName: String,
    val extraField: ByteArray,
    val fileComment: String,
    var localExtraField: ByteArray = ByteArray(0), //separate for alignment
) {
    val LFHSize: Int
        get() = LFH_HEADER_SIZE + fileName.toByteArray(Charsets.UTF_8).size + localExtraField.size

    val dataOffset: UInt
        get() = localHeaderOffset + LFHSize.toUInt()

    companion object {
        const val CDE_HEADER_SIZE = 46
        const val CDE_SIGNATURE = 0x02014b50u

        const val LFH_HEADER_SIZE = 30
        const val LFH_SIGNATURE = 0x04034b50u

        fun createWithName(fileName: String): ZipEntry {
            return ZipEntry(
                0x1403u, //made by unix, version 20
                0u,
                0u,
                0u,
                0x0821u, //seems to be static time google uses, no idea
                0x0221u, //same as above
                0u,
                0u,
                0u,
                0u,
                0u,
                0u,
                0u,
                fileName,
                ByteArray(0),
                ""
            )
        }

        fun fromCDE(input: DataInput): ZipEntry {
            val signature = input.readUIntLE()

            if (signature != CDE_SIGNATURE)
                throw IllegalArgumentException("Input doesn't start with central directory entry signature")

            val version = input.readUShortLE()
            val versionNeeded = input.readUShortLE()
            var flags = input.readUShortLE()
            val compression = input.readUShortLE()
            val modificationTime = input.readUShortLE()
            val modificationDate = input.readUShortLE()
            val crc32 = input.readUIntLE()
            val compressedSize = input.readUIntLE()
            val uncompressedSize = input.readUIntLE()
            val fileNameLength = input.readUShortLE()
            var fileName = ""
            val extraFieldLength = input.readUShortLE()
            var extraField = ByteArray(extraFieldLength.toInt())
            val fileCommentLength = input.readUShortLE()
            var fileComment = ""
            val diskNumber = input.readUShortLE()
            val internalAttributes = input.readUShortLE()
            val externalAttributes = input.readUIntLE()
            val localHeaderOffset = input.readUIntLE()

            val variableFieldsLength =
                fileNameLength.toInt() + extraFieldLength.toInt() + fileCommentLength.toInt()

            if (variableFieldsLength > 0) {
                val fileNameBytes = ByteArray(fileNameLength.toInt())
                input.readFully(fileNameBytes)
                fileName = fileNameBytes.toString(Charsets.UTF_8)

                input.readFully(extraField)

                val fileCommentBytes = ByteArray(fileCommentLength.toInt())
                input.readFully(fileCommentBytes)
                fileComment = fileCommentBytes.toString(Charsets.UTF_8)
            }

            flags = (flags and 0b1000u.inv()
                .toUShort()) //disable data descriptor flag as they are not used

            return ZipEntry(
                version,
                versionNeeded,
                flags,
                compression,
                modificationTime,
                modificationDate,
                crc32,
                compressedSize,
                uncompressedSize,
                diskNumber,
                internalAttributes,
                externalAttributes,
                localHeaderOffset,
                fileName,
                extraField,
                fileComment,
            )
        }
    }

    fun readLocalExtra(buffer: ByteBuffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        localExtraField = ByteArray(buffer.getUShort().toInt())
    }

    fun toLFH(): ByteBuffer {
        val nameBytes = fileName.toByteArray(Charsets.UTF_8)

        val buffer = ByteBuffer.allocate(LFH_HEADER_SIZE + nameBytes.size + localExtraField.size)
            .also { it.order(ByteOrder.LITTLE_ENDIAN) }

        buffer.putUInt(LFH_SIGNATURE)
        buffer.putUShort(versionNeeded)
        buffer.putUShort(flags)
        buffer.putUShort(compression)
        buffer.putUShort(modificationTime)
        buffer.putUShort(modificationDate)
        buffer.putUInt(crc32)
        buffer.putUInt(compressedSize)
        buffer.putUInt(uncompressedSize)
        buffer.putUShort(nameBytes.size.toUShort())
        buffer.putUShort(localExtraField.size.toUShort())

        buffer.put(nameBytes)
        buffer.put(localExtraField)

        buffer.flip()
        return buffer
    }

    fun toCDE(): ByteBuffer {
        val nameBytes = fileName.toByteArray(Charsets.UTF_8)
        val commentBytes = fileComment.toByteArray(Charsets.UTF_8)

        val buffer =
            ByteBuffer.allocate(CDE_HEADER_SIZE + nameBytes.size + extraField.size + commentBytes.size)
                .also { it.order(ByteOrder.LITTLE_ENDIAN) }

        buffer.putUInt(CDE_SIGNATURE)
        buffer.putUShort(version)
        buffer.putUShort(versionNeeded)
        buffer.putUShort(flags)
        buffer.putUShort(compression)
        buffer.putUShort(modificationTime)
        buffer.putUShort(modificationDate)
        buffer.putUInt(crc32)
        buffer.putUInt(compressedSize)
        buffer.putUInt(uncompressedSize)
        buffer.putUShort(nameBytes.size.toUShort())
        buffer.putUShort(extraField.size.toUShort())
        buffer.putUShort(commentBytes.size.toUShort())
        buffer.putUShort(diskNumber)
        buffer.putUShort(internalAttributes)
        buffer.putUInt(externalAttributes)
        buffer.putUInt(localHeaderOffset)

        buffer.put(nameBytes)
        buffer.put(extraField)
        buffer.put(commentBytes)

        buffer.flip()
        return buffer
    }
}

