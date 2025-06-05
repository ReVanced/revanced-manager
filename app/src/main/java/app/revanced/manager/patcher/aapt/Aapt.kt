package app.revanced.manager.patcher.aapt

import android.content.Context
import app.revanced.manager.patcher.LibraryResolver
import android.os.Build.SUPPORTED_ABIS as DEVICE_ABIS
object Aapt : LibraryResolver() {
    private val WORKING_ABIS = setOf("arm64-v8a", "x86", "x86_64", "armeabi-v7a")

    fun supportsDevice() = (DEVICE_ABIS intersect WORKING_ABIS).isNotEmpty()

    fun binary(context: Context) = findLibrary(context, "aapt")
}
