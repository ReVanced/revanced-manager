package app.revanced.manager.plugin.downloader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Package(val name: String, val version: String) : Parcelable