package app.revanced.manager.di

import app.revanced.manager.ui.viewmodel.AboutViewModel
import app.revanced.manager.ui.viewmodel.AdvancedSettingsViewModel
import app.revanced.manager.ui.viewmodel.AnnouncementsViewModel
import app.revanced.manager.ui.viewmodel.AppsViewModel
import app.revanced.manager.ui.viewmodel.BundleInformationViewModel
import app.revanced.manager.ui.viewmodel.BundleListViewModel
import app.revanced.manager.ui.viewmodel.ChangelogsViewModel
import app.revanced.manager.ui.viewmodel.ContributorViewModel
import app.revanced.manager.ui.viewmodel.DashboardViewModel
import app.revanced.manager.ui.viewmodel.DeveloperOptionsViewModel
import app.revanced.manager.ui.viewmodel.DownloadsViewModel
import app.revanced.manager.ui.viewmodel.GeneralSettingsViewModel
import app.revanced.manager.ui.viewmodel.ImportExportViewModel
import app.revanced.manager.ui.viewmodel.InstalledAppInfoViewModel
import app.revanced.manager.ui.viewmodel.MainViewModel
import app.revanced.manager.ui.viewmodel.OnboardingViewModel
import app.revanced.manager.ui.viewmodel.PatcherViewModel
import app.revanced.manager.ui.viewmodel.PatchesSelectorViewModel
import app.revanced.manager.ui.viewmodel.SelectedAppInfoViewModel
import app.revanced.manager.ui.viewmodel.UpdateViewModel
import app.revanced.manager.ui.viewmodel.UpdatesSettingsViewModel
import org.koin.core.module.dsl.viewModelOf
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
