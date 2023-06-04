package app.revanced.manager.patcher.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.revanced.manager.domain.repository.SourceRepository
import app.revanced.manager.patcher.Session
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.util.PatchesSelection
import app.revanced.manager.util.tag
import app.revanced.patcher.extensions.PatchExtensions.patchName
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException

// TODO: setup wakelock + notification so android doesn't murder us.
class PatcherWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters),
    KoinComponent {
    private val sourceRepository: SourceRepository by inject()

    @Serializable
    data class Args(
        val input: String,
        val output: String,
        val selectedPatches: PatchesSelection,
        val packageName: String,
        val packageVersion: String
    )

    companion object {
        const val ARGS_KEY = "args"
        private const val logPrefix = "[Worker]:"
        private fun String.logFmt() = "$logPrefix $this"
    }

    override suspend fun doWork(): Result {
        if (runAttemptCount > 0) {
            Log.d(tag, "Android requested retrying but retrying is disabled.".logFmt())
            return Result.failure()
        }
        val aaptPath =
            Aapt.binary(applicationContext)?.absolutePath ?: throw FileNotFoundException("Could not resolve aapt.")

        val frameworkPath =
            applicationContext.cacheDir.resolve("framework").also { it.mkdirs() }.absolutePath

        val args = Json.decodeFromString<Args>(inputData.getString(ARGS_KEY)!!)

        val bundles = sourceRepository.bundles.first()
        val integrations = bundles.mapNotNull { (_, bundle) -> bundle.integrations }

        val patchList = args.selectedPatches.flatMap { (bundleName, selected) ->
            bundles[bundleName]?.loadPatchesFiltered(args.packageName)?.filter { selected.contains(it.patchName) }
                ?: throw IllegalArgumentException("Patch bundle $bundleName does not exist")
        }

        val progressManager =
            PatcherProgressManager(applicationContext, args.selectedPatches.flatMap { (_, selected) -> selected })

        suspend fun updateProgress(progress: Progress) {
            progressManager.handle(progress)
            setProgress(progressManager.groupsToWorkData())
        }

        updateProgress(Progress.Unpacking)

        return try {
            Session(applicationContext.cacheDir.path, frameworkPath, aaptPath, File(args.input)) {
                updateProgress(it)
            }.use { session ->
                session.run(File(args.output), patchList, integrations)
            }

            Log.i(tag, "Patching succeeded".logFmt())
            progressManager.success()
            Result.success(progressManager.groupsToWorkData())
        } catch (e: Throwable) {
            Log.e(tag, "Got exception while patching".logFmt(), e)
            progressManager.failure()
            Result.failure(progressManager.groupsToWorkData())
        }
    }
}