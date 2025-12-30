package app.revanced.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import app.revanced.manager.ui.model.navigation.SelectedAppInfo
import org.koin.core.component.KoinComponent

class SourceSelectorViewModel(
    val input: SelectedAppInfo.SourceSelector.ViewModelParams
) : ViewModel(), KoinComponent {

}