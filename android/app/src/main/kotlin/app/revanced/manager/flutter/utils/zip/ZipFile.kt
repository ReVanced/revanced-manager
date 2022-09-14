package app.revanced.manager.flutter.utils.zip

import app.revanced.manager.flutter.utils.zip.structures.ZipEndRecord
import app.revanced.manager.flutter.utils.zip.structures.ZipEntry
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.zip.CRC32
import java.util.zip.Deflater

class ZipFile(val file: File) : Closeable {
    var entries: MutableList<ZipEntry> = mutableListOf()

    private val filePointer: RandomAccessFile = RandomAccessFile(file, "rw")
    private var CDNeedsRewrite = false

    private val compressionLevel = 5

    init {
        //if file isn't empty try to load entries
        if (file.length() > 0) {
            val endRecord = findEndRecord()

            if (endRecord.diskNumber > 0u || endRecord.totalEntries != endRecord.diskEntries)
                throw IllegalArgumentException("Multi-file archives are not supported")

            entries = readEntries(endRecord).toMutableList()
        }

        //seek back to start for writing
        filePointer.seek(0)
    }

    private fun findEndRecord(): ZipEndRecord {
        //look from end to start since end record is at the end
        for (i in filePointer.length() - 1 downTo 0) {
            filePointer.seek(i)
            //possible beginning of signature
            if (filePointer.readByte() == 0x50.toByte()) {
                //seek back to get the full int
                filePointer.seek(i)
                val possibleSignature = filePointer.readUIntLE()
                if (possibleSignature == ZipEndRecord.ECD_SIGNATURE) {
                    filePointer.seek(i)
                    return ZipEndRecord.fromECD(filePointer)
                }
            }
        }

        throw Exception("Couldn't find end record")
    }

    private fun readEntries(endRecord: ZipEndRecord): List<ZipEntry> {
        filePointer.seek(endRecord.centralDirectoryStartOffset.toLong())

        val numberOfEntries = endRecord.diskEntries.toInt()

        return buildList(numberOfEntries) {
            for (i in 1..numberOfEntries) {
                add(
                    ZipEntry.fromCDE(filePointer).also
                    {
                        //for some reason the local extra field can be different from the central one
                        it.readLocalExtra(
                            filePointer.channel.map(
                                FileChannel.MapMode.READ_ONLY,
                                it.localHeaderOffset.toLong() + 28,
                                2
                            )
                        )
                    })
            }
        }
    }

    private fun writeCD() {
        val CDStart = filePointer.channel.position().toUInt()

        entries.forEach {
            filePointer.channel.write(it.toCDE())
        }

        val entriesCount = entries.size.toUShort()

        val endRecord = ZipEndRecord(
            0u,
            0u,
            entriesCount,
            entriesCount,
            filePointer.channel.position().toUInt() - CDStart,
            CDStart,
            ""
        )

        filePointer.channel.write(endRecord.toECD())
    }

    private fun addEntry(entry: ZipEntry, data: ByteBuffer) {
        CDNeedsRewrite = true

        entry.localHeaderOffset = filePointer.channel.position().toUInt()

        filePointer.channel.write(entry.toLFH())
        filePointer.channel.write(data)

        entries.add(entry)
    }

    fun addEntryCompressData(entry: ZipEntry, data: ByteArray) {
        val compressor = Deflater(compressionLevel, true)
        compressor.setInput(data)
        compressor.finish()

        val uncompressedSize = data.size
        val compressedData =
            ByteArray(uncompressedSize) //i'm guessing compression won't make the data bigger

        val compressedDataLength = compressor.deflate(compressedData)
        val compressedBuffer =
            ByteBuffer.wrap(compressedData.take(compressedDataLength).toByteArray())

        compressor.end()

        val crc = CRC32()
        crc.update(data)

        entry.compression = 8u //deflate compression
        entry.uncompressedSize = uncompressedSize.toUInt()
        entry.compressedSize = compressedDataLength.toUInt()
        entry.crc32 = crc.value.toUInt()

        addEntry(entry, compressedBuffer)
    }

    fun addEntryCopyData(entry: ZipEntry, data: ByteBuffer, alignment: Int? = null) {
        alignment?.let { alignment ->
            //calculate where data would end up
            val dataOffset = filePointer.filePointer + entry.LFHSize

            val mod = dataOffset % alignment

            //wrong alignment
            if (mod != 0L) {
                //add padding at end of extra field
                entry.localExtraField =
                    entry.localExtraField.copyOf((entry.localExtraField.size + (alignment - mod)).toInt())
            }
        }

        addEntry(entry, data)
    }

    fun getDataForEntry(entry: ZipEntry): ByteBuffer {
        return filePointer.channel.map(
            FileChannel.MapMode.READ_ONLY,
            entry.dataOffset.toLong(),
            entry.compressedSize.toLong()
        )
    }

    fun copyEntriesFromFileAligned(file: ZipFile, entryAlignment: (entry: ZipEntry) -> Int?) {
        for (entry in file.entries) {
            if (entries.any { it.fileName == entry.fileName }) continue //don't add duplicates

            val data = file.getDataForEntry(entry)
            addEntryCopyData(entry, data, entryAlignment(entry))
        }
    }

    override fun close() {
        if (CDNeedsRewrite) writeCD()
        filePointer.close()
    }
}
