package app.revanced.manager.di

import app.revanced.manager.ui.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::PatchesSelectorViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AppSelectorViewModel)
    viewModelOf(::SourcesViewModel)
    viewModelOf(::InstallerViewModel)
    viewModelOf(::UpdateSettingsViewModel)
    viewModelOf(::ImportExportViewModel)
    viewModelOf(::ContributorViewModel)
}
