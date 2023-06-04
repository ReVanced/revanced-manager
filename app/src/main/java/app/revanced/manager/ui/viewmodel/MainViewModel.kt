package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.domain.repository.SourceRepository
import app.revanced.manager.util.PM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    sourceRepository: SourceRepository,
    pm: PM
) : ViewModel() {
    init {
        with(viewModelScope) {
            launch {
                sourceRepository.loadSources()
            }
            launch {
                pm.getCompatibleApps()
            }
            launch(Dispatchers.IO) {
                pm.getInstalledApps()
            }
        }
    }
}