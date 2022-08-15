package app.revanced.manager

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
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.extensions.PatchExtensions.version
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.patch.implementation.DexPatchBundle
import dalvik.system.DexClassLoader
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class MainActivity : FlutterActivity() {
    private val CHANNEL = "app.revanced.manager/patcher"
    private var patches = mutableListOf<Class<out Patch<Data>>>()
    private val tag = "Patcher"
    private lateinit var methodChannel: MethodChannel
    private lateinit var patcher: Patcher

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "loadPatches" -> {
                    val pathBundlesPaths = call.argument<List<String>>("pathBundlesPaths")
                    if (pathBundlesPaths != null) {
                        result.success(loadPatches(pathBundlesPaths))
                    } else {
                        result.notImplemented()
                    }
                }
                "getCompatiblePackages" -> result.success(getCompatiblePackages())
                "getFilteredPatches" -> {
                    val targetPackage = call.argument<String>("targetPackage")
                    val targetVersion = call.argument<String>("targetVersion")
                    val ignoreVersion = call.argument<Boolean>("ignoreVersion")
                    if (targetPackage != null && targetVersion != null && ignoreVersion != null) {
                        result.success(
                                getFilteredPatches(targetPackage, targetVersion, ignoreVersion)
                        )
                    } else {
                        result.notImplemented()
                    }
                }
                "copyInputFile" -> {
                    val originalFilePath = call.argument<String>("originalFilePath")
                    val inputFilePath = call.argument<String>("inputFilePath")
                    if (originalFilePath != null && inputFilePath != null) {
                        result.success(copyInputFile(originalFilePath, inputFilePath))
                    } else {
                        result.notImplemented()
                    }
                }
                "createPatcher" -> {
                    val inputFilePath = call.argument<String>("inputFilePath")
                    val cacheDirPath = call.argument<String>("cacheDirPath")
                    val resourcePatching = call.argument<Boolean>("resourcePatching")
                    if (inputFilePath != null && cacheDirPath != null && resourcePatching != null) {
                        result.success(createPatcher(inputFilePath, cacheDirPath, resourcePatching))
                    } else {
                        result.notImplemented()
                    }
                }
                "mergeIntegrations" -> {
                    val integrationsPath = call.argument<String>("integrationsPath")
                    if (integrationsPath != null) {
                        result.success(mergeIntegrations(integrationsPath))
                    } else {
                        result.notImplemented()
                    }
                }
                "applyPatches" -> {
                    val selectedPatches = call.argument<List<String>>("selectedPatches")
                    if (selectedPatches != null) {
                        result.success(applyPatches(selectedPatches))
                    } else {
                        result.notImplemented()
                    }
                }
                "repackPatchedFile" -> {
                    val inputFilePath = call.argument<String>("inputFilePath")
                    val patchedFilePath = call.argument<String>("patchedFilePath")
                    if (inputFilePath != null && patchedFilePath != null) {
                        result.success(repackPatchedFile(inputFilePath, patchedFilePath))
                    } else {
                        result.notImplemented()
                    }
                }
                "signPatchedFile" -> {
                    val patchedFilePath = call.argument<String>("patchedFilePath")
                    val outFilePath = call.argument<String>("outFilePath")
                    if (patchedFilePath != null && outFilePath != null) {
                        result.success(signPatchedFile(patchedFilePath, outFilePath))
                    } else {
                        result.notImplemented()
                    }
                }
                else -> result.notImplemented()
            }
        }
    }

    fun loadPatches(pathBundlesPaths: List<String>): Boolean {
        try {
            pathBundlesPaths.forEach { path ->
                patches.addAll(
                        DexPatchBundle(
                                        path,
                                        DexClassLoader(
                                                path,
                                                applicationContext.cacheDir.path,
                                                null,
                                                javaClass.classLoader
                                        )
                                )
                                .loadPatches()
                )
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    fun getCompatiblePackages(): List<String> {
        val filteredPackages = mutableListOf<String>()
        patches.forEach patch@{ patch ->
            patch.compatiblePackages?.forEach { pkg -> filteredPackages.add(pkg.name) }
        }
        return filteredPackages.distinct()
    }

    fun getFilteredPatches(
            targetPackage: String,
            targetVersion: String,
            ignoreVersion: Boolean
    ): List<Map<String, String?>> {
        val filteredPatches = mutableListOf<Map<String, String?>>()
        patches.forEach patch@{ patch ->
            patch.compatiblePackages?.forEach { pkg ->
                if (pkg.name == targetPackage &&
                                (ignoreVersion ||
                                        pkg.versions.isNotEmpty() ||
                                        pkg.versions.contains(targetVersion))
                ) {
                    var p = mutableMapOf<String, String?>()
                    p.put("name", patch.patchName)
                    p.put("version", patch.version)
                    p.put("description", patch.description)
                    filteredPatches.add(p)
                }
            }
        }
        return filteredPatches
    }

    private fun findPatchesByIds(ids: Iterable<String>): List<Class<out Patch<Data>>> {
        return patches.filter { patch -> ids.any { it == patch.patchName } }
    }

    fun copyInputFile(originalFilePath: String, inputFilePath: String): Boolean {
        val originalFile = File(originalFilePath)
        val inputFile = File(inputFilePath)
        Files.copy(originalFile.toPath(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return true
    }

    fun createPatcher(
            inputFilePath: String,
            cacheDirPath: String,
            resourcePatching: Boolean
    ): Boolean {
        val inputFile = File(inputFilePath)
        val aaptPath = Aapt.binary(applicationContext).absolutePath
        patcher =
                Patcher(
                        PatcherOptions(
                                inputFile,
                                cacheDirPath,
                                resourcePatching,
                                aaptPath,
                                cacheDirPath,
                                logger =
                                        object : app.revanced.patcher.logging.Logger {
                                            override fun error(msg: String) {
                                                methodChannel.invokeMethod("updateInstallerLog", msg)
                                            }

                                            override fun warn(msg: String) {
                                                methodChannel.invokeMethod("updateInstallerLog", msg)
                                            }

                                            override fun info(msg: String) {
                                                methodChannel.invokeMethod("updateInstallerLog", msg)
                                            }

                                            override fun trace(msg: String) {
                                                methodChannel.invokeMethod("updateInstallerLog", msg)
                                            }
                                        }
                        )
                )
        return true
    }

    fun mergeIntegrations(integrationsPath: String): Boolean {
        val integrations = File(integrationsPath)
        if (patcher == null) return false
        patcher.addFiles(listOf(integrations)) {}
        return true
    }

    fun applyPatches(selectedPatches: List<String>): Boolean {
        val patches = findPatchesByIds(selectedPatches)
        if (patches.isEmpty()) return false
        if (patcher == null) return false
        patcher.addPatches(patches)
        patcher.applyPatches().forEach { (patch, result) ->
            if (result.isSuccess) {
                val msg = "[success] $patch"
                methodChannel.invokeMethod("updateInstallerLog", msg)
                return@forEach
            }
            val msg = "[error] $patch:" + result.exceptionOrNull()!!
            methodChannel.invokeMethod("updateInstallerLog", msg)
        }
        return true
    }

    fun repackPatchedFile(inputFilePath: String, patchedFilePath: String): Boolean {
        val inputFile = File(inputFilePath)
        val patchedFile = File(patchedFilePath)
        if (patcher == null) return false
        val result = patcher.save()
        ZipFile(patchedFile).use { file ->
            result.dexFiles.forEach {
                file.addEntryCompressData(
                        ZipEntry.createWithName(it.name),
                        it.dexFileInputStream.readBytes()
                )
            }
            result.resourceFile?.let {
                file.copyEntriesFromFileAligned(ZipFile(it), ZipAligner::getEntryAlignment)
            }
            file.copyEntriesFromFileAligned(ZipFile(inputFile), ZipAligner::getEntryAlignment)
        }
        return true
    }

    fun signPatchedFile(patchedFilePath: String, outFilePath: String): Boolean {
        val patchedFile = File(patchedFilePath)
        val outFile = File(outFilePath)
        Signer("ReVanced", "s3cur3p@ssw0rd").signApk(patchedFile, outFile)
        return true
    }
}
