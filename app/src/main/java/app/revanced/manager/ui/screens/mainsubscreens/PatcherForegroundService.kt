package app.revanced.manager.ui.screens.mainsubscreens

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.backend.api.ManagerAPI
import app.revanced.manager.backend.utils.Aapt
import app.revanced.manager.backend.utils.aligning.ZipAligner
import app.revanced.manager.backend.utils.filesystem.ZipFileSystemUtils
import app.revanced.manager.backend.utils.signing.Signer
import app.revanced.manager.ui.Resource
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.implementation.DexPatchBundle
import dalvik.system.DexClassLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.IllegalArgumentException

class PatcherForegroundService : Service() {

    val patches = mutableStateOf<Resource<List<Class<out Patch<Data>>>>>(Resource.Loading)
    val tag = "Patcher"

    override fun onCreate() {
        super.onCreate()
        val notificationIntent = Intent(this, PatcherForegroundService::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val channel =
            NotificationChannel("revanced-patcher-patching",
                "Patching",
                NotificationManager.IMPORTANCE_LOW)
        val notificationManager =
            ContextCompat.getSystemService(this, NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, channel.id)
            .setContentTitle(getText(R.string.patcher_notification_title))
            .setContentText(getText(R.string.patcher_notification_message))
            //.setSmallIcon(R.drawable.icon)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            val selectedPatches = intent.getStringArrayExtra("selectedPatches")
                ?: throw IllegalArgumentException("selectedPatches is missing")
            val patchBundleFile = intent.getStringExtra("patchBundleFile")
                ?: throw IllegalArgumentException("patchBundleFile is missing")
            runPatcher(selectedPatches.toList(), patchBundleFile)
        }finally {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun runPatcher(selectedPatches: List<String>, patchBundleFile: String): Boolean {

        val aaptPath = Aapt.binary(this).absolutePath
        val frameworkPath = filesDir.resolve("framework").also { it.mkdirs() }.absolutePath
        val integrationsCacheDir = filesDir.resolve("integrations-cache").also { it.mkdirs() }

        runBlocking {
            loadPatches(patchBundleFile)
            Log.d(tag, "Checking prerequisites")
            val patches = findPatchesByIds(selectedPatches)
            if (patches.isEmpty()) return@runBlocking true
            val integrations = downloadIntegrations(integrationsCacheDir)

            Log.d(tag, "Creating directories")
            val workdir = createWorkDir()
            val inputFile = File(workdir.parentFile!!, "base.apk")
            val patchedFile = File(workdir, "patched.apk")
            val alignedFile = File(workdir, "aligned.apk")
            val outputFile = File(workdir, "out.apk")
            val cacheDirectory = workdir.resolve("cache")

            try {
                //                Log.d(tag, "Copying base.apk from ${info.packageName}")
                //                withContext(Dispatchers.IO) {
                //                    Files.copy(
                //                        File(info.publicSourceDir).toPath(),
                //                        inputFile.toPath(),
                //                        StandardCopyOption.REPLACE_EXISTING
                //                    )
                //                }

                Log.d(tag, "Creating patcher")
                val patcher = Patcher(
                    PatcherOptions(
                        inputFile,
                        cacheDirectory.absolutePath,
                        patchResources = true,
                        aaptPath = aaptPath,
                        frameworkFolderLocation = frameworkPath
                    )
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
                ZipFileSystemUtils(result.resourceFile!!, patchedFile).use { fs ->
                    result.dexFiles.forEach { fs.write(it.name, it.dexFileInputStream.readBytes()) }
                    fs.writeInput()
                    fs.uncompress(*result.doNotCompress!!.toTypedArray())
                }

                Log.d(tag, "Aligning apk")
                ZipAligner.align(patchedFile, alignedFile)
                Log.d(tag, "Signing apk")
                Signer("ReVanced", "s3cur3p@ssw0rd").signApk(alignedFile, outputFile)

                // TODO: install apk!
                Log.d(tag, "Installing apk")
            } catch (e: Exception) {
                Log.e(tag, "Error while patching", e)
            }

            Log.d(tag, "Deleting workdir")
            //workdir.deleteRecursively()
        }
        return false
    }

    private fun createWorkDir(): File {
        return filesDir.resolve("tmp-${System.currentTimeMillis()}").also { it.mkdirs() }
    }

    private fun findPatchesByIds(ids: Iterable<String>): List<Class<out Patch<Data>>> {
        val (patches) = patches.value as? Resource.Success ?: return listOf()
        return patches.filter { patch -> ids.any { it == patch.patchName } }
    }

    private suspend fun downloadIntegrations(workdir: File): File {
        return try {
            val (_, out) = ManagerAPI.downloadIntegrations(workdir)
            out
        } catch (e: Exception) {
            throw Exception("Failed to download integrations", e)
        }
    }

    private fun loadPatches(patchBundleFile: String) {
        try {
            loadPatches0(patchBundleFile)
        } catch (e: Exception) {
            Log.e(tag, "An error occurred while loading patches", e)
        }
    }

    private fun loadPatches0(path: String) {
        val patchClasses = DexPatchBundle(
            path, DexClassLoader(
                path,
                codeCacheDir.absolutePath,
                null,
                javaClass.classLoader
            )
        ).loadPatches()
        patches.value = Resource.Success(patchClasses)
    }
}