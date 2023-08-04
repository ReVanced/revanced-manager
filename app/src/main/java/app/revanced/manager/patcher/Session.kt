package app.revanced.manager.patcher

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
    private val onStepSucceeded: suspend () -> Unit
) : Closeable {
    private val temporary = File(cacheDir).resolve("manager").also { it.mkdirs() }
    private val patcher = Patcher(
        PatcherOptions(
            inputFile = input,
            resourceCacheDirectory = temporary.resolve("aapt-resources").path,
            frameworkDirectory = frameworkDir,
            aaptPath = aaptPath,
            logger = logger,
        )
    )

    private suspend fun Patcher.applyPatchesVerbose() {
        this.executePatches(true).forEach { (patch, result) ->
            if (result.isSuccess) {
                logger.info("$patch succeeded")
                onStepSucceeded()
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
        onStepSucceeded() // Unpacking
        with(patcher) {
            logger.info("Merging integrations")
            addIntegrations(integrations) {}
            addPatches(selectedPatches)
            onStepSucceeded() // Merging

            logger.info("Applying patches...")
            applyPatchesVerbose()
        }

        logger.info("Writing patched files...")
        val result = patcher.save()

        val aligned = temporary.resolve("aligned.apk").also { Aligning.align(result, input, it) }

        logger.info("Patched apk saved to $aligned")

        withContext(Dispatchers.IO) {
            Files.move(aligned.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        onStepSucceeded() // Saving
    }

    override fun close() {
        temporary.delete()
    }
}