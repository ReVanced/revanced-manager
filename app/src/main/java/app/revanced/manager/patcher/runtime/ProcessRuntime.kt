package app.revanced.manager.patcher.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import app.revanced.manager.BuildConfig
import app.revanced.manager.patcher.runtime.process.IPatcherEvents
import app.revanced.manager.patcher.runtime.process.IPatcherProcess
import app.revanced.manager.patcher.LibraryResolver
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.runtime.process.Parameters
import app.revanced.manager.patcher.runtime.process.PatchConfiguration
import app.revanced.manager.patcher.runtime.process.PatcherProcess
import app.revanced.manager.patcher.worker.ProgressEventHandler
import app.revanced.manager.ui.model.State
import app.revanced.manager.util.Options
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.tag
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.component.inject

/**
 * Runs the patcher in another process by using the app_process binary and IPC.
 */
class ProcessRuntime(private val context: Context) : Runtime(context) {
    private val pm: PM by inject()

    private suspend fun awaitBinderConnection(): IPatcherProcess {
        val binderFuture = CompletableDeferred<IPatcherProcess>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val binder =
                    intent.getBundleExtra(INTENT_BUNDLE_KEY)?.getBinder(BUNDLE_BINDER_KEY)!!

                binderFuture.complete(IPatcherProcess.Stub.asInterface(binder))
            }
        }

        ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
            addAction(CONNECT_TO_APP_ACTION)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)

        return try {
            withTimeout(10000L) {
                binderFuture.await()
            }
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    override suspend fun execute(
        inputFile: String,
        outputFile: String,
        packageName: String,
        selectedPatches: PatchSelection,
        options: Options,
        logger: Logger,
        onPatchCompleted: suspend () -> Unit,
        onProgress: ProgressEventHandler,
    ) = coroutineScope {
        // Get the location of our own Apk.
        val managerBaseApk = pm.getPackageInfo(context.packageName)!!.applicationInfo!!.sourceDir

        val limit = "${prefs.patcherProcessMemoryLimit.get()}M"
        val propOverride = resolvePropOverride(context)?.absolutePath
            ?: throw Exception("Couldn't find prop override library")

        val env =
            System.getenv().toMutableMap().apply {
                putAll(
                    mapOf(
                        "CLASSPATH" to managerBaseApk,
                        // Override the props used by ART to set the memory limit.
                        "LD_PRELOAD" to propOverride,
                        "PROP_dalvik.vm.heapgrowthlimit" to limit,
                        "PROP_dalvik.vm.heapsize" to limit,
                    )
                )
            }

        launch(Dispatchers.IO) {
            val result = process(
                APP_PROCESS_BIN_PATH,
                "-Djava.io.tmpdir=$cacheDir", // The process will use /tmp if this isn't set, which is a problem because that folder is not accessible on Android.
                "/", // The unused cmd-dir parameter
                "--nice-name=${context.packageName}:Patcher",
                PatcherProcess::class.java.name, // The class with the main function.
                context.packageName,
                env = env,
                stdout = Redirect.CAPTURE,
                stderr = Redirect.CAPTURE,
            ) { line ->
                // The process shouldn't generally be writing to stdio. Log any lines we get as warnings.
                logger.warn("[STDIO]: $line")
            }

            Log.d(tag, "Process finished with exit code ${result.resultCode}")

            if (result.resultCode != 0) throw Exception("Process exited with nonzero exit code ${result.resultCode}")
        }

        val patching = CompletableDeferred<Unit>()

        launch(Dispatchers.IO) {
            val binder = awaitBinderConnection()

            // Android Studio's fast deployment feature causes an issue where the other process will be running older code compared to the main process.
            // The patcher process is running outdated code if the randomly generated BUILD_ID numbers don't match.
            // To fix it, clear the cache in the Android settings or disable fast deployment (Run configurations -> Edit Configurations -> app -> Enable "always deploy with package manager").
            if (binder.buildId() != BuildConfig.BUILD_ID) throw Exception("app_process is running outdated code. Clear the app cache or disable disable Android 11 deployment optimizations in your IDE")

            val eventHandler = object : IPatcherEvents.Stub() {
                override fun log(level: String, msg: String) = logger.log(enumValueOf(level), msg)

                override fun patchSucceeded() {
                    launch { onPatchCompleted() }
                }

                override fun progress(name: String?, state: String?, msg: String?) =
                    onProgress(name, state?.let { enumValueOf<State>(it) }, msg)

                override fun finished(exceptionStackTrace: String?) {
                    binder.exit()

                    exceptionStackTrace?.let {
                        patching.completeExceptionally(RemoteFailureException(it))
                        return
                    }
                    patching.complete(Unit)
                }
            }

            val bundles = bundles()

            val parameters = Parameters(
                aaptPath = aaptPath,
                frameworkDir = frameworkPath,
                cacheDir = cacheDir,
                packageName = packageName,
                inputFile = inputFile,
                outputFile = outputFile,
                configurations = selectedPatches.map { (id, patches) ->
                    val bundle = bundles[id]!!

                    PatchConfiguration(
                        bundle.patchesJar.absolutePath,
                        patches,
                        options[id].orEmpty()
                    )
                }
            )

            binder.start(parameters, eventHandler)
        }

        // Wait until patching finishes.
        patching.await()
    }

    companion object : LibraryResolver() {
        private const val APP_PROCESS_BIN_PATH = "/system/bin/app_process"

        const val CONNECT_TO_APP_ACTION = "CONNECT_TO_APP_ACTION"
        const val INTENT_BUNDLE_KEY = "BUNDLE"
        const val BUNDLE_BINDER_KEY = "BINDER"

        private fun resolvePropOverride(context: Context) = findLibrary(context, "prop_override")
    }

    /**
     * An [Exception] occured in the remote process while patching.
     *
     * @param originalStackTrace The stack trace of the original [Exception].
     */
    class RemoteFailureException(val originalStackTrace: String) : Exception()
}

