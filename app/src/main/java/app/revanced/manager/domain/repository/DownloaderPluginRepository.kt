package app.revanced.manager.domain.repository

import android.app.Application
import android.content.pm.PackageManager
import android.os.Parcelable
import android.util.Log
import app.revanced.manager.data.room.AppDatabase
import app.revanced.manager.data.room.plugins.TrustedDownloaderPlugin
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.network.downloader.DownloaderPluginState
import app.revanced.manager.network.downloader.LoadedDownloaderPlugin
import app.revanced.manager.network.downloader.ParceledDownloaderData
import app.revanced.manager.plugin.downloader.DownloaderBuilder
import app.revanced.manager.plugin.downloader.PluginHostApi
import app.revanced.manager.plugin.downloader.Scope
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
import java.lang.reflect.Modifier

@OptIn(PluginHostApi::class)
class DownloaderPluginRepository(
    private val pm: PM,
    private val prefs: PreferencesManager,
    private val app: Application,
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
        val plugins =
            withContext(Dispatchers.IO) {
                pm.getPackagesWithFeature(PLUGIN_FEATURE)
                    .associate { it.packageName to loadPlugin(it.packageName) }
            }

        _pluginStates.value = plugins
        installedPluginPackageNames.value = plugins.keys

        val acknowledgedPlugins = acknowledgedDownloaderPlugins.get()
        val uninstalledPlugins = acknowledgedPlugins subtract installedPluginPackageNames.value
        if (uninstalledPlugins.isNotEmpty()) {
            Log.d(tag, "Uninstalled plugins: ${uninstalledPlugins.joinToString(", ")}")
            acknowledgedDownloaderPlugins.update(acknowledgedPlugins subtract uninstalledPlugins)
            trustDao.removeAll(uninstalledPlugins)
        }
    }

    fun unwrapParceledData(data: ParceledDownloaderData): Pair<LoadedDownloaderPlugin, Parcelable> {
        val plugin =
            (_pluginStates.value[data.pluginPackageName] as? DownloaderPluginState.Loaded)?.plugin
                ?: throw Exception("Downloader plugin with name ${data.pluginPackageName} is not available")

        return plugin to data.unwrapWith(plugin)
    }

    private suspend fun loadPlugin(packageName: String): DownloaderPluginState {
        try {
            if (!verify(packageName)) return DownloaderPluginState.Untrusted
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Got exception while verifying plugin $packageName", e)
            return DownloaderPluginState.Failed(e)
        }

        return try {
            val packageInfo = pm.getPackageInfo(packageName, flags = PackageManager.GET_META_DATA)!!
            val className = packageInfo.applicationInfo!!.metaData.getString(METADATA_PLUGIN_CLASS)
                ?: throw Exception("Missing metadata attribute $METADATA_PLUGIN_CLASS")

            val classLoader =
                PathClassLoader(packageInfo.applicationInfo!!.sourceDir, app.classLoader)
            val pluginContext = app.createPackageContext(packageName, 0)

            val downloader = classLoader
                .loadClass(className)
                .getDownloaderBuilder()
                .build(
                    scopeImpl = object : Scope {
                        override val hostPackageName = app.packageName
                        override val pluginPackageName = pluginContext.packageName
                    },
                    context = pluginContext
                )

            DownloaderPluginState.Loaded(
                LoadedDownloaderPlugin(
                    packageName,
                    with(pm) { packageInfo.label() },
                    packageInfo.versionName!!,
                    downloader.get,
                    downloader.download,
                    classLoader
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(tag, "Failed to load plugin $packageName", t)
            DownloaderPluginState.Failed(t)
        }
    }

    suspend fun trustPackage(packageName: String) {
        trustDao.upsertTrust(
            TrustedDownloaderPlugin(
                packageName,
                pm.getSignature(packageName).toByteArray()
            )
        )

        reload()
        prefs.edit {
            acknowledgedDownloaderPlugins += packageName
        }
    }

    suspend fun revokeTrustForPackage(packageName: String) =
        trustDao.remove(packageName).also { reload() }

    suspend fun acknowledgeAllNewPlugins() =
        acknowledgedDownloaderPlugins.update(installedPluginPackageNames.value)

    private suspend fun verify(packageName: String): Boolean {
        val expectedSignature =
            trustDao.getTrustedSignature(packageName) ?: return false

        return pm.hasSignature(packageName, expectedSignature)
    }

    private companion object {
        const val PLUGIN_FEATURE = "app.revanced.manager.plugin.downloader"
        const val METADATA_PLUGIN_CLASS = "app.revanced.manager.plugin.downloader.class"

        const val PUBLIC_STATIC = Modifier.PUBLIC or Modifier.STATIC
        val Int.isPublicStatic get() = (this and PUBLIC_STATIC) == PUBLIC_STATIC
        val Class<*>.isDownloaderBuilder get() = DownloaderBuilder::class.java.isAssignableFrom(this)

        @Suppress("UNCHECKED_CAST")
        fun Class<*>.getDownloaderBuilder() =
            declaredMethods
                .firstOrNull { it.modifiers.isPublicStatic && it.returnType.isDownloaderBuilder && it.parameterTypes.isEmpty() }
                ?.let { it(null) as DownloaderBuilder<Parcelable> }
                ?: throw Exception("Could not find a valid downloader implementation in class $canonicalName")
    }
}