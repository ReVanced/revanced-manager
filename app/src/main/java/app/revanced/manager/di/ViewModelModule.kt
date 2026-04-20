package app.revanced.manager.di

import app.revanced.manager.ui.viewmodel.*
import org.koin.core.module.dsl.*
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::SelectedAppInfoViewModel)
    viewModelOf(::PatchesSelectorViewModel)
    viewModelOf(::GeneralSettingsViewModel)
    viewModelOf(::AdvancedSettingsViewModel)
    viewModelOf(::PatcherViewModel)
    viewModelOf(::UpdateViewModel)
    viewModelOf(::AnnouncementsViewModel)
    viewModelOf(::ChangelogsViewModel)
    viewModelOf(::ImportExportViewModel)
    viewModelOf(::AboutViewModel)
    viewModelOf(::DeveloperOptionsViewModel)
    viewModelOf(::ContributorViewModel)
    viewModelOf(::DownloadsViewModel)
    viewModelOf(::AppsViewModel)
    viewModelOf(::InstalledAppInfoViewModel)
    viewModelOf(::UpdatesSettingsViewModel)
    viewModelOf(::BundleListViewModel)
    viewModelOf(::BundleInformationViewModel)
}
