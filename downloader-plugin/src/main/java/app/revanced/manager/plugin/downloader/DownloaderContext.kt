package app.revanced.manager.plugin.downloader

import android.content.Context
import java.io.File

@Suppress("Unused", "MemberVisibilityCanBePrivate")
/**
 * The downloader plugin context.
 *
 * @param androidContext An Android [Context].
 * @param tempDirectory The temporary directory belonging to this plugin.
 */
class DownloaderContext(val androidContext: Context, val tempDirectory: File)