package app.revanced.manager.backend.utils

import android.content.Context
import androidx.core.content.ContextCompat
import brut.util.AaptProvider
import java.io.File

object Aapt2 {
    fun binary(context: Context): File {
        return ContextCompat.getDataDir(context)!! // it should never be null!
            .resolve("lib").resolve("aapt2.so")
    }
}

internal class AndroidAaptProvider(private val context: Context) : AaptProvider {
    override fun getAapt2(): File {
        return Aapt2.binary(context)
    }
}