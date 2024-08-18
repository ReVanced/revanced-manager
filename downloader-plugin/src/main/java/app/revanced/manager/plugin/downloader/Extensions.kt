package app.revanced.manager.plugin.downloader

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

// OutputStream-based version of download
fun <A : App> DownloaderBuilder<A>.download(block: suspend DownloadScope.(A, OutputStream) -> Unit) =
    download { app ->
        val input = PipedInputStream(1024 * 1024)
        var currentThrowable: Throwable? = null

        val coroutineScope =
            CoroutineScope(Dispatchers.IO + Job() + CoroutineExceptionHandler { _, throwable ->
                currentThrowable?.let {
                    it.addSuppressed(throwable)
                    return@CoroutineExceptionHandler
                }

                currentThrowable = throwable
            })
        var started = false

        fun rethrowException() {
            currentThrowable?.let {
                currentThrowable = null
                throw it
            }
        }

        fun start() {
            started = true
            coroutineScope.launch {
                PipedOutputStream(input).use {
                    block(app, object : FilterOutputStream(it) {
                        var closed = false

                        private fun assertIsOpen() {
                            if (closed) throw IOException("Stream is closed.")
                        }

                        override fun write(b: ByteArray?, off: Int, len: Int) {
                            assertIsOpen()
                            super.write(b, off, len)
                        }

                        override fun write(b: Int) {
                            assertIsOpen()
                            super.write(b)
                        }

                        override fun close() {
                            closed = true
                        }
                    })
                }
            }
        }

        object : FilterInputStream(input) {
            override fun read(): Int {
                val array = ByteArray(1)
                if (read(array, 0, 1) != 1) return -1
                return array[0].toInt()
            }

            override fun read(b: ByteArray?, off: Int, len: Int): Int {
                if (!started) start()
                rethrowException()
                return super.read(b, off, len)
            }

            override fun close() {
                super.close()
                coroutineScope.cancel()
                rethrowException()
            }
        }
    }

fun <A : App> DownloaderBuilder<A>.download(block: suspend DownloadScope.(A, (InputStream) -> Unit) -> Unit) =
    download { app, outputStream: OutputStream ->
        block(app) { inputStream ->
            inputStream.use { it.copyTo(outputStream) }
        }
    }