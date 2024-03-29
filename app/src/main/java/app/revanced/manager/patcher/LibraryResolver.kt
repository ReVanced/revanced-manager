package app.revanced.manager.patcher

import android.content.Context
import java.io.File

abstract class LibraryResolver {
    protected fun findLibrary(context: Context, searchTerm: String): File? = File(context.applicationInfo.nativeLibraryDir).run {
        list { _, f -> !File(f).isDirectory && f.contains(searchTerm) }?.firstOrNull()?.let { resolve(it) }
    }
}