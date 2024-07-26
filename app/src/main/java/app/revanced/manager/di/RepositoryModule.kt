package app.revanced.manager.di

import app.revanced.manager.data.platform.Filesystem
import app.revanced.manager.data.platform.NetworkInfo
import app.revanced.manager.domain.repository.*
import app.revanced.manager.domain.worker.WorkerRepository
import app.revanced.manager.network.api.ReVancedAPI
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::ReVancedAPI)
    singleOf(::Filesystem) {
        createdAtStart()
    }
    singleOf(::NetworkInfo)
    singleOf(::PatchBundlePersistenceRepository)
    singleOf(::PatchSelectionRepository)
    singleOf(::PatchOptionsRepository)
    singleOf(::PatchBundleRepository) {
        // It is best to load patch bundles ASAP
        createdAtStart()
    }
    singleOf(::DownloaderPluginRepository)
    singleOf(::WorkerRepository)
    singleOf(::DownloadedAppRepository)
    singleOf(::InstalledAppRepository)
}