package app.revanced.manager.patcher.runtime

import android.content.Context
import app.revanced.manager.patcher.Session
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.worker.ProgressEventHandler
import app.revanced.manager.ui.model.State
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import java.io.File

/**
 * Simple [Runtime] implementation that runs the patcher using coroutines.
 */
class CoroutineRuntime(private val context: Context) : Runtime(context) {
    override suspend fun execute(
        inputFile: String,
        outputFile: String,
        packageName: String,
        selectedPatches: PatchSelection,
        options: Options,
        logger: Logger,
        onPatchCompleted: suspend () -> Unit,
        onProgress: ProgressEventHandler,
    ) {
        val bundles = bundles()

        val selectedBundles = selectedPatches.keys
        val allPatches = bundles.filterKeys { selectedBundles.contains(it) }
            .mapValues { (_, bundle) -> bundle.patches(packageName) }

        val patchList = selectedPatches.flatMap { (bundle, selected) ->
            allPatches[bundle]?.filter { selected.contains(it.name) }
                ?: throw IllegalArgumentException("Patch bundle $bundle does not exist")
        }

        // Set all patch options.
        options.forEach { (bundle, bundlePatchOptions) ->
            val patches = allPatches[bundle] ?: return@forEach
            bundlePatchOptions.forEach { (patchName, configuredPatchOptions) ->
                val patchOptions = patches.single { it.name == patchName }.options
                configuredPatchOptions.forEach { (key, value) ->
                    patchOptions[key] = value
                }
            }
        }

        onProgress(null, State.COMPLETED, null) // Loading patches

        Session(
            cacheDir,
            frameworkPath,
            aaptPath,
            context,
            logger,
            File(inputFile),
            onPatchCompleted = onPatchCompleted,
            onProgress
        ).use { session ->
            session.run(
                File(outputFile),
                patchList
            )
        }
    }
}