package app.revanced.manager.patcher

import android.util.Log
import app.revanced.manager.patcher.worker.Progress
import app.revanced.manager.util.tag
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.data.Context
import app.revanced.patcher.logging.Logger
import app.revanced.patcher.patch.Patch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val logger: Logger,
    private val input: File,
    private val onProgress: suspend (Progress) -> Unit = { }
) : Closeable {
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
            result.exceptionOrNull()!!.let {
                logger.error(result.exceptionOrNull()!!.stackTraceToString())

                throw it
            }
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

        withContext(Dispatchers.IO) {
            Files.move(aligned.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override fun close() {
        temporary.delete()
    }
}