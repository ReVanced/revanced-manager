package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.sources.RemotePatchBundle
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.util.toast
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeveloperOptionsViewModel(
    val prefs: PreferencesManager,
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository,
    private val downloaderRepository: DownloaderRepository
) : ViewModel() {
    fun redownloadBundles() = viewModelScope.launch {
        uiSafe(app, R.string.patches_download_fail, RemotePatchBundle.updateFailMsg) {
            patchBundleRepository.redownloadRemote()
        }
    }

    fun setApiUrl(value: String) = viewModelScope.launch(Dispatchers.Default) {
        if (value == prefs.api.get()) return@launch

        prefs.api.update(value)

        arrayOf(patchBundleRepository, downloaderRepository).forEach {
            it.reloadApiSources()
            it.updateCheck()
        }
    }

    fun resetBundles() = viewModelScope.launch {
        patchBundleRepository.reset()
    }

    fun resetDownloaders() = viewModelScope.launch {
        downloaderRepository.reset()
    }

    fun resetOnboarding() = viewModelScope.launch {
        prefs.completedOnboarding.update(false)
        app.toast(app.getString(R.string.sideeffect_restart))
    }

    fun resetAnnouncement() = viewModelScope.launch {
        prefs.readAnnouncements.update(emptySet())
        app.toast(app.getString(R.string.sideeffect_restart))
    }
}