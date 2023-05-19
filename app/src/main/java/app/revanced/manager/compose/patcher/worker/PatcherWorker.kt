package app.revanced.manager.compose.patcher.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.revanced.manager.compose.patcher.Session
import app.revanced.manager.compose.patcher.aapt.Aapt
import app.revanced.manager.compose.patcher.data.repository.PatchesRepository
import app.revanced.patcher.extensions.PatchExtensions.patchName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileNotFoundException

// TODO: setup wakelock + notification so android doesn't murder us.
class PatcherWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters),
    KoinComponent {
    private val patchesRepository: PatchesRepository by inject()

    @Serializable
    data class Args(
        val input: String,
        val output: String,
        val selectedPatches: List<String>,
        val packageName: String,
        val packageVersion: String
    )

    companion object {
        const val ARGS_KEY = "args"
    }

    override suspend fun doWork(): Result {
        if (runAttemptCount > 0) {
            Log.d("revanced-worker", "Android requested retrying but retrying is disabled.")
            return Result.failure()
        }
        val aaptPath =
            Aapt.binary(applicationContext)?.absolutePath ?: throw FileNotFoundException("Could not resolve aapt.")

        val frameworkPath =
            applicationContext.cacheDir.resolve("framework").also { it.mkdirs() }.absolutePath

        val args = Json.decodeFromString<Args>(inputData.getString(ARGS_KEY)!!)
        val selected = args.selectedPatches.toSet()

        val patchList = patchesRepository.loadPatchClassesFiltered(args.packageName)
            .filter { selected.contains(it.patchName) }

        val progressManager = PatcherProgressManager(applicationContext, args.selectedPatches)

        suspend fun updateProgress(progress: Progress) {
            progressManager.handle(progress)
            setProgress(progressManager.groupsToWorkData())
        }

        updateProgress(Progress.Unpacking)

        return try {
            Session(applicationContext.cacheDir.path, frameworkPath, aaptPath, File(args.input)) {
                updateProgress(it)
            }.use { session ->
                session.run(File(args.output), patchList, patchesRepository.getIntegrations())
            }

            Log.i("revanced-worker", "Patching succeeded")
            progressManager.success()
            Result.success(progressManager.groupsToWorkData())
        } catch (e: Throwable) {
            Log.e("revanced-worker", "Got exception while patching", e)
            progressManager.failure()
            Result.failure(progressManager.groupsToWorkData())
        }
    }
}