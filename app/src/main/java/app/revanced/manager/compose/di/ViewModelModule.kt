package app.revanced.manager.compose.di

import app.revanced.manager.compose.ui.viewmodel.AppSelectorViewModel
import app.revanced.manager.compose.ui.viewmodel.InstallerScreenViewModel
import app.revanced.manager.compose.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.compose.ui.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        PatchesSelectorViewModel(
            packageInfo = it.get(),
            patchesRepository = get()
        )
    }
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AppSelectorViewModel)
    viewModel {
        InstallerScreenViewModel(
            input = it.get(),
            selectedPatches = it.get(),
            app = get()
        )
    }
}
