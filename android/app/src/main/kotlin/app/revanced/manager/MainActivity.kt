package app.revanced.manager.flutter

import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import app.revanced.manager.flutter.utils.Aapt
import app.revanced.manager.flutter.utils.aligning.ZipAligner
import app.revanced.manager.flutter.utils.signing.Signer
import app.revanced.manager.flutter.utils.zip.ZipFile
import app.revanced.manager.flutter.utils.zip.structures.ZipEntry
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.util.patch.impl.DexPatchBundle
import dalvik.system.DexClassLoader
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File

class MainActivity : FlutterActivity() {
    private val PATCHER_CHANNEL = "app.revanced.manager.flutter/patcher"
    private val INSTALLER_CHANNEL = "app.revanced.manager.flutter/installer"
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var installerChannel: MethodChannel

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val mainChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, PATCHER_CHANNEL)
        installerChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, INSTALLER_CHANNEL)
        mainChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "copyOriginalApk" -> {
                    val originalFilePath = call.argument<String>("originalFilePath")
                    val backupFilePath = call.argument<String>("backupFilePath") 
                    if (originalFilePath != null && backupFilePath != null) {
                        File(originalFilePath).copyTo(File(backupFilePath), true)
                        result.success(null)
                    } else {
                        result.notImplemented()
                    }
                }
                "runPatcher" -> {
                    val patchBundleFilePath = call.argument<String>("patchBundleFilePath")
                    val inputFilePath = call.argument<String>("inputFilePath")
                    val patchedFilePath = call.argument<String>("patchedFilePath")
                    val outFilePath = call.argument<String>("outFilePath")
                    val integrationsPath = call.argument<String>("integrationsPath")
                    val selectedPatches = call.argument<List<String>>("selectedPatches")
                    val cacheDirPath = call.argument<String>("cacheDirPath")
                    val mergeIntegrations = call.argument<Boolean>("mergeIntegrations")
                    val resourcePatching = call.argument<Boolean>("resourcePatching")
                    val keyStoreFilePath = call.argument<String>("keyStoreFilePath")
                    if (patchBundleFilePath != null &&
                                    inputFilePath != null &&
                                    patchedFilePath != null &&
                                    outFilePath != null &&
                                    integrationsPath != null &&
                                    selectedPatches != null &&
                                    cacheDirPath != null &&
                                    mergeIntegrations != null &&
                                    resourcePatching != null &&
                                    keyStoreFilePath != null
                    ) {
                        runPatcher(
                                result,
                                patchBundleFilePath,
                                inputFilePath,
                                patchedFilePath,
                                outFilePath,
                                integrationsPath,
                                selectedPatches,
                                cacheDirPath,
                                mergeIntegrations,
                                resourcePatching,
                                keyStoreFilePath
                        )
                    } else {
                        result.notImplemented()
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    fun runPatcher(
            result: MethodChannel.Result,
            patchBundleFilePath: String,
            inputFilePath: String,
            patchedFilePath: String,
            outFilePath: String,
            integrationsPath: String,
            selectedPatches: List<String>,
            cacheDirPath: String,
            mergeIntegrations: Boolean,
            resourcePatching: Boolean,
            keyStoreFilePath: String
    ) {
        val inputFile = File(inputFilePath)
        val patchedFile = File(patchedFilePath)
        val outFile = File(outFilePath)
        val integrations = File(integrationsPath)
        val keyStoreFile = File(keyStoreFilePath)

        val patches =
                DexPatchBundle(
                                patchBundleFilePath,
                                DexClassLoader(
                                        patchBundleFilePath,
                                        cacheDirPath,
                                        null,
                                        javaClass.classLoader
                                )
                        )
                        .loadPatches()
                        .filter { patch -> selectedPatches.any { it == patch.patchName } }

        Thread(
                        Runnable {
                            handler.post {
                                installerChannel.invokeMethod(
                                        "update",
                                        mapOf(
                                                "progress" to 0.2,
                                                "header" to "Unpacking apk...",
                                                "log" to "Unpacking input apk"
                                        )
                                )
                            }
                            val patcher =
                                    Patcher(
                                            PatcherOptions(
                                                    inputFile,
                                                    cacheDirPath,
                                                    resourcePatching,
                                                    Aapt.binary(applicationContext).absolutePath,
                                                    cacheDirPath,
                                                    logger =
                                                            object :
                                                                    app.revanced.patcher.logging.Logger {
                                                                override fun error(msg: String) {
                                                                    handler.post {
                                                                        installerChannel
                                                                                .invokeMethod(
                                                                                        "update",
                                                                                        mapOf(
                                                                                                "progress" to
                                                                                                        -1.0,
                                                                                                "header" to
                                                                                                        "",
                                                                                                "log" to
                                                                                                        msg
                                                                                        )
                                                                                )
                                                                    }
                                                                }

                                                                override fun warn(msg: String) {
                                                                    handler.post {
                                                                        installerChannel
                                                                                .invokeMethod(
                                                                                        "update",
                                                                                        mapOf(
                                                                                                "progress" to
                                                                                                        -1.0,
                                                                                                "header" to
                                                                                                        "",
                                                                                                "log" to
                                                                                                        msg
                                                                                        )
                                                                                )
                                                                    }
                                                                }

                                                                override fun info(msg: String) {
                                                                    handler.post {
                                                                        installerChannel
                                                                                .invokeMethod(
                                                                                        "update",
                                                                                        mapOf(
                                                                                                "progress" to
                                                                                                        -1.0,
                                                                                                "header" to
                                                                                                        "",
                                                                                                "log" to
                                                                                                        msg
                                                                                        )
                                                                                )
                                                                    }
                                                                }

                                                                override fun trace(msg: String) {
                                                                    handler.post {
                                                                        installerChannel
                                                                                .invokeMethod(
                                                                                        "update",
                                                                                        mapOf(
                                                                                                "progress" to
                                                                                                        -1.0,
                                                                                                "header" to
                                                                                                        "",
                                                                                                "log" to
                                                                                                        msg
                                                                                        )
                                                                                )
                                                                    }
                                                                }
                                                            }
                                            )
                                    )

                            handler.post {
                                installerChannel.invokeMethod(
                                        "update",
                                        mapOf("progress" to 0.3, "header" to "", "log" to "")
                                )
                            }
                            if (mergeIntegrations) {
                                handler.post {
                                    installerChannel.invokeMethod(
                                            "update",
                                            mapOf(
                                                    "progress" to 0.4,
                                                    "header" to "Merging integrations...",
                                                    "log" to "Merging integrations"
                                            )
                                    )
                                }
                                patcher.addFiles(listOf(integrations)) {}
                            }

                            handler.post {
                                installerChannel.invokeMethod(
                                        "update",
                                        mapOf(
                                                "progress" to 0.5,
                                                "header" to "Applying patches...",
                                                "log" to ""
                                        )
                                )
                            }
                            patcher.addPatches(patches)
                            patcher.applyPatches().forEach { (patch, res) ->
                                if (res.isSuccess) {
                                    val msg = "[success] $patch"
                                    handler.post {
                                        installerChannel.invokeMethod(
                                                "update",
                                                mapOf(
                                                        "progress" to 0.5,
                                                        "header" to "",
                                                        "log" to msg
                                                )
                                        )
                                    }
                                    return@forEach
                                }
                                val msg = "[error] $patch:" + res.exceptionOrNull()!!
                                handler.post {
                                    installerChannel.invokeMethod(
                                            "update",
                                            mapOf("progress" to 0.5, "header" to "", "log" to msg)
                                    )
                                }
                            }

                            handler.post {
                                installerChannel.invokeMethod(
                                        "update",
                                        mapOf(
                                                "progress" to 0.7,
                                                "header" to "Repacking apk...",
                                                "log" to "Repacking patched apk"
                                        )
                                )
                            }
                            val res = patcher.save()
                            ZipFile(patchedFile).use { file ->
                                res.dexFiles.forEach {
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
                            handler.post {
                                installerChannel.invokeMethod(
                                        "update",
                                        mapOf(
                                                "progress" to 0.9,
                                                "header" to "Signing apk...",
                                                "log" to ""
                                        )
                                )
                            }
                            Signer("ReVanced", "s3cur3p@ssw0rd").signApk(patchedFile, outFile, keyStoreFile)

                            handler.post {
                                installerChannel.invokeMethod(
                                        "update",
                                        mapOf(
                                                "progress" to 1.0,
                                                "header" to "Finished!",
                                                "log" to "Finished!"
                                        )
                                )
                            }

                            handler.post { result.success(null) }
                        }
                )
                .start()
    }
}
