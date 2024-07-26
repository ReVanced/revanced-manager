package app.revanced.manager.plugin.downloader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Objects

@Parcelize
open class App(open val packageName: String, open val version: String) : Parcelable {
    override fun hashCode() = Objects.hash(packageName, version)
    override fun equals(other: Any?): Boolean {
        if (other !is App) return false

        return other.packageName == packageName && other.version == version
    }
}