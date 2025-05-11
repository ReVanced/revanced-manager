package app.revanced.manager.patcher.runtime.process

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import app.revanced.manager.BuildConfig
import app.revanced.manager.patcher.Session
import app.revanced.manager.patcher.logger.LogLevel
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.patch.PatchBundle
import app.revanced.manager.patcher.runtime.ProcessRuntime
import app.revanced.manager.ui.model.State
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

/**
 * The main class that runs inside the runner process launched by [ProcessRuntime].
 */
class PatcherProcess(private val context: Context) : IPatcherProcess.Stub() {
    private var eventBinder: IPatcherEvents? = null

    private val scope =
        CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            // Try to send the exception information to the main app.
            eventBinder?.let {
                try {
                    it.finished(throwable.stackTraceToString())
                    return@CoroutineExceptionHandler
                } catch (_: Exception) {
                }
            }

            throwable.printStackTrace()
            exitProcess(1)
        })

    override fun buildId() = BuildConfig.BUILD_ID
    override fun exit() = exitProcess(0)

    override fun start(parameters: Parameters, events: IPatcherEvents) {
        eventBinder = events

        scope.launch {
            val logger = object : Logger() {
                override fun log(level: LogLevel, message: String) =
                    events.log(level.name, message)
            }

            logger.info("Memory limit: ${Runtime.getRuntime().maxMemory() / (1024 * 1024)}MB")

            val patchList = parameters.configurations.flatMap { config ->
                val bundle = PatchBundle(File(config.bundlePath))

                val patches =
                    bundle.patches(parameters.packageName).filter { it.name in config.patches }
                        .associateBy { it.name }

                config.options.forEach { (patchName, opts) ->
                    val patchOptions = patches[patchName]?.options
                        ?: throw Exception("Patch with name $patchName does not exist.")

                    opts.forEach { (key, value) ->
                        patchOptions[key] = value
                    }
                }

                patches.values
            }

            events.progress(null, State.COMPLETED.name, null) // Loading patches

            Session(
                cacheDir = parameters.cacheDir,
                aaptPath = parameters.aaptPath,
                frameworkDir = parameters.frameworkDir,
                androidContext = context,
                logger = logger,
                input = File(parameters.inputFile),
                onPatchCompleted = { events.patchSucceeded() },
                onProgress = { name, state, message ->
                    events.progress(name, state?.name, message)
                }
            ).use {
                it.run(File(parameters.outputFile), patchList)
            }

            events.finished(null)
        }
    }

    companion object {
        private val longArrayClass = LongArray::class.java
        private val emptyLongArray = LongArray(0)

        @SuppressLint("PrivateApi")
        @JvmStatic
        fun main(args: Array<String>) {
            Looper.prepare()

            val managerPackageName = args[0]

            // Abuse hidden APIs to get a context.
            val systemContext = ActivityThread.systemMain().systemContext as Context
            val appContext = systemContext.createPackageContext(managerPackageName, 0)

            // Avoid annoying logs. See https://github.com/robolectric/robolectric/blob/ad0484c6b32c7d11176c711abeb3cb4a900f9258/robolectric/src/main/java/org/robolectric/android/internal/AndroidTestEnvironment.java#L376-L388
            Class.forName("android.app.AppCompatCallbacks").apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    getDeclaredMethod("install", longArrayClass, longArrayClass).also { it.isAccessible = true }(null, emptyLongArray, emptyLongArray)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    getDeclaredMethod("install", longArrayClass).also { it.isAccessible = true }(null, emptyLongArray)
                }
            }

            val ipcInterface = PatcherProcess(appContext)

            appContext.sendBroadcast(Intent().apply {
                action = ProcessRuntime.CONNECT_TO_APP_ACTION
                `package` = managerPackageName

                putExtra(ProcessRuntime.INTENT_BUNDLE_KEY, Bundle().apply {
                    putBinder(ProcessRuntime.BUNDLE_BINDER_KEY, ipcInterface.asBinder())
                })
            })

            Looper.loop()
            exitProcess(1) // Shouldn't happen
        }
    }
}