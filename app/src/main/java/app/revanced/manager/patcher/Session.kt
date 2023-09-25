package app.revanced.manager.patcher

import app.revanced.library.ApkUtils
import app.revanced.manager.ui.viewmodel.ManagerLogger
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Logger

internal typealias PatchList = List<Patch<*>>

class Session(
    cacheDir: String,
    frameworkDir: String,
    aaptPath: String,
    private val logger: ManagerLogger,
    private val input: File,
    private val onStepSucceeded: suspend () -> Unit
) : Closeable {
    private val temporary = File(cacheDir).resolve("manager").also { it.mkdirs() }
    private val patcher = Patcher(
        PatcherOptions(
            inputFile = input,
            resourceCachePath = temporary.resolve("aapt-resources"),
            frameworkFileDirectory = frameworkDir,
            aaptBinaryPath = aaptPath
        )
    )

    private suspend fun Patcher.applyPatchesVerbose() {
        this.apply(true).collect { (patch, exception) ->
            if (exception == null) {
                logger.info("$patch succeeded")
                onStepSucceeded()
                return@collect
            }
            logger.error("$patch failed:")
            logger.error(exception.stackTraceToString())
            throw exception
        }
    }

    suspend fun run(output: File, selectedPatches: PatchList, integrations: List<File>) {
        onStepSucceeded() // Unpacking
        Logger.getLogger("").apply {
            handlers.forEach {
                it.close()
                removeHandler(it)
            }

            addHandler(logger)
        }
        with(patcher) {
            logger.info("Merging integrations")
            acceptIntegrations(integrations)
            acceptPatches(selectedPatches)
            onStepSucceeded() // Merging

            logger.info("Applying patches...")
            applyPatchesVerbose()
        }

        logger.info("Writing patched files...")
        val result = patcher.get()

        val aligned = temporary.resolve("aligned.apk")
        ApkUtils.copyAligned(input, aligned, result)

        logger.info("Patched apk saved to $aligned")

        withContext(Dispatchers.IO) {
            Files.move(aligned.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        onStepSucceeded() // Saving
    }

    override fun close() {
        temporary.delete()
        patcher.close()
    }

    companion object {
        operator fun PatchResult.component1() = patch.name
        operator fun PatchResult.component2() = exception
    }
}