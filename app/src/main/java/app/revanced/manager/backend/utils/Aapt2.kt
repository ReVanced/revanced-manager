package app.revanced.manager.backend.utils

import android.content.Context
import brut.util.AaptProvider
import java.io.File

object Aapt2 {
    fun binary(context: Context): File {
        return File(context.applicationInfo.nativeLibraryDir).resolveAapt()
    }
}

internal class AndroidAaptProvider(private val context: Context) : AaptProvider {
    override fun getAapt2(): File {
        return Aapt2.binary(context)
    }
}

private fun File.resolveAapt() = resolve(list { _, f -> !File(f).isDirectory }!!.first())