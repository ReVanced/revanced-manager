package app.revanced.manager.di

import app.revanced.manager.ui.viewmodel.AboutViewModel
import app.revanced.manager.ui.viewmodel.AdvancedSettingsViewModel
import app.revanced.manager.ui.viewmodel.AppInfoViewModel
import app.revanced.manager.ui.viewmodel.AppSelectorViewModel
import app.revanced.manager.ui.viewmodel.ContributorViewModel
import app.revanced.manager.ui.viewmodel.DashboardViewModel
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import app.revanced.manager.ui.viewmodel.ImportExportViewModel
import app.revanced.manager.ui.viewmodel.InstalledAppsViewModel
import app.revanced.manager.ui.viewmodel.InstallerViewModel
import app.revanced.manager.ui.viewmodel.MainViewModel
import app.revanced.manager.ui.viewmodel.ManagerUpdateChangelogViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.SettingsViewModel
import app.revanced.manager.ui.viewmodel.UpdateProgressViewModel
import app.revanced.manager.ui.viewmodel.VersionSelectorViewModel
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
    viewModelOf(::InstallerViewModel)
    viewModelOf(::UpdateProgressViewModel)
    viewModelOf(::ManagerUpdateChangelogViewModel)
    viewModelOf(::ImportExportViewModel)
    viewModelOf(::AboutViewModel)
    viewModelOf(::ContributorViewModel)
    viewModelOf(::DownloadsViewModel)
    viewModelOf(::InstalledAppsViewModel)
    viewModelOf(::AppInfoViewModel)
}
