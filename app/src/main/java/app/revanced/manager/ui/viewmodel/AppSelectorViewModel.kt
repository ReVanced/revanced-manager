package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.lifecycle.ViewModel
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.PM
import java.io.File
import java.nio.file.Files

class AppSelectorViewModel(
    private val app: Application,
    private val pm: PM
) : ViewModel() {
    private val inputFile = File(app.cacheDir, "input.apk").also {
        it.delete()
    }
    val appList = pm.appList

    fun loadLabel(app: PackageInfo?) = with(pm) { app?.label() ?: "Not installed" }

    fun loadSelectedFile(uri: Uri) =
        app.contentResolver.openInputStream(uri)?.use { stream ->
            with(inputFile) {
                delete()
                Files.copy(stream, toPath())

                pm.getPackageInfo(this)?.let { packageInfo ->
                    SelectedApp.Local(
                        packageName = packageInfo.packageName,
                        version = packageInfo.versionName,
                        file = this,
                        temporary = true
                    )
                }
            }
        }
}