package app.revanced.manager.flutter.utils

import android.content.Context
import java.io.File

object Aapt {
    fun binary(context: Context): File {
        return File(context.applicationInfo.nativeLibraryDir).resolveAapt()
    }
}

private fun File.resolveAapt() = resolve(list { _, f -> !File(f).isDirectory }!!.first())
