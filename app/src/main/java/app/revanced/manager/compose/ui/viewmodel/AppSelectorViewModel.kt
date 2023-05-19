package app.revanced.manager.compose.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import app.revanced.manager.compose.util.PM
import java.io.File
import java.nio.file.Files

class AppSelectorViewModel(private val app: Application) : ViewModel() {
    fun loadSelectedFile(uri: Uri) =
        app.contentResolver.openInputStream(uri)!!.use { stream ->
            File(app.cacheDir, "input.apk").also {
                if (it.exists()) it.delete()
                Files.copy(stream, it.toPath())
            }.let { PM.getApkInfo(it, app) }
        }
}