package app.revanced.manager.patcher.runtime

import android.content.Context
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.Session
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.worker.ProgressEventHandler
import app.revanced.manager.ui.model.State
import app.revanced.manager.util.Options
import app.revanced.manager.util.PatchSelection
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class LocalRuntime(private val context: Context) : Runtime(context), KoinComponent {
    private val fs: Filesystem by inject()
    private val patchBundlesRepo: PatchBundleRepository by inject()

    override suspend fun execute(
        inputFile: String,
        outputFile: String,
        packageName: String,
        selectedPatches: PatchSelection,
        options: Options,
        logger: Logger,
        enableMultithreadedDexWriter: Boolean,
        onPatchCompleted: () -> Unit,
        onProgress: ProgressEventHandler,
    ) {
        val bundles = patchBundlesRepo.bundles.first()

        val selectedBundles = selectedPatches.keys
        val allPatches = bundles.filterKeys { selectedBundles.contains(it) }
            .mapValues { (_, bundle) -> bundle.patchClasses(packageName) }

        val patchList = selectedPatches.flatMap { (bundle, selected) ->
            allPatches[bundle]?.filter { selected.contains(it.name) }
                ?: throw IllegalArgumentException("Patch bundle $bundle does not exist")
        }

        val integrations = bundles.mapNotNull { (_, bundle) -> bundle.integrations }

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
            fs.tempDir.absolutePath,
            frameworkPath,
            aaptPath,
            enableMultithreadedDexWriter,
            context,
            logger,
            File(inputFile),
            onPatchCompleted = onPatchCompleted,
            onProgress
        ).use { session ->
            session.run(
                File(outputFile),
                patchList,
                integrations
            )
        }
    }
}