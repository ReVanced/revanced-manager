package app.revanced.manager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private val patchBundleRepository: PatchBundleRepository = get()
    val patchCounts = patchBundleRepository.patchCountsFlow
    var isRefreshing by mutableStateOf(false)
        private set

    val sources = combine(
        patchBundleRepository.sources,
        patchBundleRepository.patchCountsFlow
    ) { sources, patchCounts ->
        isRefreshing = false
        sources.sortedByDescending { patchCounts[it.uid] ?: 0 }
    }

    val selectedSources = mutableStateSetOf<Int>()

    fun refresh() = viewModelScope.launch {
        isRefreshing = true
        patchBundleRepository.reload()
    }

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
                    *getSelectedSources().filterIsInstance<RemotePatchBundle>().toTypedArray(),
                    showToast = true,
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