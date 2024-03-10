package app.revanced.manager.patcher

import android.content.Context
import app.revanced.library.ApkUtils
import app.revanced.library.ApkUtils.applyTo
import app.revanced.manager.R
import app.revanced.manager.patcher.logger.ManagerLogger
import app.revanced.manager.ui.model.State
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
    multithreadingDexFileWriter: Boolean,
    private val androidContext: Context,
    private val logger: ManagerLogger,
    private val input: File,
    private val patchesProgress: MutableStateFlow<Pair<Int, Int>>,
    private val onProgress: (name: String?, state: State?, message: String?) -> Unit
) : Closeable {
    private fun updateProgress(name: String? = null, state: State? = null, message: String? = null) =
        onProgress(name, state, message)

    private val tempDir = File(cacheDir).resolve("patcher").also { it.mkdirs() }
    private val patcher = Patcher(
        PatcherOptions(
            inputFile = input,
            resourceCachePath = tempDir.resolve("aapt-resources"),
            frameworkFileDirectory = frameworkDir,
            aaptBinaryPath = aaptPath,
            multithreadingDexFileWriter = multithreadingDexFileWriter,
        )
    )

    private suspend fun Patcher.applyPatchesVerbose(selectedPatches: PatchList) {
        var nextPatchIndex = 0

        updateProgress(
            name = androidContext.getString(R.string.applying_patch, selectedPatches[nextPatchIndex]),
            state = State.RUNNING
        )

        this.apply(true).collect { (patch, exception) ->
            if (patch !in selectedPatches) return@collect

            if (exception != null) {
                updateProgress(
                    name = androidContext.getString(R.string.failed_to_apply_patch, patch.name),
                    state = State.FAILED,
                    message = exception.stackTraceToString()
                )

                logger.error("${patch.name} failed:")
                logger.error(exception.stackTraceToString())
                throw exception
            }

            nextPatchIndex++

            patchesProgress.value.let {
                patchesProgress.emit(it.copy(it.first + 1))
            }

            selectedPatches.getOrNull(nextPatchIndex)?.let { nextPatch ->
                updateProgress(
                    name = androidContext.getString(R.string.applying_patch, nextPatch.name)
                )
            }

            logger.info("${patch.name} succeeded")
        }

        updateProgress(
            state = State.COMPLETED,
            name = androidContext.resources.getQuantityString(
                R.plurals.patches_applied,
                selectedPatches.size,
                selectedPatches.size
            )
        )
    }

    suspend fun run(output: File, selectedPatches: PatchList, integrations: List<File>) {
        updateProgress(state = State.COMPLETED) // Unpacking
        Logger.getLogger("").apply {
            handlers.forEach {
                it.close()
                removeHandler(it)
            }

            addHandler(logger)
        }
        with(patcher) {
            logger.info("Merging integrations")
            acceptIntegrations(integrations.toSet())
            acceptPatches(selectedPatches.toSet())
            updateProgress(state = State.COMPLETED) // Merging

            logger.info("Applying patches...")
            applyPatchesVerbose(selectedPatches.sortedBy { it.name })
        }

        logger.info("Writing patched files...")
        val result = patcher.get()

        val aligned = tempDir.resolve("aligned.apk")
        result.applyTo(aligned)

        logger.info("Patched apk saved to $aligned")

        withContext(Dispatchers.IO) {
            Files.move(aligned.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        updateProgress(state = State.COMPLETED) // Saving
    }

    override fun close() {
        tempDir.deleteRecursively()
        patcher.close()
    }

    companion object {
        operator fun PatchResult.component1() = patch
        operator fun PatchResult.component2() = exception
    }
}