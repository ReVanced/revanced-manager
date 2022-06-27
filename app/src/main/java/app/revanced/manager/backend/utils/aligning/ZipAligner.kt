package app.revanced.manager.backend.utils.aligning

import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal object ZipAligner {
    fun align(input: File, output: File, alignment: Int = 4) {
        val zipFile = ZipFile(input)
        val entries: Enumeration<out ZipEntry?> = zipFile.entries()

        // fake
        val peekingFakeStream = PeekingFakeStream()
        val fakeOutputStream = ZipOutputStream(peekingFakeStream)
        // real
        val zipOutputStream = ZipOutputStream(BufferedOutputStream(output.outputStream()))

        val multiOutputStream = MultiOutputStream(
            listOf(
                fakeOutputStream, // fake, used to add the data to the fake stream
                zipOutputStream // real
            )
        )

        var bias = 0
        while (entries.hasMoreElements()) {
            var padding = 0

            val entry = ZipEntry(entries.nextElement()!!).also {
                it.compressedSize = -1
            }
            // fake, used to calculate the file offset of the entry
            fakeOutputStream.putNextEntry(entry)

            if (entry.method == ZipEntry.STORED) {
                val fileOffset = peekingFakeStream.peek()
                val newOffset = fileOffset + bias
                padding = ((alignment - (newOffset % alignment)) % alignment).toInt()

                // real
                entry.extra = if (entry.extra == null) ByteArray(padding)
                else Arrays.copyOf(entry.extra, entry.extra.size + padding)
            }

            zipOutputStream.putNextEntry(entry)
            zipFile.getInputStream(entry).copyTo(multiOutputStream)

            // fake, used to add remaining bytes
            fakeOutputStream.closeEntry()
            // real
            zipOutputStream.closeEntry()

            bias += padding
        }

        zipFile.close()
        zipOutputStream.close()
    }
}

private class MultiOutputStream(
    private val streams: Iterable<OutputStream>,
) : OutputStream() {
    override fun write(b: ByteArray, off: Int, len: Int) = streams.forEach {
        it.write(b, off, len)
    }

    override fun write(b: ByteArray) = streams.forEach {
        it.write(b)
    }

    override fun write(b: Int) = streams.forEach {
        it.write(b)
    }

}

private class PeekingFakeStream : OutputStream() {
    private var numberOfBytes: Long = 0

    fun peek() = numberOfBytes

    override fun write(b: Int) {
        numberOfBytes++
    }

    override fun write(b: ByteArray) {
        numberOfBytes += b.size
    }

    override fun write(b: ByteArray, offset: Int, len: Int) {
        numberOfBytes += len
    }
}