package app.revanced.manager.patcher.runtime

import android.content.Context
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.worker.ProgressEventHandler
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import java.io.FileNotFoundException

abstract class Runtime(context: Context) {
    protected val aaptPath = Aapt.binary(context)?.absolutePath
        ?: throw FileNotFoundException("Could not resolve aapt.")

    protected val frameworkPath: String =
        context.cacheDir.resolve("framework").also { it.mkdirs() }.absolutePath

    abstract suspend fun execute(
        inputFile: String,
        outputFile: String,
        packageName: String,
        selectedPatches: PatchSelection,
        options: Options,
        logger: Logger,
        enableMultithreadedDexWriter: Boolean,
        onPatchCompleted: () -> Unit,
        onProgress: ProgressEventHandler,
    )
}