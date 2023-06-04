package app.revanced.manager.patcher.aapt

import android.content.Context
import java.io.File

object Aapt {
    fun binary(context: Context): File? {
        return File(context.applicationInfo.nativeLibraryDir).resolveAapt()
    }
}

private fun File.resolveAapt() =
    list { _, f -> !File(f).isDirectory && f.contains("aapt") }?.firstOrNull()?.let { resolve(it) }
