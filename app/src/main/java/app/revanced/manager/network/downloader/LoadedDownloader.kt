package app.revanced.manager.network.downloader

import android.os.Parcelable
import app.revanced.manager.downloader.Downloader
import app.revanced.manager.downloader.Scope

class LoadedDownloader(
    val packageName: String,
    val className: String,
    val name: String,
    val version: String,
    val scopeImpl: Scope,
    val impl: Downloader<Parcelable>
)