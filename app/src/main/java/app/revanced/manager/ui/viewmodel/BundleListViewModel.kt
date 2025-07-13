package app.revanced.manager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.bundles.PatchBundleSource
import app.revanced.manager.domain.bundles.RemotePatchBundle
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.util.mutableStateSetOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class BundleListViewModel : ViewModel(), KoinComponent {
    private val app: Application = get()
    private val patchBundleRepository: PatchBundleRepository = get()
    val patchCounts = patchBundleRepository.patchCountsFlow
    val sources = combine(
        patchBundleRepository.sources,
        patchBundleRepository.patchCountsFlow
    ) { sources, patchCounts ->
        sources.sortedByDescending { patchCounts[it.uid] }
    }
    val selectedSources = mutableStateSetOf<Int>()

    private suspend fun getSelectedSources() = patchBundleRepository.sources
        .first()
        .filter { it.uid in selectedSources }
        .also {
            selectedSources.clear()
        }

    fun handleEvent(event: Event) {
        when (event) {
            Event.CANCEL -> selectedSources.clear()
            Event.DELETE_SELECTED -> viewModelScope.launch {
                patchBundleRepository.remove(*getSelectedSources().toTypedArray())
            }

            Event.UPDATE_SELECTED -> viewModelScope.launch {
                patchBundleRepository.update(
                    *getSelectedSources().filterIsInstance<RemotePatchBundle>().toTypedArray()
                )
            }
        }
    }

    fun delete(src: PatchBundleSource) =
        viewModelScope.launch { patchBundleRepository.remove(src) }

    fun update(src: PatchBundleSource) = viewModelScope.launch {
        if (src !is RemotePatchBundle) return@launch

        patchBundleRepository.update(src, showToast = true)
    }

    enum class Event {
        DELETE_SELECTED,
        UPDATE_SELECTED,
        CANCEL,
    }
}