package app.revanced.manager.network.downloader

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
/**
 * A container for [Parcelable] data returned from downloader. Instances of this class can be safely stored in a bundle without needing to set the [ClassLoader].
 */
class ParceledDownloaderData private constructor(
    val pluginPackageName: String,
    private val bundle: Bundle
) : Parcelable {
    constructor(plugin: LoadedDownloaderPlugin, data: Parcelable) : this(
        plugin.packageName,
        createBundle(data)
    )

    fun unwrapWith(plugin: LoadedDownloaderPlugin): Parcelable {
        bundle.classLoader = plugin.classLoader

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val className = bundle.getString(CLASS_NAME_KEY)!!
            val clazz = plugin.classLoader.loadClass(className)

            bundle.getParcelable(DATA_KEY, clazz)!! as Parcelable
        } else @Suppress("Deprecation") bundle.getParcelable(DATA_KEY)!!
    }

    private companion object {
        const val CLASS_NAME_KEY = "class"
        const val DATA_KEY = "data"

        fun createBundle(data: Parcelable) = Bundle().apply {
            putParcelable(DATA_KEY, data)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) putString(
                CLASS_NAME_KEY,
                data::class.java.canonicalName
            )
        }
    }
}