package app.revanced.manager.di

import app.revanced.manager.data.platform.FileSystem
import app.revanced.manager.domain.repository.*
import app.revanced.manager.network.api.ManagerAPI
import app.revanced.manager.domain.worker.WorkerRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::ReVancedRepository)
    singleOf(::GithubRepository)
    singleOf(::ManagerAPI)
    singleOf(::FileSystem)
    singleOf(::SourcePersistenceRepository)
    singleOf(::PatchSelectionRepository)
    singleOf(::SourceRepository)
    singleOf(::WorkerRepository)
}