package app.revanced.manager.di

import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.repository.AnnouncementRepository
import app.revanced.manager.domain.repository.ChangelogsRepository
import app.revanced.manager.domain.repository.DownloadedAppRepository
import app.revanced.manager.domain.repository.DownloaderRepository
import app.revanced.manager.domain.repository.InstalledAppRepository
import app.revanced.manager.domain.repository.ManagerUpdateRepository
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.domain.repository.PatchOptionsRepository
import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.network.api.ReVancedAPI
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::ReVancedAPI)
    singleOf(::ManagerUpdateRepository)
    singleOf(::AnnouncementRepository)
    singleOf(::Filesystem) {
        createdAtStart()
    }
    singleOf(::NetworkInfo)
    singleOf(::PatchSelectionRepository)
    singleOf(::PatchOptionsRepository)
    singleOf(::PatchBundleRepository) {
        // It is best to load patch bundles ASAP
        createdAtStart()
    }
    singleOf(::DownloaderRepository)
    singleOf(::WorkerRepository)
    singleOf(::DownloadedAppRepository)
    singleOf(::InstalledAppRepository)
    singleOf(::ChangelogsRepository)
}