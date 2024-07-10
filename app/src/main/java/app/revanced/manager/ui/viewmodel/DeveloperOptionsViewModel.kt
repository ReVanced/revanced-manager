package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch

class DeveloperOptionsViewModel(
    val prefs: PreferencesManager,
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository
) : ViewModel() {
    fun redownloadBundles() = viewModelScope.launch {
        uiSafe(app, R.string.source_download_fail, RemotePatchBundle.updateFailMsg) {
            patchBundleRepository.redownloadRemoteBundles()
        }
    }

    fun resetBundles() = viewModelScope.launch {
        patchBundleRepository.reset()
    }
}