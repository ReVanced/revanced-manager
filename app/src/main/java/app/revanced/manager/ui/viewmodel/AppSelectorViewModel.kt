package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.ui.model.SelectedApp
import app.revanced.manager.util.PM
import app.revanced.manager.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

class AppSelectorViewModel(
    private val app: Application,
    private val pm: PM,
    private val patchBundleRepository: PatchBundleRepository
) : ViewModel() {
    private val inputFile = File(app.filesDir, "input.apk").also {
        it.delete()
    }
    val appList = pm.appList

    private val storageSelectionChannel = Channel<SelectedApp.Local>()
    val storageSelectionFlow = storageSelectionChannel.receiveAsFlow()

    val suggestedAppVersions = patchBundleRepository.suggestedVersions.flowOn(Dispatchers.Default)

    var nonSuggestedVersionDialogSubject by mutableStateOf<SelectedApp.Local?>(null)
        private set

    fun loadLabel(app: PackageInfo?) = with(pm) { app?.label() ?: "Not installed" }

    fun dismissNonSuggestedVersionDialog() {
        nonSuggestedVersionDialogSubject = null
    }

    fun handleStorageResult(uri: Uri) = viewModelScope.launch {
        val selectedApp = withContext(Dispatchers.IO) {
            loadSelectedFile(uri)
        }

        if (selectedApp == null) {
            app.toast(app.getString(R.string.failed_to_load_apk))
            return@launch
        }

        if (patchBundleRepository.isVersionAllowed(selectedApp.packageName, selectedApp.version)) {
            storageSelectionChannel.send(selectedApp)
        } else {
            nonSuggestedVersionDialogSubject = selectedApp
        }
    }

    private fun loadSelectedFile(uri: Uri) =
        app.contentResolver.openInputStream(uri)?.use { stream ->
            with(inputFile) {
                delete()
                Files.copy(stream, toPath())

                pm.getPackageInfo(this)?.let { packageInfo ->
                    SelectedApp.Local(
                        packageName = packageInfo.packageName,
                        version = packageInfo.versionName!!,
                        file = this,
                        temporary = true
                    )
                }
            }
        }
}