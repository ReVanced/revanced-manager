package app.revanced.manager.patcher.runtime

import android.content.Context
import app.revanced.manager.patcher.ProgressEvent
import app.revanced.manager.patcher.Session
import app.revanced.manager.patcher.StepId
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.patch.PatchBundle
import app.revanced.manager.patcher.runStep
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import java.io.File

/**
 * Simple [Runtime] implementation that runs the patcher using coroutines.
 */
class CoroutineRuntime(context: Context) : Runtime(context) {
    override suspend fun execute(
        inputFile: String,
        outputFile: String,
        packageName: String,
        selectedPatches: PatchSelection,
        options: Options,
        logger: Logger,
        onEvent: (ProgressEvent) -> Unit,
    ) {
        val patchList = runStep(StepId.LoadPatches, onEvent) {
            val selectedBundles = selectedPatches.keys
            val bundles = bundles()
            val uids = bundles.entries.associate { (key, value) -> value to key }

            val allPatches =
                PatchBundle.Loader.patches(bundles.values, packageName)
                    .mapKeys { (b, _) -> uids[b]!! }
                    .filterKeys { it in selectedBundles }

            val patchList = selectedPatches.flatMap { (bundle, selected) ->
                allPatches[bundle]?.filter { it.name in selected }
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

            patchList
        }

        val session = runStep(StepId.ReadAPK, onEvent) {
            Session(
                cacheDir,
                frameworkPath,
                aaptPath,
                logger,
                File(inputFile),
                onEvent,
            )
        }

        session.use { s ->
            s.run(
                File(outputFile),
                patchList
            )
        }
    }
}