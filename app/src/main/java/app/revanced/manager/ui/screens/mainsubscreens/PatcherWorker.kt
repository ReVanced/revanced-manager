package app.revanced.manager.ui.screens.mainsubscreens

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.revanced.manager.Global
import app.revanced.manager.R
import app.revanced.manager.backend.api.ManagerAPI
import app.revanced.manager.backend.utils.Aapt
import app.revanced.manager.backend.utils.aligning.ZipAligner
import app.revanced.manager.backend.utils.signing.Signer
import app.revanced.manager.backend.utils.zip.ZipFile
import app.revanced.manager.backend.utils.zip.structures.ZipEntry
import app.revanced.manager.settings
import app.revanced.manager.ui.Resource
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.implementation.DexPatchBundle
import dalvik.system.DexClassLoader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class PatcherWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    private val patches = mutableStateOf<Resource<List<Class<out Patch<Data>>>>>(Resource.Loading)
    val tag = "PatcherWorker"

    override suspend fun doWork(): Result {
        if (runAttemptCount > 0) {
            return Result.failure(
                androidx.work.Data.Builder()
                    .putString("error", "Android requested retrying but retrying is disabled")
                    .build()
            ) // don't retry
        }
        val selectedPatches = inputData.getStringArray("selectedPatches")
            ?: throw IllegalArgumentException("selectedPatches is missing")
        val patchBundleFile = inputData.getString("patchBundleFile")
            ?: throw IllegalArgumentException("patchBundleFile is missing")

        val notificationIntent = Intent(applicationContext, PatcherWorker::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val channel = NotificationChannel(
            "revanced-patcher-patching", "Patching", NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(applicationContext, channel.id)
            .setContentTitle(applicationContext.getText(R.string.patcher_notification_title))
            .setContentText(applicationContext.getText(R.string.patcher_notification_message))
            .setLargeIcon(Icon.createWithResource(applicationContext, R.drawable.manager))
            .setSmallIcon(Icon.createWithResource(applicationContext, R.drawable.manager))
            .setContentIntent(pendingIntent).build()

        setForeground(ForegroundInfo(1, notification))
        return try {
            runPatcher(selectedPatches.toList(), patchBundleFile)
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Error while patching", e)
            Result.failure(
                androidx.work.Data.Builder()
                    .putString("error", "Error while patching: ${e.message ?: e::class.simpleName}")
                    .build()
            )
        }
    }

    private suspend fun runPatcher(
        selectedPatches: List<String>, patchBundleFile: String
    ): Boolean {
        val aaptPath = Aapt.binary(applicationContext).absolutePath
        val frameworkPath =
            applicationContext.filesDir.resolve("framework").also { it.mkdirs() }.absolutePath
        val integrationsCacheDir =
            applicationContext.filesDir.resolve("integrations-cache").also { it.mkdirs() }

        loadPatches(patchBundleFile)
        Log.d(tag, "Checking prerequisites")
        val patches = findPatchesByIds(selectedPatches)
        if (patches.isEmpty()) return true
        val integrations = downloadIntegrations(integrationsCacheDir)

        Log.d(tag, "Creating directories")
        val workdir = createWorkDir()
        val inputFile = File(workdir.parentFile!!, "base.apk")
        val patchedFile = File(workdir, "patched.apk").apply { createNewFile() }
        val outputFile = File(workdir, "out.apk")
        val cacheDirectory = workdir.resolve("cache")

        try {
            // TODO: Add back when split support is added to the Patcher.
            // Log.d(tag, "Copying base.apk from ${info.packageName}")
            // withContext(Dispatchers.IO) {
            //     Files.copy(
            //         File(info.publicSourceDir).toPath(),
            //         inputFile.toPath(),
            //         StandardCopyOption.REPLACE_EXISTING
            //     )
            // }

            Log.d(tag, "Creating patcher")
            val patcher = Patcher(
                PatcherOptions(inputFile,
                    cacheDirectory.absolutePath,
                    patchResources = true,
                    aaptPath = aaptPath,
                    frameworkFolderLocation = frameworkPath,
                    logger = object : app.revanced.patcher.logging.Logger {
                        override fun error(msg: String) {
                            Log.e(tag, msg)
                        }

                        override fun warn(msg: String) {
                            Log.w(tag, msg)
                        }

                        override fun info(msg: String) {
                            Log.i(tag, msg)
                        }

                        override fun trace(msg: String) {
                            Log.v(tag, msg)
                        }
                    })
            )

            Log.d(tag, "Merging integrations")
            patcher.addFiles(listOf(integrations)) {}

            Log.d(tag, "Adding ${patches.size} patch(es)")
            patcher.addPatches(patches)

            Log.d(tag, "Applying patches")
            patcher.applyPatches().forEach { (patch, result) ->
                if (result.isSuccess) {
                    Log.i(tag, "[success] $patch")
                    return@forEach
                }
                Log.e(tag, "[error] $patch:", result.exceptionOrNull()!!)
            }

            Log.d(tag, "Saving file")
            val result = patcher.save()

            ZipFile(patchedFile).use { file ->
                result.dexFiles.forEach {
                    file.addEntryCompressData(
                        ZipEntry.createWithName(it.name), it.dexFileInputStream.readBytes()
                    )
                }

                result.resourceFile?.let {
                    file.copyEntriesFromFileAligned(ZipFile(it), ZipAligner::getEntryAlignment)
                }
                file.copyEntriesFromFileAligned(ZipFile(inputFile), ZipAligner::getEntryAlignment)
            }

            Log.d(tag, "Signing apk")
            Signer("ReVanced", "s3cur3p@ssw0rd").signApk(patchedFile, outputFile)
            Log.i(tag, "Successfully patched into $outputFile")
        } finally {
            Log.d(tag, "Deleting workdir")
            // workdir.deleteRecursively()
        }
        return false
    }

    //private fun installNonRoot(apk: File) {
    //    val intent = Intent(Intent.ACTION_VIEW);
    //    intent.setDataAndType(
    //        Uri.fromFile(apk), "application/vnd.android.package-archive"
    //    );
    //    applicationContext.startActivity(intent);
    //}

    private fun createWorkDir(): File {
        return applicationContext.filesDir.resolve("tmp-${System.currentTimeMillis()}")
            .also { it.mkdirs() }
    }

    private fun findPatchesByIds(ids: Iterable<String>): List<Class<out Patch<Data>>> {
        val (patches) = patches.value as? Resource.Success ?: return listOf()
        return patches.filter { patch -> ids.any { it == patch.patchName } }
    }

    private suspend fun downloadIntegrations(workdir: File): File {
        return try {
            val (_, out) = ManagerAPI.downloadIntegrations(
                workdir, applicationContext.settings.data.map { pref ->
                    pref.get(stringPreferencesKey("integrations")) ?: Global.ghIntegrations
                }.first()
            )
            out
        } catch (e: Exception) {
            throw Exception("Failed to download integrations", e)
        }
    }

    private fun loadPatches(patchBundleFile: String) {
        try {
            val patchClasses = DexPatchBundle(
                patchBundleFile, DexClassLoader(
                    patchBundleFile,
                    applicationContext.codeCacheDir.absolutePath,
                    null,
                    javaClass.classLoader
                )
            ).loadPatches()
            patches.value = Resource.Success(patchClasses)
        } catch (e: Exception) {
            Log.e(tag, "An error occurred while loading patches", e)
        }
    }
}