package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.manager.PreferencesManager
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.sources.Extensions.asRemoteOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class BundleInformationViewModel(uid: Int) : ViewModel(), KoinComponent {
    private val patchBundleRepository: PatchBundleRepository = get()
    val networkInfo: NetworkInfo = get()
    val prefs: PreferencesManager = get()

    var bundle = patchBundleRepository.sources.map { sources -> sources.find { it.uid == uid } }
    var patchCount = patchBundleRepository.patchCountsFlow.map { it[uid] ?: 0 }

    fun delete() = viewModelScope.launch {
        bundle.first()?.let { patchBundleRepository.remove(it) }
    }

    fun refresh() = viewModelScope.launch {
        bundle.first()?.asRemoteOrNull?.let {
            patchBundleRepository.update(it, showToast = true, force = true)
        }
    }

    fun setAutoUpdate(value: Boolean) = viewModelScope.launch {
        bundle.first()?.asRemoteOrNull?.let {
            patchBundleRepository.run { it.setAutoUpdate(value) }
        }
    }

    fun updateUsePrereleases(value: Boolean) = viewModelScope.launch {
        prefs.usePatchesPrereleases.update(value)
        refresh()
    }
}
