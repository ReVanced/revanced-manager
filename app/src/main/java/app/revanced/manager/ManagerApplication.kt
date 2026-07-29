package app.revanced.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.di.*
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.network.api.EndpointState
import app.revanced.manager.network.api.ReVancedAPI
import app.revanced.manager.util.tag
import kotlinx.coroutines.Dispatchers
import coil.Coil
import coil.ImageLoader
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.internal.BuilderImpl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import me.zhanghai.android.appiconloader.coil.AppIconKeyer
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import ru.solrudev.ackpine.Ackpine

class ManagerApplication : Application() {
    private val scope = MainScope()
    private val prefs: PreferencesManager by inject()
    private val patchBundleRepository: PatchBundleRepository by inject()
    private val downloaderRepository: DownloaderRepository by inject()
    private val downloadedAppsRepository: DownloadedAppRepository by inject()
    private val fs: Filesystem by inject()
    private val endpointState: EndpointState by inject()
    private val reVancedAPI: ReVancedAPI by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ManagerApplication)
            androidLogger()
            workManagerFactory()
            modules(
                httpModule,
                preferencesModule,
                repositoryModule,
                serviceModule,
                managerModule,
                workerModule,
                viewModelModule,
                databaseModule,
                rootModule,
                ackpineModule
            )
        }

        val pixels = 512
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(AppIconKeyer())
                    add(AppIconFetcher.Factory(pixels, true, this@ManagerApplication))
                }
                .build()
        )

        val shellBuilder = BuilderImpl.create().setFlags(Shell.FLAG_MOUNT_MASTER)
        Shell.setDefaultBuilder(shellBuilder)

        Ackpine.enableLogcatLogger()

        scope.launch {
            prefs.preload()
        }
        scope.launch(Dispatchers.Default) {
            downloaderRepository.reload()
        }
        scope.launch(Dispatchers.Default) {
            runUpdateChecks()
        }
        scope.launch(Dispatchers.Default) {
            downloadedAppsRepository.cleanUp()
        }

        // Only while the session is on a backup endpoint, periodically probe the higher-priority
        // endpoints and switch back to the earliest reachable one silently. Endpoint URLs are
        // resolved per request, so the switch takes effect without restarting the app.
        //
        // collectLatest restarts this block whenever the active endpoint changes: after a partial
        // restore (e.g. C -> B while the primary is still down) the active URL is still non-null, so
        // probing resumes for the remaining higher-priority endpoints until the primary is reached,
        // at which point activeUrl is null and the loop stays idle.
        scope.launch(Dispatchers.IO) {
            endpointState.activeUrl
                .filterNotNull()
                .collectLatest {
                    while (true) {
                        delay(PRIMARY_RECONNECT_INTERVAL)
                        val restored = reVancedAPI.restoreHigherPriorityEndpoint()
                        if (restored != null) {
                            Log.i(tag, "Higher-priority API endpoint recovered, switched to ${restored.url}")
                            // The startup update check may have run against a backup; re-run it now
                            // that we are on a higher-priority endpoint so results reflect it.
                            runUpdateChecks()
                            break
                        }
                    }
                }
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var firstActivityCreated = false

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (firstActivityCreated) return
                firstActivityCreated = true

                // We do not want to call onFreshProcessStart() if there is state to restore.
                // This can happen on system-initiated process death.
                if (savedInstanceState == null) {
                    Log.d(tag, "Fresh process created")
                    onFreshProcessStart()
                } else Log.d(tag, "System-initiated process death detected")
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun onFreshProcessStart() {
        fs.uiTempDir.apply {
            deleteRecursively()
            mkdirs()
        }
    }

    private suspend fun runUpdateChecks() {
        arrayOf(patchBundleRepository, downloaderRepository).forEach {
            with(it) {
                reload()
                updateCheck(force = false)
            }
        }
    }

    private companion object {
        val PRIMARY_RECONNECT_INTERVAL = 5.minutes
    }
}