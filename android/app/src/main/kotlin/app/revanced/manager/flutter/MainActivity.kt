package app.revanced.manager.flutter

import android.app.PendingIntent
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Handler
import android.os.Looper
import app.revanced.library.ApkUtils
import app.revanced.library.ApkUtils.applyTo
import app.revanced.library.installation.installer.LocalInstaller
import app.revanced.manager.flutter.utils.Aapt
import app.revanced.manager.flutter.utils.packageInstaller.InstallerReceiver
import app.revanced.manager.flutter.utils.packageInstaller.UninstallerReceiver
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.loadPatchesFromDex
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
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

    private lateinit var patches: Set<Patch<*>>

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val patcherChannel = "app.revanced.manager.flutter/patcher"
        val installerChannel = "app.revanced.manager.flutter/installer"
        val openBrowserChannel = "app.revanced.manager.flutter/browser"

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            openBrowserChannel
        ).setMethodCallHandler { call, result ->
            if (call.method == "openBrowser") {
                val searchQuery = call.argument<String>("query")
                openBrowser(searchQuery)
                result.success(null)
            } else {
                result.notImplemented()
            }
        }

        val mainChannel =
            MethodChannel(flutterEngine.dartExecutor.binaryMessenger, patcherChannel)

        this.installerChannel =
            MethodChannel(flutterEngine.dartExecutor.binaryMessenger, installerChannel)

        mainChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "runPatcher" -> {
                    val inFilePath = call.argument<String>("inFilePath")
                    val outFilePath = call.argument<String>("outFilePath")
                    val selectedPatches = call.argument<List<String>>("selectedPatches")
                    val options = call.argument<Map<String, Map<String, Any>>>("options")
                    val tmpDirPath = call.argument<String>("tmpDirPath")
                    val keyStoreFilePath = call.argument<String>("keyStoreFilePath")
                    val keystorePassword = call.argument<String>("keystorePassword")

                    if (
                        inFilePath != null &&
                        outFilePath != null &&
                        selectedPatches != null &&
                        options != null &&
                        tmpDirPath != null &&
                        keyStoreFilePath != null &&
                        keystorePassword != null
                    ) {
                        cancel = false
                        runPatcher(
                            result,
                            inFilePath,
                            outFilePath,
                            selectedPatches,
                            options,
                            tmpDirPath,
                            keyStoreFilePath,
                            keystorePassword
                        )
                    } else result.error(
                        "INVALID_ARGUMENTS",
                        "Invalid arguments",
                        "One or more arguments are missing"
                    )
                }

                "stopPatcher" -> {
                    cancel = true
                    stopResult = result
                }

                "getPatches" -> {
                    val patchBundleFilePath = call.argument<String>("patchBundleFilePath")!!

                    try {
                        val patchBundleFile = File(patchBundleFilePath)
                        patchBundleFile.setWritable(false)
                        patches = loadPatchesFromDex(
                            setOf(patchBundleFile),
                            optimizedDexDirectory = codeCacheDir
                        )
                    } catch (t: Throwable) {
                        return@setMethodCallHandler result.error(
                            "PATCH_BUNDLE_ERROR",
                            "Failed to load patch bundle",
                            t.stackTraceToString()
                        )
                    }

                    JSONArray().apply {
                        patches.forEach {
                            JSONObject().apply {
                                put("name", it.name)
                                put("description", it.description)
                                put("excluded", !it.use)
                                put("compatiblePackages", JSONArray().apply {
                                    it.compatiblePackages?.forEach { (name, versions) ->
                                        val compatiblePackageJson = JSONObject().apply {
                                            put("name", name)
                                            put(
                                                "versions",
                                                JSONArray().apply {
                                                    versions?.forEach { version ->
                                                        put(version)
                                                    }
                                                })
                                        }
                                        put(compatiblePackageJson)
                                    }
                                })
                                put("options", JSONArray().apply {
                                    it.options.values.forEach { option ->
                                        JSONObject().apply {
                                            put("key", option.key)
                                            put("title", option.title)
                                            put("description", option.description)
                                            put("required", option.required)

                                            fun JSONObject.putValue(
                                                value: Any?,
                                                key: String = "value"
                                            ) = if (value is Array<*>) put(
                                                key,
                                                JSONArray().apply {
                                                    value.forEach { put(it) }
                                                })
                                            else put(key, value)

                                            putValue(option.default)

                                            option.values?.let { values ->
                                                put("values",
                                                    JSONObject().apply {
                                                        values.forEach { (key, value) ->
                                                            putValue(value, key)
                                                        }
                                                    })
                                            } ?: put("values", null)
                                            put("type", option.type)
                                        }.let(::put)
                                    }
                                })
                            }.let(::put)
                        }
                    }.toString().let(result::success)
                }

                "installApk" -> {
                    val apkPath = call.argument<String>("apkPath")!!
                    PackageInstallerManager.result = result
                    installApk(apkPath)
                }

                "uninstallApp" -> {
                    val packageName = call.argument<String>("packageName")!!
                    uninstallApp(packageName)
                    PackageInstallerManager.result = result
                }

                else -> result.notImplemented()
            }
        }
    }

    private fun openBrowser(query: String?) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private fun runPatcher(
        result: MethodChannel.Result,
        inFilePath: String,
        outFilePath: String,
        selectedPatches: List<String>,
        options: Map<String, Map<String, Any>>,
        tmpDirPath: String,
        keyStoreFilePath: String,
        keystorePassword: String
    ) {
        val inFile = File(inFilePath)
        // Necessary because the file is copied from a nonwriteable location.
        inFile.setWritable(true)
        inFile.setReadable(true)
        val outFile = File(outFilePath)
        val keyStoreFile = File(keyStoreFilePath)
        val tmpDir = File(tmpDirPath)

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

            fun cancel(block: () -> Unit = {}): Boolean {
                if (cancel) {
                    block()
                    postStop()
                }

                return cancel
            }


            // Setup logger
            Logger.getLogger("").apply {
                handlers.forEach {
                    it.close()
                    removeHandler(it)
                }

                object : java.util.logging.Handler() {
                    override fun publish(record: LogRecord) {
                        if (record.loggerName?.startsWith("app.revanced") != true || cancel) return

                        updateProgress(-1.0, "", record.message)
                    }

                    override fun flush() = Unit
                    override fun close() = flush()
                }.let(::addHandler)
            }

            try {
                updateProgress(0.0, "Reading APK...", "Reading APK")

                val patcher = Patcher(
                    PatcherConfig(
                        inFile,
                        tmpDir,
                        Aapt.binary(applicationContext).absolutePath,
                        tmpDir.path,
                    )
                )

                if (cancel(patcher::close)) return@Thread
                updateProgress(0.02, "Loading patches...", "Loading patches")

                val patches = patches.filter { patch ->
                    val isCompatible = patch.compatiblePackages?.any { (name, _) ->
                        name == patcher.context.packageMetadata.packageName
                    } ?: false

                    val compatibleOrUniversal =
                        isCompatible || patch.compatiblePackages.isNullOrEmpty()

                    compatibleOrUniversal && selectedPatches.any { it == patch.name }
                }.onEach { patch ->
                    options[patch.name]?.forEach { (key, value) ->
                        patch.options[key] = value
                    }
                }.toSet()

                if (cancel(patcher::close)) return@Thread
                updateProgress(0.05, "Executing...", "")

                val patcherResult = patcher.use {
                    it += patches

                    runBlocking {
                        // Update the progress bar every time a patch is executed from 0.15 to 0.7
                        val totalPatchesCount = patches.size
                        val progressStep = 0.55 / totalPatchesCount
                        var progress = 0.05

                        patcher().collect(FlowCollector { patchResult: PatchResult ->
                            if (cancel(patcher::close)) return@FlowCollector

                            val msg = patchResult.exception?.let {
                                val writer = StringWriter()
                                it.printStackTrace(PrintWriter(writer))
                                "${patchResult.patch.name} failed: $writer"
                            } ?: run {
                                "${patchResult.patch.name} succeeded"
                            }

                            updateProgress(progress, "", msg)
                            progress += progressStep
                        })
                    }

                    if (cancel(patcher::close)) return@Thread
                    updateProgress(0.75, "Building...", "")

                    patcher.get()
                }

                if (cancel(patcher::close)) return@Thread

                patcherResult.applyTo(inFile)

                if (cancel(patcher::close)) return@Thread

                ApkUtils.signApk(
                    inFile,
                    outFile,
                    "ReVanced",
                    ApkUtils.KeyStoreDetails(
                        keyStoreFile,
                        keystorePassword,
                        "alias",
                        keystorePassword
                    )
                )

                updateProgress(.85, "Patched", "Patched APK")
            } catch (ex: Throwable) {
                if (!cancel) {
                    val stack = ex.stackTraceToString()
                    updateProgress(
                        -100.0,
                        "Failed",
                        "An error occurred:\n$stack"
                    )
                }
            } finally {
                inFile.delete()
                tmpDir.deleteRecursively()
            }

            handler.post { result.success(null) }
        }.start()
    }

    private fun installApk(apkPath: String) {
        val packageInstaller: PackageInstaller = applicationContext.packageManager.packageInstaller
        val sessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId: Int = packageInstaller.createSession(sessionParams)
        val session: PackageInstaller.Session = packageInstaller.openSession(sessionId)
        session.use { activeSession ->
            val sessionOutputStream = activeSession.openWrite(applicationContext.packageName, 0, -1)
            sessionOutputStream.use { outputStream ->
                val apkFile = File(apkPath)
                apkFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        val receiverIntent = Intent(applicationContext, InstallerReceiver::class.java).apply {
            action = "APP_INSTALL_ACTION"
        }
        val receiverPendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId,
            receiverIntent,
            PackageInstallerManager.flags
        )
        session.commit(receiverPendingIntent.intentSender)
        session.close()
    }

    private fun uninstallApp(packageName: String) {
        val packageInstaller: PackageInstaller = applicationContext.packageManager.packageInstaller
        val receiverIntent = Intent(applicationContext, UninstallerReceiver::class.java).apply {
            action = "APP_UNINSTALL_ACTION"
        }
        val receiverPendingIntent =
            PendingIntent.getBroadcast(context, 0, receiverIntent, PackageInstallerManager.flags)
        packageInstaller.uninstall(packageName, receiverPendingIntent.intentSender)
    }

    object PackageInstallerManager {
        var result: MethodChannel.Result? = null
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
}
