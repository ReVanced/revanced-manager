package app.revanced.manager.ui.viewmodel


import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import app.revanced.manager.R
import app.revanced.manager.domain.manager.KeystoreManager
import app.revanced.manager.util.toast

class ImportExportViewModel(private val app: Application, private val keystoreManager: KeystoreManager) : ViewModel() {
    private val contentResolver = app.contentResolver

    fun import(content: Uri, cn: String, pass: String) =
        keystoreManager.import(cn, pass, contentResolver.openInputStream(content)!!)

    fun export(target: Uri) = keystoreManager.export(contentResolver.openOutputStream(target)!!)

    fun regenerate() = keystoreManager.regenerate().also {
        app.toast(app.getString(R.string.regenerate_keystore_success))
    }
}