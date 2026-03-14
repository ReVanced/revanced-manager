package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchBundleRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class BundleInformationViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel(), KoinComponent {
    private val patchBundleRepository: PatchBundleRepository = get()
    val networkInfo: NetworkInfo = get()
    val prefs: PreferencesManager = get()

    private val bundleUid = savedStateHandle.get<Int>("uid")!!

    var bundle by mutableStateOf<PatchBundleSource?>(null)
        private set

    var patchCount by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            patchBundleRepository.sources.collectLatest { sources ->
                bundle = sources.find { it.uid == bundleUid }
            }
        }

        viewModelScope.launch {
            patchBundleRepository.patchCountsFlow.collectLatest { counts ->
                patchCount = counts[bundleUid] ?: 0
            }
        }
    }

    fun delete() = viewModelScope.launch {
        bundle?.let { patchBundleRepository.remove(it) }
    }

    fun refresh() = viewModelScope.launch {
        (bundle as? RemotePatchBundle)?.let {
            patchBundleRepository.update(it, showToast = true, force = true)
        }
    }

    fun setAutoUpdate(value: Boolean) = viewModelScope.launch {
        (bundle as? RemotePatchBundle)?.let {
            patchBundleRepository.run { it.setAutoUpdate(value) }
        }
    }

    fun updateUsePrereleases(value: Boolean) = viewModelScope.launch {
        prefs.usePatchesPrereleases.update(value)
        refresh()
    }
}
