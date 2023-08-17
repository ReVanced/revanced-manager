package app.revanced.manager.di

import app.revanced.manager.ui.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::PatchesSelectorViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AdvancedSettingsViewModel)
    viewModelOf(::AppSelectorViewModel)
    viewModelOf(::VersionSelectorViewModel)
    viewModelOf(::BundlesViewModel)
    viewModelOf(::InstallerViewModel)
    viewModelOf(::UpdateProgressViewModel)
    viewModelOf(::ManagerUpdateChangelogViewModel)
    viewModelOf(::ImportExportViewModel)
    viewModelOf(::ContributorViewModel)
    viewModelOf(::DownloadsViewModel)
    viewModelOf(::InstalledAppsViewModel)
    viewModelOf(::AppInfoViewModel)
}
