package app.revanced.manager.compose.patcher

import android.util.Log
import app.revanced.manager.compose.patcher.worker.Progress
import app.revanced.manager.compose.util.tag
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.data.Context
import app.revanced.patcher.logging.Logger
import app.revanced.patcher.patch.Patch
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal typealias PatchClass = Class<out Patch<Context>>
internal typealias PatchList = List<PatchClass>

class Session(
    cacheDir: String,
    frameworkDir: String,
    aaptPath: String,
    private val input: File,
    private val onProgress: suspend (Progress) -> Unit = { }
) : Closeable {
    class PatchFailedException(val patchName: String, cause: Throwable?) :
        Exception("Got exception while executing $patchName", cause)

    private val logger = LogcatLogger
    private val temporary = File(cacheDir).resolve("manager").also { it.mkdirs() }
    private val patcher = Patcher(
        PatcherOptions(
            inputFile = input,
            resourceCacheDirectory = temporary.resolve("aapt-resources").path,
            frameworkFolderLocation = frameworkDir,
            aaptPath = aaptPath,
            logger = logger,
        )
    )

    private suspend fun Patcher.applyPatchesVerbose() {
        this.executePatches(true).forEach { (patch, result) ->
            if (result.isSuccess) {
                logger.info("$patch succeeded")
                onProgress(Progress.PatchSuccess(patch))
                return@forEach
            }
            logger.error("$patch failed:")
            result.exceptionOrNull()!!.printStackTrace()

            throw PatchFailedException(patch, result.exceptionOrNull())
        }
    }

    suspend fun run(output: File, selectedPatches: PatchList, integrations: List<File>) {
        onProgress(Progress.Merging)

        with(patcher) {
            logger.info("Merging integrations")
            addIntegrations(integrations) {}
            addPatches(selectedPatches)

            logger.info("Applying patches...")
            onProgress(Progress.PatchingStart)

            applyPatchesVerbose()
        }

        onProgress(Progress.Saving)
        logger.info("Writing patched files...")
        val result = patcher.save()

        val aligned = temporary.resolve("aligned.apk").also { Aligning.align(result, input, it) }

        logger.info("Patched apk saved to $aligned")

        Files.move(aligned.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    override fun close() {
        temporary.delete()
    }
}

private object LogcatLogger : Logger {
    fun String.fmt() = "[Patcher]: $this"

    override fun error(msg: String) {
        Log.e(tag, msg.fmt())
    }

    override fun warn(msg: String) {
        Log.w(tag, msg.fmt())
    }

    override fun info(msg: String) {
        Log.i(tag, msg.fmt())
    }

    override fun trace(msg: String) {
        Log.v(tag, msg.fmt())
    }
}