package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.R
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.util.uiSafe
import kotlinx.coroutines.launch

class BundlesViewModel(
    private val app: Application,
    private val patchBundleRepository: PatchBundleRepository
) : ViewModel() {
    val sources = patchBundleRepository.sources

    fun delete(bundle: PatchBundleSource) =
        viewModelScope.launch { patchBundleRepository.remove(bundle) }

    fun update(bundle: PatchBundleSource) = viewModelScope.launch {
        if (bundle !is RemotePatchBundle) return@launch

        uiSafe(
            app,
            R.string.source_download_fail,
            RemotePatchBundle.updateFailMsg
        ) {
            bundle.update()
        }
    }
}