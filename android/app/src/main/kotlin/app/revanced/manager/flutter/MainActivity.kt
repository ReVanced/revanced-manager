package app.revanced.manager.flutter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import app.revanced.patcher.logging.Logger
import app.revanced.patcher.util.patch.PatchBundle
import dalvik.system.DexClassLoader
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File
import java.io.OutputStream

private const val PATCHER_CHANNEL = "app.revanced.manager.flutter/patcher"
private const val INSTALLER_CHANNEL = "app.revanced.manager.flutter/installer"
private const val EXPORTER_CHANNEL = "app.revanced.manager.flutter/exporter"

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

class MainActivity : FlutterActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var installerChannel: MethodChannel
    private lateinit var exporterChannel: MethodChannel


    // Export APK 
    internal val export_request_code = 1
    internal var export_result: Result? = null
    internal var export_source_path: String? = null

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val mainChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, PATCHER_CHANNEL)
        installerChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, INSTALLER_CHANNEL)
        exporterChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, EXPORTER_CHANNEL)
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
                    val mergeIntegrations = call.argument<Boolean>("mergeIntegrations")
                    val keyStoreFilePath = call.argument<String>("keyStoreFilePath")
                    if (patchBundleFilePath != null &&
                        originalFilePath != null &&
                        inputFilePath != null &&
                        patchedFilePath != null &&
                        outFilePath != null &&
                        integrationsPath != null &&
                        selectedPatches != null &&
                        cacheDirPath != null &&
                        mergeIntegrations != null &&
                        keyStoreFilePath != null
                    ) {
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
                            mergeIntegrations,
                            keyStoreFilePath
                        )
                    } else {
                        result.notImplemented()
                    }
                }
                else -> result.notImplemented()
            }
        }

        exporterChannel.setMethodCallHandler { call, result ->
            // Referenced from https://gist.github.com/MSVCode/9ccedfa6692f8bc3b82fdc74fad65bc6
            if (call.method == "exportApk") {
                export_result = result;

                export_source_path = call.argument<String>("source_path")!!;
                var name = call.argument<String>("name")!!;
                startFileExport(APK_MIME_TYPE, name);
            } else {
                result.notImplemented();
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check which request we're responding to
        if (requestCode == export_request_code) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getData() != null) {
                    exportToFile(data.getData() as Uri) // data.getData() is Uri
                } else {
                    export_result?.error("NO DATA", "Did not get valid data (Uri) for export", null)
                }
            } else {
                export_result?.error("CANCELED", "User cancelled", null)
            }
        }
    }

    private fun startFileExport(mimeType: String, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            // Create a file with the requested MIME type.
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        startActivityForResult(intent, export_request_code)
    }


    private fun exportToFile(uri: Uri) {
        val outputStream: OutputStream?
        try {
            outputStream = getContentResolver().openOutputStream(uri)
            if (outputStream != null) {
                File(export_source_path).inputStream().copyTo(outputStream)
                export_result?.success("SUCCESS")
            } else {
                export_result?.error("ERROR", "Unable to open output Uri", null)
            }
        } catch (e: Exception) {

            // log to console
            print("Error exporting apk: ${e.message}")
            e.printStackTrace()

            export_result?.error("ERROR", "Unable to write", null)
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
        mergeIntegrations: Boolean,
        keyStoreFilePath: String
    ) {
        val originalFile = File(originalFilePath)
        val inputFile = File(inputFilePath)
        val patchedFile = File(patchedFilePath)
        val outFile = File(outFilePath)
        val integrations = File(integrationsPath)
        val keyStoreFile = File(keyStoreFilePath)

        Thread {
            try {   
                val patches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
                    PatchBundle.Dex(
                        patchBundleFilePath,
                        DexClassLoader(
                            patchBundleFilePath,
                            cacheDirPath,
                            null,
                            javaClass.classLoader
                        )
                    ).loadPatches().filter { patch -> selectedPatches.any { it == patch.patchName } }
                } else {
                    TODO("VERSION.SDK_INT < CUPCAKE")
                }

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
                originalFile.copyTo(inputFile, true)

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
                            Aapt.binary(applicationContext).absolutePath,
                            cacheDirPath,
                            logger = ManagerLogger()
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
                patcher.executePatches().forEach { (patch, res) ->
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
                    val msg = "[error] $patch:" + res.exceptionOrNull()!!.printStackTrace()
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

                // Signer("ReVanced", "s3cur3p@ssw0rd").signApk(patchedFile, outFile, keyStoreFile)

                try {
                    Signer("ReVanced", "s3cur3p@ssw0rd").signApk(patchedFile, outFile, keyStoreFile)
                } catch (e: Exception) {
                    //log to console
                    print("Error signing apk: ${e.message}")
                    e.printStackTrace()
                }

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
            } catch (ex: Throwable) {
                val stack = ex.stackTraceToString()
                handler.post {
                    installerChannel.invokeMethod(
                        "update",
                        mapOf(
                            "progress" to -100.0,
                            "header" to "Aborting...",
                            "log" to "An error occurred! Aborting\nError:\n$stack"
                        )
                    )
                }
            }
            handler.post { result.success(null) }
        }.start()
    }

    inner class ManagerLogger : Logger {
        override fun error(msg: String) {
            handler.post {
                installerChannel
                    .invokeMethod(
                        "update",
                        mapOf("progress" to -1.0, "header" to "", "log" to msg)
                    )
            }
        }

        override fun warn(msg: String) {
            handler.post {
                installerChannel.invokeMethod(
                    "update",
                    mapOf("progress" to -1.0, "header" to "", "log" to msg)
                )
            }
        }

        override fun info(msg: String) {
            handler.post {
                installerChannel.invokeMethod(
                    "update",
                    mapOf("progress" to -1.0, "header" to "", "log" to msg)
                )
            }
        }

        override fun trace(msg: String) {
            handler.post {
                installerChannel.invokeMethod(
                    "update",
                    mapOf("progress" to -1.0, "header" to "", "log" to msg)
                )
            }
        }
    }
}
