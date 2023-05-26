package app.revanced.manager.compose.di

import app.revanced.manager.compose.ui.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        PatchesSelectorViewModel(
            packageInfo = it.get(),
            bundleRepository = get()
        )
    }
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AppSelectorViewModel)
    viewModelOf(::SourcesScreenViewModel)
    viewModel {
        InstallerScreenViewModel(
            input = it.get(),
            selectedPatches = it.get(),
            app = get(),
            signerService = get(),
        )
    }
    viewModelOf(::UpdateSettingsViewModel)
}
