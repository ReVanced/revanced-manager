package app.revanced.manager.flutter

import android.os.Handler
import android.os.Looper
import app.revanced.manager.flutter.utils.Aapt
import app.revanced.manager.flutter.utils.aligning.ZipAligner
import app.revanced.manager.flutter.utils.signing.Signer
import app.revanced.manager.flutter.utils.zip.ZipFile
import app.revanced.manager.flutter.utils.zip.structures.ZipEntry
import app.revanced.patcher.PatchBundleLoader
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.extensions.PatchExtensions.dependencies
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.include
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.PatchResult
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.LogRecord
import java.util.logging.Logger

class MainActivity : FlutterActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var installerChannel: MethodChannel
    private var cancel: Boolean = false
    private var stopResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val patcherChannel = "app.revanced.manager.flutter/patcher"
        val installerChannel = "app.revanced.manager.flutter/installer"

        val mainChannel =
            MethodChannel(flutterEngine.dartExecutor.binaryMessenger, patcherChannel)

        this.installerChannel =
            MethodChannel(flutterEngine.dartExecutor.binaryMessenger, installerChannel)

        mainChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "runPatcher" -> {
                    val patchBundleFilePath = call.argument<String>("patchBundleFilePath")
                    val originalFilePath = call.argument<String>("originalFilePath")
                    val inputFilePath = call.argument<String>("inputFilePath")
                    val patchedFilePath = call.argument<String>("patchedFilePath")
                    val outFilePath = call.argument<String>("outFilePath")
                    val integrationsPath = call.argument<String>("integrationsPath")
                    val selectedPatches = call.argument<List<String>>("selectedPatches")
                    val cacheDirPath = call.argument<String>("cacheDirPath")
                    val keyStoreFilePath = call.argument<String>("keyStoreFilePath")
                    val keystorePassword = call.argument<String>("keystorePassword")

                    if (patchBundleFilePath != null &&
                        originalFilePath != null &&
                        inputFilePath != null &&
                        patchedFilePath != null &&
                        outFilePath != null &&
                        integrationsPath != null &&
                        selectedPatches != null &&
                        cacheDirPath != null &&
                        keyStoreFilePath != null &&
                        keystorePassword != null
                    ) {
                        cancel = false
                        runPatcher(
                            result,
                            patchBundleFilePath,
                            originalFilePath,
                            inputFilePath,
                            patchedFilePath,
                            outFilePath,
                            integrationsPath,
                            selectedPatches,
                            cacheDirPath,
                            keyStoreFilePath,
                            keystorePassword
                        )
                    } else result.notImplemented()
                }

                "stopPatcher" -> {
                    cancel = true
                    stopResult = result
                }

                "getPatches" -> {
                    val patchBundleFilePath = call.argument<String>("patchBundleFilePath")
                    val cacheDirPath = call.argument<String>("cacheDirPath")

                    if (patchBundleFilePath != null) {
                        val patches = PatchBundleLoader.Dex(
                            File(patchBundleFilePath),
                            optimizedDexDirectory = File(cacheDirPath)
                        ).map { patch ->
                            val map = HashMap<String, Any>()
                            map["\"name\""] = "\"${patch.patchName.replace("\"","\\\"")}\""
                            map["\"description\""] = "\"${patch.description?.replace("\"","\\\"")}\""
                            map["\"excluded\""] = !patch.include
                            map["\"dependencies\""] = patch.dependencies?.map { "\"${it.java.patchName}\"" } ?: emptyList<Any>()
                            map["\"compatiblePackages\""] = patch.compatiblePackages?.map {
                                val map2 = HashMap<String, Any>()
                                map2["\"name\""] = "\"${it.name}\""
                                map2["\"versions\""] = it.versions.map { version -> "\"${version}\"" }
                                map2
                            } ?: emptyList<Any>()
                            map
                        }
                        result.success(patches)
                    } else result.notImplemented()
                }

                else -> result.notImplemented()
            }
        }
    }

    private fun runPatcher(
        result: MethodChannel.Result,
        patchBundleFilePath: String,
        originalFilePath: String,
        inputFilePath: String,
        patchedFilePath: String,
        outFilePath: String,
        integrationsPath: String,
        selectedPatches: List<String>,
        cacheDirPath: String,
        keyStoreFilePath: String,
        keystorePassword: String
    ) {
        val originalFile = File(originalFilePath)
        val inputFile = File(inputFilePath)
        val patchedFile = File(patchedFilePath)
        val outFile = File(outFilePath)
        val integrations = File(integrationsPath)
        val keyStoreFile = File(keyStoreFilePath)
        val cacheDir = File(cacheDirPath)

        Thread {
            fun updateProgress(progress: Double, header: String, log: String) {
                handler.post {
                    installerChannel.invokeMethod(
                        "update",
                        mapOf(
                            "progress" to progress,
                            "header" to header,
                            "log" to log
                        )
                    )
                }
            }

            fun postStop() = handler.post { stopResult!!.success(null) }

            // Setup logger
            Logger.getLogger("").apply {
                handlers.forEach {
                    it.close()
                    removeHandler(it)
                }

                object : java.util.logging.Handler() {
                    override fun publish(record: LogRecord) =
                        updateProgress(-1.0, "", record.message)

                    override fun flush() = Unit
                    override fun close() = flush()
                }.let(::addHandler)
            }

            try {
                updateProgress(0.0, "", "Copying APK")

                if (cancel) {
                    postStop()
                    return@Thread
                }

                originalFile.copyTo(inputFile, true)

                if (cancel) {
                    postStop()
                    return@Thread
                }

                updateProgress(0.05, "Reading APK...", "Reading APK")

                val patcher = Patcher(
                    PatcherOptions(
                        inputFile,
                        cacheDir,
                        Aapt.binary(applicationContext).absolutePath,
                        cacheDir.path,
                    )
                )

                if (cancel) {
                    postStop()
                    return@Thread
                }

                updateProgress(0.1, "Loading patches...", "Loading patches")

                val patches = PatchBundleLoader.Dex(
                    File(patchBundleFilePath),
                    optimizedDexDirectory = cacheDir
                ).filter { patch ->
                    val isCompatible = patch.compatiblePackages?.any {
                        it.name == patcher.context.packageMetadata.packageName
                    } ?: false

                    val compatibleOrUniversal =
                        isCompatible || patch.compatiblePackages.isNullOrEmpty()

                    compatibleOrUniversal && selectedPatches.any { it == patch.patchName }
                }

                if (cancel) {
                    postStop()
                    return@Thread
                }

                updateProgress(0.15, "Executing...", "")

                // Update the progress bar every time a patch is executed from 0.15 to 0.7
                val totalPatchesCount = patches.size
                val progressStep = 0.55 / totalPatchesCount
                var progress = 0.15

                patcher.apply {
                    acceptIntegrations(listOf(integrations))
                    acceptPatches(patches)

                    runBlocking {
                        apply(false).collect { patchResult: PatchResult ->
                            if (cancel) {
                                handler.post { stopResult!!.success(null) }
                                this.cancel()
                                this@apply.close()
                                return@collect
                            }

                            val msg = patchResult.exception?.let {
                                val writer = StringWriter()
                                it.printStackTrace(PrintWriter(writer))
                                "${patchResult.patchName} failed: $writer"
                            } ?: run {
                                "${patchResult.patchName} succeeded"
                            }

                            updateProgress(progress, "", msg)
                            progress += progressStep
                        }
                    }
                }

                if (cancel) {
                    postStop()
                    patcher.close()
                    return@Thread
                }

                updateProgress(0.8, "Building...", "")

                val res = patcher.get()
                patcher.close()

                ZipFile(patchedFile).use { file ->
                    res.dexFiles.forEach {
                        if (cancel) {
                            postStop()
                            return@Thread
                        }
                        file.addEntryCompressData(
                            ZipEntry.createWithName(it.name),
                            it.stream.readBytes()
                        )
                    }
                    res.resourceFile?.let {
                        file.copyEntriesFromFileAligned(
                            ZipFile(it),
                            ZipAligner::getEntryAlignment
                        )
                    }
                    file.copyEntriesFromFileAligned(
                        ZipFile(inputFile),
                        ZipAligner::getEntryAlignment
                    )
                }

                if (cancel) {
                    postStop()
                    return@Thread
                }

                updateProgress(0.9, "Signing...", "Signing APK")

                try {
                    Signer("ReVanced", keystorePassword)
                        .signApk(patchedFile, outFile, keyStoreFile)
                } catch (e: Exception) {
                    print("Error signing APK: ${e.message}")
                    e.printStackTrace()
                }

                updateProgress(1.0, "Patched", "Patched")
            } catch (ex: Throwable) {
                if (!cancel) {
                    val stack = ex.stackTraceToString()
                    updateProgress(
                        -100.0,
                        "Aborted",
                        "An error occurred:\n$stack"
                    )
                }
            }

            handler.post { result.success(null) }
        }.start()
    }
}
