package app.revanced.manager.di

import app.revanced.manager.domain.repository.PatchSelectionRepository
import app.revanced.manager.domain.repository.ReVancedRepository
import app.revanced.manager.network.api.ManagerAPI
import app.revanced.manager.domain.repository.SourcePersistenceRepository
import app.revanced.manager.domain.repository.SourceRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::ReVancedRepository)
    singleOf(::ManagerAPI)
    singleOf(::SourcePersistenceRepository)
    singleOf(::PatchSelectionRepository)
    singleOf(::SourceRepository)
}