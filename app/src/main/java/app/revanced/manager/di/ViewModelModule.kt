package app.revanced.manager.di

import app.revanced.manager.ui.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::SelectedAppInfoViewModel)
    viewModelOf(::PatchesSelectorViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AdvancedSettingsViewModel)
    viewModelOf(::AppSelectorViewModel)
    viewModelOf(::VersionSelectorViewModel)
    viewModelOf(::PatcherViewModel)
    viewModelOf(::UpdateViewModel)
    viewModelOf(::ChangelogsViewModel)
    viewModelOf(::ImportExportViewModel)
    viewModelOf(::ContributorViewModel)
    viewModelOf(::DownloadsViewModel)
    viewModelOf(::InstalledAppsViewModel)
    viewModelOf(::InstalledAppInfoViewModel)
    viewModelOf(::UpdatesSettingsViewModel)
}
