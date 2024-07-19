package app.revanced.manager.domain.repository

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Log
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.plugins.TrustedDownloaderPlugin
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.downloader.DownloaderPluginState
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.network.downloader.ParceledDownloaderApp
import app.revanced.manager.plugin.downloader.App
import app.revanced.manager.plugin.downloader.DownloadScope
import app.revanced.manager.plugin.downloader.Downloader
import app.revanced.manager.plugin.downloader.DownloaderContext
import app.revanced.manager.util.PM
import app.revanced.manager.util.tag
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Modifier

class DownloaderPluginRepository(
    private val pm: PM,
    private val fs: Filesystem,
    private val prefs: PreferencesManager,
    private val context: Context,
    db: AppDatabase
) {
    private val trustDao = db.trustedDownloaderPluginDao()
    private val _pluginStates = MutableStateFlow(emptyMap<String, DownloaderPluginState>())
    val pluginStates = _pluginStates.asStateFlow()
    val loadedPluginsFlow = pluginStates.map { states ->
        states.values.filterIsInstance<DownloaderPluginState.Loaded>().map { it.plugin }
    }

    private val acknowledgedDownloaderPlugins = prefs.acknowledgedDownloaderPlugins
    private val installedPluginPackageNames = MutableStateFlow(emptySet<String>())
    val newPluginPackageNames = combine(
        installedPluginPackageNames,
        acknowledgedDownloaderPlugins.flow
    ) { installed, acknowledged ->
        installed subtract acknowledged
    }

    suspend fun reload() {
        val pluginPackages =
            withContext(Dispatchers.IO) {
                pm.getPackagesWithFeature(
                    PLUGIN_FEATURE,
                    flags = packageFlags
                )
            }

        _pluginStates.value = pluginPackages.associate { it.packageName to loadPlugin(it) }
        installedPluginPackageNames.value = pluginPackages.map { it.packageName }.toSet()

        val acknowledgedPlugins = acknowledgedDownloaderPlugins.get()
        val uninstalledPlugins = acknowledgedPlugins subtract installedPluginPackageNames.value
        if (uninstalledPlugins.isNotEmpty()) {
            Log.d(tag, "Uninstalled plugins: ${uninstalledPlugins.joinToString(", ")}")
            acknowledgedDownloaderPlugins.update(acknowledgedPlugins subtract uninstalledPlugins)
            trustDao.removeAll(uninstalledPlugins)
        }
    }

    fun unwrapParceledApp(app: ParceledDownloaderApp): Pair<LoadedDownloaderPlugin, App> {
        val plugin =
            (_pluginStates.value[app.pluginPackageName] as? DownloaderPluginState.Loaded)?.plugin
                ?: throw Exception("Downloader plugin with name ${app.pluginPackageName} is not available")

        return plugin to app.unwrapWith(plugin)
    }

    private suspend fun loadPlugin(packageInfo: PackageInfo): DownloaderPluginState {
        try {
            if (!verify(packageInfo)) return DownloaderPluginState.Untrusted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Got exception while verifying plugin ${packageInfo.packageName}", e)
            return DownloaderPluginState.Failed(e)
        }

        val downloaderContext = DownloaderContext(
            androidContext = context,
            tempDirectory = fs.tempDir.resolve("dl_plugin_${packageInfo.packageName}")
                .also(File::mkdir)
        )

        return try {
            val className =
                packageInfo.applicationInfo.metaData.getString(METADATA_PLUGIN_CLASS)
                    ?: throw Exception("Missing metadata attribute $METADATA_PLUGIN_CLASS")
            val classLoader = PathClassLoader(
                packageInfo.applicationInfo.sourceDir,
                Downloader::class.java.classLoader
            )

            val downloader = classLoader
                .loadClass(className)
                .getDownloaderImplementation(downloaderContext)

            @Suppress("UNCHECKED_CAST")
            DownloaderPluginState.Loaded(
                LoadedDownloaderPlugin(
                    packageInfo.packageName,
                    with(pm) { packageInfo.label() },
                    packageInfo.versionName,
                    downloader.get,
                    downloader.download as suspend DownloadScope.(App) -> Unit,
                    classLoader
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(tag, "Failed to load plugin ${packageInfo.packageName}", t)
            DownloaderPluginState.Failed(t)
        }
    }

    suspend fun trustPackage(packageInfo: PackageInfo) {
        trustDao.upsertTrust(
            TrustedDownloaderPlugin(
                packageInfo.packageName,
                pm.getSignatures(packageInfo).first().toCharsString()
            )
        )

        reload()
        prefs.edit {
            acknowledgedDownloaderPlugins += packageInfo.packageName
        }
    }

    suspend fun revokeTrustForPackage(packageName: String) =
        trustDao.remove(packageName).also { reload() }

    suspend fun acknowledgeAllNewPlugins() =
        acknowledgedDownloaderPlugins.update(installedPluginPackageNames.value)

    private suspend fun verify(packageInfo: PackageInfo): Boolean {
        val expectedSignature =
            trustDao.getTrustedSignature(packageInfo.packageName)?.let(::Signature) ?: return false

        return expectedSignature in pm.getSignatures(packageInfo)
    }

    private companion object {
        const val PLUGIN_FEATURE = "app.revanced.manager.plugin.downloader"
        const val METADATA_PLUGIN_CLASS = "app.revanced.manager.plugin.downloader.class"

        val packageFlags = PackageManager.GET_META_DATA or PM.signaturesFlag

        val Class<*>.isDownloader get() = Downloader::class.java.isAssignableFrom(this)
        const val PUBLIC_STATIC = Modifier.PUBLIC or Modifier.STATIC
        val Int.isPublicStatic get() = (this and PUBLIC_STATIC) == PUBLIC_STATIC

        fun Class<*>.getDownloaderImplementation(context: DownloaderContext) =
            declaredMethods
                .filter { it.modifiers.isPublicStatic && it.returnType.isDownloader }
                .firstNotNullOfOrNull callMethod@{
                    if (it.parameterTypes contentEquals arrayOf(DownloaderContext::class.java)) return@callMethod it(
                        null,
                        context
                    ) as Downloader<*>
                    if (it.parameterTypes.isEmpty()) return@callMethod it(null) as Downloader<*>

                    return@callMethod null
                }
                ?: throw Exception("Could not find a valid downloader implementation in class $canonicalName")
    }
}