package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.repository.SourceRepository
import kotlinx.coroutines.launch

class MainViewModel(
    sourceRepository: SourceRepository
) : ViewModel() {
    init {
        with(viewModelScope) {
            launch {
                sourceRepository.loadSources()
            }
        }
    }
}