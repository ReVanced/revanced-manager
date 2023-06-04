package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.lifecycle.ViewModel
import app.revanced.manager.util.PM
import java.io.File
import java.nio.file.Files

class AppSelectorViewModel(
    private val app: Application,
    private val pm: PM
) : ViewModel() {
    private val packageManager = app.packageManager

    val appList = pm.appList

    fun loadLabel(app: PackageInfo?) = (app?.applicationInfo?.loadLabel(packageManager) ?: "Not installed").toString()

    fun loadSelectedFile(uri: Uri) =
        app.contentResolver.openInputStream(uri)!!.use { stream ->
            File(app.cacheDir, "input.apk").also {
                if (it.exists()) it.delete()
                Files.copy(stream, it.toPath())
            }.let { pm.getApkInfo(it) }
        }
}