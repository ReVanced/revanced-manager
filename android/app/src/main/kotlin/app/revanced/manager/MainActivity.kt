package app.revanced.manager

import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import app.revanced.manager.utils.Aapt
import app.revanced.manager.utils.aligning.ZipAligner
import app.revanced.manager.utils.signing.Signer
import app.revanced.manager.utils.zip.ZipFile
import app.revanced.manager.utils.zip.structures.ZipEntry
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherOptions
import app.revanced.patcher.data.Data
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.include
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.version
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.impl.DexPatchBundle
import dalvik.system.DexClassLoader
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class MainActivity : FlutterActivity() {
    private val PATCHER_CHANNEL = "app.revanced.manager/patcher"
    private val INSTALLER_CHANNEL = "app.revanced.manager/installer"
    private var patches = mutableListOf<Class<out Patch<Data>>>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var installerChannel: MethodChannel

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val mainChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, PATCHER_CHANNEL)
        installerChannel =
                MethodChannel(flutterEngine.dartExecutor.binaryMessenger, INSTALLER_CHANNEL)
        mainChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "loadPatches" -> {
                    val jarPatchBundlePath = call.argument<String>("jarPatchBundlePath")
                    val cacheDirPath = call.argument<String>("cacheDirPath")
                    if (jarPatchBundlePath != null && cacheDirPath != null) {
                        loadPatches(result, jarPatchBundlePath, cacheDirPath)
                    } else {
                        result.notImplemented()
                    }
                }
                "getCompatiblePackages" -> getCompatiblePackages(result)
                "getFilteredPatches" -> {
                    val targetPackage = call.argument<String>("targetPackage")
                    val targetVersion = call.argument<String>("targetVersion")
                    val ignoreVersion = call.argument<Boolean>("ignoreVersion")
                    if (targetPackage != null && targetVersion != null && ignoreVersion != null) {
                        getFilteredPatches(result, targetPackage, targetVersion, ignoreVersion)
                    } else {
                        result.notImplemented()
                    }
                }
                "runPatcher" -> {
                    val originalFilePath = call.argument<String>("originalFilePath")
                    val inputFilePath = call.argument<String>("inputFilePath")
                    val patchedFilePath = call.argument<String>("patchedFilePath")
                    val outFilePath = call.argument<String>("outFilePath")
                    val integrationsPath = call.argument<String>("integrationsPath")
                    val selectedPatches = call.argument<List<String>>("selectedPatches")
                    val cacheDirPath = call.argument<String>("cacheDirPath")
                    val mergeIntegrations = call.argument<Boolean>("mergeIntegrations")
                    val resourcePatching = call.argument<Boolean>("resourcePatching")
                    if (originalFilePath != null &&
                                    inputFilePath != null &&
                                    patchedFilePath != null &&
                                    outFilePath != null &&
                                    integrationsPath != null &&
                                    selectedPatches != null &&
                                    cacheDirPath != null &&
                                    mergeIntegrations != null &&
                                    resourcePatching != null
                    ) {
                        runPatcher(
                                result,
                                originalFilePath,
                                inputFilePath,
                                patchedFilePath,
                                outFilePath,
                                integrationsPath,
                                selectedPatches,
                                cacheDirPath,
                                mergeIntegrations,
                                resourcePatching
                        )
                    } else {
                        result.notImplemented()
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    fun loadPatches(
            result: MethodChannel.Result,
            jarPatchBundlePath: String,
            cacheDirPath: String
    ) {
        Thread(
                        Runnable {
                            patches.addAll(
                                    DexPatchBundle(
                                                    jarPatchBundlePath,
                                                    DexClassLoader(
                                                            jarPatchBundlePath,
                                                            cacheDirPath,
                                                            null,
                                                            javaClass.classLoader
                                                    )
                                            )
                                            .loadPatches()
                            )
                            handler.post { result.success(null) }
                        }
                )
                .start()
    }

    fun getCompatiblePackages(result: MethodChannel.Result) {
        Thread(
                        Runnable {
                            val filteredPackages = mutableListOf<String>()
                            patches.forEach patch@{ patch ->
                                patch.compatiblePackages?.forEach { pkg ->
                                    filteredPackages.add(pkg.name)
                                }
                            }
                            handler.post { result.success(filteredPackages.distinct()) }
                        }
                )
                .start()
    }

    fun getFilteredPatches(
            result: MethodChannel.Result,
            targetPackage: String,
            targetVersion: String,
            ignoreVersion: Boolean
    ) {
        Thread(
                        Runnable {
                            val filteredPatches = mutableListOf<Map<String, Any?>>()
                            patches.forEach patch@{ patch ->
                                patch.compatiblePackages?.forEach { pkg ->
                                    if (pkg.name == targetPackage &&
                                                    (ignoreVersion ||
                                                            pkg.versions.isNotEmpty() ||
                                                            pkg.versions.contains(targetVersion))
                                    ) {
                                        var p = mutableMapOf<String, Any?>()
                                        p.put("name", patch.patchName)
                                        p.put("version", patch.version)
                                        p.put("description", patch.description)
                                        p.put("include", patch.include)
                                        filteredPatches.add(p)
                                    }
                                }
                            }
                            handler.post { result.success(filteredPatches) }
                        }
                )
                .start()
    }

    fun runPatcher(
            result: MethodChannel.Result,
            originalFilePath: String,
            inputFilePath: String,
            patchedFilePath: String,
            outFilePath: String,
            integrationsPath: String,
            selectedPatches: List<String>,
            cacheDirPath: String,
            mergeIntegrations: Boolean,
            resourcePatching: Boolean
    ) {
        val originalFile = File(originalFilePath)
        val inputFile = File(inputFilePath)
        val patchedFile = File(patchedFilePath)
        val outFile = File(outFilePath)
        val integrations = File(integrationsPath)
        val filteredPatches =
                patches.filter { patch -> selectedPatches.any { it == patch.patchName } }

        Thread(
                        Runnable {
                            handler.post {
                                installerChannel.invokeMethod(
                                        "update",
                                        mapOf(
                                                "progress" to 0.1,
                                                "header" to "",
                                                "log" to "Copying original apk"
                                        )
                                )
                            }
                            Files.copy(
                                    originalFile.toPath(),
                                    inputFile.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                            )

                            handler.post {
                                installerChannel.invokeMethod(
                                        "update",
                                        mapOf(
                                                "progress" to 0.2,
                                                "header" to "Unpacking apk...",
                                                "log" to "Unpacking copied apk"
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
                                                                        installerChannel.invokeMethod(
                                                                                "update",
                                                                                mapOf(
                                                                                        "progress" to 0.2,
                                                                                        "header" to "",
                                                                                        "log" to msg
                                                                                )
                                                                        )
                                                                    }
                                                                }

                                                                override fun warn(msg: String) {
                                                                    handler.post {
                                                                        installerChannel.invokeMethod(
                                                                                "update",
                                                                                mapOf(
                                                                                        "progress" to 0.2,
                                                                                        "header" to "",
                                                                                        "log" to msg
                                                                                )
                                                                        )
                                                                    }
                                                                }

                                                                override fun info(msg: String) {
                                                                    handler.post {
                                                                        installerChannel.invokeMethod(
                                                                                "update",
                                                                                mapOf(
                                                                                        "progress" to 0.2,
                                                                                        "header" to "",
                                                                                        "log" to msg
                                                                                )
                                                                        )
                                                                    }
                                                                }

                                                                override fun trace(msg: String) {
                                                                    handler.post {
                                                                        installerChannel.invokeMethod(
                                                                                "update",
                                                                                mapOf(
                                                                                        "progress" to 0.2,
                                                                                        "header" to "",
                                                                                        "log" to msg
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
                                        mapOf(
                                                "progress" to 0.3,
                                                "header" to "",
                                                "log" to ""
                                        )
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
                            patcher.addPatches(filteredPatches)
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
                                            mapOf(
                                                    "progress" to 0.5,
                                                    "header" to "",
                                                    "log" to msg
                                            )
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
                            Signer("ReVanced", "s3cur3p@ssw0rd").signApk(patchedFile, outFile)

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
