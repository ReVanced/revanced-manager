package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val prefs: PreferencesManager = get()

    var bundle = patchBundleRepository.sources.map { sources -> sources.find { it.uid == uid } }
    var patchCount = patchBundleRepository.patchCountsFlow.map { it[uid] ?: 0 }

    fun delete() = viewModelScope.launch {
        bundle.first()?.let { patchBundleRepository.remove(it) }
    }

    fun refresh() = viewModelScope.launch {
        bundle.first()?.asRemoteOrNull?.let {
            patchBundleRepository.update(it, showToast = true)
        }
    }

    fun setAutoUpdate(value: Boolean) = viewModelScope.launch {
        bundle.first()?.asRemoteOrNull?.let {
            patchBundleRepository.run { it.setAutoUpdate(value) }
        }
    }

    fun setEndpoint(value: String) = viewModelScope.launch {
        val endpoint = value.trim()
        bundle.first()?.asRemoteOrNull?.let { current ->
            if (current.endpoint == endpoint) return@launch

            patchBundleRepository.run { current.setEndpoint(endpoint) }
            bundle.first()?.asRemoteOrNull?.let { updated ->
                patchBundleRepository.update(updated, showToast = true)
            }
        }
    }

    suspend fun validateEndpoint(value: String) = patchBundleRepository.validateRemoteUrl(value.trim())

    fun updateUsePrereleases(value: Boolean) = viewModelScope.launch {
        prefs.usePatchesPrereleases.update(value)
        refresh()
    }
}
