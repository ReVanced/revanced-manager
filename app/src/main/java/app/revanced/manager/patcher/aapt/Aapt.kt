package app.revanced.manager.patcher.aapt

import android.content.Context
import android.os.Build.SUPPORTED_ABIS as DEVICE_ABIS
import java.io.File

object Aapt {
    private val WORKING_ABIS = setOf("arm64-v8a", "x86", "x86_64")

    fun supportsDevice() = (DEVICE_ABIS intersect WORKING_ABIS).isNotEmpty()

    fun binary(context: Context): File? {
        return File(context.applicationInfo.nativeLibraryDir).resolveAapt()
    }
}

private fun File.resolveAapt() =
    list { _, f -> !File(f).isDirectory && f.contains("aapt") }?.firstOrNull()?.let { resolve(it) }
