package app.revanced.manager.domain.repository

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Log
import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.plugins.TrustedDownloaderPlugin
import app.revanced.manager.network.downloader.DownloaderPluginState
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.network.downloader.ParceledDownloaderApp
import app.revanced.manager.plugin.downloader.DownloaderPlugin
import app.revanced.manager.util.PM
import app.revanced.manager.util.tag
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class DownloaderPluginRepository(
    private val pm: PM,
    private val fs: Filesystem,
    private val context: Context,
    db: AppDatabase
) {
    private val trustDao = db.trustedDownloaderPluginDao()
    private val _pluginStates = MutableStateFlow(emptyMap<String, DownloaderPluginState>())
    val pluginStates = _pluginStates.asStateFlow()
    val loadedPluginsFlow = pluginStates.map { states ->
        states.values.filterIsInstance<DownloaderPluginState.Loaded>().map { it.plugin }
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
    }

    fun unwrapParceledApp(app: ParceledDownloaderApp): Pair<LoadedDownloaderPlugin, DownloaderPlugin.App> {
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

        val pluginParameters = DownloaderPlugin.Parameters(
            context = context,
            tempDirectory = fs.tempDir.resolve("dl_plugin_${packageInfo.packageName}")
                .also(File::mkdir)
        )

        return try {
            val pluginClassName =
                packageInfo.applicationInfo.metaData.getString(METADATA_PLUGIN_CLASS)
                    ?: throw Exception("Missing metadata attribute $METADATA_PLUGIN_CLASS")
            val classLoader = PathClassLoader(
                packageInfo.applicationInfo.sourceDir,
                DownloaderPlugin::class.java.classLoader
            )

            @Suppress("UNCHECKED_CAST")
            val downloaderPluginClass =
                classLoader.loadClass(pluginClassName) as Class<DownloaderPlugin<DownloaderPlugin.App>>

            val plugin = downloaderPluginClass
                .getDeclaredConstructor(DownloaderPlugin.Parameters::class.java)
                .newInstance(pluginParameters)

            DownloaderPluginState.Loaded(
                LoadedDownloaderPlugin(
                    packageInfo.packageName,
                    with(pm) { packageInfo.label() },
                    packageInfo.versionName,
                    plugin,
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
    }

    suspend fun revokeTrustForPackage(packageName: String) =
        trustDao.remove(packageName).also { reload() }

    private suspend fun verify(packageInfo: PackageInfo): Boolean {
        val expectedSignature =
            trustDao.getTrustedSignature(packageInfo.packageName)?.let(::Signature) ?: return false

        return expectedSignature in pm.getSignatures(packageInfo)
    }

    private companion object {
        const val PLUGIN_FEATURE = "app.revanced.manager.plugin.downloader"
        const val METADATA_PLUGIN_CLASS = "app.revanced.manager.plugin.downloader.class"

        val packageFlags = PackageManager.GET_META_DATA or PM.signaturesFlag
    }
}