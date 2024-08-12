package app.revanced.manager.plugin.downloader

import android.content.Context

@Suppress("Unused", "MemberVisibilityCanBePrivate")
/**
 * The downloader plugin context.
 *
 * @param androidContext An Android [Context] for this plugin.
 * @param pluginHostPackageName The package name of the plugin host.
 */
class DownloaderContext(val androidContext: Context, val pluginHostPackageName: String)